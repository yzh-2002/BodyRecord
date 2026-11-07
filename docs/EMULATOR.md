# 在电脑上运行「轻记」

本机已完成安装和验证，不需要另装 Android Studio，也不需要重启电脑。

## 启动

双击项目根目录的 `启动安卓测试.cmd`。它会自动启动 Android 15 虚拟手机、安装当前 APK 并打开轻记。如果虚拟手机已启动，则连接已有窗口并打开应用。

也可以在 PowerShell 中执行：

```powershell
cd E:\BodyRecord
.\scripts\start-emulator.ps1
```

鼠标点击模拟手机屏幕，鼠标拖动模拟滑动；表单可用电脑键盘输入。关闭模拟器窗口即可退出。启动脚本使用覆盖安装，保留已经保存的应用记录。启动时不加载快照，应用数据仍保存于虚拟手机的磁盘中。

修改源码后，先运行 `./scripts/build.ps1`，再双击启动入口，即可安装新构建的 APK。

## 全部大文件存储在 E 盘

| 内容 | 位置 |
| --- | --- |
| Android Emulator 37.2.13 | `E:\BodyRecord\.tools\android-sdk\emulator` |
| Android 15 / API 35 x86_64 标准系统镜像 | `E:\BodyRecord\.tools\android-sdk\system-images\android-35\default\x86_64` |
| 虚拟手机及应用数据 | `E:\BodyRecord\.tools\avd\BodyRecord_API35.avd` |
| 模拟器配置、密钥和运行日志 | `E:\BodyRecord\.tools\android-user` |
| 模拟器临时文件 | `E:\BodyRecord\.tools\tmp` |
| 安装下载缓存 | `E:\BodyRecord\.tools` 下的 ZIP 与分块文件 |

启动脚本为当前进程设置 SDK、AVD、用户配置和临时目录；不需要修改系统环境变量。当前使用 WHPX 硬件加速，720 × 1280 屏幕、3 GB 虚拟内存和 2 个虚拟 CPU。

## 添加照片供饮食功能测试

将电脑上的照片拖入模拟器窗口，等待文件传入后，再从轻记的“记录饮食 → 从相册选择”中选择。也可以通过 ADB 复制到虚拟手机并触发媒体扫描，例如：

```powershell
cd E:\BodyRecord
.\scripts\android-env.ps1
& $bodyRecordAdb -s emulator-5554 push 'E:\照片\午餐.jpg' /sdcard/Pictures/lunch.jpg
& $bodyRecordAdb -s emulator-5554 shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Pictures/lunch.jpg
```

请将示例中的本地图片路径改为真实存在的文件。

## 已完成的运行验证

2026-11-07 已启动虚拟手机并通过 ADB 安装应用，确认：

- Android 系统启动完成，WHPX 加速可用。
- 轻记主界面成功显示，前台 Activity 为 `com.bodyrecord.app/.MainActivity`。
- 体重编辑表单可打开。
- 系统相册选择器可打开（`com.android.providers.media.module`）。
- 关闭并重新打开应用正常，没有写入示例记录。
- Windows 自带 PowerShell 可执行启动脚本。

实际模拟器截图位于 `docs/screenshots/emulator-running.png`。

## 排查

运行日志：`.tools/android-user/emulator-stdout.log` 和 `emulator-stderr.log`。

如果以后更换电脑，可通过 `emulator.exe -accel-check` 检查硬件加速。只有检测失败时才需考虑启用 Windows Hypervisor Platform；`scripts/enable-emulator-acceleration.ps1` 需要管理员权限，而且不会自动重启电脑。本机已检测通过，无需运行该脚本。

目录配置依据 [Android 官方环境变量说明](https://developer.android.com/tools/variables)，加速配置参考 [Android 官方模拟器加速说明](https://developer.android.com/studio/run/emulator-acceleration)。
