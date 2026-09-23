# 仿今日头条 · Android 课程设计

一个纯 Java + XML 布局实现的今日头条客户端复刻，覆盖登录、信息流、新闻详情、视频播放、商城、消息聊天、个人中心、设置、编辑资料等多条完整页面链路。

数据全部内置在代码里，**不联网、无后台**，装上就能跑。

---

## 运行截图

| 一键登录 | 密码登录 | 首页信息流 |
|---|---|---|
| ![](screenshots/01_login_onekey.png) | ![](screenshots/02_login_pwd.png) | ![](screenshots/03_home.png) |

| 新闻详情 | 我的 | 设置 | 编辑资料 |
|---|---|---|---|
| ![](screenshots/04_news_detail.png) | ![](screenshots/05_mine.png) | ![](screenshots/06_settings.png) | ![](screenshots/07_edit_profile.png) |

> 商城、消息、聊天、视频这几个页面是后加的，截图还没补（`screenshots/` 目前只有 01~07）。

---

## 功能

### 登录

- **一键登录**：协议勾选 + 一键登录按钮 + 手机号 / Apple / 更多三个第三方入口
- **密码登录**：`+86` 区号选择、账号密码输入、找回密码、跳转手机号登录
- 两页之间可互相跳转，协议未勾选时给出提示

### 首页

- 顶部品牌红搜索栏，7 个频道标签：推荐 / 抗疫 / 小视频 / 北京 / 视频 / 热点 / 娱乐
- 前 6 个频道各自一套新闻数据，用 `RecyclerView` + `NewsMultiAdapter` 渲染，支持 **4 种 item 布局**：
  - 纯文字
  - 单图（左文右图）
  - 三图（文下三张并排）
  - 视频（封面 + 播放键）
- **热点**频道换的是另一套数据：`ListView` + `ArrayAdapter` 渲染的大国工匠列表，点击条目弹出人物详情弹窗
- 底部 5 项导航：首页 / 视频 / 添加 / 商城 / 我的（除「添加」是提示外，其余 4 项都接上了对应页面）

### 商城

- **顶部频道标签**（关注 / 推荐 / 闪购 / 国补 / 飞猪 / 新风潮 / 穿搭）：`HorizontalScrollView` 里塞一排 tab，7 个一屏放不下、可左右滑动；选中的带红色圆角下划线，并按商品分类过滤下方瀑布流的数据
- **宫格入口**（两页，左右翻页 + 圆点指示）：横向 `RecyclerView` + `PagerSnapHelper` 实现淘宝式整页翻页，一页就是一个 5 列 `GridLayout`；25 个图标从淘宝截图裁出（`drawable-nodpi/shop_entry_01~25.png`），图标下带文字、有点击波纹
- **两列瀑布流**：`RecyclerView` + `StaggeredGridLayoutManager`，12 个商品卡片高低错落；错落感来自图片本身——商品图统一 400px 宽但高度各不相同（300~560px），item 布局用 `adjustViewBounds` 让高度自适应
- 顶栏品牌红 + 搜索入口（演示），底部导航与首页 / 我的共用同一份菜单，可互相跳转
- 价格统一两位小数，已售过万显示成"x.x万件"

### 视频

- **顶部频道标签**：推荐 / 小视频 / 影视 / 音乐，复用商城页的 `item_channel_tab` 布局（同一个文件被两个页面共用），选中的带红色圆角下划线，可左右滑动
- **单列大封面卡片流**：`RecyclerView` + `LinearLayoutManager`，卡片结构为大封面（16:9，`centerCrop`）+ 居中播放键 + 右下角时长标签 + 标题（最多两行）+ 来源 / 播放量
- **播放页**：通栏大封面 + 居中播放键，点击在播放 / 暂停之间切换，并驱动一条**模拟进度条**——每 200ms 推进 2%，十秒走满后停在满格不循环（项目没有视频文件，也不联网，播放是模拟的）
- **相关视频**：播放页底部的紧凑列表，点击时用 `FLAG_ACTIVITY_CLEAR_TOP | SINGLE_TOP` 复用当前实例（走 `onNewIntent` 换数据），反复点不会堆栈
- 首页「视频」频道里的 4 条视频也接到同一个播放页。`News` 模型没有时长和播放量字段，播放页取不到时会把这两个控件**藏掉而不是编造数据**

