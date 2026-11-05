package com.shoplite.catalog;

import java.util.List;

/** Catalog management for administrators (FR-12, FR-13). */
public interface AdminCatalogService {

    record CategoryRow(Long id, String name, long productCount) {
    }

    List<Product> getAllProducts();

    Product getProduct(Long id);

    Product createProduct(ProductForm form);

    void updateProduct(Long id, ProductForm form);

    /** Hides an active product or shows a hidden one; returns the new state. */
    boolean toggleActive(Long id);

    List<CategoryRow> getCategories();

    Category createCategory(String name);

    void renameCategory(Long id, String name);

    /** Deletes an empty category; a category with products cannot be deleted. */
    void deleteCategory(Long id);
}
