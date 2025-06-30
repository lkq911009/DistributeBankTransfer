# 微服务架构银行跨行转账系统

## 项目概述

这是一个基于Spring Boot 3+和Java 17+的微服务架构银行跨行转账系统，专门用于企业向多个员工发工资的场景。系统具备高并发、资金安全和强一致性保障能力。

## 系统架构

### 微服务模块

1. **transaction-service** (端口: 8081)
   - 负责接收发起转账请求
   - 创建交易任务
   - 发送转账事件到Kafka

2. **account-service** (端口: 8082)
   - 实现账户余额校验与扣款逻辑
   - 使用Redis + Lua脚本保证原子扣款
   - 幂等控制

3. **clearing-service** (端口: 8083)
   - 模拟银联或SWIFT清算机构
   - 消费转账事件，异步处理清算
   - 返回成功或失败状态

4. **ledger-service** (端口: 8084)
   - 写入交易流水
   - 更新数据库余额快照
   - 记录清算状态，具备幂等处理能力

5. **reconciliation-service** (端口: 8085)
   - 自动对账模块
   - 支持Redis和数据库之间的余额比对与补偿交易
   - 定时执行对账任务

6. **notification-service** (端口: 8086)
   - 推送交易结果通知
   - 记录通知历史

### 技术栈

- **框架**: Spring Boot 3.2.0
- **Java版本**: 17+
- **构建工具**: Maven
- **数据库**: MySQL 8.0
- **缓存**: Redis 7
- **消息队列**: Apache Kafka 3.6.0
- **协调服务**: Zookeeper

## 快速开始

### 1. 环境要求

- Java 17+
- Maven 3.6+
- Docker & Docker Compose

### 2. 启动基础设施

```bash
# 启动MySQL、Kafka、Zookeeper、Redis等基础设施
docker-compose up -d
```

### 3. 编译项目

```bash
# 编译整个项目
mvn clean compile

# 或者编译单个模块
mvn clean compile -pl transaction-service
```

### 4. 启动微服务

```bash
# 启动所有微服务（需要分别启动）
mvn spring-boot:run -pl transaction-service
mvn spring-boot:run -pl account-service
mvn spring-boot:run -pl clearing-service
mvn spring-boot:run -pl ledger-service
mvn spring-boot:run -pl reconciliation-service
mvn spring-boot:run -pl notification-service
```

## API使用指南

### 1. 创建账户

```bash
curl -X POST http://localhost:8082/api/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": "ACC006",
    "accountName": "新员工账户",
    "bankCode": "BANK001",
    "initialBalance": 1000.00
  }'
```

### 2. 查询账户余额

```bash
curl http://localhost:8082/api/accounts/ACC001/balance
```

### 3. 发起转账

```bash
curl -X POST http://localhost:8081/api/transactions/transfer \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccountId": "ACC001",
    "toAccountId": "ACC002",
    "amount": 1000.00,
    "remark": "工资发放"
  }'
```

### 4. 批量转账（企业发工资）

```bash
curl -X POST http://localhost:8081/api/transactions/batch-transfer \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccountId": "ACC001",
    "transfers": [
      {
        "toAccountId": "ACC002",
        "amount": 5000.00,
        "remark": "员工1工资"
      },
      {
        "toAccountId": "ACC003",
        "amount": 4000.00,
        "remark": "员工2工资"
      }
    ]
  }'
```

### 5. 查询转账状态

```bash
curl http://localhost:8081/api/transactions/TXN_1234567890_ABCDEFGH
```

### 6. 手动触发对账

```bash
curl -X POST http://localhost:8085/api/reconciliation/ACC001
```

### 7. 查看对账状态

```bash
curl http://localhost:8085/api/reconciliation/status
```

## 系统特性

### 1. 高并发处理
- 使用Redis Lua脚本实现原子扣款
- Kafka异步事件驱动架构
- 微服务解耦，支持水平扩展

### 2. 资金安全
- 所有金额使用BigDecimal处理
- Redis原子操作保证余额一致性
- 幂等控制防止重复处理

### 3. 强一致性保障
- 数据库事务保证
- Redis和数据库双重余额记录
- 定时对账机制

### 4. 事件驱动架构
- Kafka消息传递
- 异步处理提高性能
- 服务解耦

## 监控和管理

### 1. Kafka UI
访问 http://localhost:8080 查看Kafka消息和主题

### 2. 数据库监控
```bash
# 连接MySQL
mysql -h localhost -P 3306 -u root -p
```

### 3. Redis监控
```bash
# 连接Redis
redis-cli -h localhost -p 6379
```

## 开发指南

### 1. 项目结构
```
DistributeBankTransfer/
├── common/                    # 公共模块
├── transaction-service/       # 转账服务
├── account-service/          # 账户服务
├── clearing-service/         # 清算服务
├── ledger-service/           # 账本服务
├── reconciliation-service/   # 对账服务
├── notification-service/     # 通知服务
├── docker-compose.yml        # 基础设施配置
├── init.sql                  # 数据库初始化脚本
└── README.md                 # 项目说明
```

### 2. 添加新的微服务
1. 在根目录创建新的模块目录
2. 添加pom.xml配置
3. 创建主启动类
4. 添加配置文件
5. 在根pom.xml中添加模块

### 3. 扩展功能
- 添加新的Kafka事件类型
- 扩展数据库表结构
- 增加新的API接口
- 集成外部服务

## 故障排除

### 1. 常见问题

**问题**: 微服务启动失败
**解决**: 检查基础设施是否正常启动，确认端口是否被占用

**问题**: Kafka连接失败
**解决**: 确认Zookeeper和Kafka服务状态，检查网络连接

**问题**: Redis连接失败
**解决**: 检查Redis服务状态，确认端口配置

### 2. 日志查看
```bash
# 查看服务日志
tail -f logs/application.log
```

### 3. 性能调优
- 调整JVM参数
- 优化数据库查询
- 配置Redis连接池
- 调整Kafka分区数

## 部署说明

### 1. 生产环境部署
- 使用Docker容器化部署
- 配置负载均衡
- 设置监控告警
- 配置日志收集

### 2. 环境变量配置
```bash
export SPRING_PROFILES_ACTIVE=prod
export MYSQL_HOST=your-mysql-host
export REDIS_HOST=your-redis-host
export KAFKA_BOOTSTRAP_SERVERS=your-kafka-servers
```

## 贡献指南

1. Fork项目
2. 创建功能分支
3. 提交代码
4. 创建Pull Request

## 许可证

MIT License

## 联系方式

如有问题，请提交Issue或联系开发团队。 