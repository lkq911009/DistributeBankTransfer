package com.distributebank.transactionservice.controller;

import com.distributebank.common.dto.Result;
import com.distributebank.common.dto.TransferRequest;
import com.distributebank.transactionservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 转账控制器
 * 提供转账相关的REST API接口
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {
    
    private final TransactionService transactionService;
    
    /**
     * 发起转账
     * POST /api/transactions/transfer
     */
    @PostMapping("/transfer")
    public Result<String> transfer(@Valid @RequestBody TransferRequest request) {
        log.info("收到转账请求: {}", request);
        try {
            String transactionId = transactionService.createTransfer(request);
            return Result.success("转账请求已提交", transactionId);
        } catch (Exception e) {
            log.error("转账失败", e);
            return Result.error(500, "转账失败: " + e.getMessage());
        }
    }
    
    /**
     * 查询转账状态
     * GET /api/transactions/{transactionId}
     */
    @GetMapping("/{transactionId}")
    public Result<Object> getTransactionStatus(@PathVariable String transactionId) {
        log.info("查询转账状态: {}", transactionId);
        try {
            Object status = transactionService.getTransactionStatus(transactionId);
            return Result.success(status);
        } catch (Exception e) {
            log.error("查询转账状态失败", e);
            return Result.error(500, "查询失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量转账（企业发工资场景）
     * POST /api/transactions/batch-transfer
     */
    @PostMapping("/batch-transfer")
    public Result<String> batchTransfer(@Valid @RequestBody TransactionService.BatchTransferRequest request) {
        log.info("收到批量转账请求: 从账户{}向{}个账户转账", 
                request.getFromAccountId(), request.getTransfers().size());
        try {
            String batchId = transactionService.createBatchTransfer(request);
            return Result.success("批量转账请求已提交", batchId);
        } catch (Exception e) {
            log.error("批量转账失败", e);
            return Result.error(500, "批量转账失败: " + e.getMessage());
        }
    }
} 