#!/bin/bash

# 微服务架构银行跨行转账系统停止脚本

echo "=========================================="
echo "停止微服务架构银行跨行转账系统"
echo "=========================================="

# 停止微服务
echo "停止微服务..."

# 读取进程ID并停止服务
if [ -f logs/transaction-service.pid ]; then
    TRANSACTION_PID=$(cat logs/transaction-service.pid)
    if kill -0 $TRANSACTION_PID 2>/dev/null; then
        echo "停止transaction-service (PID: $TRANSACTION_PID)..."
        kill $TRANSACTION_PID
    fi
    rm -f logs/transaction-service.pid
fi

if [ -f logs/account-service.pid ]; then
    ACCOUNT_PID=$(cat logs/account-service.pid)
    if kill -0 $ACCOUNT_PID 2>/dev/null; then
        echo "停止account-service (PID: $ACCOUNT_PID)..."
        kill $ACCOUNT_PID
    fi
    rm -f logs/account-service.pid
fi

if [ -f logs/clearing-service.pid ]; then
    CLEARING_PID=$(cat logs/clearing-service.pid)
    if kill -0 $CLEARING_PID 2>/dev/null; then
        echo "停止clearing-service (PID: $CLEARING_PID)..."
        kill $CLEARING_PID
    fi
    rm -f logs/clearing-service.pid
fi

if [ -f logs/ledger-service.pid ]; then
    LEDGER_PID=$(cat logs/ledger-service.pid)
    if kill -0 $LEDGER_PID 2>/dev/null; then
        echo "停止ledger-service (PID: $LEDGER_PID)..."
        kill $LEDGER_PID
    fi
    rm -f logs/ledger-service.pid
fi

if [ -f logs/reconciliation-service.pid ]; then
    RECONCILIATION_PID=$(cat logs/reconciliation-service.pid)
    if kill -0 $RECONCILIATION_PID 2>/dev/null; then
        echo "停止reconciliation-service (PID: $RECONCILIATION_PID)..."
        kill $RECONCILIATION_PID
    fi
    rm -f logs/reconciliation-service.pid
fi

if [ -f logs/notification-service.pid ]; then
    NOTIFICATION_PID=$(cat logs/notification-service.pid)
    if kill -0 $NOTIFICATION_PID 2>/dev/null; then
        echo "停止notification-service (PID: $NOTIFICATION_PID)..."
        kill $NOTIFICATION_PID
    fi
    rm -f logs/notification-service.pid
fi

# 等待进程完全停止
echo "等待进程停止..."
sleep 10

# 强制停止可能还在运行的Java进程
echo "清理Java进程..."
pkill -f "spring-boot:run"

echo ""
echo "停止基础设施..."

# 停止基础设施
echo "停止MySQL、Kafka、Zookeeper、Redis..."
docker-compose down

echo ""
echo "=========================================="
echo "系统已停止！"
echo "=========================================="
echo ""
echo "如需重新启动，请运行: ./start-services.sh"
echo "==========================================" 