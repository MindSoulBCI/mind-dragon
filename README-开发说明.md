# 意念神龙 PICO OS6 — v1.2.6 项目工程

包名：cn.mindsoul.pico.wishdragon；versionCode 9。交付日期：2026-11-14。

## 工程内容
- 仓库根目录：完整 Kotlin/SpatialUI 源码、测试、Gradle Wrapper、运行所需 GLB/音频/图片（Vibe Lab 云构建要求 Gradle 在根目录）。
- art/v1.2/：最新七颗圆点龙珠、蓝色神龙的可编辑Blender模型，导出和预览脚本。目录名称保留资产迭代起点，内容为v1.2.6最终版。
- migration-input/：原始迁移素材及来源参考。
- docs/：统一交互规范、历史变更与验收记录。
- apk/：最新可安装debug APK及校验值。
- FILES-SHA256.json：工程包内逐文件SHA256。

## 开发与构建
使用 Android Studio 打开仓库根目录。已验证工具链：JDK 21、Android SDK 35、AGP 8.13.2、Kotlin 2.1.20、PICO Spatial SDK/BOM 6.1.9，arm64-v8a。
在本机设置ANDROID_HOME，或自行创建android/local.properties并设置sdk.dir；不包含原机器的本地配置、签名私钥、SDK或Gradle缓存。首次Gradle同步需联网下载声明的依赖。

```sh
chmod +x gradlew
./gradlew assembleDebug testDebugUnitTest
```
输出app/build/outputs/apk/debug/app-debug.apk。包内APK为开发调试签名，不是商店发布包。

Blender 5.2.1导出示例（Blender可执行文件加入PATH）：
```sh
blender --background --factory-startup --python art/v1.2/build_assets.py
```
脚本已改用自身位置解析路径，可在解压后的工程运行。Android运行直接使用已导出的GLB，无需先运行Blender。

## 最新功能与边界
1–7圆点龙珠、选中拖动/双手缩放旋转/悬停、10秒汇聚召唤、3倍蓝色神龙与胡须嘴部运动、沉浸模式。
上方心愿窗、本机心愿墙与五列点赞榜、个人心愿标签及重新许愿、主板及下方窗口同步显示进度、脑机模式双向切换。
邮箱登录/注册走 mindsoul 论坛接口（appId: wish-dragon）；心愿与点赞仍仅本机保存；脑电为模拟数据。重新许愿复用原记录，不重复发布。
编译、24项逻辑测试及样式检查通过；用户确认v1.2.3选中不消失、上下窗口正常。v1.2.6新标签与模式切换的交互还需实际验收。

缓存、旧APK、Blender .blend1备份和本机路径配置已排除。当前源码、模型和设计资料均保留，便于二次开发。
