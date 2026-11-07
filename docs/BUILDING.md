# 本地构建与 Git 提交范围

## 本地 APK

当前可安装版本位于 `dist/BodyRecord-1.3.0-debug.apk`。Gradle 的原始构建输出为 `app/build/outputs/apk/debug/app-debug.apk`，当前两份文件内容一致。安装包只保留在本地，不进入 Git 历史。

在项目根目录运行：

```powershell
./scripts/build.ps1
```

脚本会使用工作区 `.tools` 中的 JDK、Android SDK 和 Gradle，执行 `assembleDebug testDebugUnitTest lintDebug`。新 APK 输出到 `app/build/outputs/apk/debug/app-debug.apk`；如需保存带版本号的副本，可在构建完成后复制到 `dist`。`./scripts/build.ps1 -SkipTests` 可仅构建 APK。

本机模拟运行：双击根目录 `启动安卓测试.cmd`，启动已有虚拟手机并安装当前构建。模拟器配置见 [EMULATOR.md](EMULATOR.md)。

## Git 提交范围

提交 `app/src` 的源码、测试、运行资源和离线数据；Gradle 构建配置、Wrapper 脚本及 `gradle-wrapper.jar`；可复用脚本和文档。内置 GIF 属于应用运行资源，保留来源清单、署名和个人非商业用途说明。

`.gitignore` 排除 `.tools` 中的 SDK、JDK、模拟器、签名、备份和个人记录，也排除构建缓存、IDE 文件、`local.properties`、数据库、ZIP / TAR、日志、生成截图、`dist` 安装包和本地打包输出。`docs/screenshots` 是本地验证输出，不随源码提交。

## 签名与发布

本机使用 `.tools/signing/debug.keystore` 中的固定调试签名，保持与现有模拟器安装兼容。私钥不进入 Git，应自行妥善备份。正式商店发布须另行配置发布签名。

已移除 GitHub Actions 的 CI、tag 发包和 runner 诊断工作流，后续在本地构建。提交代码或推送新的 tag 不再触发这些工作流。已有 `v1.3.0` tag 和 Release 保留作为历史交付，不移动旧 tag。

可选运行 `python scripts/package-apk.py`，将已构建的 APK、SHA-256、构建信息和发布说明整理到被忽略的 `ci-output` 本地目录，供手动分发。
