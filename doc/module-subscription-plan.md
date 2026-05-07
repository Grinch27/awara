<!--
需要你决定：
1. 第一阶段是否接受把单体 :app 拆成 6 到 8 个 Gradle 模块。
2. 订阅视图是否允许纯本地“智能订阅”能力，不要求服务端新增接口。
3. 下载、播放、搜索是否接受分阶段迁移，而不是一次性重构。

后续代码研究方向：
1. 先抽 build-logic 和 core/data，还是先把 player/download 作为独立 feature 落地。
2. 当前筛选模型是否要继续兼容 key/value 字符串参数，还是直接升级为类型化查询对象。

可继续优化点：
1. 用统一的 FeedQuery 模型覆盖首页、搜索页、收藏页、关注页的筛选与排序。
2. 给订阅视图补上本地持久化、导出导入、回滚恢复和分享链接能力。
3. 把外联端点、隐私说明、调试开关和发布说明整理成单独文档并纳入发版流程。
-->

# Awara 模块拆分与订阅筛选改造方案

## 1. 当前结构判断

基于当前仓库实现，可以先把问题归纳成两类：

1. 结构过于集中。
   当前只有一个 [settings.gradle.kts](../settings.gradle.kts) 中声明的 `:app` 模块，而仓库里已经同时承载了网络、数据库、下载、播放器、搜索、首页、消息、用户和设置等职责。

2. 订阅筛选模型过于轻量。
   当前筛选核心只有 [app/src/main/java/me/rerere/awara/ui/component/iwara/param/Filter.kt](../app/src/main/java/me/rerere/awara/ui/component/iwara/param/Filter.kt) 里的 `FilterValue(key, value)`，并由 [app/src/main/java/me/rerere/awara/ui/page/index/IndexVM.kt](../app/src/main/java/me/rerere/awara/ui/page/index/IndexVM.kt) 在内存里分别维护 `videoFilters` 和 `imageFilters`。这能工作，但不利于保存视图、跨页面复用、导入导出与后续扩展。

## 2. 第一阶段目标

第一阶段不追求大规模业务改写，只追求三件事：

1. 把构建逻辑和基础层先拆出来，降低 `:app` 的依赖压力。
2. 把订阅筛选从“页面内临时状态”提升为“可持久化的领域模型”。
3. 保持现有 UI 行为基本不变，优先做结构迁移而不是交互重设计。

## 3. 目标模块图

建议先按下面这张最小可落地图来拆，而不是一步拆成几十个模块。

### 3.1 第一批必须模块

1. `:app`
   只保留 Application、导航装配、顶层依赖注入入口和发布配置。

2. `:build-logic`
   放 Android application/library、Compose、Kotlin、KSP、Lint、Detekt 或 Ktlint 约定插件。这个方向直接参考 EhViewer 的 `build-logic`。

3. `:core:model`
   放通用 entity、dto、分页模型、错误模型、排序和筛选领域对象。

4. `:core:network`
   放 `IwaraAPI`、OkHttp、Retrofit、拦截器和序列化工厂，把当前 [app/src/main/java/me/rerere/awara/di/NetworkModule.kt](../app/src/main/java/me/rerere/awara/di/NetworkModule.kt) 中的网络配置迁出去。

5. `:core:database`
   放 Room 数据库、DAO、实体映射和本地查询定义。

6. `:data`
   放 `MediaRepo`、`UserRepo`、`CommentRepo` 等仓储实现，作为 network/database 的组合层。当前 [app/src/main/java/me/rerere/awara/data/repo/MediaRepo.kt](../app/src/main/java/me/rerere/awara/data/repo/MediaRepo.kt) 已经是很直接的拆分起点。

7. `:ui`
   放通用 Compose 组件、主题、公共状态组件、分页组件、筛选弹窗组件。

### 3.2 第二批 feature 模块

第一批稳定后，再拆这几个 feature：

