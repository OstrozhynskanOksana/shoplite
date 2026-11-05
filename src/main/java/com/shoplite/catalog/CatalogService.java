package com.shoplite.catalog;

import org.springframework.data.domain.Page;

import java.util.List;

/** Catalog business logic. Used by web controllers, the REST API and the order module. */
public interface CatalogService {

    int PAGE_SIZE = 12;

    Page<Product> findProducts(CatalogFilter filter);

    Product getActiveProduct(Long id);

    List<Category> getCategories();
}
