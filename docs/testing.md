# Testing

This document describes how ShopLite is tested, what the latest test run showed, which defects
the tests found and how the requirements from the specification (FR-01 – FR-15, NFR-01 – NFR-08) were checked.

## 1. Strategy

The tests follow the test pyramid: many fast unit tests, fewer component tests on a real database,
a few end-to-end system tests and one load test.

| Level | What is checked | How | Tests |
|---|---|---|---|
| Unit | business rules of every service class and domain object in isolation | JUnit Jupiter 6 + Mockito, repositories replaced with mocks, no Spring context | 65 |
| Component | modules working together: services + JPA + Flyway schema on H2, REST API, Spring Security rules, form validation, transactions | `@SpringBootTest` + MockMvc with the real security filter chain | 33 |
| System | complete user journeys over HTTP, as a browser does them: forms, session cookie, CSRF tokens, redirects | MockMvc with `formLogin()`, one session per user, nothing mocked | 5 |
| Load | response time with 50 concurrent users on a catalog of 1000 products (NFR-01) | embedded Tomcat on a random port, `java.net.http.HttpClient` with 50 threads | 1 |
| API regression (manual run) | the deployed application in Docker with PostgreSQL | Postman collection `postman/ShopLite.postman_collection.json`, 17 automated checks | — |

Rules used in the tests:

* Every component and system test creates its own users, categories and products with unique names,
  so tests do not depend on each other or on the order in which they run.
* Most component tests are `@Transactional` and roll back. Tests of order placement and the system
  tests commit, because commit and rollback are exactly what they check.
* A defect found by a test is first committed as a failing test (red), then fixed (green) in a separate commit.

## 2. How to run

```bash
./mvnw test                          # all tests, about 1 minute
./mvnw verify                        # tests + JaCoCo coverage check (NFR-07)
./mvnw test -Dgroups=performance     # only the NFR-01 load test
./mvnw test -Dtest='*SystemTest'     # only the system tests
```

IntelliJ IDEA run configurations: **ShopLite tests** (`clean test`) and **ShopLite load test**.

Reports:

* `target/surefire-reports/` — result of every test class;
* `target/site/jacoco/index.html` — coverage report;
* `target/nfr-01-load-test.txt` — summary of the load test.

## 3. Results

Run of 6 November 2026, `./mvnw clean verify` on Java 24: **104 tests, 0 failures, 0 errors, 0 skipped**,
build time about 1 minute 10 seconds.

| Level | Test class | Tests |
|---|---|---|
| Unit | `CartServiceImplTest` | 11 |
| Unit | `CartTest` | 4 |
| Unit | `CatalogServiceImplTest` | 7 |
| Unit | `AdminCatalogServiceImplTest` | 14 |
| Unit | `ProductTest` | 2 |
| Unit | `OrderServiceImplTest` | 15 |
| Unit | `OrderStatusTest` | 2 |
| Unit | `StatsServiceImplTest` | 2 |
| Unit | `UserServiceImplTest` | 6 |
| Unit | `ShopUserDetailsServiceTest` | 2 |
| Component | `ShopLiteApplicationTests` | 1 |
| Component | `ProductApiTest` | 8 |
| Component | `OrderApiTest` | 5 |
| Component | `WebSecurityTest` | 10 |
| Component | `CatalogSearchIntegrationTest` | 5 |
| Component | `OrderPlacementIntegrationTest` | 4 |
| System | `CustomerJourneySystemTest` | 3 |
| System | `AdminJourneySystemTest` | 2 |
| Load | `CatalogLoadTest` | 1 |
| **Total** | | **104** |

### Load test (NFR-01)

Catalog of 1000 products, 50 users, each user makes 10 rounds of 5 requests
(catalog page, product list, search, category filter with price sorting, product details) — 2500 requests.

| Run | Requests per second | Average | Median | 95th percentile | Max | ≥ 2 s | Errors |
|---|---|---|---|---|---|---|---|
| `ShopLite load test` only | 199 | 240 ms | 203 ms | 582 ms | 1090 ms | 0 | 0 |
| inside `clean verify` | 207 | 228 ms | 182 ms | 538 ms | 1707 ms | 0 | 0 |

