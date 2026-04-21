# 高校实验室预约系统

## 项目概览

面向高校师生的实验室在线预约系统，支持实验室查询、在线预约申请、管理员审核、消息通知等核心功能。
//
### 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue3 + Element Plus + Pinia + Vite |
| 后端 | Spring Boot 3.2 + MyBatis + MySQL 8.0 |
| 鉴权 | JWT（jjwt 0.11） |
| 连接池 | HikariCP |

---

## 项目结构

```
lab-reservation-system/
├── backend/                     # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/lab/reservation/
│       │   ├── config/          # 安全配置、JWT拦截器、全局异常处理
│       │   ├── controller/      # REST 控制器
│       │   ├── service/         # 业务逻辑层
│       │   ├── mapper/          # MyBatis Mapper 接口
│       │   ├── entity/          # 数据库实体类
│       │   ├── dto/             # 请求数据传输对象
│       │   ├── vo/              # 响应封装对象
│       │   └── util/            # JWT工具、线程上下文
│       └── resources/
│           ├── application.yml  # 配置文件
│           ├── schema.sql       # 数据库初始化脚本
│           └── mapper/          # MyBatis XML 映射文件
│
└── frontend/                    # Vue3 前端
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── api/                 # axios 封装 + 接口定义
        ├── store/               # Pinia 状态管理
        ├── router/              # Vue Router 路由配置
        ├── assets/styles/       # 全局 CSS（flex+rem 移动端适配）
        └── views/
            ├── Login.vue        # 登录页
            ├── Register.vue     # 注册页
            ├── user/            # 用户端页面
            │   ├── Layout.vue       # 带底部导航栏的布局
            │   ├── LabList.vue      # 实验室列表
            │   ├── BookLab.vue      # 预约页（含冲突检测）
            │   ├── MyReservations.vue # 我的预约
            │   └── Notices.vue      # 消息通知
            └── admin/           # 管理端页面
                ├── Layout.vue       # 侧边栏布局
                ├── Dashboard.vue    # 数据看板
                ├── Audit.vue        # 预约审核
                ├── LabManage.vue    # 实验室管理
                └── Blacklist.vue    # 黑名单管理
```

---

## 数据库设计（8张核心表）

| 表名 | 说明 |
|------|------|
| `user` | 用户表（学生/管理员） |
| `lab` | 实验室基础信息表 |
| `time_slot` | 实验室开放时段模板表 |
| `reservation` | 预约记录表（含 `cancel_time` 字段） |
| `notice` | 站内消息通知表 |
| `lab_admin` | 实验室-管理员关联表 |
| `blacklist` | 黑名单表 |
| `statistics_log` | 统计日志表 |

**冲突检测核心索引：**

```sql
KEY `idx_lab_time` (`lab_id`, `start_time`, `end_time`)
```

**冲突检测 SQL 逻辑（区间重叠判断）：**

```sql
SELECT COUNT(*) FROM reservation
WHERE lab_id = ? AND status IN (0, 1)
  AND start_time < #{newEndTime}
  AND end_time   > #{newStartTime}
```

---

## 快速启动

### 后端

**1. 准备数据库**

```bash
mysql -u root -p < backend/src/main/resources/schema.sql
```

**2. 修改配置**

编辑 `backend/src/main/resources/application.yml`，填写你的 MySQL 密码：

```yaml
spring:
  datasource:
    password: your_password_here
```

**3. 启动后端**

```bash
cd backend
mvn spring-boot:run
```

后端运行在 `http://localhost:8080`

---

### 前端

**1. 安装依赖**

```bash
cd frontend
npm install
```

**2. 启动开发服务器**

```bash
npm run dev
```

前端运行在 `http://localhost:5173`，已配置 `/api` 代理到 `localhost:8080`。

---

## 接口说明

| 方法 | 路径 | 说明 | 需要登录 |
|------|------|------|---------|
| POST | `/api/auth/login` | 登录 | 否 |
| POST | `/api/auth/register` | 注册 | 否 |
| GET | `/api/labs` | 实验室列表 | 否 |
| GET | `/api/labs/{id}/slots?date=` | 查询某天空闲时段 | 否 |
| POST | `/api/reservations` | 提交预约 | 是 |
| GET | `/api/reservations/my` | 我的预约 | 是 |
| PUT | `/api/reservations/{id}/cancel` | 取消预约 | 是 |
| GET | `/api/reservations/pending` | 待审核列表 | 管理员 |
| PUT | `/api/reservations/{id}/audit` | 审核预约 | 管理员 |
| GET | `/api/notices` | 通知列表 | 是 |
| GET | `/api/admin/dashboard` | 数据看板 | 管理员 |
| GET | `/api/admin/blacklist` | 黑名单列表 | 管理员 |

**JWT 使用方式：**

```
Authorization: Bearer <token>
```

---

## 默认测试账号

| 账号 | 密码 | 角色 |
|------|------|------|
| admin | 123456 | 管理员 |
| student001 | 123456 | 普通用户 |
| student002 | 123456 | 普通用户 |

---

## 核心功能说明

### 预约冲突检测
预约提交时，后端通过**区间重叠 SQL 查询**（`start < newEnd AND end > newStart`）检测冲突，防止同一时段被多人预约。前端在用户选择时间后**实时预检**，利用已加载的时段数据做即时反馈，减少无效提交。

### 1小时提醒通知
后端通过 `@Scheduled` 定时任务（每5分钟执行），查询 1 小时内即将开始且尚未发送提醒的预约，自动向用户发送站内信通知。

### 移动端适配
- 全局 CSS 使用 `flex` 布局 + `rem` 单位
- 移动端（< 768px）底部导航栏替代顶部 Tab
- `viewport` 禁止用户缩放，保障移动端体验一致性
- iOS 安全区域适配（`env(safe-area-inset-bottom)`）
