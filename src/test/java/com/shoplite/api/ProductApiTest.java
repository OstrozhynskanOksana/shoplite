package com.shoplite.api;

import com.jayway.jsonpath.JsonPath;
import com.shoplite.catalog.Category;
import com.shoplite.catalog.Product;
import com.shoplite.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Component test of the public catalog API: controller, service, repository and JSON together. */
@Transactional
class ProductApiTest extends IntegrationTestSupport {

    @Test
    @DisplayName("FR-03: the first page has at most 12 products and page metadata")
    void listReturnsFirstPage() throws Exception {
        mvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(12))
                .andExpect(jsonPath("$.items.length()", lessThanOrEqualTo(12)))
                .andExpect(jsonPath("$.totalItems", greaterThanOrEqualTo(12)));
    }

    @Test
    @DisplayName("FR-04: search by part of the name")
    void searchByName() throws Exception {
        mvc.perform(get("/api/products").param("q", "KETTLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].name", everyItem(containsStringIgnoringCase("kettle"))))
                .andExpect(jsonPath("$.items.length()", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("FR-04: category filter with price sorting, highest first")
    void categoryFilterAndSorting() throws Exception {
        Category category = newCategory();
        newProduct("Cheap", "10.00", 5, category.getId());
        newProduct("Expensive", "99.90", 5, category.getId());
        newProduct("Middle", "50.00", 5, category.getId());

        String body = mvc.perform(get("/api/products")
                        .param("category", category.getId().toString())
                        .param("sort", "price_desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].categoryId", everyItem(is(category.getId().intValue()))))
                .andReturn().getResponse().getContentAsString();

        List<String> names = JsonPath.read(body, "$.items[*].name");
        assertEquals(List.of("Expensive", "Middle", "Cheap"), names);
    }

    @Test
    @DisplayName("FR-05: product details")
    void productDetails() throws Exception {
        Product product = newProduct("10.50", 3);

        mvc.perform(get("/api/products/{id}", product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product.getId().intValue()))
                .andExpect(jsonPath("$.name").value(product.getName()))
                .andExpect(jsonPath("$.price").value(10.5))
                .andExpect(jsonPath("$.stock").value(3))
                .andExpect(jsonPath("$.inStock").value(true))
                .andExpect(jsonPath("$.categoryName").value(product.getCategory().getName()));
    }

    @Test
    void outOfStockProductIsShownButMarked() throws Exception {
        Product product = newProduct("10.00", 0);

        mvc.perform(get("/api/products/{id}", product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inStock").value(false));
    }

    @Test
    @DisplayName("NFR-05: unknown and hidden products give 404 with a JSON error")
    void unknownOrHiddenProductIs404() throws Exception {
        Product hidden = newProduct("10.00", 1);
        adminCatalog.toggleActive(hidden.getId());

        mvc.perform(get("/api/products/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Product not found"));
        mvc.perform(get("/api/products/{id}", hidden.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("NFR-05: a wrong parameter type gives 400 with a JSON error")
    void invalidParameterIs400() throws Exception {
        mvc.perform(get("/api/products").param("category", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Invalid value for parameter \"category\""));
    }

    @Test
    void pricesAreNeverZeroOrNegative() throws Exception {
        String body = mvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Number> prices = JsonPath.read(body, "$.items[*].price");
        assertTrue(prices.stream().allMatch(p -> new BigDecimal(p.toString()).signum() > 0));
    }
}
