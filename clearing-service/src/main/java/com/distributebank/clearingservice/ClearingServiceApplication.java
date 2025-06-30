package com.distributebank.clearingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 清算服务主启动类
 * 模拟银联或SWIFT清算机构
 */
@SpringBootApplication
public class ClearingServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ClearingServiceApplication.class, args);
    }
} 