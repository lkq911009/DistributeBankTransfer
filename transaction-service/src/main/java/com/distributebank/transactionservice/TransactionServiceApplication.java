package com.distributebank.transactionservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 转账服务主启动类
 * 负责接收转账请求并创建转账事件
 */
@SpringBootApplication
public class TransactionServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(TransactionServiceApplication.class, args);
    }
} 