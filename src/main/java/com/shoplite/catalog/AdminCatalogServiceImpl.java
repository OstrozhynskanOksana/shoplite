package com.shoplite.catalog;

import com.shoplite.common.exception.BusinessException;
import com.shoplite.common.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AdminCatalogServiceImpl implements AdminCatalogService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public AdminCatalogServiceImpl(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAllByOrderByNameAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public Product getProduct(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new NotFoundException("Product not found"));
    }

    @Override
    public Product createProduct(ProductForm form) {
        Product product = new Product(form.getName().trim(), clean(form.getDescription()), form.getPrice(),
                form.getStock(), getCategory(form.getCategoryId()));
        product.setImageUrl(clean(form.getImageUrl()));
        return productRepository.save(product);
    }

    @Override
    public void updateProduct(Long id, ProductForm form) {
        Product product = getProduct(id);
        product.setName(form.getName().trim());
        product.setDescription(clean(form.getDescription()));
        product.setPrice(form.getPrice());
        product.setStock(form.getStock());
        product.setCategory(getCategory(form.getCategoryId()));
        product.setImageUrl(clean(form.getImageUrl()));
    }

    @Override
    public boolean toggleActive(Long id) {
        Product product = getProduct(id);
        product.setActive(!product.isActive());
        return product.isActive();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryRow> getCategories() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .map(c -> new CategoryRow(c.getId(), c.getName(), productRepository.countByCategoryId(c.getId())))
                .toList();
    }

    @Override
    public Category createCategory(String name) {
        String cleanName = validCategoryName(name);
        if (categoryRepository.existsByNameIgnoreCase(cleanName)) {
            throw new BusinessException("Category \"" + cleanName + "\" already exists");
        }
        return categoryRepository.save(new Category(cleanName));
    }

    @Override
    public void renameCategory(Long id, String name) {
        Category category = getCategory(id);
        String cleanName = validCategoryName(name);
        if (!category.getName().equalsIgnoreCase(cleanName) && categoryRepository.existsByNameIgnoreCase(cleanName)) {
            throw new BusinessException("Category \"" + cleanName + "\" already exists");
        }
        category.setName(cleanName);
    }

    @Override
    public void deleteCategory(Long id) {
        Category category = getCategory(id);
        if (productRepository.countByCategoryId(id) > 0) {
            throw new BusinessException("Category \"" + category.getName() + "\" has products and cannot be deleted");
        }
        categoryRepository.delete(category);
    }

    private Category getCategory(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> new NotFoundException("Category not found"));
    }

    private static String validCategoryName(String name) {
        String clean = name == null ? "" : name.trim();
        if (clean.isEmpty()) {
            throw new BusinessException("Category name is required");
        }
        if (clean.length() > 100) {
            throw new BusinessException("Category name is too long");
        }
        return clean;
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
