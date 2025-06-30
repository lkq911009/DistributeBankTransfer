package com.distributebank.notificationservice.handler;

import com.distributebank.common.event.TransferEvent;
import com.distributebank.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 通知事件处理器
 * 负责处理Kafka中的通知相关事件
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventHandler {
    
    private final NotificationService notificationService;
    
    /**
     * 消费清算成功事件，发送成功通知
     */
    @KafkaListener(topics = "transfer-events", groupId = "notification-service-success")
    public void handleClearingSuccessEvent(TransferEvent event) {
        if (event.getEventType() != TransferEvent.EventType.CLEARING_SUCCESS) {
            return;
        }
        
        log.info("处理清算成功通知: {}", event.getTransactionId());
        
        try {
            notificationService.sendSuccessNotification(event);
        } catch (Exception e) {
            log.error("发送成功通知异常: {}", event.getTransactionId(), e);
        }
    }
    
    /**
     * 消费清算失败事件，发送失败通知
     */
    @KafkaListener(topics = "transfer-events", groupId = "notification-service-failed")
    public void handleClearingFailedEvent(TransferEvent event) {
        if (event.getEventType() != TransferEvent.EventType.CLEARING_FAILED) {
            return;
        }
        
        log.info("处理清算失败通知: {}", event.getTransactionId());
        
        try {
            notificationService.sendFailureNotification(event);
        } catch (Exception e) {
            log.error("发送失败通知异常: {}", event.getTransactionId(), e);
        }
    }
} 