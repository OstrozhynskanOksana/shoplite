package com.shoplite.catalog;

import com.shoplite.common.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CatalogServiceImpl implements CatalogService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public CatalogServiceImpl(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public Page<Product> findProducts(CatalogFilter filter) {
        PageRequest pageRequest = PageRequest.of(filter.page(), PAGE_SIZE, toSort(filter.sort()));
        return productRepository.searchActive(escapeLike(filter.query()), filter.categoryId(), pageRequest);
    }

    /**
     * Escapes the LIKE wildcards, so "%" and "_" typed by a customer are searched as plain characters
     * (SCRUM-39). The search query declares '\' as its escape character.
     */
    static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    @Override
    public Product getActiveProduct(Long id) {
        return productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new NotFoundException("Product not found"));
    }

    @Override
    public List<Category> getCategories() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    static Sort toSort(String sort) {
        if ("price_asc".equals(sort)) {
            return Sort.by("price").ascending();
        }
        if ("price_desc".equals(sort)) {
            return Sort.by("price").descending();
        }
        return Sort.by("name").ascending();
    }
}
