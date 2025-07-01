package com.distributebank.ledgerservice.service;

import com.distributebank.common.event.TransferEvent;
import com.distributebank.common.model.Account;
import com.distributebank.common.model.Transaction;
import com.distributebank.ledgerservice.repository.AccountRepository;
import com.distributebank.ledgerservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Set;
import java.util.Optional;

/**
 * 账本服务业务逻辑类
 * 负责账户余额更新和交易记录，使用乐观锁保证数据一致性
 * 结合延时双删策略保证缓存一致性
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerService {
    
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final RedisTemplate<String, String> redisTemplate;
    
    private static final String ACCOUNT_BALANCE_PREFIX = "account:balance:";
    private static final String TRANSACTION_PROCESSED_PREFIX = "ledger:processed:";
    
    /**
     * 处理转账事件，更新账户余额
     */
    @Transactional
    public void processTransferEvent(TransferEvent event) {
        String transactionId = event.getTransactionId();
        String fromAccountId = event.getFromAccountId();
        String toAccountId = event.getToAccountId();
        BigDecimal amount = event.getAmount();
        
        log.info("开始处理转账事件: {}", transactionId);
        
        try {
            // 1. 先删除源账户和目标账户的缓存
            deleteCacheFirst(fromAccountId);
            deleteCacheFirst(toAccountId);
            
            // 2. 使用乐观锁更新源账户余额
            updateFromAccountBalance(fromAccountId, amount, transactionId);
            
            // 3. 使用乐观锁更新目标账户余额
            updateToAccountBalance(toAccountId, amount, transactionId);
            
            // 4. 保存交易记录
            saveTransactionRecord(event);
            
            // 5. 延时删除缓存
            scheduleDelayedDelete(fromAccountId, 500);
            scheduleDelayedDelete(toAccountId, 500);
            
            log.info("转账事件处理成功: {}", transactionId);
            
        } catch (Exception e) {
            log.error("转账事件处理失败: {}", transactionId, e);
            throw new RuntimeException("转账处理失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新源账户余额（乐观锁）
     */
    private void updateFromAccountBalance(String accountId, BigDecimal amount, String transactionId) {
        int maxRetries = 3;
        
        for (int i = 0; i < maxRetries; i++) {
            try {
                Account account = accountRepository.findByAccountId(accountId)
                        .orElseThrow(() -> new RuntimeException("源账户不存在: " + accountId));
                
                if (account.getBalance().compareTo(amount) < 0) {
                    throw new RuntimeException("源账户余额不足: " + accountId);
                }
                
                BigDecimal newBalance = account.getBalance().subtract(amount);
                account.setBalance(newBalance);
                account.setUpdatedAt(LocalDateTime.now());
                accountRepository.save(account);
                
                log.info("源账户扣款成功: {} 扣款金额: {} 新余额: {}", accountId, amount, newBalance);
                return;
                
            } catch (ObjectOptimisticLockingFailureException e) {
                if (i == maxRetries - 1) {
                    log.error("源账户扣款失败，乐观锁冲突: {}", accountId, e);
                    throw new RuntimeException("并发冲突，请重试");
                }
                // 重试前删除缓存
                deleteCache(accountId);
                log.warn("源账户扣款乐观锁冲突，重试第{}次: {}", i + 1, accountId);
                try {
                    Thread.sleep(10 * (i + 1)); // 递增延时
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("扣款操作被中断");
                }
            }
        }
        
        throw new RuntimeException("源账户扣款失败，重试次数已用完");
    }
    
    /**
     * 更新目标账户余额（乐观锁）
     */
    private void updateToAccountBalance(String accountId, BigDecimal amount, String transactionId) {
        int maxRetries = 3;
        
        for (int i = 0; i < maxRetries; i++) {
            try {
                Account account = accountRepository.findByAccountId(accountId)
                        .orElseThrow(() -> new RuntimeException("目标账户不存在: " + accountId));
                
                BigDecimal newBalance = account.getBalance().add(amount);
                account.setBalance(newBalance);
                account.setUpdatedAt(LocalDateTime.now());
                accountRepository.save(account);
                
                log.info("目标账户收款成功: {} 收款金额: {} 新余额: {}", accountId, amount, newBalance);
                return;
                
            } catch (ObjectOptimisticLockingFailureException e) {
                if (i == maxRetries - 1) {
                    log.error("目标账户收款失败，乐观锁冲突: {}", accountId, e);
                    throw new RuntimeException("并发冲突，请重试");
                }
                // 重试前删除缓存
                deleteCache(accountId);
                log.warn("目标账户收款乐观锁冲突，重试第{}次: {}", i + 1, accountId);
                try {
                    Thread.sleep(10 * (i + 1)); // 递增延时
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("收款操作被中断");
                }
            }
        }
        
        throw new RuntimeException("目标账户收款失败，重试次数已用完");
    }
    
    /**
     * 保存交易记录
     */
    private void saveTransactionRecord(TransferEvent event) {
        Transaction transaction = Transaction.builder()
                .transactionId(event.getTransactionId())
                .fromAccountId(event.getFromAccountId())
                .toAccountId(event.getToAccountId())
                .amount(event.getAmount())
                .status(Transaction.TransactionStatus.SUCCESS)
                .clearingStatus(Transaction.ClearingStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        transactionRepository.save(transaction);
        log.info("交易记录保存成功: {}", event.getTransactionId());
    }
    
    /**
     * 先删除缓存
     */
    private void deleteCacheFirst(String accountId) {
        String balanceKey = ACCOUNT_BALANCE_PREFIX + accountId;
        redisTemplate.delete(balanceKey);
        log.debug("先删除缓存: {}", balanceKey);
    }
    
    /**
     * 延时删除缓存
     */
    private void scheduleDelayedDelete(String accountId, long delayMs) {
        new Thread(() -> {
            try {
                Thread.sleep(delayMs);
                String balanceKey = ACCOUNT_BALANCE_PREFIX + accountId;
                redisTemplate.delete(balanceKey);
                log.info("延时删除缓存成功: {} (延时{}ms)", balanceKey, delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("延时删除被中断: {}", accountId);
            } catch (Exception e) {
                log.error("延时删除缓存失败: {}", accountId, e);
            }
        }).start();
    }
    
    /**
     * 删除缓存
     */
    private void deleteCache(String accountId) {
        String balanceKey = ACCOUNT_BALANCE_PREFIX + accountId;
        redisTemplate.delete(balanceKey);
        log.debug("删除缓存: {}", balanceKey);
    }
    
    /**
     * 获取账户余额
     */
    public BigDecimal getAccountBalance(String accountId) {
        Optional<Account> account = accountRepository.findByAccountId(accountId);
        return account.map(Account::getBalance).orElse(BigDecimal.ZERO);
    }
    
    /**
     * 获取交易记录
     */
    public Optional<Transaction> getTransaction(String transactionId) {
        return transactionRepository.findByTransactionId(transactionId);
    }
} 