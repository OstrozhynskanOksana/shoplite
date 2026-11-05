package com.shoplite.catalog;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Catalog pages (FR-03, FR-04) and product page (FR-05). */
@Controller
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/products";
    }

    @GetMapping("/products")
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) Long category,
                       @RequestParam(required = false) String sort,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        CatalogFilter filter = new CatalogFilter(q, category, sort, page);
        Page<Product> products = catalogService.findProducts(filter);
        model.addAttribute("products", products);
        model.addAttribute("categories", catalogService.getCategories());
        model.addAttribute("filter", filter);
        return "catalog/list";
    }

    @GetMapping("/products/{id}")
    public String product(@PathVariable Long id, Model model) {
        model.addAttribute("product", catalogService.getActiveProduct(id));
        return "catalog/product";
    }
}
