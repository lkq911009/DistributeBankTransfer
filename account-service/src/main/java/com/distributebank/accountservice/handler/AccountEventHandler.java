package com.distributebank.accountservice.handler;

import com.distributebank.common.event.TransferEvent;
import com.distributebank.accountservice.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 账户事件处理器
 * 负责处理Kafka中的账户相关事件
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AccountEventHandler {
    
    private final AccountService accountService;
    private final KafkaTemplate<String, TransferEvent> kafkaTemplate;
    
    private static final String TRANSFER_TOPIC = "transfer-events";
    
    /**
     * 处理转账创建事件，执行扣款逻辑
     */
    @KafkaListener(topics = "transfer-events", groupId = "account-service")
    public void handleTransferEvent(TransferEvent event) {
        if (event.getEventType() != TransferEvent.EventType.TRANSFER_CREATED) {
            return;
        }
        
        log.info("处理转账事件: {}", event.getTransactionId());
        
        try {
            // 执行扣款
            AccountService.DeductResult result = accountService.deductBalance(
                event.getFromAccountId(), 
                event.getAmount(), 
                event.getTransactionId()
            );
            
            if (result.isSuccess()) {
                // 扣款成功，发送处理完成事件
                TransferEvent processedEvent = TransferEvent.builder()
                        .transactionId(event.getTransactionId())
                        .fromAccountId(event.getFromAccountId())
                        .toAccountId(event.getToAccountId())
                        .amount(event.getAmount())
                        .fromBalanceAfter(result.getNewBalance())
                        .eventType(TransferEvent.EventType.TRANSFER_PROCESSED)
                        .timestamp(LocalDateTime.now())
                        .build();
                
                kafkaTemplate.send(TRANSFER_TOPIC, event.getTransactionId(), processedEvent);
                log.info("转账处理完成: {} 新余额: {}", event.getTransactionId(), result.getNewBalance());
            } else {
                // 扣款失败，发送失败事件
                TransferEvent failedEvent = TransferEvent.builder()
                        .transactionId(event.getTransactionId())
                        .fromAccountId(event.getFromAccountId())
                        .toAccountId(event.getToAccountId())
                        .amount(event.getAmount())
                        .eventType(TransferEvent.EventType.CLEARING_FAILED)
                        .timestamp(LocalDateTime.now())
                        .build();
                
                kafkaTemplate.send(TRANSFER_TOPIC, event.getTransactionId(), failedEvent);
                log.error("转账处理失败: {} 原因: {}", event.getTransactionId(), result.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("处理转账事件异常: {}", event.getTransactionId(), e);
            
            // 发送失败事件
            TransferEvent failedEvent = TransferEvent.builder()
                    .transactionId(event.getTransactionId())
                    .fromAccountId(event.getFromAccountId())
                    .toAccountId(event.getToAccountId())
                    .amount(event.getAmount())
                    .eventType(TransferEvent.EventType.CLEARING_FAILED)
                    .timestamp(LocalDateTime.now())
                    .build();
            
            kafkaTemplate.send(TRANSFER_TOPIC, event.getTransactionId(), failedEvent);
        }
    }
} 