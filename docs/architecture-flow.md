# 分布式银行转账系统架构流程图

## 系统整体架构

```mermaid
graph TB
    subgraph "Client Layer"
        A[客户端] --> B[Transaction Service]
        A --> C[Account Service]
        A --> D[Notification Service]
    end
    
    subgraph "Service Layer"
        B --> E[Kafka Events]
        C --> E
        F[Clearing Service]
        G[Ledger Service]
        H[Reconciliation Service]
    end
    
    subgraph "Data Layer"
        I[(MySQL)]
        J[(Redis)]
        K[(Kafka)]
    end
    
    E --> K
    F --> K
    G --> I
    G --> J
    C --> J
    H --> I
    H --> J
```

## 转账流程时序图

```mermaid
sequenceDiagram
    participant Client as 客户端
    participant TS as Transaction Service
    participant AS as Account Service
    participant CS as Clearing Service
    participant LS as Ledger Service
    participant NS as Notification Service
    participant Kafka as Kafka
    participant Redis as Redis
    participant MySQL as MySQL

    Client->>TS: 1. 发起转账请求
    TS->>MySQL: 2. 创建交易记录
    TS->>Kafka: 3. 发送 TRANSFER_CREATED 事件
    
    Kafka->>AS: 4. 消费 TRANSFER_CREATED 事件
    AS->>Redis: 5. 执行Redis Lua脚本扣款
    alt 扣款成功
        AS->>Kafka: 6. 发送 TRANSFER_PROCESSED 事件
    else 扣款失败
        AS->>Kafka: 6. 发送 CLEARING_FAILED 事件
    end
    
    Kafka->>CS: 7. 消费 TRANSFER_PROCESSED 事件
    CS->>CS: 8. 模拟清算处理(1-3秒)
    alt 清算成功(95%)
        CS->>Kafka: 9. 发送 CLEARING_SUCCESS 事件
    else 清算失败(5%)
        CS->>Kafka: 9. 发送 CLEARING_FAILED 事件
    end
    
    Kafka->>LS: 10. 消费 CLEARING_SUCCESS 事件
    LS->>MySQL: 11. 更新交易状态为成功
    LS->>MySQL: 12. 更新目标账户余额
    LS->>Redis: 13. 同步余额到Redis
    
    Kafka->>NS: 14. 消费 CLEARING_SUCCESS 事件
    NS->>NS: 15. 发送成功通知
    
    alt 清算失败
        Kafka->>LS: 16. 消费 CLEARING_FAILED 事件
        LS->>MySQL: 17. 更新交易状态为失败
        Kafka->>NS: 18. 消费 CLEARING_FAILED 事件
        NS->>NS: 19. 发送失败通知
    end
```

## 事件驱动架构图

```mermaid
graph LR
    subgraph "Event Producers"
        A[Transaction Service]
        B[Account Service]
        C[Clearing Service]
    end
    
    subgraph "Kafka Topics"
        D[transfer-events]
    end
    
    subgraph "Event Consumers"
        E[Account Handler]
        F[Clearing Handler]
        G[Ledger Handler]
        H[Notification Handler]
        I[Transaction Handler]
    end
    
    A --> D
    B --> D
    C --> D
    
    D --> E
    D --> F
    D --> G
    D --> H
    D --> I
```

## 事件类型和状态流转

```mermaid
stateDiagram-v2
    [*] --> PENDING: 创建转账
    
    PENDING --> PROCESSING: TRANSFER_CREATED
    PROCESSING --> SUCCESS: CLEARING_SUCCESS
    PROCESSING --> FAILED: CLEARING_FAILED
    
    SUCCESS --> [*]
    FAILED --> [*]
    
    note right of PENDING
        初始状态：交易已创建
        等待账户服务处理
    end note
    
    note right of PROCESSING
        处理中：账户扣款成功
        等待清算服务处理
    end note
    
    note right of SUCCESS
        成功：清算成功
        目标账户已收款
    end note
    
    note right of FAILED
        失败：扣款失败或清算失败
        需要回滚或补偿
    end note
```

## 微服务依赖关系

```mermaid
graph TD
    subgraph "API Gateway Layer"
        AG[API Gateway]
    end
    
    subgraph "Business Services"
        TS[Transaction Service<br/>8081]
        AS[Account Service<br/>8082]
        CS[Clearing Service<br/>8083]
        LS[Ledger Service<br/>8084]
        RS[Reconciliation Service<br/>8085]
        NS[Notification Service<br/>8086]
    end
    
    subgraph "Infrastructure"
        K[Kafka<br/>9092]
        R[Redis<br/>6379]
        M[MySQL<br/>3307]
    end
    
    AG --> TS
    AG --> AS
    AG --> NS
    
    TS -.->|事件| K
    AS -.->|事件| K
    CS -.->|事件| K
    
    K -.->|消费| AS
    K -.->|消费| CS
    K -.->|消费| LS
    K -.->|消费| NS
    K -.->|消费| TS
    
    AS --> R
    LS --> R
    RS --> R
    
    AS --> M
    LS --> M
    RS --> M
    TS --> M
```

## 数据一致性保障

```mermaid
graph TB
    subgraph "幂等控制"
        A[Redis Lua脚本]
        B[交易ID去重]
        C[状态机校验]
    end
    
    subgraph "并发控制"
        D[Redis分布式锁]
        E[乐观锁版本号]
        F[悲观锁行级锁]
    end
    
    subgraph "数据一致性"
        G[最终一致性]
        H[补偿机制]
        I[定时对账]
    end
    
    A --> G
    B --> G
    C --> G
    D --> G
    E --> G
    F --> G
    H --> G
    I --> G
```

## 部署架构

```mermaid
graph TB
    subgraph "Docker Containers"
        TS[Transaction Service<br/>Container]
        AS[Account Service<br/>Container]
        CS[Clearing Service<br/>Container]
        LS[Ledger Service<br/>Container]
        RS[Reconciliation Service<br/>Container]
        NS[Notification Service<br/>Container]
    end
    
    subgraph "Infrastructure Services"
        K[Kafka + Zookeeper<br/>Container]
        R[Redis<br/>Container]
        M[MySQL<br/>Container]
        UI[Kafka UI<br/>Container]
    end
    
    subgraph "Network"
        N[Docker Network<br/>distribute-bank]
    end
    
    TS --> N
    AS --> N
    CS --> N
    LS --> N
    RS --> N
    NS --> N
    K --> N
    R --> N
    M --> N
    UI --> N
```

## 关键特性说明

### 1. 事件驱动架构
- 使用Kafka作为消息中间件
- 服务间通过事件进行解耦
- 支持异步处理和水平扩展

### 2. 数据一致性
- Redis Lua脚本保证原子性
- 幂等处理防止重复消费
- 最终一致性模型

### 3. 高可用设计
- 微服务独立部署
- 容器化部署
- 服务间松耦合

### 4. 监控和运维
- 统一的日志记录
- 健康检查接口
- 可观测性设计 