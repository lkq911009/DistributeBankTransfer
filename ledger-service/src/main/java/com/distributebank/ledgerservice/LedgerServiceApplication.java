package com.distributebank.ledgerservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 账本服务主启动类
 * 负责交易流水记录和数据库余额更新
 */
@SpringBootApplication
public class LedgerServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(LedgerServiceApplication.class, args);
    }
} 