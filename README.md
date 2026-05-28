# ShopPilot

ShopPilot 是一个基于 Java + Spring Boot + Vue 3 的电商运营后台展示项目。当前已完成项目骨架、登录权限、商品管理、订单管理和 RBAC 权限菜单模块。

当前项目目录：

```text
F:\Project_by_codex\shop-pilot
```

## 技术栈

后端：

- Java 17
- Spring Boot 3.3.5
- Maven
- MySQL 8
- MyBatis-Plus 3.5.9
- Redis
- JWT
- Lombok
- Spring Validation

前端：

- Vue 3
- Vite
- TypeScript
- Element Plus
- Pinia
- Axios
- Vue Router

## 项目结构

```text
shop-pilot/
|-- .git/
|-- .github/
|-- backend/
|   |-- pom.xml
|   `-- src/
|-- frontend/
|   |-- package.json
|   |-- vite.config.ts
|   `-- src/
|-- docs/
|   `-- sql/
|       |-- 001_sys_user.sql
|       |-- 002_product_category.sql
|       |-- 003_orders.sql
|       `-- 004_rbac.sql
|-- .gitignore
`-- README.md
```

## 环境准备

请确认本机已安装：

- JDK 17
- Maven 3.8+
- Node.js 18+
- MySQL 8
- Redis

## 后端配置

后端配置文件：

```text
backend/src/main/resources/application.yml
```

默认端口：

```text
http://localhost:8080
```

默认数据库连接：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/shop_pilot?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: ${SHOP_PILOT_DB_USERNAME:root}
    password: ${SHOP_PILOT_DB_PASSWORD:}
  data:
    redis:
      host: localhost
      port: 6379
```

本地开发可用 `backend/src/main/resources/application-local.yml` 覆盖数据库账号密码。

## 数据库初始化

先创建数据库：

```sql
CREATE DATABASE shop_pilot DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

然后按顺序执行：

```text
docs/sql/001_sys_user.sql
docs/sql/002_product_category.sql
docs/sql/003_orders.sql
docs/sql/004_rbac.sql
```

RBAC 初始化账号：

```text
admin / admin123      系统管理员，拥有商品、订单和全部按钮权限
operator / admin123   订单运营，只能看到工作台和订单管理
```

## 启动后端

```bash
cd F:\Project_by_codex\shop-pilot\backend
mvn spring-boot:run
```

健康检查：

```bash
curl http://localhost:8080/api/health
```

## 启动前端

```bash
cd F:\Project_by_codex\shop-pilot\frontend
npm install
npm run dev
```

前端地址：

```text
http://localhost:5173
```

开发环境下，Vite 会把 `/api` 请求代理到 `http://localhost:8080`。

## 登录与 RBAC

登录接口：

```text
POST /api/auth/login
```

登录成功后返回：

- JWT token
- 用户信息
- 角色编码列表
- 权限标识列表
- 菜单树

示例：

```bash
curl -X POST http://localhost:8080/api/auth/login ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"admin\",\"password\":\"admin123\"}"
```

受保护接口需要携带：

```text
Authorization: Bearer <token>
```

## RBAC 表

```text
sys_user        用户表
sys_role        角色表
sys_menu        菜单和按钮权限表
sys_user_role   用户角色关联表
sys_role_menu   角色菜单关联表
```

菜单权限示例：

```text
dashboard:view
product:view
product:add
product:edit
product:delete
product:status
order:view
order:detail
order:status
```

## 商品接口

商品接口均需要登录。

```text
GET    /api/categories/options       商品分类选项
GET    /api/products                 商品分页查询，支持 page、size、keyword、categoryId、status
GET    /api/products/{id}            商品详情
POST   /api/products                 新增商品
PUT    /api/products/{id}            编辑商品
PATCH  /api/products/{id}/status     修改上下架状态，status: 1 上架，0 下架
DELETE /api/products/{id}            删除商品
```

## 订单接口

订单接口均需要登录。

```text
GET   /api/orders/status-options     订单状态选项
GET   /api/orders                    订单分页查询，支持 page、size、keyword、status
GET   /api/orders/{id}               订单详情，包含订单明细
PATCH /api/orders/{id}/status        修改订单状态
```

订单状态：

```text
0 待付款
1 待发货
2 已发货
3 已完成
4 已取消
```

允许的状态流转：

```text
待付款 -> 待发货、已取消
待发货 -> 已发货、已取消
已发货 -> 已完成
已完成 -> 终态
已取消 -> 终态
```

## 测试流程

1. 启动 MySQL 和 Redis。
2. 创建 `shop_pilot` 数据库。
3. 依次执行 `docs/sql/001_sys_user.sql`、`002_product_category.sql`、`003_orders.sql`、`004_rbac.sql`。
4. 启动后端：`cd F:\Project_by_codex\shop-pilot\backend && mvn spring-boot:run`。
5. 启动前端：`cd F:\Project_by_codex\shop-pilot\frontend && npm run dev`。
6. 打开 `http://localhost:5173`。
7. 使用 `admin / admin123` 登录，确认左侧菜单显示 `工作台`、`商品管理`、`订单管理`，商品页显示新增、编辑、删除、上下架按钮。
8. 退出后使用 `operator / admin123` 登录，确认左侧菜单只显示 `工作台`、`订单管理`，不会显示商品菜单。
9. 在订单页确认运营账号可以查看详情和变更订单状态。
10. 清理浏览器 localStorage 后重新登录，可验证菜单树和动态路由会重新生成。

## 常用命令

后端测试：

```bash
cd F:\Project_by_codex\shop-pilot\backend
mvn test
```

前端构建：

```bash
cd F:\Project_by_codex\shop-pilot\frontend
npm run build
```

前端预览：

```bash
cd F:\Project_by_codex\shop-pilot\frontend
npm run preview
```
