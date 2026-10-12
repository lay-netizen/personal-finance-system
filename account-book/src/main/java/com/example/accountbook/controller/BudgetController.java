package com.example.accountbook.controller;

import com.example.accountbook.entity.Budget;
import com.example.accountbook.service.BudgetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    @Autowired
    public BudgetController(BudgetService budgetService) { this.budgetService = budgetService; }

    @PostMapping("/monthly")
    public ResponseEntity<Budget> setMonthly(@RequestBody Map<String, String> body) {
        BigDecimal limit = new BigDecimal(body.getOrDefault("limitAmount","0"));
        return ResponseEntity.ok(budgetService.setMonthlyBudget(limit));
    }

    @PostMapping("/category")
    public ResponseEntity<Budget> setCategory(@RequestBody Map<String, String> body) {
        Long categoryId = Long.valueOf(body.get("categoryId"));
        BigDecimal limit = new BigDecimal(body.getOrDefault("limitAmount","0"));
        return ResponseEntity.ok(budgetService.setCategoryBudget(categoryId, limit));
    }

    @GetMapping("/monthly/status")
    public ResponseEntity<Map<String,Object>> monthlyStatus() {
        return ResponseEntity.ok(budgetService.getMonthlyBudgetStatus());
    }

    @GetMapping
    public ResponseEntity<List<Budget>> list() { return ResponseEntity.ok(budgetService.listCurrentMonthBudgets()); }
}
