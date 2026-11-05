package com.shoplite.catalog;

/**
 * Catalog browsing parameters.
 *
 * @param query      part of the product name, or null
 * @param categoryId category id, or null
 * @param sort       "price_asc", "price_desc", or null (by name)
 * @param page       zero-based page number
 */
public record CatalogFilter(String query, Long categoryId, String sort, int page) {

    public CatalogFilter {
        page = Math.max(page, 0);
        query = query == null ? "" : query.trim();
    }
}
