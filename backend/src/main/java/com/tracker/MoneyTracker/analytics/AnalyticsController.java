package com.tracker.MoneyTracker.analytics;

import com.tracker.MoneyTracker.analytics.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController() {
        this.analyticsService = null;
    }

    @Autowired
    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    public ResponseEntity<SpendingSummary> getSpendingSummary(@RequestParam("userId") String userId) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (analyticsService == null) {
            return ResponseEntity.ok(null);
        }
        return ResponseEntity.ok(analyticsService.getSpendingSummary(userId));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryBreakdown>> getCategoryBreakdown(
            @RequestParam("userId") String userId,
            @RequestParam(value = "start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(value = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (analyticsService == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(analyticsService.getCategoryBreakdown(userId, start, end));
    }

    @GetMapping("/trends")
    public ResponseEntity<List<MonthlyTrend>> getMonthlyTrends(
            @RequestParam("userId") String userId,
            @RequestParam(value = "months", defaultValue = "6") int months) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (analyticsService == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(analyticsService.getMonthlyTrends(userId, months));
    }

    @GetMapping("/budget")
    public ResponseEntity<List<BudgetStatus>> getBudgetStatus(@RequestParam("userId") String userId) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (analyticsService == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(analyticsService.getBudgetStatus(userId));
    }
}
