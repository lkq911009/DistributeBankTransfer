package com.distributebank.accountservice.dto;

import java.math.BigDecimal;

/**
 * 创建账户请求DTO
 */
public class CreateAccountRequest {
    private String accountId;
    private String accountName;
    private String bankCode;
    private BigDecimal initialBalance;

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }
    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }
    public BigDecimal getInitialBalance() { return initialBalance; }
    public void setInitialBalance(BigDecimal initialBalance) { this.initialBalance = initialBalance; }
} 