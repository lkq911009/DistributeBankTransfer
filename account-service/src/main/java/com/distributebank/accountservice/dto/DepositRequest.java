package com.distributebank.accountservice.dto;

import java.math.BigDecimal;

/**
 * 充值请求DTO
 */
public class DepositRequest {
    private BigDecimal amount;
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
} 