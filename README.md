# 轻记 · BodyRecord

中文原生安卓应用，记录体重、饮食和训练。使用 Kotlin + Jetpack Compose，支持 Android 8.0（API 26）及以上。

已构建的安装包位于 `dist/BodyRecord-1.3.0-debug.apk`，是可直接安装试用的调试版本。

本机已配置安卓模拟器：双击根目录 `启动安卓测试.cmd` 即可启动虚拟手机并打开应用。模拟器、镜像、虚拟手机数据和缓存均在 E 盘，详见 `docs/EMULATOR.md`。

## 已实现

- **今日**：按日查看和补记；每日体重卡片；饮食和训练快捷入口；最近 7 天曲线；每周训练打卡。
- **体重趋势**：7 天 / 30 天切换、历史区间翻页、实际日期绘图、平均 / 最低 / 最高 / 区间变化、目标参考线、明细编辑删除。同一天的体重更新原有记录；空缺日期不填零。
- **饮食**：系统相册选图，每条记录 1–6 张，早餐 / 午餐 / 晚餐 / 加餐分类，可写备注、标记放纵餐、预览大图、编辑删除。图片自动校正方向并压缩，复制至私有目录，不依赖相册长期授权。
- **饮食页**：碳水 / 蛋白质 / 脂肪三类常见来源，27 种食物的离线介绍及 USDA 营养数据，标明生熟状态和每 100 g 可食部分，支持份量换算；可新增、编辑食物，隐藏和恢复内置条目、删除自定义条目。
- **图示摄入参考**：在“我的”中按用户提供的配额图展示范围，可用性别与最新体重换算克数。场景与特殊分档由用户选择；不设置营养目标、估算热量或自动按 BMI 下调摄入。说明见 `docs/FOOD.md`。
- **训练**：自定义训练动作、常用类型、训练时长、组数、每组次数、负重和备注，同一天可以记录多项训练。
- **动作库**：按提供的训练图整理 70 个动作，分为背、胸、肩前中束、肩后束、肱二头、肱三头、腿、腹、臀九类；可按肌群、器械、难度搜索筛选。每个动作有中文介绍、步骤和要点，其中 62 个内置经过画面核对的 GIF 动图，8 个暂缺准确演示，可自行导入。
- **动作管理**：创建自己的动作和分类，编辑内置动作、调整“容易 / 一般 / 困难”标签、导入本地 GIF / WebP / 图片，隐藏与恢复内置动作。编辑或删除动作库内容不会改写历史训练的名称、肌群和难度。
- **训练计划**：三分化、四分化（手臂单练）、四分化（肩单练）；按训练日筛选动作，并将三分化图示组数带入训练表单。腹和臀均可选用。
- **日历记录**：月历显示训练日与部位、休息日、放纵餐；点日期查看体重、饮食照片和训练明细，可补记或编辑当天标记和备注。未记录不自动判为休息日；原有全部记录搜索仍保留。
- **目标**：目标体重、身高、BMI 参考值、每周训练天数、累计与连续记录天数。
- **备份恢复**：通过系统文件选择器导出 / 导入 ZIP，包含照片、所有记录、日历标记、食物库修改、身体信息、目标、自定义动作和自选演示。新版继续读取 v1 / v2 / v3 旧备份；校验失败不会覆盖现有数据。

## 隐私与数据

应用无需账号，不申请网络权限和广泛相册 / 存储权限，没有广告或数据上报。这里的“上传照片”是从相册导入到本地饮食记录；当前没有云服务器、账号同步、图片识别或自动热量估算。

内置动图来自 ExerciseDB V1 免费版，仅供当前个人、非商业测试用途，来源与逐项素材清单见 `docs/EXERCISES.md`。请勿将当前素材版本直接用于商业发布。

SQLite 保存结构化记录；照片位于应用私有目录。卸载会删除这些本地数据，建议定期在“我的”中导出 ZIP 并自行保管。备份是未加密文件。恢复会替换现有数据。导入上限为解压后 300 MB、每个文件 12 MB、最多 10,000 个 ZIP 条目。

## 构建与安装

在 Android Studio 中打开本目录，使用 JDK 17、Android SDK 35、Build Tools 35.0.0，同步 Gradle。也可以命令行运行：

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest lintDebug
```

当前工作区内已准备好本地构建工具，PowerShell 可直接执行 `./scripts/build.ps1` 自动选择本地 JDK / SDK 并完成构建和检查；`.tools` 和 `local.properties` 不进入源码版本控制。

当前工作区的调试签名已固定于 `.tools/signing/debug.keystore`，与原模拟器安装保持一致，避免 SDK 用户目录切换导致无法覆盖升级。该文件是私有本地文件，不进入源码版本控制；迁移开发环境时须自行妥善保存。正式发布仍应使用自己的发布签名。

调试 APK：`app/build/outputs/apk/debug/app-debug.apk`。复制到安卓手机，允许该文件来源安装后打开。通过 ADB 安装：

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

调试 APK 用于本地试用；商店发布需配置自己的发布签名，再构建 release AAB。请固定并备份签名，后续升级才能保留用户数据。

## 结构与扩展

```text
app/src/main/java/com/bodyrecord/app/
  MainActivity.kt           应用入口、导航、系统文件选择器
  RecordViewModel.kt        StateFlow 状态和异步操作
  data/
    Models.kt               记录模型、日期窗口、连续记录和输入规则
    RecordDatabase.kt       SQLite 表、索引、事务和版本迁移入口
    RecordRepository.kt     存储访问、图片处理、事务恢复、文件清理
    BackupCodec.kt          与数据库解耦的版本化 JSON 格式
  ui/
    Theme.kt                颜色、字体、形状
    Components.kt           可复用组件、日期导航
    Editors.kt              带校验的记录表单
    Screens.kt              今日、趋势、记录、我的
    WeightChart.kt          按真实日期绘制曲线
```

新增身体指标可增加数据表和模型，通过 repository 暴露给 ViewModel；升级数据库时必须补充 `onUpgrade` 的非破坏性迁移。备份数据结构变更时单独升级 `BackupCodec` 格式并保持旧版本读取。当前 UI 与存储解耦，后续可添加账号同步、提醒、训练模板、围度和导出 CSV。

## 验证

自动化测试覆盖日期边界、跨年区间、连续记录、无效输入、同日体重更新、数据库和备份往返、无效备份保护及删除隔离，并验证实际界面的体重 / 训练保存、导航和趋势切换。完整检查记录见 `docs/VALIDATION.md`。

构建版本依据 [Android 官方 AGP 8.9 兼容表](https://developer.android.com/build/releases/agp-8-9-0-release-notes) 固定；Compose 编译器插件与 Kotlin 同版本，参考 [Compose 官方设置说明](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler)。

## GitHub 自动构建

源码提交范围、忽略规则、打 tag 发包方式和签名说明见 [docs/CI.md](docs/CI.md)。推送任意 tag 后自动检查并生成 APK，可在 [Releases](https://github.com/yzh-2002/BodyRecord/releases) 或 [Actions](https://github.com/yzh-2002/BodyRecord/actions) 下载。云端默认使用临时调试签名；覆盖本地版前需配置同一签名的加密 Secret。
