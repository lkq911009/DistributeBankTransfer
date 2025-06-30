package com.distributebank.reconciliationservice.controller;

import com.distributebank.common.dto.Result;
import com.distributebank.reconciliationservice.service.ReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 对账控制器
 * 提供对账相关的REST API接口
 */
@RestController
@RequestMapping("/api/reconciliation")
@RequiredArgsConstructor
@Slf4j
public class ReconciliationController {
    
    private final ReconciliationService reconciliationService;
    
    /**
     * 手动触发单个账户对账
     * POST /api/reconciliation/{accountId}
     */
    @PostMapping("/{accountId}")
    public Result<ReconciliationService.ReconciliationResult> manualReconciliation(@PathVariable String accountId) {
        log.info("手动触发账户对账: {}", accountId);
        try {
            ReconciliationService.ReconciliationResult result = reconciliationService.manualReconciliation(accountId);
            return Result.success("对账完成", result);
        } catch (Exception e) {
            log.error("手动对账失败", e);
            return Result.error(500, "对账失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取所有账户的对账状态
     * GET /api/reconciliation/status
     */
    @GetMapping("/status")
    public Result<java.util.List<ReconciliationService.ReconciliationResult>> getAllReconciliationStatus() {
        log.info("获取所有账户对账状态");
        try {
            java.util.List<ReconciliationService.ReconciliationResult> results = 
                    reconciliationService.getAllAccountReconciliationStatus();
            return Result.success("获取对账状态成功", results);
        } catch (Exception e) {
            log.error("获取对账状态失败", e);
            return Result.error(500, "获取对账状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 立即执行定时对账任务
     * POST /api/reconciliation/execute
     */
    @PostMapping("/execute")
    public Result<String> executeReconciliation() {
        log.info("立即执行对账任务");
        try {
            reconciliationService.scheduledReconciliation();
            return Result.success("对账任务执行完成");
        } catch (Exception e) {
            log.error("执行对账任务失败", e);
            return Result.error(500, "执行对账任务失败: " + e.getMessage());
        }
    }
} 