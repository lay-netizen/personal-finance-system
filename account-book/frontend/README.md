# 前端（Vue3 + Vite + Element Plus）

## 开发启动（Windows PowerShell）

1. 安装依赖（首次）
```
cd frontend
npm install
```

2. 启动开发服务器（注意 PowerShell 不要使用 &&）
```
cd frontend
npm run dev
```
启动后，访问控制台提示地址（如 http://localhost:5173 ）。

> 已在 `vite.config.js` 配置了开发代理：将以 `/api` 开头的请求转发到 `http://localhost:8080`，无需手工处理 CORS。

## 生产构建
```
cd frontend
npm run build
```

## 目录说明
- `src/App.vue`：首页布局（核心概览、快速操作、近期流水、分类分析）
- `src/api/http.js`：Axios 实例
- `vite.config.js`：Vite 配置，含本地代理
