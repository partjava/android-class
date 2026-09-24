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
- **双列等高网格**：`RecyclerView` + `GridLayoutManager(2)`，4 个频道共用。一格 160dp 宽、封面 16:9（`160 ÷ 16 × 9 = 90dp`，由 `layout_constraintDimensionRatio` 算出，不是写死的高度），一屏能看 4~6 条。用**等高网格**而不是商城那种瀑布流：视频封面统一 16:9，等高才排得整齐，瀑布流是给高矮不一的商品图用的
- **播放页**：竖屏全屏沉浸页 —— 进入即隐藏状态栏、整页黑底、无红顶栏，封面铺满**上半屏**，返回键浮在左上角的半透明黑圆上。点击封面在播放 / 暂停之间切换，并驱动一条**模拟进度条**——每 200ms 推进 2%，十秒走满后停在满格不循环（项目没有视频文件，也不联网，播放是模拟的）。标题 / 来源·播放量 / 简介 / 相关视频一个不丢，全在黑底信息区里
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
│   ├── VideoActivity.java          视频频道页（双列 16:9 网格）
│   ├── VideoDetailActivity.java    全屏沉浸播放页（模拟播放进度）
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

- **`colors.xml`** —— 语义化色板，共 34 个 token。品牌红 `#E63939`、三级文字色、三级背景色、图标色、分隔线色，替代原先散落在各布局里的硬编码色值。其中 6 个（`bg_player` / `text_on_dark` / `text_on_dark_secondary` / `divider_on_dark` / `press_on_dark` / `progress_track`）是视频播放页专用的深色 token
- **`dimens.xml`** —— 字号收成 6 档（`text_display` / `text_headline` / `text_title` / `text_body` / `text_body_small` / `text_caption`），间距按 4dp 栅格收成 7 档
- **`styles.xml`** —— 27 个 style，把三处高频重复抽成模板：
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

视频列表页与播放页已单独复验过：列表页在深色模式下与浅色模式**逐像素一致**（截图对比）；播放页本来就是**故意**的黑底（沉浸播放器就该是黑的），深色模式下同样保持黑底白字，没有被反色——上面第 2 条 `forceDarkAllowed=false` 挡着，验证时也确认了这一点。

### 视频封面比例的一个取舍

项目里的图**没有一张是横构图**——`image*` 是分辨率最高的一组，却全是竖图或近方图（最方的 `image7` 是 939×925）。`centerCrop` 之后保留的原图高度 = 源图宽高比 ÷ 框的宽高比，所以**框越方/越竖，裁得越少**。这条规律决定了两个页面用了两种比例：

**列表页网格卡片：16:9。** 每条视频的封面框严格 16:9，用 `0dp × 0dp` + `app:layout_constraintDimensionRatio="H,16:9"` 让高度由实际宽度算出。**这里刻意不写死高度**：一格的宽度是 `GridLayoutManager` 平分出来的、随屏幕宽度变（360dp 屏上 160dp、411dp 屏上 185dp），写死 90dp 就只在 360dp 屏上等于 16:9，换台机器就不是了——这个坑改造时真的踩到过一次，模拟器 411dp 宽下量出来是 2.06:1。

**播放页：故意不是 16:9。** 播放区用 `0dp + weight=1` 占上半屏，封面 `centerCrop` 把这块整铺满，比例由屏幕形状决定。这不是漏改：这一页的画面是**竖构图的照片而不是真视频**，没有「必须保持的原始画幅」，留黑边等于为不存在的问题付代价。

半屏框的宽高比 = 屏宽 ÷ 半屏高（本机 411.43 ÷ 353.52 ≈ **1.16**），比 16:9 的 1.778 更接近方图，所以裁切明显变轻：

