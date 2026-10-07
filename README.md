# 晴川 · 校园资讯与三维导览

Java + XML 开发的 Android 校园应用，采用松绿与浅米白界面，整合校园资讯、三维导览、个人内容和生活服务演示。

详细功能、操作范围及演示限制见 [项目功能说明.txt](项目功能说明.txt)。README 仅保留快速开始与最新运行截图。

## 最新界面

以下图片来自 2026-10-04 覆盖安装当前 APK 后的运行界面。

| 首页（展开） | 首页（折叠） |
| --- | --- |
| ![晴川首页展开](screenshots/qingchuan/home-expanded.png) | ![晴川首页折叠](screenshots/qingchuan/home.png) |

| 我的 | 网络配置 |
| --- | --- |
| ![我的](screenshots/qingchuan/mine.png) | ![模拟器与真机网络配置](screenshots/qingchuan/network.png) |

| 校园导览 | 三维校园 |
| --- | --- |
| ![校园导览](screenshots/qingchuan/guide.png) | ![三维校园](screenshots/qingchuan/campus3d.png) |

## 快速开始

### 三维校园：地图操作与路线规划

- 单指默认平移，双指围绕手指中心缩放；松开一根手指后可继续拖动。
- 点“俯视”切换顶视图；俯视状态下旋转保持俯视角度。
- 顶部“隐藏名称 / 显示名称”切换建筑及场地标签。
- 右侧“路线”选择起点、终点并显示路线，支持交换地点与清除路线。实线为沿路网计算的路线，虚线为地点连接段；没有可用道路或道路不连通时显示原因。
- “更多”中包含底图、巡航、平移/旋转、复位、画路和移楼。编辑面板可收起，退出编辑恢复原底图；画路与移楼自动互相切换。
- 移楼支持撤销、重做和自动保存；复位恢复用户校准后的默认布局。画路时双指缩放会撤回本次未完成的编辑，避免误画、误擦。

路线使用当前道路和建筑位置，编辑完成后会重新计算已显示的路线。湖泊目的地使用可连接道路的湖岸位置。地图为截图近似重建，未标注真实建筑入口，未提供 GPS、室内导航或真实步行距离。

默认布局已固化在 `app/src/main/assets/campus3d/campus-data.js` 中。覆盖安装保留本地编辑数据；需要使用仓库默认布局时，在“移楼”中点“全部复位”。

### Android 客户端

1. 用 Android Studio 打开项目，配置本机 Android SDK。
2. 同步 Gradle，然后在设备或模拟器上运行 `app`。
3. `local.properties` 使用本机 SDK 路径，不应复制其他电脑的配置。

项目使用 Java 8、compileSdk 32、minSdk 29，适用于 Android 10 及以上。校园三维资源在 APK 内，可离线浏览。

### 校园新闻服务：Windows 一键启动

先安装 Python 3.8+；双击根目录 **启动校园新闻服务.bat**。

终端依次显示五步：寻找可用 Python、检查依赖、安装缺失包、验证导入、启动服务。解释器从本机环境发现，不使用开发者电脑的固定路径。依赖已具备时跳过下载；首次安装需联网，默认使用清华 PyPI 镜像，失败后回退官方源；终端显示源地址、pip 下载输出及失败原因。

窗口保持打开即可提供服务，按 `Ctrl+C` 停止。代码更新后需要重启服务，避免同时启动多个进程占用 5000 端口。

仅检查环境、不安装或启动：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\start-campus-news.ps1 -CheckOnly
```

其他系统或手动运行：

```shell
python -m pip install flask requests beautifulsoup4 urllib3
python -u campus_news_server.py
```

### 模拟器与真机连接

在“我的”右上角点网络图标（好友图标左侧）：

- 模拟器选择 `http://10.0.2.2:5000`。
- 真机填写爬虫电脑的局域网地址，例如 `http://192.168.1.8:5000`。手机和电脑需处于可互通的局域网，电脑防火墙允许对应端口。
- 两种模式都能点“测试连接”，确认后点“保存”，返回首页刷新。

`/api/health` 验证服务连接与身份；测试成功不等于官网抓取一定成功。跨网络使用需要另行部署可访问的服务。

## 常见问题

- **没有找到 Python**：安装 Python 并添加到 PATH，或确保本机 `py` launcher 可用，再重新双击。
- **依赖下载失败**：检查 pip 输出与网络；可以配置自己的 pip 镜像后重试。安装器使用找到的同一个解释器。
- **端口占用**：关闭重复启动的校园服务，再重新运行。
- **服务可访问但检测接口缺失**：重启更新后的 Python 服务。
- **手机无法连接**：检查电脑 IP、端口、局域网隔离及防火墙；手机不能用模拟器的 `10.0.2.2`。
- **新闻抓取失败**：检查电脑是否能访问学校官网；官网结构或代理环境变化可能影响解析。

## 验证与文件入口

```shell
gradlew.bat testDebugUnitTest assembleDebug
```

安装 Node.js 后，在 PowerShell 中运行地图的 13 个几何、交互及路线检查：

```powershell
Get-ChildItem tests/campus3d/test_*.cjs | ForEach-Object {
    node $_.FullName
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}
```

2026-10-07 更新已验证 320px、420px 和 1100px 浏览器界面，以及双指缩放、松指继续拖动、绘制/擦除防误触、路线交换、湖岸路线和编辑后的路线更新。Android 单元测试与 debug APK 构建通过；本次尚未进行真机验证。更新记录见 `docs/superpowers/plans/2026-10-07-map-updates/`。

- Android 源码：`app/src/main/java`、`app/src/main/res`
- 离线三维场景：`app/src/main/assets/campus3d`
- 服务：`campus_news_server.py`
- Windows 启动器：`启动校园新闻服务.bat`、`start-campus-news.ps1`
- 功能清单：[项目功能说明.txt](项目功能说明.txt)

课程演示项目。购物、聊天回复和部分社交数据为本地演示；建筑外观、尺寸和设施依据截图近似重建，未提供入口或室内导航。