### 我的

- 头像、昵称、IP 属地标签
- 关注 / 粉丝 / 获赞三项数据
- 九宫格功能入口（消息私信、浏览历史、创作中心、书架、购物/订单、收藏、客服中心、退款/售后），部分带角标
- 头条聊天室卡片
- 作品区：收藏 / 赞过子标签 + 全部 / 视频 / 微头条筛选胶囊

### 消息与聊天

- **会话列表**：8 个会话，每行是圆底头像 + 会话名 + 最后一条消息 + 时间；未读带红色角标，未读为 0 时角标置 `GONE`（不是 `INVISIBLE`），否则时间会被挤得对不齐
- 头像用 5 个矢量圆底（蓝 / 绿 / 橙 / 紫，加一个红底白铃铛的「系统通知」），不依赖位图
- **聊天详情**：左右两种气泡布局。输入框是真的能发的——点「发送」把气泡追加到列表底部并清空输入框，一秒后再自动补一条对方回复（本地定时器造的，不联网）
- 全项目唯一有软键盘交互的页面：`AndroidManifest` 里配了 `windowSoftInputMode="adjustResize"`，不配置的话键盘弹出来会直接盖住底部输入栏
- 入口有两个：我的页右上角的信封图标（改造前**完全没绑点击事件**）、九宫格「消息私信」（改造前只弹一句 Toast）

### 设置

26 个设置项，覆盖账号与安全、隐私设置、深色模式、大字模式、字体大小、图文详情滑动方式等，其中 7 项是开关控件，底部为退出登录。

### 编辑资料

资料完成度进度、头像更换、用户名 / 简介 / 背景图 / 性别 / 生日 / 所在地等 9 项，已填与未填的值用不同颜色区分。

---

## 环境与依赖

| 项 | 版本 |
|---|---|
| Android Gradle Plugin | 7.3.1 |
| Gradle | 7.4 |
| compileSdk / targetSdk | 32 |
| minSdk | 29 |
| Java 版本 | 源码兼容 8（运行 Gradle 需 JDK 11 及以上） |
| 开发工具 | Android Studio Dolphin (2021.3.1) |

依赖只有两个（无网络库、无图片加载库；`RecyclerView` 由 material 传递引入）：

```gradle
implementation 'androidx.appcompat:appcompat:1.4.1'
implementation 'com.google.android.material:material:1.5.0'
```

---

## 如何运行

1. 用 Android Studio 打开项目根目录，等待 Gradle Sync 完成
2. 直接运行 `app` 模块，或命令行构建：

```bash
./gradlew assembleDebug
```

> **注意**：`local.properties` 记录的是本机 SDK 路径，已被 `.gitignore` 排除，不会上传。克隆后 Android Studio 会在首次打开时自动生成，无需手动处理。

---

## 项目结构

