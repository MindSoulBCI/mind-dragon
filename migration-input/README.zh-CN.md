# MindSoul 许愿神龙 · PICO 迁移素材包

从现有 Android XR 工作区提取，附 visionOS 原始实现用于效果对照。不是独立可编译工程、APK 或已完成的 PICO 移植。

## 内容
包含七龙珠 wish-balls、原神龙、MindSoul 版、带动作版、金色光环与蓄力/召唤音效，保留 Blender 工程与转换说明。效果逻辑见 visionOS 源码及 player.shared.js 中 dragon / wish 分支，包括专注力蓄力、召唤、升起与环绕。请读 masters/WishDragon/WISH_ASSET_CREDITS.md，保留模型署名及音效许可；角色与商标权不由模型许可授予。

- assets/：可移植 GLB、贴图、音频及封面；保留原始运行时路径。
- masters/：USDZ / Blender 原始素材，用于重建材质与动画。
- source/android/：当前原生 Android XR 场景实现与共享依赖参考。
- source/visionOS/：原始游戏逻辑与视觉行为参考。
- source/web/player.shared.js：多游戏共用的 Three.js 源代码，只作为当前游戏效果参考；包含其他游戏分支但不附其他游戏资产，不能单独运行。
- catalog.json：本游戏的原始资源索引。原索引 reference 路径请对应到 source/visionOS/。
- inventory.json：全部交付文件大小、SHA-256 与 GLB 网格/动画信息。

## 给 PICO CLI 的迁移交接
1. 在目标 PICO 工程中导入 assets/ 下的本游戏资源，保留署名与许可文件。
2. 优先使用 GLB；USDZ 不直接当作 PICO 运行时资产。保留模型纹理、透明混合、双面与动画，不将毛发或玻璃直接替换为不透明材质。
3. 用 PICO 的实体、加载、输入、拖动、缩放与生命周期接口重写 Android XR 适配层。现有 Jetpack XR 类型不可直接编译进 PICO。
4. 依据原始源码实现动作与效果；有动画的模型播放实际 clip。退出游戏时取消动画/定时器并释放实体，避免模型残留。
5. 默认把模型放在用户前方的独立空间，控制面板独立可拖动；不更换环境背景。保留 hover、捏合拖动与双手缩放目标，需在 PICO 真机验证。
6. 只迁移游戏本体。账号、真实 EEG、FMRI 看板、训练记录、权限与全局导航需要接入目标项目，未打包为独立服务。
7. tools/ 中脚本为原仓库参考，输入输出路径须按本包与目标工程调整。大模型须在设备上测帧率与内存，不能仅凭桌面预览确认性能。

测试/模拟专注力与真实 EEG 必须区分。包内源代码引用其他原工程类型时，需要在目标工程提供适配，不代表缺失游戏素材。
