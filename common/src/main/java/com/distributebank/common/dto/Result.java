package com.distributebank.common.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 统一响应结果类
 * 用于所有API的统一返回格式
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Result<T> {
    
    /**
     * 响应码：200-成功，其他-失败
     */
    private Integer code;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private T data;
    
    /**
     * 响应时间戳
     */
    private LocalDateTime timestamp;
    
    /**
     * 成功响应（无数据）
     */
    public static <T> Result<T> success() {
        return Result.<T>builder()
                .code(200)
                .message("操作成功")
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 成功响应（有数据）
     */
    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
                .code(200)
                .message("操作成功")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 成功响应（自定义消息）
     */
    public static <T> Result<T> success(String message, T data) {
        return Result.<T>builder()
                .code(200)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 失败响应
     */
    public static <T> Result<T> error(Integer code, String message) {
        return Result.<T>builder()
                .code(code)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 失败响应（有数据）
     */
    public static <T> Result<T> error(Integer code, String message, T data) {
        return Result.<T>builder()
                .code(code)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
} 