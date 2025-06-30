package com.distributebank.reconciliationservice.service;

import com.distributebank.common.model.Account;
import com.distributebank.reconciliationservice.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

/**
 * 对账服务业务逻辑类
 * 负责Redis和数据库之间的余额比对与补偿交易
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReconciliationService {
    
    private final AccountRepository accountRepository;
    private final RedisTemplate<String, String> redisTemplate;
    
    private static final String ACCOUNT_BALANCE_PREFIX = "account:balance:";
    
    /**
     * 定时对账任务，每5分钟执行一次
     * 比对Redis和数据库中的账户余额
     */
    @Scheduled(fixedRate = 300000) // 5分钟
    public void scheduledReconciliation() {
        log.info("开始执行定时对账任务");
        
        try {
            List<Account> accounts = accountRepository.findAll();
            
            for (Account account : accounts) {
                reconcileAccountBalance(account);
            }
            
            log.info("定时对账任务执行完成，共处理{}个账户", accounts.size());
        } catch (Exception e) {
            log.error("定时对账任务执行异常", e);
        }
    }
    
    /**
     * 对单个账户进行余额对账
     */
    public void reconcileAccountBalance(Account account) {
        String accountId = account.getAccountId();
        BigDecimal dbBalance = account.getBalance();
        
        // 获取Redis中的余额
        String balanceKey = ACCOUNT_BALANCE_PREFIX + accountId;
        String redisBalanceStr = redisTemplate.opsForValue().get(balanceKey);
        
        if (redisBalanceStr == null) {
            // Redis中没有余额记录，同步数据库余额到Redis
            redisTemplate.opsForValue().set(balanceKey, dbBalance.toString());
            log.info("账户{}余额同步到Redis: {}", accountId, dbBalance);
            return;
        }
        
        BigDecimal redisBalance = new BigDecimal(redisBalanceStr);
        
        // 比较Redis和数据库余额
        if (dbBalance.compareTo(redisBalance) != 0) {
            log.warn("账户{}余额不一致 - 数据库: {}, Redis: {}", accountId, dbBalance, redisBalance);
            
            // 记录差异
            recordBalanceDifference(accountId, dbBalance, redisBalance);
            
            // 可以选择自动修复（以数据库为准）
            autoFixBalance(accountId, dbBalance);
        } else {
            log.debug("账户{}余额一致: {}", accountId, dbBalance);
        }
    }
    
    /**
     * 记录余额差异
     */
    private void recordBalanceDifference(String accountId, BigDecimal dbBalance, BigDecimal redisBalance) {
        String diffKey = "balance:diff:" + accountId;
        String diffInfo = String.format("DB:%s,Redis:%s,Time:%s", 
                dbBalance, redisBalance, java.time.LocalDateTime.now());
        
        redisTemplate.opsForValue().set(diffKey, diffInfo, Duration.ofSeconds(86400)); // 24小时过期
        log.warn("记录账户{}余额差异: {}", accountId, diffInfo);
    }
    
    /**
     * 自动修复余额（以数据库为准）
     */
    private void autoFixBalance(String accountId, BigDecimal dbBalance) {
        String balanceKey = ACCOUNT_BALANCE_PREFIX + accountId;
        redisTemplate.opsForValue().set(balanceKey, dbBalance.toString());
        log.info("自动修复账户{}余额: {}", accountId, dbBalance);
    }
    
    /**
     * 手动触发对账
     */
    public ReconciliationResult manualReconciliation(String accountId) {
        log.info("手动触发账户{}对账", accountId);
        
        try {
            Account account = accountRepository.findByAccountId(accountId)
                    .orElseThrow(() -> new RuntimeException("账户不存在: " + accountId));
            
            BigDecimal dbBalance = account.getBalance();
            String balanceKey = ACCOUNT_BALANCE_PREFIX + accountId;
            String redisBalanceStr = redisTemplate.opsForValue().get(balanceKey);
            
            BigDecimal redisBalance = redisBalanceStr != null ? new BigDecimal(redisBalanceStr) : BigDecimal.ZERO;
            boolean isConsistent = dbBalance.compareTo(redisBalance) == 0;
            
            if (!isConsistent) {
                // 自动修复
                autoFixBalance(accountId, dbBalance);
            }
            
            return new ReconciliationResult(accountId, dbBalance, redisBalance, isConsistent);
        } catch (Exception e) {
            log.error("手动对账失败", e);
            throw new RuntimeException("对账失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取所有账户的对账状态
     */
    public List<ReconciliationResult> getAllAccountReconciliationStatus() {
        List<Account> accounts = accountRepository.findAll();
        
        return accounts.stream()
                .map(account -> {
                    String accountId = account.getAccountId();
                    BigDecimal dbBalance = account.getBalance();
                    String balanceKey = ACCOUNT_BALANCE_PREFIX + accountId;
                    String redisBalanceStr = redisTemplate.opsForValue().get(balanceKey);
                    BigDecimal redisBalance = redisBalanceStr != null ? new BigDecimal(redisBalanceStr) : BigDecimal.ZERO;
                    boolean isConsistent = dbBalance.compareTo(redisBalance) == 0;
                    
                    return new ReconciliationResult(accountId, dbBalance, redisBalance, isConsistent);
                })
                .toList();
    }
    
    /**
     * 对账结果类
     */
    public static class ReconciliationResult {
        private String accountId;
        private BigDecimal dbBalance;
        private BigDecimal redisBalance;
        private boolean isConsistent;
        
        public ReconciliationResult(String accountId, BigDecimal dbBalance, BigDecimal redisBalance, boolean isConsistent) {
            this.accountId = accountId;
            this.dbBalance = dbBalance;
            this.redisBalance = redisBalance;
            this.isConsistent = isConsistent;
        }
        
        // getters
        public String getAccountId() { return accountId; }
        public BigDecimal getDbBalance() { return dbBalance; }
        public BigDecimal getRedisBalance() { return redisBalance; }
        public boolean isConsistent() { return isConsistent; }
    }
} 