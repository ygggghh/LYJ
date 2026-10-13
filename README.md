# 智培系统

智培系统是一个基于 Spring Cloud 微服务架构的企业智能培训平台，为企业提供员工培训、技能考核、知识管理、培训运营等完整的数字化培训解决方案。项目采用前后端分离设计，后端由多个独立微服务通过网关统一对外提供服务。

## 技术栈

- 后端：Java 11、Spring Boot 2.7、Spring Cloud Gateway、OpenFeign、Nacos、Spring Security、JWT、MyBatis-Plus
- 数据：MySQL 8、Redis、Elasticsearch 7
- 中间件：Nacos（注册中心/配置中心）、RabbitMQ、XXL-Job、Seata
- 云服务：阿里云 OSS/KMS、腾讯云 COS/VOD、支付宝支付
- 部署：Docker、Jenkins

## 微服务模块

```text
tj-common         共享基础设施：统一响应、异常处理、工具类、Redis/MyBatis/MQ 自动配置、分布式锁
tj-auth           认证授权中心：员工登录、JWT 签发、角色/菜单/权限管理（RBAC）
tj-api            共享 Feign 客户端接口和 DTO，用于服务间通信
tj-gateway        API 网关：路由转发、JWT 认证过滤、跨域处理
tj-user           员工服务：员工、讲师、管理员三类用户管理
tj-course         培训课程服务：培训课程、目录、分类、科目的创建与发布管理
tj-learning       学习中心：培训报名、学习进度跟踪、问答互动、学习笔记、签到打卡、积分排行榜
tj-exam           考核服务：题库管理、题目导入、考核关联
tj-trade          订单服务：培训购物车、订单管理、退款申请
tj-pay            支付服务：培训费用支付、退款、支付渠道配置、支付回调
tj-promotion      营销服务：优惠券规则、发放、兑换码管理
tj-search         搜索服务：培训课程全文检索、智能推荐、热门榜单
tj-media          媒体服务：培训资料上传下载、视频管理与播放
tj-message        消息服务：短信通知、培训提醒、消息模板、员工收件箱
tj-data           数据服务：培训数据看板、今日统计、排行榜分析
tj-remark         互动服务：点赞/取消点赞、点赞计数
```

## 服务端口

```text
网关服务          10010
认证服务          8081
课程服务          8086
用户服务          8082
学习服务          8083
交易服务          8084
支付服务          8085
搜索服务          8087
考试服务          8088
媒体服务          8089
消息服务          8090
营销服务          8091
数据服务          8092
互动服务          8093
```

## 数据库划分

每个业务服务拥有独立数据库：

- `tj_auth` - 认证授权
- `tj_course` - 课程管理
- `tj_user` - 用户管理
- `tj_learning` - 学习中心
- `tj_trade` - 交易订单
- `tj_pay` - 支付记录
- `tj_promotion` - 营销活动
- `tj_exam` - 考试题库
- `tj_media` - 媒体资源
- `tj_message` - 消息通知
- `tj_data` - 数据统计
- `tj_remark` - 互动记录

## 启动方式

### 1. 环境准备

确保以下中间件已启动：

- Nacos（注册中心/配置中心）
- MySQL 8
- Redis
- RabbitMQ
- Elasticsearch 7

### 2. 配置 Nacos

在 Nacos 中创建以下共享配置文件：

- `shared-spring.yaml` - Spring 基础配置
- `shared-redis.yaml` - Redis 连接配置
- `shared-mybatis.yaml` - MyBatis 配置
- `shared-logs.yaml` - 日志配置
- `shared-feign.yaml` - Feign 客户端配置
- `shared-mq.yaml` - RabbitMQ 配置
- `shared-seata.yaml` - Seata 分布式事务配置
- `shared-xxljob.yaml` - XXL-Job 定时任务配置

### 3. 初始化数据库

创建各业务数据库并执行对应的 SQL 脚本。

### 4. 启动服务

按以下顺序启动微服务：

1. 网关服务（tj-gateway）
2. 认证服务（tj-auth）
3. 其他业务服务

## 本地构建

```bash
# 编译并运行测试
mvn clean test

# 跳过测试打包
mvn clean package -DskipTests

# 打包单个服务
mvn clean package -pl tj-course -am -DskipTests
```

## 网关路由

所有请求通过网关统一入口，路由规则如下：

```text
/as/**    -> auth-service       认证授权
/us/**    -> user-service       用户管理
/cs/**    -> course-service     课程管理
/ls/**    -> learning-service   学习中心
/ts/**    -> trade-service      交易订单
/ps/**    -> pay-service        支付服务
/ss/**    -> search-service     搜索服务
/es/**    -> exam-service       考试服务
/ms/**    -> media-service      媒体服务
/sms/**   -> message-service    消息服务
/prs/**   -> promotion-service  营销服务
/ds/**    -> data-service       数据统计
/rs/**    -> remark-service     互动服务
```

## 关键能力

- 网关统一入口，JWT 认证过滤，支持跨域访问
- 基于 RBAC 的权限模型，支持角色、菜单、权限精细化管理
- 培训课程服务支持完整的课程创建、发布、上下架生命周期管理
- 学习中心提供培训报名、进度跟踪、问答互动、笔记记录、签到打卡、积分排行
- 订单服务与支付服务解耦，支持多种支付渠道（支付宝、微信）
- 搜索服务基于 Elasticsearch 实现培训课程全文检索和智能推荐
- 消息服务支持短信通知、定时提醒、消息模板管理
- 各服务通过 OpenFeign 进行服务间调用，避免跨库直接访问
- 集成 Seata 实现分布式事务，保证数据一致性
- 使用 XXL-Job 管理定时任务，支持任务调度和日志监控