The requirement "response time ≤ 2 s with 50 concurrent users" is met: 95 % of the responses are
3–4 times faster than the limit and no request reached 2 seconds. The load is generated inside the test
JVM, so it does not take the processor away from the application the way the Postman runner did in Lab 5.
The application, the database (H2) and the load generator still share one laptop, so the numbers are a
lower bound; see the recommendations.

## 4. Coverage (JaCoCo)

| Metric | After unit tests only | After all tests |
|---|---|---|
| Lines | 68.2 % (456 / 669) | **91.2 % (612 / 671)** |
| Branches | 62.2 % | **85.0 % (85 / 100)** |
| Instructions | — | 91.3 % |
| Methods | — | 92.9 % |

Every `*ServiceImpl` class has 100 % line coverage; the build fails if any of them drops below 70 % (`check-service-coverage` rule in `pom.xml`).
The lowest coverage is in the admin controllers (`AdminCategoryController` 52 %, `AdminProductController` 67 %),
`CartController` (63 %) and `CheckoutController` (68 %): the uncovered lines are the error branches that turn a
`BusinessException` into a flash message.

## 5. Defects found by the tests

| Jira | Found by | Problem | Impact | Fix |
|---|---|---|---|---|
| SCRUM-37 | unit test `CartServiceImplTest.hugeQuantityIsRejected` | adding `Integer.MAX_VALUE` items overflowed `int` in the stock check, so the check passed | negative quantity and total in the cart, error 500 at checkout (medium) | compare the request with the remaining stock instead of adding the numbers; `Math.addExact` in `Cart.addItem` |
| SCRUM-38 | component test `OrderPlacementIntegrationTest.lastItemCannotBeSoldTwice` (two threads) | two customers ordering the last item at the same moment both succeeded. The cart had already loaded the product, so `SELECT ... FOR UPDATE` returned Hibernate's cached copy with an out-of-date stock value (lost update) | the last item is sold twice; one order has to be cancelled by hand, stock reports are wrong (high) | `EntityManager.refresh(product, PESSIMISTIC_WRITE)` locks the row and reads it again before the stock check |
| SCRUM-39 | component test `CatalogSearchIntegrationTest.wildcardCharactersAreMatchedLiterally` | `%` and `_` typed in the search box worked as `LIKE` wildcards | `%` returned the whole catalog, `_` matched any character (low; no SQL injection, the value is a bind parameter) | escape `\`, `%`, `_` in `CatalogServiceImpl` and declare `escape '\'` in the query |

SCRUM-38 is the most important result of this round: unit tests with mocks cannot show it, because it only
appears with a real database and two transactions.

Findings about the test setup (no change to the application):

* Spring Security chooses the response by the `Accept` header: a browser (`text/html`) is redirected to `/login`,
  a client without it gets `401`. Logout behaves the same way (`302` for a browser, `204` for other clients).
  The page tests send `Accept: text/html`, like a browser.
* Spring Security 7 redirects to the relative URL `/login`, not to `http://localhost/login`.

Defects found earlier by integration in Lab 5 (already fixed): order time two hours off in Docker
(JVM in UTC, fixed with `TZ=Europe/Kyiv`), `LazyInitializationException` on the cart and order pages
(fixed with `@EntityGraph`).

## 6. Requirements validation

### Functional

