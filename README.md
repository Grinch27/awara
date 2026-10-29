<kbd>
<img src="doc/icon.png"  width="128" height="128" />
</kbd>

# Awara
[English](doc/README.en.md) | [日本語説明](doc/README.ja.md) | [한국어 설명](doc/README.ko.md)

下一代I站客户端，使用Material You设计，使用Jetpack Compose构建UI。   

## 下载
本仓库改进版通过 [GitHub Actions](https://github.com/Grinch27/awara/actions/workflows/build-apk.yml) 构建。
选择成功的构建，在 Artifacts 中下载 `app-debug`，解压后安装 APK。`app-release` 目前为未配置正式签名的发行构建。
原上游版本见 [awara-app Releases](https://github.com/awara-app/awara/releases)，与本仓库的新构建分开。

## 本次改进
保留 Material 3，以紧凑卡片呈现标题、作者、日期和计数；首页、搜索和历史共用封面策略。
七类搜索增加请求隔离、去重和状态恢复。视频/图集支持进度保存，下载支持持久队列、续传、暂停/继续/取消、本地播放及文件导出。
会话使用 Android Keystore 加密；导入备份前提供校验和预览；记录删除与文件删除分开选择。

[实施状态、验证记录和后续路线](doc/module-subscription-plan.md) · [运行验收范围](doc/ANDROID-RUNTIME-SMOKE.md)

## 注意事项 （必读)
* 本项目仅供学习交流，不得用于盈利或商业用途，不得公开传播本项目。
* 本项目仅支持Android 8.0及以上版本，不会支持老版本的Android。
* 本项目为第三方项目，与I站官方无关

## 主要功能
- 🎨 Material You 设计
- 💻 自动多屏幕适配
- 🌏 本地化语言支持
- 📺 DLNA 投屏支持

## 开发
- [架构指南](https://developer.android.com/topic/architecture)
- Kotlin、Compose、Media3、Room、WorkManager；当前模块为 app、core:model、core:ui、data、feature:search、feature:player。
- compileSdk 37、targetSdk 33、minSdk 26；ABI 为 arm64-v8a / x86_64。
- 保留最新稳定 Gradle 和依赖解析策略，实际工具链以成功 CI 的报告为准。
- APK 编译和 JVM 回归在 GitHub Actions 完成；设备安装、真实账号/CDN 和窗口布局验收另行记录。

## 开源协议
```text
                   GNU GENERAL PUBLIC LICENSE
                       Version 3, 29 June 2007

 Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
 Everyone is permitted to copy and distribute verbatim copies
 of this license document, but changing it is not allowed.
```