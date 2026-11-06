package com.shoplite.catalog;

import com.shoplite.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Component test: catalog service and the JPQL search query on the real schema (FR-03, FR-04). */
@Transactional
class CatalogSearchIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private CatalogService catalogService;

    private List<String> names(Page<Product> page) {
        return page.getContent().stream().map(Product::getName).toList();
    }

    @Test
    void searchIgnoresCaseAndSkipsHiddenProducts() {
        Category category = newCategory();
        Product teapot = newProduct("Glass Teapot", "350.00", 5, category.getId());
        Product vase = newProduct("Glass vase", "420.00", 5, category.getId());
        adminCatalog.toggleActive(vase.getId());

        Page<Product> found = catalogService.findProducts(new CatalogFilter("gLaSs", null, null, 0));

        assertTrue(names(found).contains(teapot.getName()));
        assertFalse(names(found).contains(vase.getName()), "hidden products are not shown");
    }

    @Test
    void categoryFilterAndPriceSorting() {
        Category category = newCategory();
        newProduct("Cup", "120.00", 5, category.getId());
        newProduct("Plate", "90.00", 5, category.getId());
        newProduct("Bowl", "150.00", 5, category.getId());

        Page<Product> cheapFirst = catalogService.findProducts(new CatalogFilter("", category.getId(), "price_asc", 0));
        Page<Product> byName = catalogService.findProducts(new CatalogFilter("", category.getId(), null, 0));

        assertEquals(List.of("Plate", "Cup", "Bowl"), names(cheapFirst));
        assertEquals(List.of("Bowl", "Cup", "Plate"), names(byName));
    }

    @Test
    void catalogIsPagedBy12() {
        Category category = newCategory();
        for (int i = 1; i <= 13; i++) {
            newProduct(String.format("Item %02d", i), "10.00", 1, category.getId());
        }

        Page<Product> first = catalogService.findProducts(new CatalogFilter("", category.getId(), null, 0));
        Page<Product> second = catalogService.findProducts(new CatalogFilter("", category.getId(), null, 1));

        assertEquals(12, first.getContent().size());
        assertEquals(13, first.getTotalElements());
        assertEquals(2, first.getTotalPages());
        assertEquals(List.of("Item 13"), names(second));
    }

    @Test
    @DisplayName("SCRUM-39: % and _ typed in the search box are matched as plain characters")
    void wildcardCharactersAreMatchedLiterally() {
        Category category = newCategory();
        newProduct("Cotton towel 100%", "199.00", 5, category.getId());
        newProduct("Cotton towel 70x140", "249.00", 5, category.getId());
        newProduct("Box_set of cups", "320.00", 5, category.getId());
        newProduct("Box set of plates", "350.00", 5, category.getId());

        Page<Product> percent = catalogService.findProducts(new CatalogFilter("%", category.getId(), null, 0));
        Page<Product> underscore = catalogService.findProducts(new CatalogFilter("box_set", category.getId(), null, 0));

        assertEquals(List.of("Cotton towel 100%"), names(percent));
        assertEquals(List.of("Box_set of cups"), names(underscore));
    }

    @Test
    void seedDataIsLoadedByFlyway() {
        Page<Product> kettles = catalogService.findProducts(new CatalogFilter("kettle", null, null, 0));

        assertEquals(List.of("Electric kettle 1.7 L"), names(kettles));
        assertTrue(catalogService.getCategories().stream().map(Category::getName).toList()
                .containsAll(List.of("Bathroom", "Cleaning", "Kitchen", "Textiles")));
    }
}
