package com.distributebank.common.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易流水实体类
 * 记录所有转账交易的详细信息
 */
@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 交易ID，业务主键，用于幂等控制
     */
    @Column(name = "transaction_id", unique = true, nullable = false)
    private String transactionId;
    
    /**
     * 源账户ID
     */
    @Column(name = "from_account_id", nullable = false)
    private String fromAccountId;
    
    /**
     * 目标账户ID
     */
    @Column(name = "to_account_id", nullable = false)
    private String toAccountId;
    
    /**
     * 转账金额
     */
    @Column(name = "amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;
    
    /**
     * 交易状态：PENDING-待处理，PROCESSING-处理中，SUCCESS-成功，FAILED-失败
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status;
    
    /**
     * 清算状态：PENDING-待清算，SUCCESS-清算成功，FAILED-清算失败
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "clearing_status", nullable = false)
    private ClearingStatus clearingStatus;
    
    /**
     * 错误信息
     */
    @Column(name = "error_message", length = 500)
    private String errorMessage;
    
    /**
     * 源账户扣款后余额
     */
    @Column(name = "from_balance_after", precision = 19, scale = 2)
    private BigDecimal fromBalanceAfter;
    
    /**
     * 目标账户收款后余额
     */
    @Column(name = "to_balance_after", precision = 19, scale = 2)
    private BigDecimal toBalanceAfter;
    
    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * 交易状态枚举
     */
    public enum TransactionStatus {
        PENDING, PROCESSING, SUCCESS, FAILED
    }
    
    /**
     * 清算状态枚举
     */
    public enum ClearingStatus {
        PENDING, SUCCESS, FAILED
    }
} 