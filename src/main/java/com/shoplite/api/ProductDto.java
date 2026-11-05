package com.shoplite.api;

import com.shoplite.catalog.Product;

import java.math.BigDecimal;

/** Product representation for the REST API. */
public record ProductDto(Long id, String name, String description, BigDecimal price, int stock,
                         boolean inStock, Long categoryId, String categoryName) {

    public static ProductDto from(Product p) {
        return new ProductDto(p.getId(), p.getName(), p.getDescription(), p.getPrice(), p.getStock(),
                p.isInStock(), p.getCategory().getId(), p.getCategory().getName());
    }
}
