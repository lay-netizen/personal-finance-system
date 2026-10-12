package com.example.accountbook.repository;

import com.example.accountbook.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    Optional<Budget> findByMonthAndCategoryId(String month, Long categoryId);
    List<Budget> findByMonth(String month);
}
