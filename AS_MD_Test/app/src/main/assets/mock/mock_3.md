# 数据库设计

## ER 图说明

核心实体关系如下：

- **用户 (User)** 1:N **订单 (Order)**
- **订单 (Order)** 1:N **订单明细 (OrderItem)**
- **商品 (Product)** 1:N **订单明细 (OrderItem)**

### 用户表 (users)

| 字段 | 类型 | 说明 |
|:-----|:-----|:-----|
| id | BIGINT | 主键 |
| username | VARCHAR(50) | 用户名 |
| email | VARCHAR(100) | 邮箱 |
| created_at | DATETIME | 创建时间 |

### 订单表 (orders)

| 字段 | 类型 | 说明 |
|:-----|:-----|:-----|
| id | BIGINT | 主键 |
| user_id | BIGINT | 外键，关联 users.id |
| total_amount | DECIMAL(10,2) | 订单总金额 |
| status | TINYINT | 0=待支付 1=已支付 2=已取消 |
