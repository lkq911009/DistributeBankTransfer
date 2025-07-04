package com.distributebank.accountservice.service;

import com.distributebank.common.model.Account;
import com.distributebank.accountservice.repository.AccountRepository;
import com.distributebank.accountservice.dto.CreateAccountRequest;
import com.distributebank.accountservice.dto.DepositRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 账户服务业务逻辑类
 * 负责账户余额管理和扣款逻辑，使用Redis Lua脚本保证原子性
 * 结合乐观锁和延时双删策略保证数据一致性
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {
    
    private final AccountRepository accountRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final CacheService cacheService;
    
    private static final String ACCOUNT_BALANCE_PREFIX = "account:balance:";
    private static final String TRANSACTION_PROCESSED_PREFIX = "transaction:processed:";
    
    // Redis Lua脚本：原子扣款，包含幂等检查
    private static final String DEDUCT_BALANCE_SCRIPT = 
        "local balanceKey = KEYS[1] " +
        "local processedKey = KEYS[2] " +
        "local amount = tonumber(ARGV[1]) " +
        "local transactionId = ARGV[2] " +
        " " +
        "-- 检查是否已处理过 " +
        "if redis.call('EXISTS', processedKey) == 1 then " +
        "    return {0, '交易已处理过'} " +
        "end " +
        " " +
        "-- 获取当前余额 " +
        "local currentBalance = tonumber(redis.call('GET', balanceKey)) " +
        "if not currentBalance then " +
        "    return {0, '账户余额不存在'} " +
        "end " +
        " " +
        "-- 检查余额是否足够 " +
        "if currentBalance < amount then " +
        "    return {0, '余额不足'} " +
        "end " +
        " " +
        "-- 执行扣款 " +
        "local newBalance = currentBalance - amount " +
        "redis.call('SET', balanceKey, tostring(newBalance)) " +
        " " +
        "-- 标记交易已处理 " +
        "redis.call('SETEX', processedKey, 86400, '1') " +
        " " +
        "return {1, tostring(newBalance)}";
    
    /**
     * 获取账户余额（优先从Redis获取）
     */
    public BigDecimal getBalance(String accountId) {
        // 先从Redis获取
        String cachedBalance = cacheService.getCache(accountId);
        if (cachedBalance != null) {
            return new BigDecimal(cachedBalance);
        }
        
        // Redis没有，从数据库获取并缓存
        Account account = accountRepository.findByAccountId(accountId)
                .orElseThrow(() -> new RuntimeException("账户不存在: " + accountId));
        
        cacheService.setCache(accountId, account.getBalance().toString());
        return account.getBalance();
    }
    
    /**
     * 查询账户信息
     */
    public Object getAccount(String accountId) {
        Account account = accountRepository.findByAccountId(accountId)
                .orElseThrow(() -> new RuntimeException("账户不存在: " + accountId));
        
        BigDecimal redisBalance = getBalance(accountId);
        
        return new AccountInfo(
                account.getAccountId(),
                account.getAccountName(),
                account.getBankCode(),
                account.getBalance(),
                redisBalance,
                account.getStatus().name()
        );
    }
    
    /**
     * 创建账户
     */
    @Transactional
    public String createAccount(CreateAccountRequest request) {
        Account account = Account.builder()
                .accountId(request.getAccountId())
                .accountName(request.getAccountName())
                .bankCode(request.getBankCode())
                .balance(request.getInitialBalance() != null ? request.getInitialBalance() : BigDecimal.ZERO)
                .status(Account.AccountStatus.ACTIVE)
                .version(0L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        accountRepository.save(account);
        
        // 同步余额到Redis
        cacheService.setCache(account.getAccountId(), account.getBalance().toString());
        
        log.info("创建账户成功: {}", account.getAccountId());
        return account.getAccountId();
    }
    
    /**
     * 充值账户（乐观锁 + 延时双删）
     */
    @Transactional
    public BigDecimal deposit(String accountId, BigDecimal amount) {
        int maxRetries = 3;
        long delayMs = 500; // 延时500ms
        
        for (int i = 0; i < maxRetries; i++) {
            try {
                // 1. 先删除缓存
                cacheService.deleteCacheFirst(accountId);
                
                // 2. 乐观锁更新数据库
                Account account = accountRepository.findByAccountId(accountId)
                        .orElseThrow(() -> new RuntimeException("账户不存在: " + accountId));
                
                BigDecimal newBalance = account.getBalance().add(amount);
                account.setBalance(newBalance);
                account.setUpdatedAt(LocalDateTime.now());
                accountRepository.save(account);
                
                // 3. 延时删除缓存
                cacheService.scheduleDelayedDelete(accountId, delayMs);
                
                log.info("账户充值成功: {} 金额: {} 新余额: {}", accountId, amount, newBalance);
                return newBalance;
                
            } catch (ObjectOptimisticLockingFailureException e) {
                if (i == maxRetries - 1) {
                    log.error("账户充值失败，乐观锁冲突: {}", accountId, e);
                    throw new RuntimeException("并发冲突，请重试");
                }
                // 重试前也删除缓存
                cacheService.deleteCache(accountId);
                log.warn("账户充值乐观锁冲突，重试第{}次: {}", i + 1, accountId);
                try {
                    Thread.sleep(10 * (i + 1)); // 递增延时
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("充值操作被中断");
                }
            } catch (Exception e) {
                log.error("账户充值异常: {}", accountId, e);
                throw new RuntimeException("充值失败: " + e.getMessage());
            }
        }
        
        throw new RuntimeException("充值失败，重试次数已用完");
    }
    
    /**
     * 使用Redis Lua脚本执行原子扣款
     */
    public DeductResult deductBalance(String accountId, BigDecimal amount, String transactionId) {
        String balanceKey = ACCOUNT_BALANCE_PREFIX + accountId;
        String processedKey = TRANSACTION_PROCESSED_PREFIX + transactionId;
        
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setScriptText(DEDUCT_BALANCE_SCRIPT);
        script.setResultType(List.class);
        
        List<String> keys = Arrays.asList(balanceKey, processedKey);
        List<String> args = Arrays.asList(amount.toString(), transactionId);
        
        List<Object> result = redisTemplate.execute(script, keys, args.toArray());
        
        if (result == null || result.isEmpty()) {
            return new DeductResult(false, "扣款执行失败", null);
        }
        
        Long success = (Long) result.get(0);
        if (success == 1) {
            String newBalanceStr = (String) result.get(1);
            return new DeductResult(true, "扣款成功", new BigDecimal(newBalanceStr));
        } else {
            String errorMsg = (String) result.get(1);
            return new DeductResult(false, errorMsg, null);
        }
    }
    
    /**
     * 扣款结果类
     */
    public static class DeductResult {
        private boolean success;
        private String errorMessage;
        private BigDecimal newBalance;
        
        public DeductResult(boolean success, String errorMessage, BigDecimal newBalance) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.newBalance = newBalance;
        }
        
        // getters
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        public BigDecimal getNewBalance() { return newBalance; }
    }
    
    /**
     * 账户信息响应类
     */
    public static class AccountInfo {
        private String accountId;
        private String accountName;
        private String bankCode;
        private BigDecimal dbBalance;
        private BigDecimal redisBalance;
        private String status;
        
        public AccountInfo(String accountId, String accountName, String bankCode, 
                          BigDecimal dbBalance, BigDecimal redisBalance, String status) {
            this.accountId = accountId;
            this.accountName = accountName;
            this.bankCode = bankCode;
            this.dbBalance = dbBalance;
            this.redisBalance = redisBalance;
            this.status = status;
        }
        
        // getters
        public String getAccountId() { return accountId; }
        public String getAccountName() { return accountName; }
        public String getBankCode() { return bankCode; }
        public BigDecimal getDbBalance() { return dbBalance; }
        public BigDecimal getRedisBalance() { return redisBalance; }
        public String getStatus() { return status; }
    }
} 