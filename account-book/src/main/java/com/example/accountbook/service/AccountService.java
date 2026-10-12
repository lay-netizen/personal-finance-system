package com.example.accountbook.service;

import com.example.accountbook.entity.Account;
import com.example.accountbook.entity.Transaction;
import com.example.accountbook.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class AccountService {

    private final AccountRepository accountRepository;

    // 手动添加构造函数
    @Autowired
    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    // 获取所有账户
    public List<Account> getAllAccounts() {
        return accountRepository.findAllByOrderByName();
    }

    // 创建新账户
    public Account createAccount(Account account) {
        if (account.getBalance() == null) {
            account.setBalance(BigDecimal.ZERO);
        }
        return accountRepository.save(account);
    }

    // 根据ID获取账户
    public Account getAccountById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("账户不存在，ID: " + id));
    }

    // 获取总资产
    public BigDecimal getTotalBalance() {
        return accountRepository.findAll().stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // 修正：使用正确的枚举类型
    public void updateAccountBalance(Long accountId, BigDecimal amount, Transaction.TransactionType type) {
        Account account = getAccountById(accountId);

        // 根据交易类型更新余额：收入增加，支出减少
        BigDecimal balanceChange = (type == Transaction.TransactionType.INCOME) ? amount : amount.negate();
        BigDecimal newBalance = account.getBalance().add(balanceChange);

        account.setBalance(newBalance);
        accountRepository.save(account);

        System.out.println("更新账户余额: " + account.getName() + " -> " + newBalance);
    }

    public void deleteAccount(Long id) {
        accountRepository.deleteById(id);
    }
}