package com.example.accountbook.service;

import com.example.accountbook.entity.Account;
import com.example.accountbook.entity.Transaction;
import com.example.accountbook.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Service
@Transactional
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final CategoryService categoryService;

    // 手动添加构造函数
    @Autowired
    public TransactionService(TransactionRepository transactionRepository,
                              AccountService accountService,
                              CategoryService categoryService) {
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
        this.categoryService = categoryService;
    }

    public Transaction createTransaction(Transaction transaction) {
        // 验证关联的账户和分类存在
        accountService.getAccountById(transaction.getAccount().getId());
        categoryService.getCategoryById(transaction.getCategory().getId());

        if (transaction.getTransactionDate() == null) {
            transaction.setTransactionDate(LocalDateTime.now());
        }

        Transaction savedTransaction = transactionRepository.save(transaction);

        // 更新账户余额
        accountService.updateAccountBalance(
                transaction.getAccount().getId(),
                transaction.getAmount(),
                transaction.getType()
        );

        System.out.println("创建交易成功: " + transaction.getAccount().getName() +
                " " + transaction.getAmount() + "元, 类型: " + transaction.getType());

        return savedTransaction;
    }

    public Transaction updateTransaction(Long id, Transaction payload) {
        Transaction tx = getTransactionById(id);
        if (payload.getAmount() != null) tx.setAmount(payload.getAmount());
        if (payload.getType() != null) tx.setType(payload.getType());
        if (payload.getCategory() != null) tx.setCategory(categoryService.getCategoryById(payload.getCategory().getId()));
        if (payload.getAccount() != null) tx.setAccount(accountService.getAccountById(payload.getAccount().getId()));
        if (payload.getTransactionDate() != null) tx.setTransactionDate(payload.getTransactionDate());
        if (payload.getNote() != null) tx.setNote(payload.getNote());
        return transactionRepository.save(tx);
    }

    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        List<Transaction> list = transactionRepository.findAllById(ids);
        transactionRepository.deleteAll(list);
    }

    // 移除 exportCsv 与 importCsv

    public int importXlsx(MultipartFile file) {
        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sh = wb.getSheetAt(0);
            int count = 0;
            for (int r=1; r<=sh.getLastRowNum(); r++) {
                Row row = sh.getRow(r);
                if (row == null) continue;
                Long accountId = (long)row.getCell(0).getNumericCellValue();
                Long categoryId = (long)row.getCell(1).getNumericCellValue();
                java.math.BigDecimal amount = new java.math.BigDecimal(row.getCell(2).getNumericCellValue()).setScale(2, java.math.RoundingMode.HALF_UP);
                Transaction.TransactionType type = Transaction.TransactionType.valueOf(row.getCell(3).getStringCellValue());
                java.time.LocalDateTime dt = java.time.LocalDateTime.parse(row.getCell(4).getStringCellValue());
                String note = row.getCell(5)!=null? row.getCell(5).getStringCellValue(): null;
                Transaction tx = new Transaction();
                tx.setAccount(accountService.getAccountById(accountId));
                tx.setCategory(categoryService.getCategoryById(categoryId));
                tx.setAmount(amount);
                tx.setType(type);
                tx.setTransactionDate(dt);
                tx.setNote(note);
                transactionRepository.save(tx);
                count++;
            }
            return count;
        } catch (Exception e) {
            throw new RuntimeException("Excel导入失败: "+e.getMessage());
        }
    }

    // 其他方法保持不变...
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    public Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("交易记录不存在，ID: " + id));
    }

    public void deleteTransaction(Long id) {
        Transaction transaction = getTransactionById(id);
        transactionRepository.delete(transaction);
    }

    public List<Transaction> getRecentTransactions(int days) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(days);
        return transactionRepository.findByTransactionDateBetweenOrderByTransactionDateDesc(start, end);
    }

    public BigDecimal getTotalIncome(LocalDateTime start, LocalDateTime end) {
        BigDecimal total = transactionRepository.sumAmountByTypeAndDateRange(Transaction.TransactionType.INCOME, start, end);
        return total == null ? BigDecimal.ZERO : total;
    }

    public BigDecimal getTotalExpense(LocalDateTime start, LocalDateTime end) {
        BigDecimal total = transactionRepository.sumAmountByTypeAndDateRange(Transaction.TransactionType.EXPENSE, start, end);
        return total == null ? BigDecimal.ZERO : total;
    }

    public String getFinancialOverview() {
        BigDecimal totalIncome = getTotalIncome(LocalDateTime.MIN, LocalDateTime.now());
        BigDecimal totalExpense = getTotalExpense(LocalDateTime.MIN, LocalDateTime.now());
        BigDecimal balance = totalIncome.subtract(totalExpense);
        return String.format("总收入：%s, 总支出：%s, 结余：%s", totalIncome, totalExpense, balance);
    }

    // 分类统计分析（按分类分组，统计本月各分类收入/支出总额）
    public List<Map<String,Object>> getCategorySummary(LocalDateTime start, LocalDateTime end) {
        // 可以直接用原生SQL或分组stream操作简单实现
        List<Transaction> txs = transactionRepository.findByTransactionDateBetweenOrderByTransactionDateDesc(start, end);
        Map<String, Map<String, Object>> statMap = new java.util.HashMap<>();
        for (Transaction tx : txs) {
            String key = tx.getCategory().getName() + ":" + tx.getType();
            Map<String, Object> cur = statMap.computeIfAbsent(key, k -> {
                Map<String, Object> m = new java.util.HashMap<>();
                m.put("categoryName", tx.getCategory().getName());
                m.put("type", tx.getType().toString());
                m.put("totalAmount", java.math.BigDecimal.ZERO);
                return m;
            });
            java.math.BigDecimal prev = (java.math.BigDecimal)cur.get("totalAmount");
            cur.put("totalAmount", prev.add(tx.getAmount()));
        }
        return new java.util.ArrayList<>(statMap.values());
    }

    public List<Transaction> advancedSearch(String keyword,
                                            Transaction.TransactionType type,
                                            Long categoryId,
                                            Long accountId,
                                            BigDecimal amountMin,
                                            BigDecimal amountMax,
                                            LocalDateTime start,
                                            LocalDateTime end) {
        List<Transaction> list;
        if (start != null && end != null) {
            list = transactionRepository.findByTransactionDateBetweenOrderByTransactionDateDesc(start, end);
        } else {
            list = transactionRepository.findAll();
        }
        return list.stream()
                .filter(t -> type == null || t.getType() == type)
                .filter(t -> categoryId == null || (t.getCategory()!=null && categoryId.equals(t.getCategory().getId())))
                .filter(t -> accountId == null || (t.getAccount()!=null && accountId.equals(t.getAccount().getId())))
                .filter(t -> amountMin == null || t.getAmount().compareTo(amountMin) >= 0)
                .filter(t -> amountMax == null || t.getAmount().compareTo(amountMax) <= 0)
                .filter(t -> {
                    if (keyword == null || keyword.isBlank()) return true;
                    String k = keyword.toLowerCase();
                    String note = t.getNote()==null? "": t.getNote().toLowerCase();
                    String cat = (t.getCategory()==null? "": t.getCategory().getName()).toLowerCase();
                    return note.contains(k) || cat.contains(k);
                })
                .collect(java.util.stream.Collectors.toList());
    }

    public byte[] exportSelectedXlsx(List<Long> ids) {
        List<Transaction> list = transactionRepository.findAllById(ids);
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sh = wb.createSheet("transactions");
            int r = 0;
            Row h = sh.createRow(r++);
            String[] heads = {"id","account","category","amount","type","transactionDate","note"};
            for (int i=0;i<heads.length;i++) h.createCell(i).setCellValue(heads[i]);
            for (Transaction t : list) {
                Row row = sh.createRow(r++);
                row.createCell(0).setCellValue(t.getId());
                row.createCell(1).setCellValue(t.getAccount()!=null? t.getAccount().getName(): "");
                row.createCell(2).setCellValue(t.getCategory()!=null? t.getCategory().getName(): "");
                row.createCell(3).setCellValue(t.getAmount().doubleValue());
                row.createCell(4).setCellValue(t.getType().name());
                row.createCell(5).setCellValue(t.getTransactionDate()!=null? t.getTransactionDate().toString(): "");
                row.createCell(6).setCellValue(t.getNote()!=null? t.getNote(): "");
            }
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("导出Excel失败: "+e.getMessage());
        }
    }

    public byte[] exportTemplateXlsx() {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sh = wb.createSheet("template");
            Row h = sh.createRow(0);
            String[] heads = {"accountId","categoryId","amount","type(INCOME|EXPENSE)","transactionDate(ISO)","note"};
            for (int i=0;i<heads.length;i++) h.createCell(i).setCellValue(heads[i]);
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("模板导出失败: "+e.getMessage());
        }
    }

    // 获取指定时间范围内的每日收支汇总
    public List<Map<String, Object>> getDailySummary(LocalDateTime start, LocalDateTime end) {
        List<Transaction> txs = transactionRepository.findByTransactionDateBetweenOrderByTransactionDateDesc(start, end);
        Map<String, Map<String, Object>> dailyMap = new java.util.LinkedHashMap<>();
        LocalDateTime current = start;
        while (!current.isAfter(end)) {
            String key = current.toLocalDate().toString();
            dailyMap.put(key, new java.util.HashMap<>(Map.of("date", key, "income", BigDecimal.ZERO, "expense", BigDecimal.ZERO)));
            current = current.plusDays(1);
        }
        for (Transaction tx : txs) {
            String key = tx.getTransactionDate().toLocalDate().toString();
            Map<String, Object> day = dailyMap.get(key);
            if (day != null) {
                if (tx.getType() == Transaction.TransactionType.INCOME) {
                    day.put("income", ((BigDecimal)day.get("income")).add(tx.getAmount()));
                } else {
                    day.put("expense", ((BigDecimal)day.get("expense")).add(tx.getAmount()));
                }
            }
        }
        return new java.util.ArrayList<>(dailyMap.values());
    }

    // 获取本周和上周的收支对比
    public Map<String, Map<String, BigDecimal>> getWeeklyComparison() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thisWeekStart = now.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime thisWeekEnd = now.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY)).withHour(23).withMinute(59).withSecond(59);
        LocalDateTime lastWeekStart = thisWeekStart.minusWeeks(1);
        LocalDateTime lastWeekEnd = thisWeekEnd.minusWeeks(1);
        BigDecimal thisWeekIncome = getTotalIncome(thisWeekStart, thisWeekEnd);
        BigDecimal thisWeekExpense = getTotalExpense(thisWeekStart, thisWeekEnd);
        BigDecimal lastWeekIncome = getTotalIncome(lastWeekStart, lastWeekEnd);
        BigDecimal lastWeekExpense = getTotalExpense(lastWeekStart, lastWeekEnd);
        return Map.of(
            "thisWeek", Map.of("income", thisWeekIncome, "expense", thisWeekExpense),
            "lastWeek", Map.of("income", lastWeekIncome, "expense", lastWeekExpense)
        );
    }

    // 获取支出分类Top5（用于玫瑰图）
    public List<Map<String, Object>> getTopExpenseCategories(LocalDateTime start, LocalDateTime end, int limit) {
        List<Map<String, Object>> all = getCategorySummary(start, end);
        return all.stream()
            .filter(m -> "EXPENSE".equals(m.get("type")))
            .sorted((a, b) -> ((BigDecimal)b.get("totalAmount")).compareTo((BigDecimal)a.get("totalAmount")))
            .limit(limit)
            .collect(java.util.stream.Collectors.toList());
    }
}