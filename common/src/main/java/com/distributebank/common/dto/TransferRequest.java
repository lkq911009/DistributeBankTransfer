package com.distributebank.common.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

/**
 * 转账请求DTO类
 * 用于接收转账请求参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRequest {
    
    /**
     * 源账户ID
     */
    @NotBlank(message = "源账户ID不能为空")
    private String fromAccountId;
    
    /**
     * 目标账户ID
     */
    @NotBlank(message = "目标账户ID不能为空")
    private String toAccountId;
    
    /**
     * 转账金额
     */
    @NotNull(message = "转账金额不能为空")
    @DecimalMin(value = "0.01", message = "转账金额必须大于0")
    private BigDecimal amount;
    
    /**
     * 转账备注
     */
    private String remark;
} 