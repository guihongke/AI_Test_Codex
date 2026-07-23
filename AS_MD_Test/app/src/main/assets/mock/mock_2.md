# API 接口文档

## 用户模块

### 1. 用户注册

```http
POST /api/v1/users/register
Content-Type: application/json

{
  "username": "demo_user",
  "password": "******",
  "email": "demo@example.com"
}
```

**响应示例：**

```json
{
  "code": 200,
  "data": { "userId": 10001 },
  "message": "注册成功"
}
```

### 2. 获取用户信息

`GET /api/v1/users/{userId}`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | Long | 是 | 用户 ID |
| fields | String | 否 | 返回字段，逗号分隔 |
