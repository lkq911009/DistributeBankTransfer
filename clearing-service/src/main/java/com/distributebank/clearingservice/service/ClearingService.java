package com.distributebank.clearingservice.service;

import com.distributebank.common.event.TransferEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

/**
 * 清算服务业务逻辑类
 * 模拟银联或SWIFT清算机构，处理跨行转账清算
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClearingService {
    
    private final KafkaTemplate<String, TransferEvent> kafkaTemplate;
    
    private static final String TRANSFER_TOPIC = "transfer-events";
    private static final Random random = new Random();
    
    /**
     * 处理清算逻辑（供事件处理器调用）
     */
    public void processClearing(TransferEvent event) {
        log.info("开始清算处理: {}", event.getTransactionId());
        
        try {
            // 模拟清算处理时间（1-3秒）
            Thread.sleep(1000 + random.nextInt(2000));
            
            // 模拟清算成功率（95%成功，5%失败）
            boolean clearingSuccess = random.nextDouble() > 0.05;
            
            if (clearingSuccess) {
                // 清算成功
                TransferEvent successEvent = TransferEvent.builder()
                        .transactionId(event.getTransactionId())
                        .fromAccountId(event.getFromAccountId())
                        .toAccountId(event.getToAccountId())
                        .amount(event.getAmount())
                        .fromBalanceAfter(event.getFromBalanceAfter())
                        .eventType(TransferEvent.EventType.CLEARING_SUCCESS)
                        .timestamp(LocalDateTime.now())
                        .build();
                
                kafkaTemplate.send(TRANSFER_TOPIC, event.getTransactionId(), successEvent);
                log.info("清算成功: {}", event.getTransactionId());
            } else {
                // 清算失败
                TransferEvent failedEvent = TransferEvent.builder()
                        .transactionId(event.getTransactionId())
                        .fromAccountId(event.getFromAccountId())
                        .toAccountId(event.getToAccountId())
                        .amount(event.getAmount())
                        .fromBalanceAfter(event.getFromBalanceAfter())
                        .eventType(TransferEvent.EventType.CLEARING_FAILED)
                        .timestamp(LocalDateTime.now())
                        .build();
                
                kafkaTemplate.send(TRANSFER_TOPIC, event.getTransactionId(), failedEvent);
                log.error("清算失败: {}", event.getTransactionId());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("清算处理被中断", e);
        } catch (Exception e) {
            log.error("清算处理异常", e);
            
            // 发送清算失败事件
            TransferEvent failedEvent = TransferEvent.builder()
                    .transactionId(event.getTransactionId())
                    .fromAccountId(event.getFromAccountId())
                    .toAccountId(event.getToAccountId())
                    .amount(event.getAmount())
                    .fromBalanceAfter(event.getFromBalanceAfter())
                    .eventType(TransferEvent.EventType.CLEARING_FAILED)
                    .timestamp(LocalDateTime.now())
                    .build();
            
            kafkaTemplate.send(TRANSFER_TOPIC, event.getTransactionId(), failedEvent);
        }
    }
} 