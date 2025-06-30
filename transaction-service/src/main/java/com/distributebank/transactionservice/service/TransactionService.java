package com.distributebank.transactionservice.service;

import com.distributebank.common.dto.TransferRequest;
import com.distributebank.common.event.TransferEvent;
import com.distributebank.common.model.Transaction;
import com.distributebank.transactionservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 转账服务业务逻辑类
 * 负责创建转账交易并发送Kafka事件
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {
    
    private final TransactionRepository transactionRepository;
    private final KafkaTemplate<String, TransferEvent> kafkaTemplate;
    
    private static final String TRANSFER_TOPIC = "transfer-events";
    
    /**
     * 创建单笔转账
     */
    @Transactional
    public String createTransfer(TransferRequest request) {
        // 生成交易ID
        String transactionId = generateTransactionId();
        
        // 创建交易记录
        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .fromAccountId(request.getFromAccountId())
                .toAccountId(request.getToAccountId())
                .amount(request.getAmount())
                .status(Transaction.TransactionStatus.PENDING)
                .clearingStatus(Transaction.ClearingStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        // 保存交易记录
        transactionRepository.save(transaction);
        log.info("创建转账交易: {}", transactionId);
        
        // 发送转账事件到Kafka
        TransferEvent event = TransferEvent.builder()
                .transactionId(transactionId)
                .fromAccountId(request.getFromAccountId())
                .toAccountId(request.getToAccountId())
                .amount(request.getAmount())
                .eventType(TransferEvent.EventType.TRANSFER_CREATED)
                .timestamp(LocalDateTime.now())
                .build();
        
        kafkaTemplate.send(TRANSFER_TOPIC, transactionId, event);
        log.info("发送转账事件: {}", event);
        
        return transactionId;
    }
    
    /**
     * 创建批量转账（企业发工资场景）
     */
    @Transactional
    public String createBatchTransfer(BatchTransferRequest request) {
        String batchId = "BATCH_" + generateTransactionId();
        
        List<String> transactionIds = request.getTransfers().stream()
                .map(transfer -> {
                    // 设置源账户ID
                    transfer.setFromAccountId(request.getFromAccountId());
                    return createTransfer(transfer);
                })
                .toList();
        
        log.info("批量转账创建完成，批次ID: {}, 交易数量: {}", batchId, transactionIds.size());
        return batchId;
    }
    
    /**
     * 查询转账状态
     */
    public Object getTransactionStatus(String transactionId) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("交易不存在: " + transactionId));
        
        return new TransactionStatusResponse(
                transaction.getTransactionId(),
                transaction.getStatus().name(),
                transaction.getClearingStatus().name(),
                transaction.getErrorMessage()
        );
    }
    
    /**
     * 生成交易ID
     */
    private String generateTransactionId() {
        return "TXN_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * 更新交易状态（供事件处理器调用）
     */
    @Transactional
    public void updateTransactionStatus(String transactionId, String status) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("交易不存在: " + transactionId));
        
        switch (status) {
            case "PROCESSING":
                transaction.setStatus(Transaction.TransactionStatus.PROCESSING);
                break;
            case "SUCCESS":
                transaction.setStatus(Transaction.TransactionStatus.SUCCESS);
                transaction.setClearingStatus(Transaction.ClearingStatus.SUCCESS);
                break;
            case "FAILED":
                transaction.setStatus(Transaction.TransactionStatus.FAILED);
                transaction.setClearingStatus(Transaction.ClearingStatus.FAILED);
                break;
            default:
                log.warn("未知状态: {}", status);
                return;
        }
        
        transaction.setUpdatedAt(LocalDateTime.now());
        transactionRepository.save(transaction);
        log.info("交易状态更新: {} -> {}", transactionId, status);
    }
    
    /**
     * 批量转账请求内部类
     */
    public static class BatchTransferRequest {
        private String fromAccountId;
        private List<TransferRequest> transfers;
        
        public String getFromAccountId() { return fromAccountId; }
        public void setFromAccountId(String fromAccountId) { this.fromAccountId = fromAccountId; }
        public List<TransferRequest> getTransfers() { return transfers; }
        public void setTransfers(List<TransferRequest> transfers) { this.transfers = transfers; }
    }
    
    /**
     * 交易状态响应类
     */
    public static class TransactionStatusResponse {
        private String transactionId;
        private String status;
        private String clearingStatus;
        private String errorMessage;
        
        public TransactionStatusResponse(String transactionId, String status, String clearingStatus, String errorMessage) {
            this.transactionId = transactionId;
            this.status = status;
            this.clearingStatus = clearingStatus;
            this.errorMessage = errorMessage;
        }
        
        // getters
        public String getTransactionId() { return transactionId; }
        public String getStatus() { return status; }
        public String getClearingStatus() { return clearingStatus; }
        public String getErrorMessage() { return errorMessage; }
    }
} 