package com.shoplite.catalog;

import com.shoplite.common.exception.BusinessException;
import com.shoplite.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.shoplite.support.TestData.assertMoney;
import static com.shoplite.support.TestData.category;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Unit tests for catalog management by an administrator (FR-12, FR-13). */
class AdminCatalogServiceImplTest {

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final AdminCatalogService service = new AdminCatalogServiceImpl(productRepository, categoryRepository);

    private Category kitchen;
    private Product kettle;

    @BeforeEach
    void setUp() {
        kitchen = category(1L, "Kitchen");
        kettle = new Product("Kettle", "Old description", new BigDecimal("899.00"), 4, kitchen);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(kitchen));
        when(productRepository.findById(10L)).thenReturn(Optional.of(kettle));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static ProductForm form(String name, String price, int stock) {
        ProductForm form = new ProductForm();
        form.setName(name);
        form.setDescription("   ");
        form.setPrice(new BigDecimal(price));
        form.setStock(stock);
        form.setCategoryId(1L);
        return form;
    }

    @Test
    void createProductTrimsNameAndStoresEmptyFieldsAsNull() {
        Product created = service.createProduct(form("  Teapot ", "350.00", 7));

        assertEquals("Teapot", created.getName());
        assertNull(created.getDescription());
        assertNull(created.getImageUrl());
        assertMoney("350.00", created.getPrice());
        assertEquals(7, created.getStock());
        assertSame(kitchen, created.getCategory());
        assertTrue(created.isActive());
    }

    @Test
    void createProductInUnknownCategoryFails() {
        ProductForm form = form("Teapot", "350.00", 7);
        form.setCategoryId(99L);
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.createProduct(form));
        verify(productRepository, never()).save(any());
    }

    @Test
    void updateProductChangesPriceStockAndDescription() {
        ProductForm form = form("Kettle 2.0", "950.00", 6);
        form.setDescription("  New body  ");

        service.updateProduct(10L, form);

        assertEquals("Kettle 2.0", kettle.getName());
        assertEquals("New body", kettle.getDescription());
        assertMoney("950.00", kettle.getPrice());
        assertEquals(6, kettle.getStock());
    }

    @Test
    void unknownProductIsNotFound() {
        when(productRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getProduct(404L));
        assertThrows(NotFoundException.class, () -> service.toggleActive(404L));
    }

    @Test
    @DisplayName("FR-12: a product is hidden instead of deleted and can be shown again")
    void toggleActiveHidesAndShowsProduct() {
        assertFalse(service.toggleActive(10L));
        assertFalse(kettle.isActive());

        assertTrue(service.toggleActive(10L));
        assertTrue(kettle.isActive());
    }

    @Test
    void allProductsIncludingHiddenOnesAreListed() {
        when(productRepository.findAllByOrderByNameAsc()).thenReturn(List.of(kettle));

        assertEquals(List.of(kettle), service.getAllProducts());
    }

    @Test
    void createCategoryTrimsName() {
        Category created = service.createCategory("  Garden ");

        assertEquals("Garden", created.getName());
    }

    @Test
    @DisplayName("FR-13: category names are unique regardless of case")
    void duplicateCategoryNameIsRejected() {
        when(categoryRepository.existsByNameIgnoreCase("kitchen")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createCategory("kitchen"));

        assertEquals("Category \"kitchen\" already exists", ex.getMessage());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void blankOrTooLongCategoryNameIsRejected() {
        assertEquals("Category name is required",
                assertThrows(BusinessException.class, () -> service.createCategory("   ")).getMessage());
        assertEquals("Category name is required",
                assertThrows(BusinessException.class, () -> service.createCategory(null)).getMessage());
        assertEquals("Category name is too long",
                assertThrows(BusinessException.class, () -> service.createCategory("x".repeat(101))).getMessage());
    }

    @Test
    void renamingToTheSameNameInAnotherCaseIsAllowed() {
        service.renameCategory(1L, "KITCHEN");

        assertEquals("KITCHEN", kitchen.getName());
    }

    @Test
    void renamingToAnExistingNameIsRejected() {
        when(categoryRepository.existsByNameIgnoreCase("Cleaning")).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.renameCategory(1L, "Cleaning"));
        assertEquals("Kitchen", kitchen.getName());
    }

    @Test
    @DisplayName("FR-13: a category with products cannot be deleted")
    void categoryWithProductsCannotBeDeleted() {
        when(productRepository.countByCategoryId(1L)).thenReturn(3L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.deleteCategory(1L));

        assertTrue(ex.getMessage().contains("has products"));
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void emptyCategoryIsDeleted() {
        when(productRepository.countByCategoryId(1L)).thenReturn(0L);

        service.deleteCategory(1L);

        verify(categoryRepository).delete(kitchen);
    }

    @Test
    void categoryListShowsProductCounts() {
        when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of(kitchen));
        when(productRepository.countByCategoryId(1L)).thenReturn(4L);

        assertEquals(List.of(new AdminCatalogService.CategoryRow(1L, "Kitchen", 4L)), service.getCategories());
    }
}
