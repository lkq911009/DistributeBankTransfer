package com.distributebank.common.event;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 转账事件类
 * 用于Kafka消息传递，包含转账的完整信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferEvent {
    
    /**
     * 交易ID，用于幂等控制
     */
    private String transactionId;
    
    /**
     * 源账户ID
     */
    private String fromAccountId;
    
    /**
     * 目标账户ID
     */
    private String toAccountId;
    
    /**
     * 转账金额
     */
    private BigDecimal amount;
    
    /**
     * 源账户扣款后余额
     */
    private BigDecimal fromBalanceAfter;
    
    /**
     * 目标账户收款后余额
     */
    private BigDecimal toBalanceAfter;
    
    /**
     * 事件类型：TRANSFER_CREATED-转账创建，TRANSFER_PROCESSED-转账处理完成
     */
    private EventType eventType;
    
    /**
     * 事件时间戳
     */
    private LocalDateTime timestamp;
    
    /**
     * 事件类型枚举
     */
    public enum EventType {
        TRANSFER_CREATED,    // 转账创建事件
        TRANSFER_PROCESSED,  // 转账处理完成事件
        CLEARING_SUCCESS,    // 清算成功事件
        CLEARING_FAILED      // 清算失败事件
    }
} 