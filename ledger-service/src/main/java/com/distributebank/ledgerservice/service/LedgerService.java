package com.distributebank.ledgerservice.service;

import com.distributebank.common.event.TransferEvent;
import com.distributebank.common.model.Account;
import com.distributebank.common.model.Transaction;
import com.distributebank.ledgerservice.repository.AccountRepository;
import com.distributebank.ledgerservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Set;

/**
 * 账本服务业务逻辑类
 * 负责交易流水记录和数据库余额更新，具备幂等处理能力
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerService {
    
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final RedisTemplate<String, String> redisTemplate;
    
    private static final String ACCOUNT_BALANCE_PREFIX = "account:balance:";
    private static final String TRANSACTION_PROCESSED_PREFIX = "ledger:processed:";
    
    /**
     * 处理清算成功事件（供事件处理器调用）
     */
    public void processClearingSuccess(TransferEvent event) {
        log.info("处理清算成功事件: {}", event.getTransactionId());
        
        try {
            // 幂等检查
            String processedKey = TRANSACTION_PROCESSED_PREFIX + event.getTransactionId();
            if (Boolean.TRUE.equals(redisTemplate.hasKey(processedKey))) {
                log.info("交易已处理过，跳过: {}", event.getTransactionId());
                return;
            }
            
            // 更新交易状态
            updateTransactionStatus(event);
            
            // 更新目标账户余额
            updateTargetAccountBalance(event);
            
            // 标记已处理
            redisTemplate.opsForValue().set(processedKey, "1", Duration.ofSeconds(86400)); // 24小时过期
            
            log.info("账本更新完成: {}", event.getTransactionId());
        } catch (Exception e) {
            log.error("处理清算成功事件异常", e);
        }
    }
    
    /**
     * 处理清算失败事件（供事件处理器调用）
     */
    public void processClearingFailed(TransferEvent event) {
        log.info("处理清算失败事件: {}", event.getTransactionId());
        
        try {
            // 幂等检查
            String processedKey = TRANSACTION_PROCESSED_PREFIX + event.getTransactionId() + ":failed";
            if (Boolean.TRUE.equals(redisTemplate.hasKey(processedKey))) {
                log.info("失败事件已处理过，跳过: {}", event.getTransactionId());
                return;
            }
            
            // 更新交易状态为失败
            updateTransactionStatusFailed(event);
            
            // 标记已处理
            redisTemplate.opsForValue().set(processedKey, "1", Duration.ofSeconds(86400));
            
            log.info("失败状态更新完成: {}", event.getTransactionId());
        } catch (Exception e) {
            log.error("处理清算失败事件异常", e);
        }
    }
    
    /**
     * 更新交易状态为成功
     */
    @Transactional
    private void updateTransactionStatus(TransferEvent event) {
        Transaction transaction = transactionRepository.findByTransactionId(event.getTransactionId())
                .orElseThrow(() -> new RuntimeException("交易不存在: " + event.getTransactionId()));
        
        transaction.setStatus(Transaction.TransactionStatus.SUCCESS);
        transaction.setClearingStatus(Transaction.ClearingStatus.SUCCESS);
        transaction.setFromBalanceAfter(event.getFromBalanceAfter());
        transaction.setUpdatedAt(LocalDateTime.now());
        
        transactionRepository.save(transaction);
        log.info("交易状态更新为成功: {}", event.getTransactionId());
    }
    
    /**
     * 更新交易状态为失败
     */
    @Transactional
    private void updateTransactionStatusFailed(TransferEvent event) {
        Transaction transaction = transactionRepository.findByTransactionId(event.getTransactionId())
                .orElseThrow(() -> new RuntimeException("交易不存在: " + event.getTransactionId()));
        
        transaction.setStatus(Transaction.TransactionStatus.FAILED);
        transaction.setClearingStatus(Transaction.ClearingStatus.FAILED);
        transaction.setErrorMessage("清算失败");
        transaction.setUpdatedAt(LocalDateTime.now());
        
        transactionRepository.save(transaction);
        log.info("交易状态更新为失败: {}", event.getTransactionId());
    }
    
    /**
     * 更新目标账户余额
     */
    @Transactional
    private void updateTargetAccountBalance(TransferEvent event) {
        Account targetAccount = accountRepository.findByAccountId(event.getToAccountId())
                .orElseThrow(() -> new RuntimeException("目标账户不存在: " + event.getToAccountId()));
        
        BigDecimal newBalance = targetAccount.getBalance().add(event.getAmount());
        targetAccount.setBalance(newBalance);
        targetAccount.setUpdatedAt(LocalDateTime.now());
        
        accountRepository.save(targetAccount);
        
        // 同步到Redis
        String balanceKey = ACCOUNT_BALANCE_PREFIX + event.getToAccountId();
        redisTemplate.opsForValue().set(balanceKey, newBalance.toString());
        
        log.info("目标账户余额更新: {} 新余额: {}", event.getToAccountId(), newBalance);
    }
    
    /**
     * 获取账户交易历史
     */
    public java.util.List<Transaction> getAccountTransactions(String accountId) {
        return transactionRepository.findByFromAccountIdOrToAccountIdOrderByCreatedAtDesc(accountId, accountId);
    }
    
    /**
     * 获取账户余额
     */
    public BigDecimal getAccountBalance(String accountId) {
        Account account = accountRepository.findByAccountId(accountId)
                .orElseThrow(() -> new RuntimeException("账户不存在: " + accountId));
        return account.getBalance();
    }
} 