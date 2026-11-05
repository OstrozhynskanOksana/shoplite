package com.shoplite.api;

import com.shoplite.catalog.CatalogFilter;
import com.shoplite.catalog.CatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public read-only catalog API (FR-03 – FR-05). Reuses the same CatalogService as the web pages. */
@RestController
@RequestMapping("/api/products")
public class ProductApiController {

    private final CatalogService catalogService;

    public ProductApiController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public PageDto<ProductDto> list(@RequestParam(required = false) String q,
                                    @RequestParam(required = false) Long category,
                                    @RequestParam(required = false) String sort,
                                    @RequestParam(defaultValue = "0") int page) {
        return PageDto.of(catalogService.findProducts(new CatalogFilter(q, category, sort, page)), ProductDto::from);
    }

    @GetMapping("/{id}")
    public ProductDto get(@PathVariable Long id) {
        return ProductDto.from(catalogService.getActiveProduct(id));
    }
}