| Requirement | Checked by | Result |
|---|---|---|
| FR-01 Registration | `UserServiceImplTest`, `WebSecurityTest.registrationValidation`, `duplicateEmailIsRejected`, `CustomerJourneySystemTest` | passed |
| FR-02 Log in / log out | `ShopUserDetailsServiceTest`, `CustomerJourneySystemTest.wrongPasswordIsRejected`, logout step of `customerBuysAndCancels` | passed |
| FR-03 Catalog, 12 per page | `CatalogServiceImplTest`, `CatalogSearchIntegrationTest.catalogIsPagedBy12`, `ProductApiTest.listReturnsFirstPage` | passed |
| FR-04 Search, filter, sorting | `CatalogSearchIntegrationTest`, `ProductApiTest.searchByName`, `categoryFilterAndSorting` | passed after SCRUM-39 |
| FR-05 Product page | `ProductApiTest.productDetails`, `outOfStockProductIsShownButMarked`, `WebSecurityTest.catalogIsPublic` | passed |
| FR-06 Cart | `CartServiceImplTest`, `CartTest`, `CustomerJourneySystemTest` | passed after SCRUM-37 |
| FR-07 Checkout | `OrderServiceImplTest`, `WebSecurityTest.checkoutValidation`, `CustomerJourneySystemTest` | passed |
| FR-08 Stock control | `OrderPlacementIntegrationTest`, `ProductTest`, `CustomerJourneySystemTest` | passed after SCRUM-38 |
| FR-09 Order history | `OrderServiceImplTest.getForUserHidesOtherCustomersOrders`, `OrderApiTest.anotherCustomerGets404`, `CustomerJourneySystemTest` | passed |
| FR-10 Cancel a new order | `OrderServiceImplTest`, `OrderPlacementIntegrationTest.cancellationIsPersisted`, `CustomerJourneySystemTest` | passed |
| FR-11 Profile | `UserServiceImplTest`, `CustomerJourneySystemTest.customerChangesProfileAndPassword` | passed |
| FR-12 Manage products | `AdminCatalogServiceImplTest`, `WebSecurityTest.productFormValidation`, `AdminJourneySystemTest.adminManagesCatalog` | passed |
| FR-13 Manage categories | `AdminCatalogServiceImplTest`, `AdminJourneySystemTest.adminManagesCatalog` | passed |
| FR-14 Process orders | `OrderServiceImplTest`, `OrderStatusTest`, `OrderApiTest`, `AdminJourneySystemTest.adminProcessesOrderAndSeesSales` | passed |
| FR-15 Monthly sales | `StatsServiceImplTest`, `AdminJourneySystemTest.adminProcessesOrderAndSeesSales` | passed |

### Non-functional

| Requirement | Checked by | Result |
|---|---|---|
| NFR-01 Response time ≤ 2 s, 50 users | `CatalogLoadTest`: P95 538–582 ms, max 1.7 s, 0 errors | met |
| NFR-02 Password hashing | `UserServiceImplTest` (BCrypt hash stored, raw password never stored) | met |
| NFR-03 Access control | `WebSecurityTest`, `OrderApiTest` (401, 403, 404 for other customers' orders, CSRF) | met |
| NFR-04 Reliable checkout | `OrderPlacementIntegrationTest` (full rollback, two parallel orders) | met after SCRUM-38 |
| NFR-05 Input validation | `WebSecurityTest` (form errors), `ProductApiTest` (JSON 400 / 404) | met |
| NFR-06 Responsive UI | manual check at 360–1920 px (Lab 5) | met, not automated |
| NFR-07 Test coverage | JaCoCo: 91.2 % lines, every service ≥ 70 % enforced by the build | met |
| NFR-08 One-command deployment | `docker compose up --build`, Postman collection run (Lab 5) | met |

## 7. Recommendations

Testing:

* Run `./mvnw verify` on every pull request with GitHub Actions, so the enforcer rules, all tests and the coverage gate block a bad merge.
* Run the component tests on PostgreSQL with Testcontainers: H2 in PostgreSQL mode is close, but locking and `LIKE` behaviour can differ.
* Repeat the load test against the Docker deployment with PostgreSQL from a separate machine (k6 or Gatling), and add a checkout scenario.
* Add browser tests (Selenium or Playwright) for the Thymeleaf pages, and tests for the error branches of the admin controllers.
* Configure Mockito as a Java agent in the Surefire `argLine`: the current self-attaching will stop working in future JDKs (warning in the build log).

Optimization and extension:

* Lock products in a fixed order (by id) when an order is placed, so two carts with the same products cannot deadlock.
* Add `id` as a second sort key for price sorting: products with the same price can otherwise move between pages.
* Use a trigram index (`pg_trgm`) for name search: `LIKE '%text%'` cannot use a normal index on a large catalog.
* Reduce the cold start (about 11 s in Docker, 20 s with the test context): Class Data Sharing / AOT cache, or a warm-up before the container is reported healthy.
* New features: online payment, e-mail notifications about order status, product images, saved delivery addresses.
