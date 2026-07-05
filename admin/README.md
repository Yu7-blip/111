# 平台管理后台

基于 Vue 3 + Element Plus 的外卖平台管理后台，使用 Mock 数据实现完整的前端静态演示。

## 技术栈

- Vue 3 + Vite
- Element Plus + Icons
- Pinia 状态管理
- Vue Router 路由
- ECharts 图表
- SCSS 样式

## 功能模块

| 模块 | 路径 | 说明 |
|------|------|------|
| 首页 | /dashboard | 数据统计 + 订单趋势图 |
| 用户管理 | /users | 用户列表、增删改查 |
| 商家管理 | /merchants | 商家列表、审核 |
| 商家审核 | /merchants/audit | 待审核商家管理 |
| 骑手管理 | /delivery | 骑手状态管理 |
| 订单管理 | /orders | 订单列表、详情 |
| 满减活动 | /marketing/full-reduce | 活动配置管理 |

## 启动

```bash
npm install
npm run dev
```

登录账号：admin / 123456