1. `:feature:home`
   首页、推荐、订阅流、首页筛选。

2. `:feature:search`
   搜索、搜索结果、搜索筛选。

3. `:feature:player`
   播放器、播放页、相关视频、播放偏好。

4. `:feature:download`
   下载页、下载任务、下载通知、下载数据库映射。

5. `:feature:user`
   登录、用户页、关注、好友、消息。

6. `:feature:library`
   收藏、历史、播放列表、稍后看。

## 4. 模块拆分顺序

建议按下面顺序迁移，避免一次性动太多页面。

### 阶段 A：只拆工具链和基础层

1. 新增 `:build-logic`，把 `build.gradle.kts` 和 `app/build.gradle.kts` 中重复的 Android/Kotlin/Compose 约定抽成插件。
2. 新增 `:core:model`，先迁移 entity、dto、分页模型、错误模型。
3. 新增 `:core:network`，迁移网络配置和 API 声明。
4. 新增 `:data`，迁移 `MediaRepo`、`UserRepo`、`CommentRepo`。

这一阶段结束后，`IndexVM`、`SearchVM`、`DownloadVM` 仍可在 `:app` 内，但依赖的 repo 已从 `:data` 提供。

### 阶段 B：先拆高收益 feature

1. 先拆 `:feature:player`
   播放器依赖最重，且单独拆出后最利于后续升级媒体栈和缓存策略。

2. 再拆 `:feature:download`
   下载链路天然适合隔离，因为它已经有 `DownloadVM`、`DownloadWorker` 和本地数据库查询。

3. 然后拆 `:feature:home` 与 `:feature:search`
   这两块和筛选体系耦合最深，适合在订阅视图设计稳定后一起迁移。

## 5. 订阅筛选体系改造方案

## 5.1 当前问题

当前实现的主要问题有四个：

1. 筛选对象无类型。
   `FilterValue` 只有字符串键值，缺少日期区间、标签逻辑、评分区间、媒体类型、已读状态等语义。

2. 页面之间不能复用。
   首页筛选、搜索筛选、收藏筛选未来都会走类似查询，但当前每个页面更像自己拼参数。

3. 筛选不可持久化。
   `IndexVM` 的 `videoFilters`、`imageFilters` 都只活在当前 ViewModel 生命周期内，不能保存为“订阅视图”。

4. 参数转换散落在页面层。
   当前由 `toParams()` 直接把 UI 层对象转换成 API 查询参数，不利于扩展到本地数据库筛选与导出导入。

## 5.2 目标模型

建议新增一套明确的查询领域对象。

### 核心领域对象

1. `FeedScope`
   表示查询作用域，例如首页视频、首页图片、订阅视频、订阅图片、搜索结果、收藏、历史。

2. `FeedSort`
   替代当前裸字符串排序值，统一首页和搜索页排序定义。

3. `FeedFilter`
   使用密封类表达不同筛选语义，例如：
   标签、日期范围、评分范围、时长范围、作者、是否订阅、是否已下载、是否已观看、是否 NSFW。

4. `FeedQuery`
   一个完整查询对象，包含 `scope`、`keyword`、`sort`、`filters`、`page`、`pageSize`。

5. `SavedFeedView`
   表示“保存的订阅视图”，包含名称、描述、图标、默认排序、筛选集和是否固定到首页。

## 5.3 数据层设计

建议在 Room 里新增两张表：

1. `saved_feed_view`
   保存视图基本信息，如 `id`、`name`、`scope`、`sort`、`pinned`、`createdAt`、`updatedAt`。

2. `saved_feed_filter`
   保存视图下的筛选条目，如 `viewId`、`type`、`operator`、`value`、`extraValue`。

这样做有两个好处：

1. 本地保存的智能视图不依赖服务端支持。
2. 同一套视图既能驱动远端 API 参数，也能驱动本地数据库筛选。

## 5.4 参数转换边界

