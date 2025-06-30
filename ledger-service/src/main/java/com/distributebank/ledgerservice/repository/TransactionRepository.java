package com.distributebank.ledgerservice.repository;

import com.distributebank.common.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 交易数据访问层
 * 提供交易数据的CRUD操作
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    /**
     * 根据交易ID查询交易
     */
    Optional<Transaction> findByTransactionId(String transactionId);
    
    /**
     * 根据源账户ID或目标账户ID查询交易列表
     */
    java.util.List<Transaction> findByFromAccountIdOrToAccountIdOrderByCreatedAtDesc(String fromAccountId, String toAccountId);
    
    /**
     * 根据交易状态查询交易列表
     */
    java.util.List<Transaction> findByStatus(Transaction.TransactionStatus status);
    
    /**
     * 根据清算状态查询交易列表
     */
    java.util.List<Transaction> findByClearingStatus(Transaction.ClearingStatus clearingStatus);
} 