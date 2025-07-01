package com.distributebank.accountservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 缓存管理服务
 * 实现延时双删策略，保证缓存一致性
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    private static final String ACCOUNT_BALANCE_PREFIX = "account:balance:";
    
    /**
     * 先删除缓存
     */
    public void deleteCacheFirst(String accountId) {
        String balanceKey = ACCOUNT_BALANCE_PREFIX + accountId;
        redisTemplate.delete(balanceKey);
        log.debug("先删除缓存: {}", balanceKey);
    }
    
    /**
     * 延时删除缓存
     */
    @Async("cacheTaskExecutor")
    public CompletableFuture<Void> scheduleDelayedDelete(String accountId, long delayMs) {
        return CompletableFuture.runAsync(() -> {
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
                // 可以考虑重试机制
                retryDelayedDelete(accountId, delayMs);
            }
        });
    }
    
    /**
     * 重试延时删除
     */
    private void retryDelayedDelete(String accountId, long delayMs) {
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(delayMs * 2); // 重试延时加倍
                String balanceKey = ACCOUNT_BALANCE_PREFIX + accountId;
                redisTemplate.delete(balanceKey);
                log.info("重试延时删除缓存成功: {}", balanceKey);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("重试延时删除被中断: {}", accountId);
            } catch (Exception e) {
                log.error("重试延时删除缓存失败: {}", accountId, e);
            }
        });
    }
    
    /**
     * 设置缓存
     */
    public void setCache(String accountId, String value) {
        String balanceKey = ACCOUNT_BALANCE_PREFIX + accountId;
        redisTemplate.opsForValue().set(balanceKey, value, 24, TimeUnit.HOURS);
        log.debug("设置缓存: {} = {}", balanceKey, value);
    }
    
    /**
     * 获取缓存
     */
    public String getCache(String accountId) {
        String balanceKey = ACCOUNT_BALANCE_PREFIX + accountId;
        return redisTemplate.opsForValue().get(balanceKey);
    }
    
    /**
     * 删除缓存
     */
    public void deleteCache(String accountId) {
        String balanceKey = ACCOUNT_BALANCE_PREFIX + accountId;
        redisTemplate.delete(balanceKey);
        log.debug("删除缓存: {}", balanceKey);
    }
} 