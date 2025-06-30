package com.distributebank.accountservice.controller;

import com.distributebank.common.dto.Result;
import com.distributebank.accountservice.service.AccountService;
import com.distributebank.accountservice.dto.CreateAccountRequest;
import com.distributebank.accountservice.dto.DepositRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 账户控制器
 * 提供账户相关的REST API接口
 */
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {
    
    private final AccountService accountService;
    
    /**
     * 查询账户余额
     * GET /api/accounts/{accountId}/balance
     */
    @GetMapping("/{accountId}/balance")
    public Result<BigDecimal> getBalance(@PathVariable String accountId) {
        log.info("查询账户余额: {}", accountId);
        try {
            BigDecimal balance = accountService.getBalance(accountId);
            return Result.success(balance);
        } catch (Exception e) {
            log.error("查询账户余额失败", e);
            return Result.error(500, "查询失败: " + e.getMessage());
        }
    }
    
    /**
     * 查询账户信息
     * GET /api/accounts/{accountId}
     */
    @GetMapping("/{accountId}")
    public Result<Object> getAccount(@PathVariable String accountId) {
        log.info("查询账户信息: {}", accountId);
        try {
            Object account = accountService.getAccount(accountId);
            return Result.success(account);
        } catch (Exception e) {
            log.error("查询账户信息失败", e);
            return Result.error(500, "查询失败: " + e.getMessage());
        }
    }
    
    /**
     * 创建账户
     * POST /api/accounts
     */
    @PostMapping
    public Result<String> createAccount(@RequestBody CreateAccountRequest request) {
        log.info("创建账户: {}", request);
        try {
            String accountId = accountService.createAccount(request);
            return Result.success("账户创建成功", accountId);
        } catch (Exception e) {
            log.error("创建账户失败", e);
            return Result.error(500, "创建失败: " + e.getMessage());
        }
    }
    
    /**
     * 充值账户
     * POST /api/accounts/{accountId}/deposit
     */
    @PostMapping("/{accountId}/deposit")
    public Result<BigDecimal> deposit(@PathVariable String accountId, @RequestBody DepositRequest request) {
        log.info("账户充值: {} 金额: {}", accountId, request.getAmount());
        try {
            BigDecimal newBalance = accountService.deposit(accountId, request.getAmount());
            return Result.success("充值成功", newBalance);
        } catch (Exception e) {
            log.error("账户充值失败", e);
            return Result.error(500, "充值失败: " + e.getMessage());
        }
    }
} 