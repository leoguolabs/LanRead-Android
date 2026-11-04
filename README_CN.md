# LanRead 安卓版 — AI 辅助 EPUB 阅读器

[English](README.md) | 简体中文

LanRead（澜阅）是一款使用 Kotlin 和 Jetpack Compose 开发的安卓 EPUB 阅读应用。你可以导入电子书、整理书架、添加高亮和笔记、记录阅读进度，也可以使用 AI 总结、翻译、解释或就选中的内容提问。

**平台：** Android（本仓库） · [iOS / iPadOS 源码](https://github.com/visiontrail/LanRead) · [官网](https://isla-reader.top)

安卓与 iOS 是分别维护的姊妹项目，各平台功能及发布情况可能不同。

## 功能

- 导入 EPUB，浏览目录与章节，翻页并调整字体、排版和阅读主题。
- 按书名或作者搜索书架，使用阅读状态和收藏整理书籍。
- 选中文字添加高亮和笔记，管理书签并返回保存的位置。
- 查看阅读进度、阅读时长和每日目标。
- 生成阅读前摘要或章节略读摘要，对选中文本进行翻译、解释和提问。AI 功能需要网络与可用的服务配置。
- 应用包含英语、简体中文、日语和韩语资源。

阅读数据使用 Room 和 DataStore 保存在设备上。AI 请求会将相关内容发送至网络服务；使用前请查看应用内的 AI 同意和隐私说明。项目集成了 Google Mobile Ads。iOS 项目包含 Notion 集成；安卓功能及同步能力请以安卓实现为准。

## 构建与运行

应用最低支持 **Android 8.0（API 26）**。本仓库提供源码，可在 [Releases](https://github.com/visiontrail/LanRead-Android/releases) 查看是否已有发布版本。构建调试 APK 需要 JDK 17、API 36 Android SDK 和命令行工具或 Android Studio，以及用于下载 Gradle 和依赖的网络连接。

```bash
git clone https://github.com/visiontrail/LanRead-Android.git
cd LanRead-Android
./gradlew assembleDebug
```

Windows 使用 `gradlew.bat assembleDebug`。构建产物位于 `app/build/outputs/apk/debug/app-debug.apk`，可安装到兼容设备或模拟器，也可以使用 Android Studio 打开项目。

AI 请求需要可访问的 AI 配置服务和服务商凭据。当前源码先尝试从配置服务获取信息；失败后使用空 API 密钥回退，因此构建成功并不代表 AI 已可用。部署分支前请检查配置和凭据管理，不要提交密钥。

## 技术与目录

项目使用 Kotlin 2.2.20、Java 17、Jetpack Compose、Material 3、Room、DataStore、协程和 WebView；Gradle wrapper 为 9.2.1，compile/target SDK 为 36。

- `app/`：安卓应用、资源、EPUB 解析、AI 与数据服务。
- `e2e-tests/`：端到端测试用例与 Windows 自动化脚本。
- `gradle/`：Gradle wrapper。
- `tools/`：开发工具。
- [SPEC.md](SPEC.md)：从 iOS 移植的规格，其中可能同时包含目标行为与已实现行为，请以源码为准。

Windows 开发辅助入口包括 `debug.bat` 和 `e2e-tests/run_e2e_suite.ps1`；运行前请查看脚本所需的环境。

## 反馈与许可

欢迎在 [GitHub Issues](https://github.com/visiontrail/LanRead-Android/issues) 提交问题或功能建议。请提供设备、安卓版本、复现步骤及必要日志，并移除私人书籍内容和凭据。

本项目使用 [MIT 许可证](LICENSE)。
