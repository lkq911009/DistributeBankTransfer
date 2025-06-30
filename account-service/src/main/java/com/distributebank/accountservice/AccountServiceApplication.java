package com.distributebank.accountservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 账户服务主启动类
 * 负责账户余额管理和扣款逻辑
 */
@SpringBootApplication
public class AccountServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }
} 