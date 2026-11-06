package com.shoplite.catalog;

import com.shoplite.common.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static com.shoplite.support.TestData.category;
import static com.shoplite.support.TestData.product;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Unit tests for catalog browsing (FR-03 – FR-05). */
class CatalogServiceImplTest {

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final CatalogService service = new CatalogServiceImpl(productRepository, categoryRepository);

    @Test
    void searchUsesTrimmedQueryCategoryAndPagesOf12() {
        when(productRepository.searchActive(anyString(), any(), any(Pageable.class))).thenReturn(Page.empty());

        service.findProducts(new CatalogFilter("  kettle ", 2L, null, 1));

        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).searchActive(eq("kettle"), eq(2L), page.capture());
        assertEquals(1, page.getValue().getPageNumber());
        assertEquals(12, page.getValue().getPageSize());
        assertEquals(Sort.by("name").ascending(), page.getValue().getSort());
    }

    @Test
    void sortOptions() {
        assertEquals(Sort.by("price").ascending(), CatalogServiceImpl.toSort("price_asc"));
        assertEquals(Sort.by("price").descending(), CatalogServiceImpl.toSort("price_desc"));
        assertEquals(Sort.by("name").ascending(), CatalogServiceImpl.toSort("something else"));
        assertEquals(Sort.by("name").ascending(), CatalogServiceImpl.toSort(null));
    }

    @Test
    void filterNormalisesMissingQueryAndNegativePage() {
        CatalogFilter filter = new CatalogFilter(null, null, null, -5);

        assertEquals("", filter.query());
        assertEquals(0, filter.page());
    }

    @Test
    void activeProductIsReturned() {
        Product kettle = product(1L, "Kettle", "899.00", 4);
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(kettle));

        assertSame(kettle, service.getActiveProduct(1L));
    }

    @Test
    void hiddenOrMissingProductIsNotFound() {
        when(productRepository.findByIdAndActiveTrue(7L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> service.getActiveProduct(7L));
        assertEquals("Product not found", ex.getMessage());
    }

    @Test
    void categoriesComeSortedFromRepository() {
        List<Category> categories = List.of(category(3L, "Bathroom"), category(1L, "Kitchen"));
        when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(categories);

        assertEquals(categories, service.getCategories());
    }
}
