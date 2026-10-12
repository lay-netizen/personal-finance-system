package com.example.accountbook.repository;

import com.example.accountbook.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByTransactionDateBetweenOrderByTransactionDateDesc(
            LocalDateTime start, LocalDateTime end);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.type = :type AND t.transactionDate BETWEEN :start AND :end")
    BigDecimal sumAmountByTypeAndDateRange(
            @Param("type") Transaction.TransactionType type,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}