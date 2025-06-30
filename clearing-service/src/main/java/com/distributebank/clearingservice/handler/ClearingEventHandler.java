package com.distributebank.clearingservice.handler;

import com.distributebank.common.event.TransferEvent;
import com.distributebank.clearingservice.service.ClearingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 清算事件处理器
 * 负责处理Kafka中的清算相关事件
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClearingEventHandler {
    
    private final ClearingService clearingService;
    
    /**
     * 处理转账处理完成事件，执行清算逻辑
     */
    @KafkaListener(topics = "transfer-events", groupId = "clearing-service")
    public void handleTransferEvent(TransferEvent event) {
        if (event.getEventType() != TransferEvent.EventType.TRANSFER_PROCESSED) {
            return;
        }
        
        log.info("开始清算处理: {}", event.getTransactionId());
        
        try {
            clearingService.processClearing(event);
        } catch (Exception e) {
            log.error("清算处理异常: {}", event.getTransactionId(), e);
        }
    }
} 