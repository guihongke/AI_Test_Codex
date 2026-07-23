# 部署运维手册

## 环境要求

- **JDK**: 17+
- **Node.js**: 18+
- **Docker**: 24.0+
- **Kubernetes**: 1.28+

## 部署步骤

1. 编译项目：`./gradlew clean build`
2. 构建镜像：`docker build -t myapp:v1.0 .`
3. 推送镜像：`docker push registry.example.com/myapp:v1.0`
4. 更新 K8s：`kubectl apply -f deployment.yaml`

### 环境变量

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `DB_HOST` | localhost | 数据库地址 |
| `DB_PORT` | 3306 | 数据库端口 |
| `REDIS_HOST` | localhost | Redis 地址 |
| `LOG_LEVEL` | INFO | 日志级别 |

---

> 生产环境请务必修改默认密码！
