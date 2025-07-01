package com.distributebank.common.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账户实体类
 * 用于存储账户基本信息和余额快照
 */
@Entity
@Table(name = "accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 账户ID，业务主键
     */
    @Column(name = "account_id", unique = true, nullable = false)
    private String accountId;
    
    /**
     * 账户名称
     */
    @Column(name = "account_name", nullable = false)
    private String accountName;
    
    /**
     * 银行代码
     */
    @Column(name = "bank_code", nullable = false)
    private String bankCode;
    
    /**
     * 账户余额（数据库快照）
     */
    @Column(name = "balance", precision = 19, scale = 2, nullable = false)
    private BigDecimal balance;
    
    /**
     * 账户状态：ACTIVE-激活，FROZEN-冻结，CLOSED-关闭
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AccountStatus status;
    
    /**
     * 乐观锁版本号
     */
    @Version
    @Column(name = "version")
    private Long version;
    
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
     * 账户状态枚举
     */
    public enum AccountStatus {
        ACTIVE, FROZEN, CLOSED
    }
} 