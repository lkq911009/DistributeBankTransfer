package com.distributebank.notificationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 通知服务主启动类
 * 负责推送交易结果通知
 */
@SpringBootApplication
public class NotificationServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
} 