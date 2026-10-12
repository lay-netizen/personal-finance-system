package com.example.accountbook.controller;

import com.example.accountbook.entity.Transaction;
import com.example.accountbook.service.TransactionService;
import com.example.accountbook.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final AccountService accountService;

    // 手动添加构造函数
    @Autowired
    public TransactionController(TransactionService transactionService, AccountService accountService) {
        this.transactionService = transactionService;
        this.accountService = accountService;
    }

    /**
     * 创建交易记录
     */
    @PostMapping
    public ResponseEntity<Transaction> createTransaction(@RequestBody Transaction transaction) {
        Transaction savedTransaction = transactionService.createTransaction(transaction);
        return ResponseEntity.ok(savedTransaction);
    }

    /**
     * 获取所有交易记录
     */
    @GetMapping
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        List<Transaction> transactions = transactionService.getAllTransactions();
        return ResponseEntity.ok(transactions);
    }

    /**
     * 获取最近的交易记录
     */
    @GetMapping("/recent")
    public ResponseEntity<List<Transaction>> getRecentTransactions(
            @RequestParam(defaultValue = "30") int days) {
        List<Transaction> transactions = transactionService.getRecentTransactions(days);
        return ResponseEntity.ok(transactions);
    }

    /**
     * 根据ID获取交易记录
     */
    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getTransactionById(@PathVariable Long id) {
        Transaction transaction = transactionService.getTransactionById(id);
        return ResponseEntity.ok(transaction);
    }

    /**
     * 删除交易记录
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.ok().build();
    }

    /** 编辑交易 */
    @PutMapping("/{id}")
    public ResponseEntity<Transaction> update(@PathVariable Long id, @RequestBody Transaction payload) {
        Transaction updated = transactionService.updateTransaction(id, payload);
        return ResponseEntity.ok(updated);
    }

    /** 批量删除 */
    @DeleteMapping
    public ResponseEntity<Void> batchDelete(@RequestBody List<Long> ids) {
        transactionService.batchDelete(ids);
        return ResponseEntity.ok().build();
    }

    /** 导出CSV -- 已移除 */
    /** 导入CSV -- 已移除 */

    /** 导入Excel（xlsx） */
    @PostMapping(value = "/import/xlsx", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> importXlsx(@RequestParam("file") MultipartFile file) {
        int count = transactionService.importXlsx(file);
        return ResponseEntity.ok(Map.of("imported", count));
    }

    /**
     * 获取财务统计
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, BigDecimal>> getTransactionSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        BigDecimal totalIncome = transactionService.getTotalIncome(start, end);
        BigDecimal totalExpense = transactionService.getTotalExpense(start, end);
        BigDecimal balance = totalIncome.subtract(totalExpense);

        Map<String, BigDecimal> summary = Map.of(
                "totalIncome", totalIncome,
                "totalExpense", totalExpense,
                "balance", balance
        );

        return ResponseEntity.ok(summary);
    }

    /**
     * 获取财务概览
     */
    @GetMapping("/overview")
    public ResponseEntity<String> getFinancialOverview() {
        String overview = transactionService.getFinancialOverview();
        return ResponseEntity.ok(overview);
    }

    /**
     * 首页聚合数据接口
     */
    @GetMapping("/dashboard-summary")
    public ResponseEntity<Map<String, Object>> getDashboardSummary() {
        // 1、核心数据
        BigDecimal totalBalance = accountService.getTotalBalance();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        BigDecimal monthlyIncome = transactionService.getTotalIncome(monthStart, now);
        BigDecimal monthlyExpense = transactionService.getTotalExpense(monthStart, now);
        BigDecimal balance = monthlyIncome.subtract(monthlyExpense);
        Map<String, Object> coreStats = Map.of(
            "totalBalance", totalBalance,
            "monthlyIncome", monthlyIncome,
            "monthlyExpense", monthlyExpense,
            "balance", balance
        );
        // 2、近期流水（近10条）
        List<Transaction> recentTransactions = transactionService.getRecentTransactions(10);
        // 3、分类统计（按本月分组并统计）
        List<Map<String,Object>> categoryStats = transactionService.getCategorySummary(monthStart, now);
        // 4、整体打包
        Map<String, Object> result = Map.of(
            "coreStats", coreStats,
            "recentTransactions", recentTransactions,
            "categoryStats", categoryStats
        );
        return ResponseEntity.ok(result);
    }

    /** 高级搜索 */
    @GetMapping("/search")
    public ResponseEntity<List<Transaction>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Transaction.TransactionType type,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) BigDecimal amountMin,
            @RequestParam(required = false) BigDecimal amountMax,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        List<Transaction> list = transactionService.advancedSearch(keyword, type, categoryId, accountId, amountMin, amountMax, start, end);
        return ResponseEntity.ok(list);
    }

    /** 导出所选为Excel */
    @GetMapping(value = "/export/xlsx")
    public ResponseEntity<byte[]> exportSelectedXlsx(@RequestParam("ids") List<Long> ids) {
        byte[] data = transactionService.exportSelectedXlsx(ids);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=transactions.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    /** 导出导入模板（Excel） */
    @GetMapping(value = "/template.xlsx")
    public ResponseEntity<byte[]> templateXlsx() {
        byte[] data = transactionService.exportTemplateXlsx();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=template.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    /** 获取每日收支汇总（用于趋势图） */
    @GetMapping("/daily-summary")
    public ResponseEntity<List<Map<String, Object>>> getDailySummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(transactionService.getDailySummary(start, end));
    }

    /** 获取本周和上周的收支对比 */
    @GetMapping("/weekly-comparison")
    public ResponseEntity<Map<String, Map<String, BigDecimal>>> getWeeklyComparison() {
        return ResponseEntity.ok(transactionService.getWeeklyComparison());
    }

    /** 获取支出分类Top5（用于玫瑰图） */
    @GetMapping("/top-expense-categories")
    public ResponseEntity<List<Map<String, Object>>> getTopExpenseCategories(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(transactionService.getTopExpenseCategories(start, end, limit));
    }
}