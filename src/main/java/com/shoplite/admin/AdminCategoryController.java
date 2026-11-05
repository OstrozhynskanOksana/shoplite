package com.shoplite.admin;

import com.shoplite.catalog.AdminCatalogService;
import com.shoplite.common.exception.BusinessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Category management in the admin panel (FR-13). */
@Controller
@RequestMapping("/admin/categories")
public class AdminCategoryController {

    private final AdminCatalogService adminCatalog;

    public AdminCategoryController(AdminCatalogService adminCatalog) {
        this.adminCatalog = adminCatalog;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", adminCatalog.getCategories());
        return "admin/categories";
    }

    @PostMapping
    public String create(@RequestParam String name, RedirectAttributes flash) {
        try {
            adminCatalog.createCategory(name);
            flash.addFlashAttribute("success", "Category created");
        } catch (BusinessException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/rename")
    public String rename(@PathVariable Long id, @RequestParam String name, RedirectAttributes flash) {
        try {
            adminCatalog.renameCategory(id, name);
            flash.addFlashAttribute("success", "Category renamed");
        } catch (BusinessException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        try {
            adminCatalog.deleteCategory(id);
            flash.addFlashAttribute("success", "Category deleted");
        } catch (BusinessException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/categories";
    }
}
