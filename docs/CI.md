# Git 提交范围与自动构建

## 提交范围

提交 `app/src` 的源码、测试、运行资源与离线数据；Gradle 构建配置、Wrapper 脚本及 `gradle-wrapper.jar`；可复用脚本、文档和 `.github/workflows`。内置 GIF 属于应用运行资源，连同来源清单、署名和个人非商业用途说明保留；当前仓库保持私有。

`.gitignore` 排除 `.tools` 中的 SDK / JDK / 模拟器、签名、备份和个人记录，也排除构建缓存、IDE 文件、`local.properties`、数据库、ZIP / TAR、日志、生成截图及 `dist` 安装包。`docs/screenshots` 是本地验证输出，不随源码提交。APK 通过 Actions / Releases 分发，不放进 Git 历史。

## 工作流

- `ci.yml`：main 推送、PR、手动运行时构建，执行 `assembleDebug testDebugUnitTest lintDebug`，上传检查报告。
- `runner-check.yml`：手动启动最小 echo 任务，用于确认 GitHub runner 是否可以启动，不下载源码或构建依赖。
- `tag-apk.yml`：任意 tag 推送触发，重新运行全部检查，生成 APK、SHA-256 和构建信息，上传 Actions Artifact（90 天），自动创建同名 GitHub 预发布并附上 APK。失败的测试 / Lint 会阻止发布。手动从分支运行时仅上传 Artifact。
- Action 固定到已核对的提交 SHA，GitHub 托管的 Ubuntu 24.04 runner 配置 JDK 17、Android SDK 35、Build Tools 35.0.0；Gradle 基础缓存不上传本地签名。

首次版本为 `v1.3.0`。之后先更新 `app/build.gradle.kts` 中的 `versionName` 和递增的 `versionCode`，提交后打新的 tag：

```sh
git tag -a v1.3.1 -m "Release v1.3.1"
git push origin main
git push origin v1.3.1
```

触发依据是 tag 的**推送**；只在本地打 tag 不会启动 GitHub Actions。tag 名不强制等于应用版本，实际版本记录在 `build-info.json`。

## 调试签名

私钥不进入 Git，也没有自动上传为 Secret。未配置签名 Secret 时 CI 使用 runner 临时生成的调试签名，可安装测试，但不能保证覆盖本地版或其他构建。不要为此直接卸载仍有数据的应用。

如需让云端构建与现有本地版保持覆盖兼容，可由仓库管理员将同一调试 keystore 的 Base64 内容配置到仓库 Actions Secret `ANDROID_DEBUG_KEYSTORE_BASE64`。工作流将其临时恢复到 `.tools/signing/debug.keystore`，任务结束删除。仅支持 Android 标准调试身份 `AndroidDebugKey`，store/key 密码为 `android`；正式商店发布须另行配置发布签名。

工作流依据官方 [Gradle Actions](https://github.com/gradle/actions) 与 [Artifact Actions](https://github.com/actions/upload-artifact) 文档配置。


## 当前 GitHub 账户限制（2026-11-07）

首次提交和 `v1.3.0` tag 已推送。GitHub 的 tag 推送与手动触发均返回 `startup_failure`，0 个 job，尚未执行 checkout 或构建。用户在运行页面确认错误为：近期账户付款失败或需要提高支出上限；仓库 Actions 已启用。

需由账户所有者在 [Billing & plans](https://github.com/settings/billing) 处理付款 / 额度问题，之后可先手动运行 `Runner diagnostic` 检查 runner 启动，再进入 `Tag APK` 工作流选择 tag `v1.3.0` 运行，或推送新的 tag。无需修改代码，也不要移动已发布的 tag。此前不能将云端产包记为验证成功。

本地已验证的 APK 已上传至 [v1.3.0 Release](https://github.com/yzh-2002/BodyRecord/releases/tag/v1.3.0)，附 SHA-256 与构建信息，当前附件使用与本机原应用一致的调试签名；GitHub Actions 后续恢复时会按 tag 自动构建、上传 Artifact 和发布 APK。不会自动修改账单设置、提高预算或上传私钥。
