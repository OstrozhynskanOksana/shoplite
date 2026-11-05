package com.shoplite.admin;

import com.shoplite.stats.StatsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/** Monthly sales statistics (FR-15). */
@Controller
public class AdminStatsController {

    private final StatsService statsService;

    public AdminStatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/admin/stats")
    public String stats(@RequestParam(required = false) String month, Model model) {
        YearMonth selected = parse(month);
        model.addAttribute("sales", statsService.getMonthlySales(selected));
        model.addAttribute("month", selected.toString());
        return "admin/stats";
    }

    private static YearMonth parse(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException ex) {
            return YearMonth.now();
        }
    }
}