| 素材 | 尺寸 | 16:9 框（列表页） | 半屏框 1.16（播放页） |
|---|---|---|---|
| `image7` | 939×925 | 57% | **87%** |
| `img4` | 784×944 | 47% | **71%** |
| `image2` | 1080×1447 | 42% | **64%** |
| `image5` | 960×1280 | 42% | **64%** |
| `image8` | 960×1358 | 40% | **61%** |
| `image6` | 1080×1621 | 37% | **57%** |
| `image1` / `image4` | 640×1386 / 1000×2166 | 26% | 40% |

（半屏框的比例随屏幕形状变，但手机上半屏普遍落在 1.1~1.2 这一带，所以这组数字有代表性。列表页那一列是固定的 16:9。）

所以视频页只挑裁得动的 `image7 / image2 / image5 / image8 / img4 / image6` 这几张用，并刻意避开只剩一条横带的 `image1` / `image4`。另外视频标题都写成「纪录片 / 影像 / 现场」这类与具体画面无关的措辞，避免裁切后出现图文错位。

列表页一格 160dp 宽、封面 90dp 高（`160 ÷ 16 × 9`），这个高度是比例算出来的、不写在布局里；想换成 4:3 只改 `item_video.xml` 里的 `layout_constraintDimensionRatio` 一处。

**左右留白为什么拆成两段**：`GridLayoutManager` 只平分「父容器**去掉 padding 之后**」的宽度，所以 16dp 的页面留白必须拆成 `rv_video` 的 `paddingHorizontal=12dp` 加卡片自己的 `layout_marginHorizontal=4dp`，相加才等于 `page_padding`。两列之间的 8dp 由两个 4dp 的外边距合成。改成一边写 16dp 会让第一列和顶栏标题错位。

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

改造成双列 + 沉浸播放页时又发现三处「浅色页面里本来是对的、一搬到黑底上就失效」的问题：

- 播放页「相关视频」整行的按压反馈**在黑底上完全看不见**——它原来用 `?attr/selectableItemBackground`，波纹色取自 Light 主题的 `colorControlHighlight`（12% 黑），压在黑底上等于零对比度，点上去像没反应。已换成自带白波纹的 `bg_ripple_on_dark`（`press_on_dark`，12% 白）
- 分隔线和小节标题**不能照搬浅色页面的 token**：`divider` 的 `#EEEEEE` 在黑底上是一条刺眼的白线，`SectionTitle` 的 `#222222` 更是直接看不见。加了 `Widget.Toutiao.Divider.OnDark` / `Widget.Toutiao.SectionTitle.OnDark` 两个变体（靠点号隐式继承，只写差异项）。用变体而不是在元素上写 `android:background` 覆盖，是因为 `styles.xml` 头部第 1 条就禁止元素级覆盖 style——那样会静默盖掉整个样式
- 模拟进度条的轨道**原来选错了参照系**。最初定的是 20% 白（`#33FFFFFF`），理由是"12% 白在 3dp 细线上看不出进度条总长"——那是**按轨道压在黑底上**想的。可轨道其实压在**封面**上（进度条贴播放区下沿，而播放区整块被封面铺满），实测第一条视频（`image2`，红色日历）播到 50% 时，左半截红色填充条和右半截"20% 白 + 红封面"糊成一片红，整条进度条看不见。已改成 60% 黑的遮罩 `#99000000`：亮封面上压出一段深灰、暗封面上几乎融进黑，两种情况下红色填充条都还是红的

### 一个 Material 主题的坑

切换到 Material 主题后，布局里的 `<Button>` 会被 `viewInflaterClass` 自动替换成 `MaterialButton`，而 **`MaterialButton` 会直接丢弃 `android:background`**，改用自带的 `MaterialShapeDrawable` + `backgroundTint`（默认 `?attr/colorPrimary`）填底。

后果是自定义背景 drawable 完全不生效——筛选胶囊的灰底、浅红底全被主色覆盖，红底红字导致文字不可见。给 `backgroundTint` 设 `@null` 也没用，因为背景整个被替换掉了。

最终处理：

