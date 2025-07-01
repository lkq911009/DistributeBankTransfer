-- 创建数据库
CREATE DATABASE IF NOT EXISTS distribute_bank DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE distribute_bank;

-- 创建账户表
CREATE TABLE IF NOT EXISTS accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id VARCHAR(50) UNIQUE NOT NULL COMMENT '账户ID',
    account_name VARCHAR(100) NOT NULL COMMENT '账户名称',
    bank_code VARCHAR(20) NOT NULL COMMENT '银行代码',
    balance DECIMAL(19,2) NOT NULL DEFAULT 0.00 COMMENT '账户余额',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '账户状态',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_account_id (account_id),
    INDEX idx_bank_code (bank_code),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账户表';

-- 创建交易流水表
CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id VARCHAR(100) UNIQUE NOT NULL COMMENT '交易ID',
    from_account_id VARCHAR(50) NOT NULL COMMENT '源账户ID',
    to_account_id VARCHAR(50) NOT NULL COMMENT '目标账户ID',
    amount DECIMAL(19,2) NOT NULL COMMENT '转账金额',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '交易状态',
    clearing_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '清算状态',
    error_message TEXT COMMENT '错误信息',
    from_balance_after DECIMAL(19,2) COMMENT '源账户扣款后余额',
    to_balance_after DECIMAL(19,2) COMMENT '目标账户收款后余额',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_transaction_id (transaction_id),
    INDEX idx_from_account (from_account_id),
    INDEX idx_to_account (to_account_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易流水表';

-- 插入测试账户数据
INSERT INTO accounts (account_id, account_name, bank_code, balance, status, version) VALUES
('ACC001', '张三账户', 'BANK001', 10000.00, 'ACTIVE', 0),
('ACC002', '李四账户', 'BANK001', 5000.00, 'ACTIVE', 0),
('ACC003', '王五账户', 'BANK002', 8000.00, 'ACTIVE', 0),
('ACC004', '赵六账户', 'BANK002', 3000.00, 'ACTIVE', 0),
('ACC005', '企业账户A', 'BANK001', 100000.00, 'ACTIVE', 0),
('ACC006', '企业账户B', 'BANK002', 200000.00, 'ACTIVE', 0)
ON DUPLICATE KEY UPDATE
    balance = VALUES(balance),
    version = VALUES(version),
    updated_at = CURRENT_TIMESTAMP;

-- 创建用户并授权
CREATE USER IF NOT EXISTS 'user'@'%' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES ON distribute_bank.* TO 'user'@'%';
FLUSH PRIVILEGES; 