package com.example.accountbook.service;

import com.example.accountbook.entity.Budget;
import com.example.accountbook.entity.Transaction;
import com.example.accountbook.repository.BudgetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final TransactionService transactionService;

    @Autowired
    public BudgetService(BudgetRepository budgetRepository, TransactionService transactionService) {
        this.budgetRepository = budgetRepository;
        this.transactionService = transactionService;
    }

    private String currentMonth() {
        LocalDate now = LocalDate.now();
        return String.format("%04d-%02d", now.getYear(), now.getMonthValue());
    }

    public Budget setMonthlyBudget(BigDecimal limitAmount) {
        String month = currentMonth();
        Budget b = budgetRepository.findByMonthAndCategoryId(month, null).orElseGet(Budget::new);
        b.setMonth(month);
        b.setCategoryId(null);
        b.setLimitAmount(limitAmount);
        return budgetRepository.save(b);
    }

    public Budget setCategoryBudget(Long categoryId, BigDecimal limitAmount) {
        String month = currentMonth();
        Budget b = budgetRepository.findByMonthAndCategoryId(month, categoryId).orElseGet(Budget::new);
        b.setMonth(month);
        b.setCategoryId(categoryId);
        b.setLimitAmount(limitAmount);
        return budgetRepository.save(b);
    }

    public Map<String,Object> getMonthlyBudgetStatus() {
        String month = currentMonth();
        LocalDateTime start = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime end = LocalDateTime.now();
        BigDecimal totalExpense = transactionService.getTotalExpense(start, end);
        Optional<Budget> ob = budgetRepository.findByMonthAndCategoryId(month, null);
        BigDecimal limit = ob.map(Budget::getLimitAmount).orElse(BigDecimal.ZERO);
        BigDecimal ratio = limit.compareTo(BigDecimal.ZERO)==0 ? BigDecimal.ZERO : totalExpense.divide(limit, 4, RoundingMode.HALF_UP);
        Map<String,Object> m = new HashMap<>();
        m.put("limit", limit);
        m.put("used", totalExpense);
        m.put("ratio", ratio);
        m.put("warn", ratio.compareTo(new BigDecimal("0.8"))>=0);
        m.put("exceed", totalExpense.compareTo(limit)>0);
        return m;
    }

    public List<Budget> listCurrentMonthBudgets() {
        return budgetRepository.findByMonth(currentMonth());
    }
}