- **筛选胶囊**改用 `<TextView>`，绕开 `MaterialButton` 的这套逻辑
- **主按钮**改用 `MaterialButton` 自己的属性表达：`backgroundTint` 指向一个 ColorStateList（`res/color/button_primary_tint.xml`）承载常态 / 按压 / 禁用三态，`cornerRadius` 负责圆角

### 沉浸式为什么没在 API 29 上崩

播放页整页沉浸（隐藏状态栏、内容铺满全屏、边缘下滑临时唤出）。这套 API 在 `minSdk 29` 上有个不算明显的分界：

- 平台自带的 **`WindowInsetsController` 是 API 30 才有的**，直接用会崩。所以全程走 `androidx.core` 的 `WindowInsetsControllerCompat`。它在 API 29 上的落地其实是**老的 `systemUiVisibility`**（构造器按 `Impl30 → Impl26 → Impl23 → Impl20` 分派）：`hide(statusBars)` 变成 `SYSTEM_UI_FLAG_FULLSCREEN`，`BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE` 变成 `IMMERSIVE_STICKY`。所以这一段**不需要任何 `Build.VERSION` 判断**
- `setDecorFitsSystemWindows(false)` 在 API 29 上给 decorView 加的是 `LAYOUT_STABLE | LAYOUT_HIDE_NAVIGATION | LAYOUT_FULLSCREEN`——**导航栏那条 flag 也在里面**。只补顶部内边距的话，滚到最底下时最后一条「相关视频」会被导航栏压住且滚不出来，所以 `systemBars()` 要整组取
- 状态栏隐藏后 `statusBars` 的 top 归零，有刘海 / 挖孔的机器上真正会挡住返回键的是**挖孔本身**，所以取值要写成 `systemBars() | displayCutout()`。用模拟器的「模拟刘海屏」实测过：不带上 `displayCutout` 时返回键会顶进挖孔区
- **`getRootWindowInsets()` 在 `onCreate` 里返回 `null`**（那一刻视图还没 attach、第一次 traversal 也还没发生），所以"先读一次 inset 再 hide"这条捷径不成立，只能用 `ViewCompat.setOnApplyWindowInsetsListener` 监听
- 监听器里必须用**绝对值** `setPadding(...)`，**不能写 `+=`**：系统栏状态变化时这个回调会反复触发，累加的话内边距会一次比一次大。这也是实测过的——开 / 关刘海模拟时顶部内边距在 48dp 和 0 之间正确切换，没有叠加

另外两个和沉浸式配套、但不属于同一类问题的处理：状态栏底色设成透明（隐藏期间无所谓，但边缘下滑临时唤出时它是**浮在内容上**的，留着主题里的品牌红会在黑页顶部横一条红带）；窗口底色换成黑（主题里是白色 `bg_surface`，刘海让位的缝、转场动画第一帧、临时唤出状态栏时后面那一层，露出来都会是白的）。

### 已知遗留

两处**知道、但这次没做**的地方，写下来免得被当成 bug 查：

- **视频列表每个频道只有 5 条，双列排出来最后一行是单张卡**（2+2+1）。补齐成 2×3 需要每个频道加第 6 条，而现有约定是「频道内封面不重复」、且只有 6 张图可用（见上面的封面比例一节），成本不小，所以维持现状
- **冷启动进入播放页时第一帧可能还有极短的白闪**。`setBackgroundDrawableResource(bg_player)` 已经解决了窗口底色这一层，但如果启动预览窗口（starting window）仍是白的，只能靠在 manifest 上给该 Activity 单独挂一个 theme 解决——代价是新增一个 style，且 `values/` 和 `values-night/` 两个 `themes.xml` 都得写一遍。收益（一帧）与代价不成比例，这次没做

---

## 说明

- 应用内新闻、图片、用户资料等**全部为本地内置的演示数据**，不涉及任何网络请求，也没有申请任何系统权限
- 代码结构以课程演示为目的，未做分层架构与单元测试覆盖
