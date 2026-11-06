package com.shoplite.performance;

import com.shoplite.catalog.AdminCatalogService;
import com.shoplite.catalog.Category;
import com.shoplite.catalog.CategoryRepository;
import com.shoplite.catalog.ProductForm;
import com.shoplite.catalog.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Load test for NFR-01: the catalog is filled up to 1000 products, then 50 virtual users browse it
 * at the same time over real HTTP (catalog page, product list, search, category filter with sorting,
 * product details).
 * 95 % of the responses must be faster than 2 seconds and no request may fail.
 * <p>
 * The load is generated inside the test JVM with {@link HttpClient}, so no external tool competes
 * with the application for the processor. The summary is written to {@code target/nfr-01-load-test.txt}.
 * Run only this test with {@code ./mvnw test -Dgroups=performance}.
 */
@Tag("performance")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CatalogLoadTest {

    private static final int PRODUCTS = 1000;
    private static final int USERS = 50;
    private static final int ROUNDS = 10;
    private static final long LIMIT_MS = 2_000;
    private static final List<String> PATHS = List.of(
            "/products",
            "/api/products",
            "/api/products?q=kettle",
            "/api/products?category=1&sort=price_desc",
            "/api/products/1");

    @Value("${local.server.port}")
    private int port;
    @Autowired
    private AdminCatalogService adminCatalog;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    private final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private int get(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    /** Adds products spread over the existing categories until the catalog has {@value #PRODUCTS} of them. */
    private void fillCatalog() {
        List<Category> categories = categoryRepository.findAllByOrderByNameAsc();
        long missing = PRODUCTS - productRepository.count();
        for (int i = 0; i < missing; i++) {
            ProductForm form = new ProductForm();
            form.setName(String.format("Load test product %04d", i));
            form.setPrice(BigDecimal.valueOf(10 + i % 990));
            form.setStock(100);
            form.setCategoryId(categories.get(i % categories.size()).getId());
            adminCatalog.createProduct(form);
        }
    }

    @Test
    @DisplayName("NFR-01: 50 users browse a catalog of 1000 products, 95 % of responses are faster than 2 s")
    void fiftyUsersBrowseTheCatalog() throws Exception {
        fillCatalog();
        long catalogSize = productRepository.count();

        // warm-up: the first requests load classes and compile templates (see the cold start note in docs/testing.md)
        for (int i = 0; i < 5; i++) {
            for (String path : PATHS) {
                assertEquals(200, get(path), path);
            }
        }

        List<Long> times = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger errors = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(USERS);
        List<Future<?>> users = new ArrayList<>();
        for (int user = 0; user < USERS; user++) {
            users.add(pool.submit(() -> {
                start.await();
                for (int round = 0; round < ROUNDS; round++) {
                    for (String path : PATHS) {
                        long begin = System.nanoTime();
                        int status;
                        try {
                            status = get(path);
                        } catch (IOException ex) {
                            status = -1;
                        }
                        times.add(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - begin));
                        if (status != 200) {
                            errors.incrementAndGet();
                        }
                    }
                }
                return null;
            }));
        }

        long begin = System.nanoTime();
        start.countDown();
        for (Future<?> user : users) {
            user.get(5, TimeUnit.MINUTES);
        }
        double seconds = (System.nanoTime() - begin) / 1e9;
        pool.shutdown();

        List<Long> sorted = new ArrayList<>(times);
        Collections.sort(sorted);
        long p50 = percentile(sorted, 50);
        long p95 = percentile(sorted, 95);
        long max = sorted.get(sorted.size() - 1);
        double average = sorted.stream().mapToLong(Long::longValue).average().orElse(0);
        long slow = sorted.stream().filter(time -> time >= LIMIT_MS).count();
        String summary = String.format(Locale.ROOT,
                "NFR-01 load test: catalog of %d products, %d users x %d rounds x %d requests = %d requests"
                        + " in %.1f s (%.0f req/s)%n"
                        + "average %.0f ms, median %d ms, 95th percentile %d ms, max %d ms, 2 s or slower %d, errors %d%n",
                catalogSize, USERS, ROUNDS, PATHS.size(), sorted.size(), seconds, sorted.size() / seconds,
                average, p50, p95, max, slow, errors.get());
        System.out.print(summary);
        Files.writeString(Path.of("target", "nfr-01-load-test.txt"), summary);

        assertEquals(USERS * ROUNDS * PATHS.size(), sorted.size());
        assertEquals(0, errors.get(), "no request may fail");
        assertTrue(p95 < LIMIT_MS, "95th percentile " + p95 + " ms must be under " + LIMIT_MS + " ms");
    }

    private static long percentile(List<Long> sorted, int percent) {
        int index = (int) Math.ceil(percent / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(index, 0));
    }
}
