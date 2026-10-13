# FastAPI 后端与 Android 动态网络编程实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建独立的 FastAPI 后端服务（端口 8000，提供 channel、news、goods、cart 等 RESTful 接口并支持 GET/POST），同时对 Android 客户端的网络请求层进行升级，支持一键切换 Flask/FastAPI 引擎并实现数据的动态拉取与展示，完全不影响原有的 Flask 服务。

**Architecture:**
1. 后端：基于 FastAPI + Uvicorn 搭建独立服务，定义统一的 `/api/v1` 路由，提供完整的 Swagger UI；复用稳定的爬虫抓取逻辑与数据源。
2. Android 端：使用原生 `HttpURLConnection` 构建零外部依赖的 `ApiClient` 网络请求工具（支持异步 GET/POST）；在 `CampusNetworkDialog` 中新增引擎一键切换按钮；改造频道、商品、购物车及新闻详情的数据请求，由网络驱动并保留本地离线 fallback。

**Tech Stack:**
- Python: FastAPI, Uvicorn, Requests, BeautifulSoup4
- Android: Java 8, HttpURLConnection, org.json, Android SDK 32

---

### Task 1: 编写 FastAPI 独立后端服务 (`campus_news_fastapi.py`)

**Files:**
- Create: `campus_news_fastapi.py`
- Create: `启动校园新闻服务_FastAPI.bat`

- [ ] **Step 1: 创建 `campus_news_fastapi.py`**
  - 定义统一响应格式 `{"code": 200, "msg": "success", "data": ...}`
  - 实现 `GET /api/health`（返回 `service: campus-news`, `engine: fastapi`, `port: 8000`）
  - 实现 `GET /api/v1/channel` 与 `POST /api/v1/channel`
  - 实现 `GET /api/v1/news`、`GET /api/v1/news/{id}` 与 `POST /api/v1/news/search`
  - 实现 `GET /api/v1/goods` 与 `POST /api/v1/goods/filter`
  - 实现 `GET /api/v1/cart`、`POST /api/v1/cart/add` 与 `POST /api/v1/cart/update`
  - 实现 `GET /api/image_proxy`
  - 支持向后兼容的旧路由映射（如 `/api/news`）

- [ ] **Step 2: 创建 Windows 一键启动脚本 `启动校园新闻服务_FastAPI.bat`**
  - 检测 Python 环境
  - 检查并自动安装 `fastapi`, `uvicorn`
  - 启动 `uvicorn campus_news_fastapi:app --host 0.0.0.0 --port 8000`

- [ ] **Step 3: 验证 FastAPI 启动与接口响应**
  - 使用 Python 测试脚本验证 `/api/health`, `/api/v1/channel`, `/api/v1/goods`, `/api/v1/cart` 均能正确返回。

---

### Task 2: 编写 Android 原生网络请求库 (`ApiClient.java`)

**Files:**
- Create: `app/src/main/java/com/example/toutiao/demo/ApiClient.java`

- [ ] **Step 1: 实现 `ApiClient.java`**
  - 封装异步 `get(String url, Callback callback)`
  - 封装异步 `post(String url, String jsonBody, Callback callback)`
  - 使用 `HttpURLConnection` 处理超时与流读取
  - 使用 `Handler(Looper.getMainLooper())` 确保回调在 UI 线程执行
  - 定义 `Callback` 接口（`onSuccess(JSONObject result)` 与 `onError(String msg)`）

- [ ] **Step 2: 验证网络客户端类编译无误**

---

### Task 3: 改造网络设置弹窗与引擎切换 (`CampusNetworkConfig` & `CampusNetworkDialog`)

**Files:**
- Modify: `app/src/main/java/com/example/toutiao/demo/CampusNetworkConfig.java`
- Modify: `app/src/main/java/com/example/toutiao/demo/CampusNetworkDialog.java`

- [ ] **Step 1: 更新 `CampusNetworkConfig.java`**
  - 添加引擎常量 `ENGINE_FLASK = "flask"`, `ENGINE_FASTAPI = "fastapi"`
  - 添加 `FASTAPI_EMULATOR_URL = "http://10.0.2.2:8000"`
  - 添加获取 BaseURL 的方法 `getApiBaseUrl()`（FastAPI 自动拼接 `/api/v1`，Flask 保持 `/`）
  - 保存和加载当前引擎设置

- [ ] **Step 2: 更新 `CampusNetworkDialog.java`**
  - 添加单选按钮：“Flask (5000)” 与 “FastAPI (8000)”
  - 切换时联动更新输入框中的端口与默认地址
  - 测试连接时，根据 `/api/health` 返回的 `engine` 字段显示连接成功的后端类型

---

### Task 4: 改造频道与新闻业务层，实现网络动态更新

**Files:**
- Modify: `app/src/main/java/com/example/toutiao/demo/ChannelStore.java`
- Modify: `app/src/main/java/com/example/toutiao/demo/HomeFragment.java` (如需要接入动态刷新)

- [ ] **Step 1: 改造 `ChannelStore.java`**
  - 新增 `fetchChannelsFromNetwork(Runnable onComplete)` 方法，向 `baseurl/channel` 发起 GET 请求
  - 新增 `syncChannelsToNetwork(List<Channel> channels)` 方法，向 `baseurl/channel` 发起 POST 请求
  - 网络拉取失败或离线时，优雅回退到现有本地持久化配置

---

### Task 5: 改造商城与购物车，实现网络动态更新

**Files:**
- Modify: `app/src/main/java/com/example/toutiao/demo/ShopFragment.java`
- Modify: `app/src/main/java/com/example/toutiao/demo/ShoppingDialogs.java`

- [ ] **Step 1: 改造 `ShopFragment.java`**
  - 在 `initShopData()` 中调用 `GET /api/v1/goods`
  - 请求成功后将返回的商品列表更新到 `productList` 并通知 `shopAdapter` 刷新
  - 若网络不可用或返回失败，自动回退到 `buildCatalog()` 本地静态数据

- [ ] **Step 2: 改造 `ShoppingDialogs.java`**
  - 加购时调用 `POST /api/v1/cart/add` 同步后端
  - 打开购物车时调用 `GET /api/v1/cart` 动态获取购物车明细与总价

---

### Task 6: 联调验证与单元/构建测试

**Files:**
- Test verification across Python server & Android build

- [ ] **Step 1: 测试 FastAPI 各接口在 8000 端口的表现（GET 和 POST）**
- [ ] **Step 2: 运行 Gradle 编译与单元测试 (`./gradlew assembleDebug testDebugUnitTest`)**
- [ ] **Step 3: 验证原 Flask 服务 (`campus_news_server.py`) 仍然可以正常独立启动不受任何影响**
