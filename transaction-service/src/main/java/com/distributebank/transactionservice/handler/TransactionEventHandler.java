package com.distributebank.transactionservice.handler;

import com.distributebank.common.event.TransferEvent;
import com.distributebank.transactionservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 转账事件处理器
 * 负责处理Kafka中的转账相关事件
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventHandler {
    
    private final TransactionService transactionService;
    
    /**
     * 处理转账状态更新事件
     */
    @KafkaListener(topics = "transfer-events", groupId = "transaction-service-status")
    public void handleTransferStatusEvent(TransferEvent event) {
        log.info("收到转账状态事件: {} - {}", event.getTransactionId(), event.getEventType());
        
        try {
            switch (event.getEventType()) {
                case TRANSFER_PROCESSED:
                    transactionService.updateTransactionStatus(event.getTransactionId(), "PROCESSING");
                    break;
                case CLEARING_SUCCESS:
                    transactionService.updateTransactionStatus(event.getTransactionId(), "SUCCESS");
                    break;
                case CLEARING_FAILED:
                    transactionService.updateTransactionStatus(event.getTransactionId(), "FAILED");
                    break;
                default:
                    log.debug("忽略事件类型: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("处理转账状态事件异常: {}", event.getTransactionId(), e);
        }
    }
} 