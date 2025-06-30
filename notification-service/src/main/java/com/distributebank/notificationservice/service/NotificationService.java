package com.distributebank.notificationservice.service;

import com.distributebank.common.event.TransferEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通知服务业务逻辑类
 * 负责推送交易结果通知
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    
    // 模拟通知记录存储
    private final ConcurrentHashMap<String, NotificationRecord> notificationRecords = new ConcurrentHashMap<>();
    
    /**
     * 发送成功通知（供事件处理器调用）
     */
    public void sendSuccessNotification(TransferEvent event) {
        log.info("处理清算成功通知: {}", event.getTransactionId());
        
        try {
            // 模拟发送通知（实际项目中可能是短信、邮件、推送等）
            String message = String.format(
                    "转账成功通知 - 交易ID: %s, 从账户: %s, 到账户: %s, 金额: %s, 时间: %s",
                    event.getTransactionId(),
                    event.getFromAccountId(),
                    event.getToAccountId(),
                    event.getAmount(),
                    LocalDateTime.now()
            );
            
            log.info("发送成功通知: {}", message);
            
            // 记录通知
            recordNotification(event.getTransactionId(), "SUCCESS", "转账成功");
            
            log.info("成功通知已发送: {}", event.getTransactionId());
            
            // 这里可以集成实际的通知渠道
            // 例如：短信服务、邮件服务、推送服务等
        } catch (Exception e) {
            log.error("发送成功通知异常", e);
        }
    }
    
    /**
     * 发送失败通知（供事件处理器调用）
     */
    public void sendFailureNotification(TransferEvent event) {
        log.info("处理清算失败通知: {}", event.getTransactionId());
        
        try {
            // 模拟发送通知
            String message = String.format(
                    "转账失败通知 - 交易ID: %s, 从账户: %s, 到账户: %s, 金额: %s, 时间: %s",
                    event.getTransactionId(),
                    event.getFromAccountId(),
                    event.getToAccountId(),
                    event.getAmount(),
                    LocalDateTime.now()
            );
            
            log.error("发送失败通知: {}", message);
            
            // 记录通知
            recordNotification(event.getTransactionId(), "FAILED", "转账失败");
            
            log.info("失败通知已发送: {}", event.getTransactionId());
            
            // 这里可以集成实际的通知渠道
        } catch (Exception e) {
            log.error("发送失败通知异常", e);
        }
    }
    
    /**
     * 记录通知
     */
    private void recordNotification(String transactionId, String status, String message) {
        NotificationRecord record = new NotificationRecord(
                transactionId,
                status,
                message,
                LocalDateTime.now()
        );
        
        notificationRecords.put(transactionId, record);
    }
    
    /**
     * 获取通知记录
     */
    public NotificationRecord getNotificationRecord(String transactionId) {
        return notificationRecords.get(transactionId);
    }
    
    /**
     * 获取所有通知记录
     */
    public java.util.List<NotificationRecord> getAllNotificationRecords() {
        return notificationRecords.values().stream().toList();
    }
    
    /**
     * 通知记录类
     */
    public static class NotificationRecord {
        private String transactionId;
        private String status;
        private String message;
        private LocalDateTime timestamp;
        
        public NotificationRecord(String transactionId, String status, String message, LocalDateTime timestamp) {
            this.transactionId = transactionId;
            this.status = status;
            this.message = message;
            this.timestamp = timestamp;
        }
        
        // getters
        public String getTransactionId() { return transactionId; }
        public String getStatus() { return status; }
        public String getMessage() { return message; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
} 