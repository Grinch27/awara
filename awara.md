# Workspace: 正在收集工作区信息# 项目中的网站链接

以下是在代码仓库中找到的所有网站链接：

## API 基础链接

- [https://api.iwara.tv](https://api.iwara.tv) - 在 NetworkModule.kt 中设置为 Iwara API 的基础 URL
- [https://v1.hitokoto.cn](https://v1.hitokoto.cn) - 在 NetworkModule.kt 中用于一言 API

## 网站和资源链接

- [https://github.com/awara-app/awara/releases](https://github.com/awara-app/awara/releases) - 应用下载链接，在 README.md 和 IndexPage.kt 中定义为 `DOWNLOAD_LINK`
- [https://github.com/re-ovo/awara](https://github.com/re-ovo/awara) - 在 SettingPage.kt 中指向源代码库
- [https://iwara.tv/register](https://iwara.tv/register) - 在 LoginPage.kt 中链接到 Iwara 注册页面
- [https://www.iwara.tv/video/${vm.id}](https://www.iwara.tv/video/${vm.id}) - 在 VideoOverviewPage.kt 中用于分享视频链接
- [https://developer.android.com/topic/architecture](https://developer.android.com/topic/architecture) - 在 README.md、README.en.md 和 README.ja.md 中链接到 Android 架构指南
- [https://discord.gg/G22sSzJbzh](https://discord.gg/G22sSzJbzh) - 在 README.ja.md 中提供的 Discord 链接
- [https://fsf.org/](https://fsf.org/) - 在 LICENSE 以及 README 文件中提到的自由软件基金会链接

## 图片链接

- [https://i.iwara.tv/image/avatar/a90cf846-fb84-4965-adbd-131c411abc93/picture-294150-1628430683.jpg](https://i.iwara.tv/image/avatar/a90cf846-fb84-4965-adbd-131c411abc93/picture-294150-1628430683.jpg) - 在 SettingPage.kt 中使用的作者头像

此外，代码中还包含用于处理链接的通用功能，例如 ContextUtil.kt 中的 `openUrl()` 方法，它可以在设备的浏览器中打开 URL。

# GitHub Copilot: # 定位并注释掉一言 API 链接

我会先找到文件中包含一言 API 链接的位置，然后注释掉相关代码。

## 查找一言 API 链接

```bash
grep -n "hitokoto" /workspaces/awara/app/src/main/java/me/rerere/awara/di/NetworkModule.kt
```

## 查看完整的 NetworkModule.kt 文件

```bash
cat /workspaces/awara/app/src/main/java/me/rerere/awara/di/NetworkModule.kt
```

## 修改文件，注释掉一言 API 部分

```bash
sed -i 's|@Provides.*hitokoto.*|// &|' /workspaces/awara/app/src/main/java/me/rerere/awara/di/NetworkModule.kt
sed -i 's|.*v1\.hitokoto\.cn.*|// &|' /workspaces/awara/app/src/main/java/me/rerere/awara/di/NetworkModule.kt
```

这个命令会：

1. 查找包含`@Provides`和`hitokoto`的行并在前面添加`// `进行注释
2. 查找包含`v1.hitokoto.cn`的行并在前面添加`// `进行注释

## 验证修改结果

```bash
grep -A5 -B5 "hitokoto" NetworkModule.kt
```

您现在可以运行以上命令来执行这些操作，或者手动编辑 NetworkModule.kt 文件，找到包含`https://v1.hitokoto.cn`的部分并将其注释掉。您现在可以运行以上命令来执行这些操作，或者手动编辑 NetworkModule.kt 文件，找到包含`https://v1.hitokoto.cn`的部分并将其注释掉。

# Workspace: 正在收集工作区信息# Awara 项目构建指南

Awara 是一个使用 Kotlin 和 Jetpack Compose 开发的 Iwara.tv 第三方客户端。下面我将详细解释项目的构建方式和核心文件。

## 项目架构

该项目采用了 MVVM 架构（Model-View-ViewModel）结合 Clean Architecture 思想，文件组织清晰：

### 1. 应用入口

- `App.kt` - 应用程序入口点，初始化依赖注入、图片加载、通知通道等
- `RouterActivity.kt` - 主 Activity，包含导航系统和路由配置

### 2. 数据层 (Model)

- **DAO**: `app/src/main/java/me/rerere/awara/data/dao/` - 数据库访问对象
- **实体**: `app/src/main/java/me/rerere/awara/data/entity/` - 数据库实体类
- **仓库**: `app/src/main/java/me/rerere/awara/data/repo/` - 处理数据操作的仓库
- **API**: `app/src/main/java/me/rerere/awara/data/source/` - 网络 API 定义

### 3. 视图层 (View)

- **页面**: `app/src/main/java/me/rerere/awara/ui/page/` - 各种页面实现
- **组件**: `app/src/main/java/me/rerere/awara/ui/component/` - 可复用 UI 组件
- **主题**: `app/src/main/java/me/rerere/awara/ui/theme/` - 应用主题定义

### 4. 视图模型层 (ViewModel)

- 各页面的 ViewModel，如`IndexVM.kt`

### 5. 依赖注入

- `app/src/main/java/me/rerere/awara/di/` - 使用 Koin 进行依赖注入

## 主要实现文件

### 核心功能文件

1. **首页实现**

   - `IndexPage.kt` - 首页主要布局
   - `IndexPagePhoneLayout.kt` - 手机布局
   - `IndexPageTabletLayout.kt` - 平板布局
   - `IndexDrawer.kt` - 侧边导航抽屉

2. **视频播放功能**

   - `VideoPage.kt` - 视频页面
   - `PlayerBase.kt` - 播放器基础组件

3. **用户相关**

   - `UserPage.kt` - 用户页面
   - `LoginPage.kt` - 登录页面
   - `UserStore.kt` - 用户状态管理

4. **下载功能**
   - `DownloadPage.kt` - 下载页面
   - `DownloadWorker.kt` - 下载后台任务

### UI 公共组件

- `UiState.kt` - UI 状态管理
- `Message.kt` - 消息提示组件
- `ImageAppBar.kt` - 带图片的顶部应用栏

### 实用工具

- `ContextUtil.kt` - 上下文工具，包含打开 URL 等功能
- `Crypto.kt` - 加密相关工具
- `FileUtil.kt` - 文件操作工具

## 如何构建 APP

1. **设置开发环境**:

   ```bash
   # 克隆仓库
   git clone https://github.com/re-ovo/awara.git
   cd awara

   # 使用Gradle构建
   ./gradlew assembleDebug
   ```

2. **或在浏览器中打开 GitHub 仓库**:
   ```bash
   "$BROWSER" https://github.com/awara-app/awara
   ```

## 开发和维护建议

1. **添加新页面**:

   - 在`ui/page/`下创建新的页面目录
   - 在 RouterActivity.kt 中注册新路由

2. **修改主题**:

   - 编辑 Theme.kt 和 DynamicTheme.kt

3. **添加 API 接口**:

   - 在`data/source/`下修改或添加 API 接口
   - 在`di/NetworkModule.kt`中注册

4. **多语言支持**:

   - 修改`res/values-*/strings.xml`文件

5. **自适应布局**:
   - 参考现有的 Phone/Tablet 布局实现，通过条件组合创建响应式 UI

这个项目使用了现代 Android 开发技术，包括 Jetpack Compose、协程、Material You 设计和依赖注入，是学习现代 Android 开发的很好参考。
