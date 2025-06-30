package com.distributebank.ledgerservice.repository;

import com.distributebank.common.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 账户数据访问层
 * 提供账户数据的CRUD操作
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    
    /**
     * 根据账户ID查询账户
     */
    Optional<Account> findByAccountId(String accountId);
    
    /**
     * 根据银行代码查询账户列表
     */
    java.util.List<Account> findByBankCode(String bankCode);
    
    /**
     * 根据账户状态查询账户列表
     */
    java.util.List<Account> findByStatus(Account.AccountStatus status);
} 