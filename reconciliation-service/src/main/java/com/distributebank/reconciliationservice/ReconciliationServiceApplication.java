package com.distributebank.reconciliationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 对账服务主启动类
 * 负责Redis和数据库之间的余额比对与补偿交易
 */
@SpringBootApplication
@EnableScheduling
public class ReconciliationServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ReconciliationServiceApplication.class, args);
    }
} 