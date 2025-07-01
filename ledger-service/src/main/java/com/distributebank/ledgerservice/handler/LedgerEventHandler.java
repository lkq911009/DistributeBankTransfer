package com.distributebank.ledgerservice.handler;

import com.distributebank.common.event.TransferEvent;
import com.distributebank.ledgerservice.service.LedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 账本服务事件处理器
 * 监听转账事件，更新账户余额和交易记录
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LedgerEventHandler {
    
    private final LedgerService ledgerService;
    
    /**
     * 监听转账事件
     * 处理清算成功后的余额更新
     */
    @KafkaListener(topics = "transfer-events", groupId = "ledger-service")
    public void handleTransferEvent(TransferEvent event) {
        log.info("收到转账事件: {}", event.getTransactionId());
        
        try {
            ledgerService.processTransferEvent(event);
            log.info("转账事件处理完成: {}", event.getTransactionId());
        } catch (Exception e) {
            log.error("转账事件处理失败: {}", event.getTransactionId(), e);
            // 可以考虑发送失败事件到死信队列
        }
    }
} 