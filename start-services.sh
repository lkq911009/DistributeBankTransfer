#!/bin/bash

# 微服务架构银行跨行转账系统启动脚本

echo "=========================================="
echo "微服务架构银行跨行转账系统"
echo "=========================================="

# 检查Java版本
echo "检查Java版本..."
java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$java_version" -lt 17 ]; then
    echo "错误: 需要Java 17或更高版本，当前版本: $java_version"
    exit 1
fi
echo "Java版本检查通过: $(java -version 2>&1 | head -n 1)"

# 检查Maven
echo "检查Maven..."
if ! command -v mvn &> /dev/null; then
    echo "错误: 未找到Maven，请先安装Maven"
    exit 1
fi
echo "Maven检查通过: $(mvn -version | head -n 1)"

# 检查Docker
echo "检查Docker..."
if ! command -v docker &> /dev/null; then
    echo "错误: 未找到Docker，请先安装Docker"
    exit 1
fi
echo "Docker检查通过: $(docker --version)"

# 检查Docker Compose
echo "检查Docker Compose..."
if ! command -v docker-compose &> /dev/null; then
    echo "错误: 未找到Docker Compose，请先安装Docker Compose"
    exit 1
fi
echo "Docker Compose检查通过: $(docker-compose --version)"

echo ""
echo "开始启动基础设施..."

# 启动基础设施
echo "启动MySQL、Kafka、Zookeeper、Redis..."
docker-compose up -d

# 等待基础设施启动
echo "等待基础设施启动..."
sleep 30

# 检查基础设施状态
echo "检查基础设施状态..."
if ! docker-compose ps | grep -q "Up"; then
    echo "错误: 基础设施启动失败"
    docker-compose logs
    exit 1
fi
echo "基础设施启动成功"

echo ""
echo "开始编译项目..."

# 编译项目
echo "编译整个项目..."
mvn clean compile

if [ $? -ne 0 ]; then
    echo "错误: 项目编译失败"
    exit 1
fi
echo "项目编译成功"

echo ""
echo "开始启动微服务..."

# 启动微服务（后台运行）
echo "启动transaction-service (端口: 8081)..."
mvn spring-boot:run -pl transaction-service > logs/transaction-service.log 2>&1 &
TRANSACTION_PID=$!

echo "启动account-service (端口: 8082)..."
mvn spring-boot:run -pl account-service > logs/account-service.log 2>&1 &
ACCOUNT_PID=$!

echo "启动clearing-service (端口: 8083)..."
mvn spring-boot:run -pl clearing-service > logs/clearing-service.log 2>&1 &
CLEARING_PID=$!

echo "启动ledger-service (端口: 8084)..."
mvn spring-boot:run -pl ledger-service > logs/ledger-service.log 2>&1 &
LEDGER_PID=$!

echo "启动reconciliation-service (端口: 8085)..."
mvn spring-boot:run -pl reconciliation-service > logs/reconciliation-service.log 2>&1 &
RECONCILIATION_PID=$!

echo "启动notification-service (端口: 8086)..."
mvn spring-boot:run -pl notification-service > logs/notification-service.log 2>&1 &
NOTIFICATION_PID=$!

# 创建logs目录
mkdir -p logs

# 保存进程ID
echo $TRANSACTION_PID > logs/transaction-service.pid
echo $ACCOUNT_PID > logs/account-service.pid
echo $CLEARING_PID > logs/clearing-service.pid
echo $LEDGER_PID > logs/ledger-service.pid
echo $RECONCILIATION_PID > logs/reconciliation-service.pid
echo $NOTIFICATION_PID > logs/notification-service.pid

# 等待服务启动
echo "等待微服务启动..."
sleep 60

echo ""
echo "=========================================="
echo "系统启动完成！"
echo "=========================================="
echo ""
echo "服务访问地址:"
echo "- Transaction Service: http://localhost:8081"
echo "- Account Service: http://localhost:8082"
echo "- Clearing Service: http://localhost:8083"
echo "- Ledger Service: http://localhost:8084"
echo "- Reconciliation Service: http://localhost:8085"
echo "- Notification Service: http://localhost:8086"
echo "- Kafka UI: http://localhost:8080"
echo ""
echo "数据库连接:"
echo "- MySQL: localhost:3306 (root/password)"
echo "- Redis: localhost:6379"
echo ""
echo "日志文件位置: logs/"
echo ""
echo "停止服务请运行: ./stop-services.sh"
echo "==========================================" 