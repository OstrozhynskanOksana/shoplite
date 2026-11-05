package com.shoplite.admin;

import com.shoplite.catalog.AdminCatalogService;
import com.shoplite.catalog.CatalogService;
import com.shoplite.catalog.ProductForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Product management in the admin panel (FR-12). Access is limited to ADMIN by SecurityConfig. */
@Controller
@RequestMapping("/admin/products")
public class AdminProductController {

    private final AdminCatalogService adminCatalog;
    private final CatalogService catalogService;

    public AdminProductController(AdminCatalogService adminCatalog, CatalogService catalogService) {
        this.adminCatalog = adminCatalog;
        this.catalogService = catalogService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("products", adminCatalog.getAllProducts());
        return "admin/products";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new ProductForm());
        return formView(model, null);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") ProductForm form, BindingResult result, Model model,
                         RedirectAttributes flash) {
        if (result.hasErrors()) {
            return formView(model, null);
        }
        adminCatalog.createProduct(form);
        flash.addFlashAttribute("success", "Product \"" + form.getName().trim() + "\" created");
        return "redirect:/admin/products";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", ProductForm.from(adminCatalog.getProduct(id)));
        return formView(model, id);
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") ProductForm form,
                         BindingResult result, Model model, RedirectAttributes flash) {
        if (result.hasErrors()) {
            return formView(model, id);
        }
        adminCatalog.updateProduct(id, form);
        flash.addFlashAttribute("success", "Product \"" + form.getName().trim() + "\" saved");
        return "redirect:/admin/products";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes flash) {
        boolean active = adminCatalog.toggleActive(id);
        flash.addFlashAttribute("success", active ? "Product is visible in the catalog again"
                : "Product is hidden from the catalog");
        return "redirect:/admin/products";
    }

    private String formView(Model model, Long productId) {
        model.addAttribute("productId", productId);
        model.addAttribute("categories", catalogService.getCategories());
        return "admin/product-form";
    }
}
