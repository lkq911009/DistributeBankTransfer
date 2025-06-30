package com.distributebank.notificationservice.controller;

import com.distributebank.common.dto.Result;
import com.distributebank.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 通知控制器
 * 提供通知相关的REST API接口
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {
    
    private final NotificationService notificationService;
    
    /**
     * 获取交易通知记录
     * GET /api/notifications/{transactionId}
     */
    @GetMapping("/{transactionId}")
    public Result<NotificationService.NotificationRecord> getNotificationRecord(@PathVariable String transactionId) {
        log.info("获取交易通知记录: {}", transactionId);
        try {
            NotificationService.NotificationRecord record = notificationService.getNotificationRecord(transactionId);
            if (record != null) {
                return Result.success(record);
            } else {
                return Result.error(404, "通知记录不存在");
            }
        } catch (Exception e) {
            log.error("获取通知记录失败", e);
            return Result.error(500, "获取通知记录失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取所有通知记录
     * GET /api/notifications
     */
    @GetMapping
    public Result<java.util.List<NotificationService.NotificationRecord>> getAllNotificationRecords() {
        log.info("获取所有通知记录");
        try {
            java.util.List<NotificationService.NotificationRecord> records = 
                    notificationService.getAllNotificationRecords();
            return Result.success("获取通知记录成功", records);
        } catch (Exception e) {
            log.error("获取所有通知记录失败", e);
            return Result.error(500, "获取通知记录失败: " + e.getMessage());
        }
    }
} 