建议把“领域查询对象 -> API 参数”的转换集中到 `:data` 层，而不是继续放在 UI 层。

可以抽一个 `FeedQueryMapper`，专门负责：

1. `FeedQuery -> Map<String, String>`
2. `SavedFeedView + page -> FeedQuery`
3. `FilterValue legacy -> FeedFilter`

这样做可以先兼容现有 UI，再逐步把旧的 `FilterValue` 调用点迁走。

## 5.5 首页与搜索页的统一方式

建议不要继续让首页和搜索页各写一套查询状态，而是引入统一状态容器。

### 建议的 ViewModel 责任划分

1. `HomeFeedVM`
   只负责首页推荐、订阅、图片、视频等首页分区的视图状态。

2. `FeedFilterVM`
   只负责筛选、排序、保存视图、恢复视图。

3. `SearchVM`
   只负责搜索输入和结果分页，但查询模型与首页共用 `FeedQuery`。

当前 [app/src/main/java/me/rerere/awara/ui/page/search/SearchVM.kt](../app/src/main/java/me/rerere/awara/ui/page/search/SearchVM.kt) 与 [app/src/main/java/me/rerere/awara/ui/page/index/IndexVM.kt](../app/src/main/java/me/rerere/awara/ui/page/index/IndexVM.kt) 都是自己拼接查询条件，后面应该改为共同依赖 `FeedQueryUseCase`。

## 5.6 第一版用户可见能力

第一版不需要一口气做完所有高级筛选，建议先交付这 6 个能力：

1. 保存当前视频筛选为订阅视图。
2. 保存当前图片筛选为订阅视图。
3. 首页固定显示若干个保存视图入口。
4. 订阅视图支持导出和导入。
5. 视图支持一键重置到默认排序。
6. 搜索页可以读取保存视图作为初始筛选模板。

## 6. 具体落地任务拆分

### 任务 1：提取基础查询模型

影响范围：

1. `:core:model`
2. 当前 `FilterValue`、排序选项、分页模型

完成标志：

1. `FeedQuery`、`FeedFilter`、`FeedSort` 已存在。
2. 旧 `FilterValue` 仍可兼容，但只作为 UI 过渡层。

### 任务 2：新增保存视图数据库

影响范围：

1. `:core:database`
2. 导出导入逻辑
3. 订阅视图仓储

完成标志：

1. 能保存、读取、删除和排序保存视图。
2. 能把视图导出为 JSON。

### 任务 3：首页接入保存视图

影响范围：

1. `:feature:home`
2. 首页顶部入口和筛选弹窗

完成标志：

1. 首页可以切换多个保存视图。
2. 切换视图后自动刷新列表。

### 任务 4：搜索页复用查询模型

影响范围：

1. `:feature:search`
2. 查询到参数映射

完成标志：

1. 搜索页排序和筛选不再自管裸字符串。
2. 搜索结果页可从保存视图初始化。

## 7. 与 EhViewer 和 FreshRSS 的对应关系

## 7.1 借鉴 EhViewer 的点

1. 先拆 `build-logic`，再拆 feature。
2. 先统一工具链和约定，再升级依赖。
3. 用模块边界而不是包目录来降低回归面。

## 7.2 借鉴 FreshRSS 的点

1. 查询和视图是长期资产，不是一次性 UI 状态。
2. 保存视图、标签视图、导入导出、同步恢复比“多几个筛选按钮”更值钱。
3. 扩展点和数据边界要先定义清楚，再往上叠功能。

## 8. 建议的下一个提交顺序

如果你要继续往前推进，建议直接按这个顺序做：

1. 新增 `:build-logic`，把 Android/Kotlin/Compose 约定抽离。
2. 新增 `:core:model` 和 `FeedQuery` 相关领域对象。
3. 新增保存视图的 Room 表与仓储接口。
4. 把首页视频/图片筛选从 `FilterValue` 改接 `FeedQuery`。
5. 再把搜索页接入同一套查询模型。
