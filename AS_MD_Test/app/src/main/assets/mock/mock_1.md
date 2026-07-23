# 系统架构设计文档

## 整体架构

本系统采用**微服务架构**，各服务独立部署、独立扩展。

- 网关层：Nginx + Spring Cloud Gateway
- 服务层：Spring Boot 微服务集群
- 数据层：MySQL + Redis + Elasticsearch

### 核心服务

| 服务名称 | 端口 | 职责 | 功能 | 风险  |
|---------|------|------|--|-----|
| ***user-service*** | 8081 | 用户管理与认证 | 实现用户的登录 | 高  |
| ## order-service ### | 8082 | 订单处理 | 实现用户的登录 |  高   |
| payment-service | 8083 | 支付与结算 | 实现用户的登录 |  高   |
| notify-service | 8084 | 消息推送 | 实现用户的登录 |  高   |
| notify-service | 8084 | 消息推送 | 实现用户的登录 |  高   |
| notify-service | 8084 | 消息推送 | 实现用户的登录 |  高   |
| notify-service | 8084 | 消息推送 | 实现用户的登录 |  高   |
| notify-service | 8084 | 消息推送 | 实现用户的登录 |  高   |
| notify-service | 8084 | 消息推送 | 实现用户的登录 |  高   |
| notify-service | 8084 | 消息推送 | 实现用户的登录 |  高   |
| notify-service | 8084 | 消息推送 | 实现用户的登录 |  高   |
| notify-service | 8084 | 消息推送消息推送消息推送消息推送消息推送消息推送消息推送消息推送消息推送消息推送消息推送消息推送消息推送 | 实现用户的登录 |  高   |
| notify-service | 8084 | 消息推送 | 实现用户的登录 |  高   |
| notify-service | 8084 | 消息推送 | 实现用户的登录实现用户的登录实现用户的登录实现用户的登录实现用户的登录实现用户的登录实现用户的登录实现用户的登录 |  高   |
| notify-service | 8084 | 消息推送 | 实现用户的登录 |  高   |
| notify-service | 8084 | 消息推送 | 实现用户的登录 |  高   |
| notify-service | 8084 | 消息推送 | 实现用户的登录 |  高   |
| notify-service | 8084 | 消息推送 | 实现用户的登录 |  高高高高高高高高高高高高高高高高高高高高高高高高   |
| notify-service | 8084 | 消息推送 | 实现用户的登录 |  高   |
| notify-service | 80848084808480848084808480848084808480848084 | 消息推送 | 实现用户的登录 |  高   |
| notify-servicenotify-servicenotify-servicenotify-servicenotify-servicenotify-servicenotify-servicenotify-service | 8084 | 消息推送 | 实现用户的登录 |  高   |
| notify-service | 8084 | 消息推送 | 实现用户的登录 |  高   |
| ![Android Logo](https://img.netbian.com/file/2024/1010/003132wNTFM.jpg) | 8084 | 消息推送 | 实现用户的登录 |  高   |
| notify-service | 8084 | 消息推送 | 实现用户的登录 |  高   |
| notify-service | 8084 | 消息推送 | 实现用户的登录 |  高   |
| notify-service | 8084 | 消息推送 | 实现用户的登录 |  高   |

> 设计原则：高内聚、低耦合，服务间通过 gRPC 通信。