```
app/src/main/
├── java/com/example/toutiao/demo/
│   ├── LoginOneKeyActivity.java    启动页，一键登录
│   ├── LoginPwdActivity.java       密码登录
│   ├── HomeActivity.java           首页信息流 + 大国工匠列表
│   │                               （内含 Craftsman / CraftsmanAdapter 内部类）
│   ├── NewsDetailActivity.java     新闻详情
│   ├── VideoActivity.java          视频频道页（单列大封面卡片流）
│   ├── VideoDetailActivity.java    视频播放页（模拟播放进度）
│   ├── ShopActivity.java           商城（两列瀑布流）
│   ├── MsgActivity.java            消息会话列表
│   ├── ChatActivity.java           聊天详情（可发送 + 自动回复）
│   ├── MineActivity.java           个人中心
│   ├── SettingsActivity.java       设置
│   ├── EditProfileActivity.java    编辑资料
│   ├── News.java                   新闻数据模型
│   ├── NewsMultiAdapter.java       多布局 RecyclerView 适配器
│   ├── VideoItem.java              视频数据模型
│   ├── VideoAdapter.java           视频列表适配器
│   ├── ShopItem.java               商品数据模型
│   ├── ShopAdapter.java            瀑布流适配器
│   ├── MsgItem.java                会话数据模型
│   ├── MsgAdapter.java             会话列表适配器
│   ├── ChatMessage.java            聊天消息模型
│   └── ChatAdapter.java            聊天气泡适配器（左右两种布局）
│
└── res/
    ├── layout/        27 个布局文件
    ├── drawable/      矢量图标与图片资源
    ├── drawable-nodpi/  商城商品照片 + 淘宝宫格图标（按原始像素渲染，不随密度缩放）
    ├── values/        colors / dimens / styles / themes / strings
    ├── values-night/  深色模式下的主题定义
    ├── color/         状态着色表（底部导航、按钮）
    └── menu/          底部导航菜单
```

---

## UI 设计说明

页面样式没有散落在各个布局里硬编码，而是收敛成了一套可复用的资源体系：

### 设计变量

- **`colors.xml`** —— 语义化色板，共 28 个 token。品牌红 `#E63939`、三级文字色、三级背景色、图标色、分隔线色，替代原先散落在各布局里的硬编码色值
- **`dimens.xml`** —— 字号收成 6 档（`text_display` / `text_headline` / `text_title` / `text_body` / `text_body_small` / `text_caption`），间距按 4dp 栅格收成 7 档
- **`styles.xml`** —— 25 个 style，把三处高频重复抽成模板：
  - 设置项行（设置页 26 行 + 编辑资料页 9 行）
  - 分隔线（全项目曾手写 80 处）
  - 底部导航（首页 / 商城 / 我的 / 视频 4 个页面共用；改造前是逐字重复的 11 行）

### 主题

`Theme.Toutiao` 基于 `Theme.MaterialComponents.Light.NoActionBar`，配好 `colorPrimary` / `colorSecondary` / `statusBarColor` / 文字默认色，并通过 `AndroidManifest.xml` 真正挂到应用上。

### 深色模式的防御

这套布局是**强制浅色**的（硬编码浅色底 + 深色字），所以对系统深色模式做了三重防御，保证深色设备上表现与浅色完全一致：

1. 主题 parent 用 `Light` 而非 `DayNight`，不激活系统的深色资源切换
2. `android:forceDarkAllowed="false"` 挡掉 Android 10 的强制反色
3. `values-night/themes.xml` 写入与 `values/` 逐项一致的定义，避免主题属性在深色设备上缺失

登录、首页、新闻详情、我的、设置、编辑资料几个页面已在模拟器上开启系统深色模式逐页验证，与浅色模式表现一致。后加的商城 / 消息 / 聊天 / 视频页沿用同一套颜色 token、没有引入硬编码色值，理论上表现相同，但**尚未逐页重新验证**。

### 视频封面比例的一个取舍

视频封面用的是标准 16:9，但项目里的图**没有一张是横构图**——`image*` 是分辨率最高的一组，却全是竖图或近方图（最方的 `image7` 是 939×925）。`centerCrop` 到 16:9 后保留的原图高度 = 宽高比 ÷ 1.778：

| 素材 | 尺寸 | 16:9 保留高度 |
|---|---|---|
| `image1` / `image4` | 640×1386 / 1000×2166 | 26% |
| `image3` | 960×1707 | 32% |
| `image2` / `image5` | 1080×1447 / 960×1280 | 42% |
| `image7` | 939×925 | 57% |

所以视频页只挑裁得动的 `image7 / image2 / image5 / image8 / img4 / image6` 这几张用，并刻意避开只剩一条横带的 `image1` / `image4`。另外视频标题都写成「纪录片 / 影像 / 现场」这类与具体画面无关的措辞，避免裁切后出现图文错位。

