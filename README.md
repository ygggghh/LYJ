<div align="center">

# 智培系统

### 企业智能培训平台 · 一站式员工学习与考核解决方案

**赋能企业数字化转型，构建高效人才培养体系**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.2-6DB33F?style=flat-square&logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2021.0.3-6DB33F?style=flat-square&logo=spring&logoColor=white)](https://spring.io/projects/spring-cloud)
[![Java](https://img.shields.io/badge/Java-11+-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://www.java.com/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat-square&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-6.0+-DC382D?style=flat-square&logo=redis&logoColor=white)](https://redis.io/)
[![License](https://img.shields.io/badge/license-MIT-blue?style=flat-square)](LICENSE)

**[在线演示](#在线演示) · [快速开始](#快速开始) · [系统架构](#系统架构) · [功能模块](#功能模块) · [API文档](#api文档) · [常见问题](#常见问题)**

<code>mvn clean package -DskipTests</code>

> [!IMPORTANT]
> 本系统为企业内部培训平台，请在授权范围内使用。
> 如需技术支持，请联系系统管理员。

<br />

<table>
<tr>
<td align="center" width="800">
  <strong>🎯 核心特性</strong><br />
  培训课程管理 · 在线学习跟踪 · 考核测评系统 · 数据分析看板 · 多端适配
</td>
</tr>
</table>

<br />

</div>

---

智培系统是专为企业设计的智能培训平台，帮助企业构建完善的员工培训体系。系统涵盖培训课程管理、在线学习、考核测评、数据统计等核心功能，支持员工自主学习、进度跟踪、技能考核等完整培训流程。

系统采用微服务架构设计，各业务模块独立部署，通过 API 网关统一对外提供服务。支持高并发访问，可根据企业规模灵活扩展。

## ✨ 核心特性

- 🎓 **培训课程管理** - 支持课程创建、分类管理、目录结构、多媒体资源
- 📚 **在线学习中心** - 课程报名、进度跟踪、笔记记录、问答互动
- 📝 **考核测评系统** - 题库管理、在线考试、成绩统计、证书发放
- 📊 **数据分析看板** - 培训数据统计、学习排行、效果分析
- 💬 **互动社区** - 问答交流、点赞互动、学习分享
- 🔔 **消息通知** - 培训提醒、短信通知、站内消息
- 💰 **订单支付** - 培训报名、在线支付、退款管理（支持对接主流第三方支付）
- 🎁 **营销活动** - 优惠券、兑换码、促销活动

## 🏗️ 系统架构

### 技术栈

**后端核心**
- Java 11 + Spring Boot 2.7.2
- Spring Cloud 2021.0.3 + Spring Cloud Alibaba
- Spring Cloud Gateway（API 网关）
- OpenFeign（服务间调用）
- Nacos（注册中心/配置中心）
- Spring Security + JWT（认证授权）
- MyBatis-Plus（数据访问）

**数据存储**
- MySQL 8.0（关系型数据库）
- Redis（缓存/分布式锁）
- Elasticsearch 7（全文检索）

**中间件**
- RabbitMQ（消息队列）
- XXL-Job（定时任务）
- Seata（分布式事务）

**云服务集成**
- 阿里云 OSS（对象存储）
- 腾讯云 VOD（视频点播）

### 微服务模块

```text
┌─────────────────────────────────────────────────────────────┐
│                      API Gateway (10010)                     │
│                   路由转发 · JWT认证 · 跨域处理                │
└──────────────────────────┬──────────────────────────────────┘
                           │
    ┌──────────────────────┼──────────────────────┐
    │                      │                      │
┌───┴──────────┐  ┌────────┴────────┐  ┌─────────┴────────┐
│  认证授权服务  │  │   员工管理服务   │  │  培训课程服务     │
│  auth-service │  │  user-service   │  │  course-service  │
│    (8081)     │  │    (8082)       │  │    (8086)        │
└───────────────┘  └─────────────────┘  └──────────────────┘

┌───────────────┐  ┌─────────────────┐  ┌──────────────────┐
│  学习中心服务  │  │   考核测评服务   │  │   订单交易服务    │
│ learning-svc  │  │   exam-service  │  │   trade-service  │
│    (8083)     │  │    (8088)       │  │    (8084)        │
└───────────────┘  └─────────────────┘  └──────────────────┘

┌───────────────┐  ┌─────────────────┐  ┌──────────────────┐
│  支付服务      │  │   搜索服务       │  │   媒体资源服务    │
│  pay-service  │  │  search-service │  │  media-service   │
│    (8085)     │  │    (8087)       │  │    (8089)        │
└───────────────┘  └─────────────────┘  └──────────────────┘

┌───────────────┐  ┌─────────────────┐  ┌──────────────────┐
│  消息通知服务  │  │   营销服务       │  │   数据分析服务    │
│ message-svc   │  │ promotion-svc   │  │   data-service   │
│    (8090)     │  │    (8091)       │  │    (8092)        │
└───────────────┘  └─────────────────┘  └──────────────────┘

┌───────────────┐  ┌─────────────────┐
│  互动服务      │  │   共享基础设施   │
│ remark-service│  │    tj-common    │
│    (8093)     │  │                 │
└───────────────┘  └─────────────────┘
```

## 🚀 快速开始

### 环境要求

- JDK 11+
- Maven 3.6+
- MySQL 8.0
- Redis 6.0+
- Nacos 2.x
- RabbitMQ 3.x
- Elasticsearch 7.x

### 1. 克隆项目

```bash
git clone <repository-url>
cd tianji
```

### 2. 初始化数据库

创建以下数据库并执行对应的 SQL 脚本：

```sql
CREATE DATABASE tj_auth DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE tj_course DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE tj_user DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE tj_learning DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE tj_trade DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE tj_pay DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE tj_promotion DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE tj_exam DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE tj_media DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE tj_message DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE tj_data DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE tj_remark DEFAULT CHARACTER SET utf8mb4;
```

### 3. 配置 Nacos

在 Nacos 中创建以下共享配置文件：

- `shared-spring.yaml` - Spring 基础配置
- `shared-redis.yaml` - Redis 连接配置
- `shared-mybatis.yaml` - MyBatis 配置
- `shared-logs.yaml` - 日志配置
- `shared-feign.yaml` - Feign 客户端配置
- `shared-mq.yaml` - RabbitMQ 配置
- `shared-seata.yaml` - Seata 分布式事务配置
- `shared-xxljob.yaml` - XXL-Job 定时任务配置

### 4. 编译构建

```bash
# 编译并运行测试
mvn clean test

# 跳过测试打包
mvn clean package -DskipTests

# 打包单个服务
mvn clean package -pl tj-course -am -DskipTests
```

### 5. 启动服务

按以下顺序启动微服务：

1. **网关服务** - tj-gateway (10010)
2. **认证服务** - tj-auth (8081)
3. **其他业务服务** - 按需启动

## 📋 功能模块

### 核心业务模块

| 模块 | 服务名 | 端口 | 功能描述 |
|------|--------|------|----------|
| 🔐 认证授权 | auth-service | 8081 | 员工登录、JWT 签发、角色权限管理 |
| 👥 员工管理 | user-service | 8082 | 员工、讲师、管理员信息管理 |
| 📚 培训课程 | course-service | 8086 | 课程创建、分类、目录、发布管理 |
| 🎓 学习中心 | learning-service | 8083 | 培训报名、进度跟踪、笔记、签到 |
| 📝 考核测评 | exam-service | 8088 | 题库管理、在线考试、成绩统计 |
| 🛒 订单交易 | trade-service | 8084 | 购物车、订单管理、退款申请 |
| 💳 支付服务 | pay-service | 8085 | 支付处理、退款、第三方支付渠道对接 |
| 🔍 搜索服务 | search-service | 8087 | 课程检索、智能推荐 |
| 🎬 媒体资源 | media-service | 8089 | 文件上传、视频管理 |
| 📨 消息通知 | message-service | 8090 | 短信、通知、消息模板 |
| 🎁 营销服务 | promotion-service | 8091 | 优惠券、兑换码、促销 |
| 📊 数据分析 | data-service | 8092 | 数据看板、统计分析 |
| 👍 互动服务 | remark-service | 8093 | 点赞、互动计数 |

### 网关路由

所有请求通过统一网关入口（端口 10010），路由规则如下：

```text
/as/**    → auth-service       认证授权
/us/**    → user-service       员工管理
/cs/**    → course-service     培训课程
/ls/**    → learning-service   学习中心
/ts/**    → trade-service      订单交易
/ps/**    → pay-service        支付服务
/ss/**    → search-service     搜索服务
/es/**    → exam-service       考核测评
/ms/**    → media-service      媒体资源
/sms/**   → message-service    消息通知
/prs/**   → promotion-service  营销服务
/ds/**    → data-service       数据分析
/rs/**    → remark-service     互动服务
```

## 📖 API文档

系统集成了 Knife4j（Swagger 增强版），各服务启动后可访问对应的 API 文档：

```text
认证服务：http://localhost:8081/doc.html
课程服务：http://localhost:8086/doc.html
用户服务：http://localhost:8082/doc.html
...（其他服务类似）
```

## 🔧 配置说明

### 服务端口分配

```text
网关服务          10010
认证服务          8081
员工服务          8082
学习服务          8083
订单服务          8084
支付服务          8085
课程服务          8086
搜索服务          8087
考试服务          8088
媒体服务          8089
消息服务          8090
营销服务          8091
数据服务          8092
互动服务          8093
```

### 关键配置项

各服务的配置存储在 Nacos 配置中心，主要配置项包括：

- 数据库连接信息
- Redis 连接配置
- RabbitMQ 连接配置
- JWT 密钥配置
- 云服务 AccessKey/SecretKey
- 支付渠道配置

## 📊 系统监控

### 健康检查

各服务提供健康检查端点：

```bash
curl http://localhost:{port}/actuator/health
```

### 日志管理

统一日志配置存储在 Nacos 的 `shared-logs.yaml`，支持：
- 日志级别动态调整
- 日志文件滚动
- 链路追踪集成

## ❓ 常见问题

<details>
<summary><strong>如何修改服务端口？</strong></summary>

修改对应服务的 `bootstrap.yml` 文件中的 `server.port` 配置，或在 Nacos 配置中心修改。

</details>

<details>
<summary><strong>数据库连接失败怎么办？</strong></summary>

1. 检查 MySQL 服务是否启动
2. 确认数据库是否已创建
3. 检查 Nacos 中的数据库连接配置
4. 确认防火墙设置

</details>

<details>
<summary><strong>如何添加新的培训课程分类？</strong></summary>

通过课程管理服务的 API 接口添加，或访问管理后台进行配置。

</details>

<details>
<summary><strong>系统支持多少并发用户？</strong></summary>

系统采用微服务架构，支持水平扩展。实际并发能力取决于：
- 服务器配置
- 数据库性能
- 网络带宽
- 缓存策略

建议通过压力测试确定具体容量。

</details>

<details>
<summary><strong>如何备份数据？</strong></summary>

建议定期备份以下数据：
- MySQL 数据库（mysqldump）
- Redis 持久化文件
- 上传的文件和媒体资源
- Nacos 配置数据

</details>

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

- 提交 Bug 报告时，请包含详细的复现步骤
- 提交新功能建议前，可以先开 Issue 讨论
- 代码提交前请确保通过所有测试

## 📄 开源协议

MIT License

## 📞 技术支持

如有问题，请联系：
- 系统管理员：admin@company.com
- 技术支持群：[内部沟通群]

---

<div align="center">

**智培系统** - 让企业培训更智能、更高效

</div>
