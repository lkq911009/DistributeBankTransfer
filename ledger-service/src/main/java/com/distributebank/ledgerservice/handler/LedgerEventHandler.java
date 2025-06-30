package com.distributebank.ledgerservice.handler;

import com.distributebank.common.event.TransferEvent;
import com.distributebank.ledgerservice.service.LedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 账本事件处理器
 * 负责处理Kafka中的账本相关事件
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LedgerEventHandler {
    
    private final LedgerService ledgerService;
    
    /**
     * 处理清算成功事件，更新交易状态和目标账户余额
     */
    @KafkaListener(topics = "transfer-events", groupId = "ledger-service")
    public void handleTransferEvent(TransferEvent event) {
        if (event.getEventType() != TransferEvent.EventType.CLEARING_SUCCESS) {
            return;
        }
        
        log.info("处理清算成功事件: {}", event.getTransactionId());
        
        try {
            ledgerService.processClearingSuccess(event);
        } catch (Exception e) {
            log.error("处理清算成功事件异常: {}", event.getTransactionId(), e);
        }
    }
    
    /**
     * 处理清算失败事件，更新交易状态
     */
    @KafkaListener(topics = "transfer-events", groupId = "ledger-service-failed")
    public void handleClearingFailedEvent(TransferEvent event) {
        if (event.getEventType() != TransferEvent.EventType.CLEARING_FAILED) {
            return;
        }
        
        log.info("处理清算失败事件: {}", event.getTransactionId());
        
        try {
            ledgerService.processClearingFailed(event);
        } catch (Exception e) {
            log.error("处理清算失败事件异常: {}", event.getTransactionId(), e);
        }
    }
} 