# Spark Match Engine

<div align="center">

![Java](https://img.shields.io/badge/Java-21-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.2-brightgreen.svg)
![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Version](https://img.shields.io/badge/version-1.0.0-green.svg)

**高性能、高可用的加密货币交易所撮合引擎系统**

[特性](#-核心特性) • [快速开始](#-快速开始) • [文档](#-文档) • [贡献](#-贡献指南)

</div>

---

## 📖 项目简介

Spark Match Engine 是一个专为加密货币交易所设计的高性能撮合引擎系统，采用微服务架构，提供微秒级订单处理延迟、严格的消息顺序保障、零消息丢失的可靠性。

### 核心能力

- 🚀 **高性能**: 单交易对订单处理能力 > 100,000 TPS
- ⚡ **低延迟**: 微秒级订单处理延迟（P99 < 10ms）
- 🔒 **高可靠**: 零消息丢失，WAL持久化保障
- 📈 **可扩展**: 支持水平扩展，动态添加交易对
- 🎯 **严格顺序**: 同一交易对订单严格按时间顺序处理

---

## ✨ 核心特性

### V1.0 已实现功能

- ✅ **订单服务 (Order Service)**

  - RESTful API 订单创建和取消
  - 订单状态管理（PENDING → PARTIAL_FILLED → FILLED）
  - 订单查询和分页
  - 订单验证和风控检查
- ✅ **撮合引擎 (Match Engine Service)**

  - 限价订单簿管理（TreeMap实现，O(logN)查询）
  - 市价单撮合
  - TIF订单类型支持（GTC、IOC、FOK）
  - 价格时间优先撮合算法
- ✅ **消息队列集成**

  - Kafka消息队列（可靠传输、顺序投递）
  - 成交通知发布
  - 订单簿更新通知
- ✅ **高性能队列**

  - LMAX Disruptor无锁队列
  - 单线程事件处理（保障顺序）
- ✅ **数据持久化**

  - WAL（Write-Ahead Log）持久化
  - Snapshot快照机制
  - 订单簿恢复服务
- ✅ **配置管理**

  - MySQL数据库配置存储
  - 交易对配置管理
  - 动态添加新交易对

---

## 🛠️ 技术栈

### 核心框架

- **Java 21** - 开发语言
- **Spring Boot 3.2.2** - 应用框架
- **Spring Kafka** - 消息队列集成
- **MyBatis Plus 3.5.5** - ORM框架

### 高性能组件

- **LMAX Disruptor 4.0.0** - 无锁队列
- **TreeMap** - 订单簿数据结构（O(logN)）
- **HashMap** - 订单索引（O(1)）

### 中间件

- **Apache Kafka** - 消息队列
- **MySQL 8.0** - 关系数据库（配置存储）

### 开发工具

- **Maven** - 构建工具
- **Docker & Docker Compose** - 容器化部署
- **JUnit 5** - 单元测试框架
- **Mockito** - Mock框架

---

## 📁 项目结构

```
spark-match-engine/
├── common/                      # 公共模块
│   ├── enums/                   # 枚举类
│   ├── model/                   # 消息模型
│   ├── util/                    # 工具类
│   └── exception/               # 异常类
│
├── order-service/               # 订单服务
│   ├── controller/             # REST API控制器
│   ├── service/                 # 业务逻辑层
│   ├── mapper/                  # MyBatis Mapper
│   ├── producer/                # Kafka生产者
│   └── consumer/                # Kafka消费者
│
├── match-engine-service/         # 撮合引擎服务
│   ├── consumer/                # Kafka消费者
│   ├── disruptor/               # Disruptor配置
│   ├── orderbook/               # 订单簿管理
│   ├── matcher/                 # 撮合器
│   ├── wal/                     # WAL持久化
│   ├── snapshot/                # Snapshot快照
│   └── recovery/                # 恢复服务
│
├── scripts/                     # 部署脚本
│   ├── build.sh                  # 构建脚本
│   ├── docker-start-standalone.sh/bat  # 一键启动脚本（推荐）
│   ├── docker-start.sh/bat      # 开发环境启动脚本
│   ├── docker-stop.sh/bat       # 停止脚本
│   └── ...
│
├── sql/                         # SQL脚本
│   └── init.sql                 # 数据库初始化脚本
│
├── docker-compose.yml           # Docker Compose配置
└── pom.xml                      # Maven父POM
```

---

## 🚀 快速开始

### 前置要求

- **Java 21** JDK
- **Maven 3.6+**
- **Docker & Docker Compose**
- **MySQL 8.0**
- **Kafka**

### 一键启动

提供真正的一键启动体验：

```bash
docker-compose -f docker-compose.standalone.yml up -d --build
```

**说明**：

- 首次启动会自动构建镜像，可能需要几分钟时间
- 文件MySQL数据库会自动初始化（通过init.sql，包含交易对配置）
- Kafka topics会自动创建

**停止服务**：

```bash
docker-compose -f docker-compose.standalone.yml down
```

### 验证部署

等待所有服务启动完成后（约1-2分钟），访问以下地址验证服务是否正常：

**使用一键启动方式**:

```bash
# 查看服务状态
docker-compose -f docker-compose.standalone.yml ps

# 查看服务日志
docker-compose -f docker-compose.standalone.yml logs -f [service-name]

# 健康检查
curl http://localhost:8081/actuator/health  # 订单服务
curl http://localhost:8082/actuator/health  # 撮合引擎
```

**使用开发环境方式**:

```bash
# 查看服务状态
docker-compose -f docker-compose.yml -f docker-compose.dev.yml ps

# 查看服务日志
docker-compose -f docker-compose.yml -f docker-compose.dev.yml logs -f [service-name]

# 健康检查
curl http://localhost:8081/actuator/health  # 订单服务
curl http://localhost:8082/actuator/health  # 撮合引擎
```

### 服务列表


| 服务      | 端口 | 访问地址              | 说明                                 |
| --------- | ---- | --------------------- | ------------------------------------ |
| 订单服务  | 8081 | http://localhost:8081 | REST API服务                         |
| 撮合引擎  | 8082 | http://localhost:8082 | 撮合引擎服务                         |
| MySQL     | 3306 | localhost:3306        | 数据库（用户名: spark, 密码: spark） |
| Kafka     | 9092 | localhost:9092        | 消息队列                             |
| Zookeeper | 2181 | localhost:2181        | Kafka依赖服务                        |

### 一键启动原理说明

本项目实现了一键启动功能，核心原理如下：

1. **单一docker-compose文件**：所有服务（基础设施+应用服务）都在 `docker-compose.standalone.yml` 中定义
2. **硬编码配置**：所有配置直接写在docker-compose文件中，无需 `.env` 文件
3. **自动构建**：使用 `build` 指令在docker-compose中直接构建镜像，无需预先构建项目
4. **依赖管理**：使用 `depends_on` 和 `condition: service_healthy` 管理服务启动顺序
5. **自动初始化**：通过MySQL的 `init.sql` 自动初始化数据库（包含交易对配置）
6. ### 常用命令

**使用一键启动方式**:

```bash
# 查看所有服务状态
docker-compose -f docker-compose.standalone.yml ps

# 查看特定服务日志
docker-compose -f docker-compose.standalone.yml logs -f [service-name]

# 查看所有服务日志
docker-compose -f docker-compose.standalone.yml logs -f

# 停止所有服务
docker-compose -f docker-compose.standalone.yml down

# 停止并删除数据卷（清理数据）
docker-compose -f docker-compose.standalone.yml down -v

# 重启特定服务
docker-compose -f docker-compose.standalone.yml restart [service-name]

# 重新构建并启动服务
docker-compose -f docker-compose.standalone.yml up -d --build
```

**使用开发环境方式**:

```bash
# 查看所有服务状态
docker-compose -f docker-compose.yml -f docker-compose.dev.yml ps

# 查看特定服务日志
docker-compose -f docker-compose.yml -f docker-compose.dev.yml logs -f [service-name]

# 查看所有服务日志
docker-compose -f docker-compose.yml -f docker-compose.dev.yml logs -f

# 停止所有服务
docker-compose -f docker-compose.yml -f docker-compose.dev.yml down

# 停止并删除数据卷（清理数据）
docker-compose -f docker-compose.yml -f docker-compose.dev.yml down -v

# 重启特定服务
docker-compose -f docker-compose.yml -f docker-compose.dev.yml restart [service-name]

# 重新构建并启动服务
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d --build
```

### 注意事项

1. **一键启动方式（推荐）**

   - 使用 `docker-compose.standalone.yml`，所有配置已硬编码
   - 自动构建镜像，无需预先构建项目
   - 自动初始化 MySQL 数据库（包含交易对配置）
   - 适用于快速体验和开发环境
2. **开发环境方式**

   - 使用 `docker-compose.yml` + `docker-compose.dev.yml`
   - 需要预先构建项目（`mvn clean package`）
   - 所有配置已硬编码为开发环境默认值
   - 适用于日常开发
3. **系统要求**

   - 至少16GB可用磁盘空间
   - 确保端口 3306, 8081, 8082, 9092, 2181 未被占用

---

## 📚 文档

> **注意**: 详细的架构设计文档、实现计划文档和部署文档不在本GitHub仓库中。如需获取完整文档，请联系项目维护者。

### 快速参考

本README.md已包含：

- ✅ 项目简介和核心特性
- ✅ 技术栈说明
- ✅ 快速开始指南
- ✅ API使用示例
- ✅ 测试运行方法
- ✅ 架构设计概述
- ✅ 配置说明
- ✅ 故障排查指南

---

## 🔌 API文档

### 订单服务 API

#### 创建订单

```http
POST /api/orders
Content-Type: application/json

{
  "userId": "user123",
  "symbol": "BTC/USDT",
  "side": "BUY",
  "orderType": "LIMIT",
  "price": "50000.00",
  "quantity": "0.1",
  "timeInForce": "GTC"
}
```

#### 取消订单

```http
POST /api/orders/{orderId}/cancel
```

#### 查询订单

```http
GET /api/orders?userId=user123&status=PENDING&page=1&size=20
```

### 撮合引擎 API

#### 查询订单簿

```http
GET /api/orderbook/{symbolId}?depth=10
```

> **提示**: 详细API文档请参考项目文档（不在GitHub仓库中）

---

## 🏗️ 架构设计

### 系统架构图

```
┌─────────────┐
│  订单服务    │ ──┐
│  (8081)     │   │
└─────────────┘   │
                  │ Kafka
┌─────────────┐   │
│  撮合引擎    │ ◄─┘
│  (8082)     │
└─────────────┘
      │
      ├──► Disruptor (无锁队列)
      ├──► OrderBook (订单簿)
      ├──► WAL (持久化)
      └──► Snapshot (快照)
```

### 核心设计原则

1. **单一事实源**: WAL是订单簿状态的唯一事实来源
2. **严格顺序**: 同一交易对订单严格按时间顺序处理
3. **零消息丢失**: Kafka生产者 `acks=all` + 幂等性
4. **高性能**: Disruptor无锁队列 + TreeMap订单簿

> **提示**: 详细架构设计文档请参考项目文档（不在GitHub仓库中）

---

## 📊 性能指标

### V1.0 性能目标


| 指标       | 目标值        | 说明                 |
| ---------- | ------------- | -------------------- |
| 订单吞吐量 | > 100,000 TPS | 单交易对订单处理能力 |
| 订单延迟   | P99 < 10ms    | 99%订单处理延迟      |
| 支持交易对 | 10-50 个      | V1.0支持主流交易对   |
| 用户规模   | 100,000+      | 支持中小型交易所     |

### 性能优化

- **Disruptor无锁队列**: 微秒级延迟
- **TreeMap订单簿**: O(logN)查询复杂度
- **HashMap订单索引**: O(1)撤单操作
- **WAL同步写入**: 保障数据一致性

---

## 🔧 配置说明

### 配置文件说明

本项目支持两种部署方式，所有配置都已硬编码，**无需** `.env` 文件：

1. **一键启动方式** (`docker-compose.standalone.yml`)

   - 所有配置硬编码
   - 自动构建镜像
   - 自动初始化 MySQL 数据库（包含交易对配置）
2. **开发环境方式** (`docker-compose.yml` + `docker-compose.dev.yml`)

   - 所有配置硬编码为开发环境默认值
   - 需要预先构建项目
   - 自动初始化 MySQL 数据库（包含交易对配置）

### 交易对配置

交易对配置存储在 MySQL 数据库的 `symbol_config` 表中：

- 通过 SQL 脚本初始化（`sql/init.sql`）
- 可通过 SQL 直接修改配置
- 修改后需要重启服务或调用配置刷新接口（如果提供）

> **提示**: 详细配置说明请参考项目文档（不在GitHub仓库中）

---

## 🐛 故障排查

### 常见问题

1. **端口被占用**

   ```bash
   # Linux/Mac查看端口占用
   lsof -i :8081

   # 解决方案：修改docker-compose.dev.yml中的端口映射
   ```
2. **Kafka连接失败**

   - 检查Kafka服务状态:
     - 一键启动: `docker-compose -f docker-compose.standalone.yml logs kafka`
     - 开发环境: `docker-compose -f docker-compose.yml -f docker-compose.dev.yml logs kafka`
3. **内存不足**

   - 检查系统内存: `free -h`
   - 调整JVM参数（在docker-compose.dev.yml中）
4. **服务启动失败**

   ```bash
   # 一键启动方式
   docker-compose -f docker-compose.standalone.yml logs [service-name]
   docker-compose -f docker-compose.standalone.yml ps
   docker-compose -f docker-compose.standalone.yml restart [service-name]

   # 开发环境方式
   docker-compose -f docker-compose.yml -f docker-compose.dev.yml logs [service-name]
   docker-compose -f docker-compose.yml -f docker-compose.dev.yml ps
   docker-compose -f docker-compose.yml -f docker-compose.dev.yml restart [service-name]
   ```
5. **配置读取失败**

   - 检查MySQL数据库是否正常启动
   - 检查 `symbol_config` 表是否存在
   - 查看服务日志: `docker-compose logs [service-name]`
6. **MySQL连接失败**

   - 等待MySQL完全启动（健康检查通过）
   - 检查MySQL日志:
     - 一键启动: `docker-compose -f docker-compose.standalone.yml logs mysql`
     - 开发环境: `docker-compose -f docker-compose.yml -f docker-compose.dev.yml logs mysql`
   - 验证数据库是否创建: `docker exec -it spark-mysql-standalone mysql -uroot -proot -e "SHOW DATABASES;"` (一键启动)
   - 验证数据库是否创建: `docker exec -it spark-mysql-dev mysql -uroot -proot -e "SHOW DATABASES;"` (开发环境)
7. **服务健康检查失败**

   ```bash
   # 手动检查服务健康状态
   curl http://localhost:8081/actuator/health
   curl http://localhost:8082/actuator/health
   ```

### 清理和重置

**一键启动方式**:

```bash
# 停止所有服务
docker-compose -f docker-compose.standalone.yml down

# 停止并删除所有数据卷（会清除所有数据）
docker-compose -f docker-compose.standalone.yml down -v

# 重新构建并启动服务
docker-compose -f docker-compose.standalone.yml up -d --build
```

**开发环境方式**:

```bash
# 停止所有服务
docker-compose -f docker-compose.yml -f docker-compose.dev.yml down

# 停止并删除所有数据卷（会清除所有数据）
docker-compose -f docker-compose.yml -f docker-compose.dev.yml down -v

# 重新构建并启动服务
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d --build
```

---

## 🤝 贡献指南

我们欢迎所有形式的贡献！

### 贡献流程

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

### 代码规范

- 遵循 Java 编码规范
- 使用 4 空格缩进
- 更新相关文档

---

## 📄 许可证

本项目采用 [Apache License](LICENSE) 许可证。

---

## 👥 作者

- **Jeffrey ** - 初始开发

---

## 🙏 致谢

- [Spring Boot](https://spring.io/projects/spring-boot) - 应用框架
- [LMAX Disruptor](https://lmax-exchange.github.io/disruptor/) - 高性能队列
- [Apache Kafka](https://kafka.apache.org/) - 消息队列
- [MySQL](https://www.mysql.com/) - 数据库和配置存储

---

## 📮 联系方式

- **Issues**: [GitHub Issues](https://github.com/JeffreyLee9527/match-engin/issues)
- **Discussions**: [GitHub Discussions](https://github.com/JeffreyLee9527/match-engin/discussions)
- **Email**: jeffreymax9527@gmail.com

---

## ⭐ Star History

如果这个项目对你有帮助，请给我们一个 Star ⭐




---

<div align="center">

**Made with ❤️ by Jeffrey**

[⬆ 回到顶部](#spark-match-engine)

</div>
