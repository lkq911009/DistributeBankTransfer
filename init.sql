-- 创建数据库
CREATE DATABASE IF NOT EXISTS distribute_bank;
USE distribute_bank;

-- 创建账户表
CREATE TABLE IF NOT EXISTS accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id VARCHAR(50) UNIQUE NOT NULL,
    account_name VARCHAR(100) NOT NULL,
    bank_code VARCHAR(20) NOT NULL,
    balance DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    status ENUM('ACTIVE', 'FROZEN', 'CLOSED') NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_account_id (account_id),
    INDEX idx_bank_code (bank_code),
    INDEX idx_status (status)
);

-- 创建交易表
CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id VARCHAR(100) UNIQUE NOT NULL,
    from_account_id VARCHAR(50) NOT NULL,
    to_account_id VARCHAR(50) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    status ENUM('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED') NOT NULL DEFAULT 'PENDING',
    clearing_status ENUM('PENDING', 'SUCCESS', 'FAILED') NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    from_balance_after DECIMAL(19,2),
    to_balance_after DECIMAL(19,2),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_transaction_id (transaction_id),
    INDEX idx_from_account_id (from_account_id),
    INDEX idx_to_account_id (to_account_id),
    INDEX idx_status (status),
    INDEX idx_clearing_status (clearing_status),
    INDEX idx_created_at (created_at)
);

-- 插入测试数据
INSERT INTO accounts (account_id, account_name, bank_code, balance, status, created_at, updated_at) VALUES
('ACC001', '企业账户A', 'BANK001', 100000.00, 'ACTIVE', NOW(), NOW()),
('ACC002', '员工账户1', 'BANK002', 5000.00, 'ACTIVE', NOW(), NOW()),
('ACC003', '员工账户2', 'BANK003', 3000.00, 'ACTIVE', NOW(), NOW()),
('ACC004', '员工账户3', 'BANK001', 2000.00, 'ACTIVE', NOW(), NOW()),
('ACC005', '员工账户4', 'BANK002', 1500.00, 'ACTIVE', NOW(), NOW());

-- 创建用户并授权
CREATE USER IF NOT EXISTS 'user'@'%' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES ON distribute_bank.* TO 'user'@'%';
FLUSH PRIVILEGES; 