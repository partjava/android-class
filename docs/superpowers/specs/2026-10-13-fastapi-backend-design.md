# 晴川校园应用 - FastAPI 后端接口与 Android 网络编程设计规范

**日期**：2026-10-13  
**状态**：已批准 (Approved)  

---

## 1. 核心目标与原则

1. **原服务完全隔离与保护**：现有 Flask 服务 (`campus_news_server.py:5000`) 100% 保持不变，零修改、不删除。
2. **新增现代化 FastAPI 服务**：新建 `campus_news_fastapi.py`，默认监听 `8000` 端口，提供 Swagger UI 交互式文档 (`http://localhost:8000/docs`)。
3. **API 规范化**：基础路径为 `baseurl = http://<ip>:8000/api/v1`，数据返回统一格式。
4. **双请求协议**：必须支持 `GET` 和 `POST` 两种请求方法。
5. **Android 动态驱动**：修改之前写死的 Mock 数据，实现频道、新闻、商品、购物车四大模块网络动态更新，同时保留离线保底逻辑，无闪退风险。
6. **一键切换**：在 App 校园网络配置对话框中提供“Flask (5000) / FastAPI (8000)”一键切换按钮。

---

## 2. API 规范设计 (`baseurl/接口地址`)

### 2.1 统一响应格式
```json
{
  "code": 200,
  "msg": "success",
  "data": {}
}
```

### 2.2 接口详细清单

| 模块 | 接口地址 | 方法 | 说明 |
| :--- | :--- | :--- | :--- |
| **健康检测** | `/api/health` | `GET` | 返回服务状态、引擎标识 (`fastapi`) 与端口 |
| **频道** | `/api/v1/channel` | `GET` | 获取当前选中的频道列表与待选频道列表 |
| **频道** | `/api/v1/channel` | `POST` | 保存提交用户定制的频道订阅顺序 |
| **新闻列表** | `/api/v1/news` | `GET` | 分页获取校园/实时新闻流 (`channel`, `page`, `size`) |
| **新闻详情** | `/api/v1/news/{id}` | `GET` | 获取指定 ID 的新闻全文及图文 blocks |
| **新闻搜索** | `/api/v1/news/search`| `POST`| 提交搜索关键词及分类参数进行查询 |
| **商品列表** | `/api/v1/goods` | `GET` | 获取商城商品列表（支持 `category` 筛选） |
| **商品筛选** | `/api/v1/goods/filter`| `POST`| 提交价格区间、分类排序等复杂筛选条件 |
| **购物车** | `/api/v1/cart` | `GET` | 获取购物车商品、件数与总价小计 |
| **购物车加购**| `/api/v1/cart/add` | `POST` | 添加商品到购物车 (`product_id`, `count`) |
| **购物车更新**| `/api/v1/cart/update` | `POST` | 更新数量或移除商品 (`product_id`, `count`) |
| **图片代理** | `/api/image_proxy` | `GET` | 转发学校官网新闻图片（防盗链/SSL证书适配） |

---

## 3. Android 端改造设计

### 3.1 网络客户端 (`ApiClient.java`)
- 基于原生 `HttpURLConnection` 实现，无外部第三方网络库依赖（零 Gradle 冲突）。
- 支持 `get(url, callback)` 与 `post(url, jsonBody, callback)`。
- 主线程 `Handler` 自动切换回调。

### 3.2 引擎切换与配置 (`CampusNetworkConfig` & `CampusNetworkDialog`)
- 在网络配置对话框添加 `Flask (5000)` 与 `FastAPI (8000)` 单选切换按钮。
- 切换时自动适配 `10.0.2.2:5000` / `10.0.2.2:8000` 或保留局域网 IP 仅切换端口。
- 记录引擎状态，自动使用对应 BaseURL。

### 3.3 业务模块动态化
- **频道** (`ChannelStore.java`)：优先向 `GET /api/v1/channel` 拉取并保存，POST 提交修改，失败回退本地。
- **商品** (`ShopFragment.java`)：向 `GET /api/v1/goods` 拉取商品，刷新 StaggeredGridLayout，保留本地作为离线保底。
- **购物车** (`ShoppingDialogs.java`)：加购走 `POST /api/v1/cart/add`，打开购物车走 `GET /api/v1/cart`。
- **新闻详情**：点击新闻时根据 ID 查询 `GET /api/v1/news/{id}`。

---

## 4. 文件列表
- 新建 `campus_news_fastapi.py`
- 新建 `启动校园新闻服务_FastAPI.bat`
- 新建 `app/src/main/java/com/example/toutiao/demo/ApiClient.java`
- 修改 `app/src/main/java/com/example/toutiao/demo/CampusNetworkConfig.java`
- 修改 `app/src/main/java/com/example/toutiao/demo/CampusNetworkDialog.java`
- 修改 `app/src/main/java/com/example/toutiao/demo/ChannelStore.java`
- 修改 `app/src/main/java/com/example/toutiao/demo/ShopFragment.java`
- 修改 `app/src/main/java/com/example/toutiao/demo/ShoppingDialogs.java`
- 零改动保护 `campus_news_server.py`