封面框高度写在 `item_video.xml`（184dp = 328dp 宽 ÷ 16 × 9），想换成 4:3 只改这一个值（改成 246dp，保留高度立刻涨到 56%）。

### 顺带修掉的问题

- 首页 tab 选中色 / 未选中色原来在 Java 里硬编码 `0xFFE63939` / `0xFF333333`，与色板 token 脱节，已改为引用 `colors.xml`（`selectTab()` / `resetTabColor()`），6 个新闻标签的点击监听也收敛成 `bindNewsTab()` 一处

- 设置页「图文详情滑动方式」标题与右侧值**重叠**——原因是行高写死 64dp，而该行内容实测约 368dp，超出 360dp 的屏幕宽度。改为 `wrap_content` + `minHeight`，左侧文字列用 `0dp` + `weight=1` 主动占满剩余宽度
- 登录页按钮上的 `android:radius="12dp"` **从未生效**——该属性只对 `<shape>` 根节点有效，写在 `<Button>` 上会被忽略
- 我的页「0关注 1粉丝 1获赞」原是一个 TextView 用空格硬拼，数字变四位数就会折行，已拆成三个独立 TextView
- 详情页正文用次级灰 `#333333`，与标题层级倒挂，已改为主文字色
- 视频封面的播放键原用系统 `@android:drawable/ic_media_play`（纯黑三角），在浅色封面上几乎不可见，已重绘为半透明黑圆 + 白色三角
- 启动图标原是 Android Studio 默认的绿色机器人，已重绘为品牌红 + 白色资讯卡片
- **商城页底部导航整条是空的**——`activity_shop.xml` 里的 `BottomNavigationView` 漏了 `style="@style/Widget.Toutiao.BottomNav"`，而五个菜单项恰恰是挂在这个 style 的 `app:menu` 上的。连带后果比"没有图标"严重得多：`setSelectedItemId()` / `setOnItemSelectedListener()` 全部静默失效，商城页变成一个点不动的死胡同。同时补上了 `fitsSystemWindows` 和底部约束——原来用 `tools:ignore="MissingConstraints"` 把 lint 警告压掉了，而那个警告是对的，视图实际被扔在了左上角
- **我的页跳商城漏了 flag**——全项目就这一处没加 `CLEAR_TOP | SINGLE_TOP`，反复「我的 → 商城 → 返回」会一直堆新实例
- **商城三个频道指向同一种商品**——「飞猪 / 新风潮 / 穿搭」原本共用一条 `return`，全指向只有 1 个商品的「服饰」。而 `filterByTab()` 的兜底只在结果**为空**时才触发，1 个不算空，兜底救不了，点进去就是个孤零零的双肩包

### 一个 Material 主题的坑

切换到 Material 主题后，布局里的 `<Button>` 会被 `viewInflaterClass` 自动替换成 `MaterialButton`，而 **`MaterialButton` 会直接丢弃 `android:background`**，改用自带的 `MaterialShapeDrawable` + `backgroundTint`（默认 `?attr/colorPrimary`）填底。

后果是自定义背景 drawable 完全不生效——筛选胶囊的灰底、浅红底全被主色覆盖，红底红字导致文字不可见。给 `backgroundTint` 设 `@null` 也没用，因为背景整个被替换掉了。

最终处理：

- **筛选胶囊**改用 `<TextView>`，绕开 `MaterialButton` 的这套逻辑
- **主按钮**改用 `MaterialButton` 自己的属性表达：`backgroundTint` 指向一个 ColorStateList（`res/color/button_primary_tint.xml`）承载常态 / 按压 / 禁用三态，`cornerRadius` 负责圆角

---

## 说明

- 应用内新闻、图片、用户资料等**全部为本地内置的演示数据**，不涉及任何网络请求，也没有申请任何系统权限
- 代码结构以课程演示为目的，未做分层架构与单元测试覆盖
