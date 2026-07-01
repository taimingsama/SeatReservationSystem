# Web API 清单

> 摘自 `2026-06-30-webapi-design.md`

## 认证（公开）

```
POST   /api/auth/register    UC-01  注册
POST   /api/auth/login       UC-02  登录
```

## 认证（需登录）

```
GET    /api/auth/me          UC-03  获取当前用户信息
```

## 自习室

```
GET    /api/rooms            UC-04  获取所有 OPEN 状态的自习室
GET    /api/rooms/{id}/seats UC-05  获取某自习室所有座位及状态
POST   /api/admin/rooms      UC-06  创建自习室（管理员）
PUT    /api/admin/rooms/{id} UC-06  更新自习室（管理员）
DELETE /api/admin/rooms/{id} UC-06  删除自习室（管理员）
```

## 座位

```
POST   /api/admin/seats      UC-07  创建座位（管理员）
PUT    /api/admin/seats/{id} UC-07  更新座位（管理员）
DELETE /api/admin/seats/{id} UC-07  删除座位（管理员）
```

## 预约

```
POST   /api/reservations               UC-08  创建预约（学生）
POST   /api/reservations/{id}/check-in  UC-09  签到（学生）
POST   /api/reservations/{id}/check-out UC-10  退座（学生）
DELETE /api/reservations/{id}          UC-11  取消预约（学生）
GET    /api/reservations/my            UC-12  查看我的预约（学生）
GET    /api/admin/reservations         UC-13  查看所有预约（管理员）
```

## 统计

```
GET    /api/admin/stats                UC-15  数据统计（管理员）
```

## 端点总览

| 方法     | 路径                                 | 用例    | 权限   |
|--------|------------------------------------|-------|------|
| POST   | `/api/auth/register`               | UC-01 | 公开   |
| POST   | `/api/auth/login`                  | UC-02 | 公开   |
| GET    | `/api/auth/me`                     | UC-03 | 登录用户 |
| GET    | `/api/rooms`                       | UC-04 | 公开   |
| GET    | `/api/rooms/{id}/seats`            | UC-05 | 公开   |
| POST   | `/api/admin/rooms`                 | UC-06 | 管理员  |
| PUT    | `/api/admin/rooms/{id}`            | UC-06 | 管理员  |
| DELETE | `/api/admin/rooms/{id}`            | UC-06 | 管理员  |
| POST   | `/api/admin/seats`                 | UC-07 | 管理员  |
| PUT    | `/api/admin/seats/{id}`            | UC-07 | 管理员  |
| DELETE | `/api/admin/seats/{id}`            | UC-07 | 管理员  |
| POST   | `/api/reservations`                | UC-08 | 学生   |
| POST   | `/api/reservations/{id}/check-in`  | UC-09 | 学生   |
| POST   | `/api/reservations/{id}/check-out` | UC-10 | 学生   |
| DELETE | `/api/reservations/{id}`           | UC-11 | 学生   |
| GET    | `/api/reservations/my`             | UC-12 | 学生   |
| GET    | `/api/admin/reservations`          | UC-13 | 管理员  |
| GET    | `/api/admin/stats`                 | UC-15 | 管理员  |

> 注：UC-14（AutoReleaseUseCase）是系统定时任务，不暴露 HTTP 端点。
