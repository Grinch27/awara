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

## 0. 现状校正（2026-05-08）

当前分阶段改造已经先落下了几块基础能力，这里先校正一次状态，避免后续继续按旧前提推进：

1. `:build-logic`、`:core:model`、`:data` 已经落地，并且在提交 `4240d0f753eb8252d0be3c25a79b08d519345632` 对应的 `build-apk.yml` 中验证通过。
2. 保存视图不再只是“能保存”：当前首页视频/图片筛选面板已经可以读取本地保存视图，并直接复用到当前排序和筛选条件；保存视图的 JSON 导出导入能力也已经接上。
3. 设置页的数据入口不再只覆盖日志与保存视图：当前已经补上统一的本地数据备份包，覆盖保存视图、历史、下载记录和不含登录态的安全设置；应用日志仍保持单独的脱敏导出通道。
4. 首页壳已经继续按 EhViewer 的使用习惯收敛：手机首页改为抽屉优先、平板首页改为常驻左侧主导航，频道切换、快捷入口和固定保存视图都统一收进首页左侧壳层，不再依赖底部导航或顶部标签条作为主入口。
5. 首页左侧壳层已经继续往 EhViewer / FreshRSS 的信息组织方式靠拢：主浏览、社区、快捷入口和保存视图分段展示，选中态和保存视图条目都已经改成更高信息密度的两行布局。
6. 保存视图已经继续进入“标签化 + 智能订阅 + 独立管理”阶段：本地模型、导出导入和首页保存入口都已经补上 tags / smartSubscription / pinned 元数据，同时已经新增显式 `pinOrder`、标签过滤、固定顺序调整和独立保存视图管理页。
7. 网络栈已经补上第一层共享边界：DoH 偏好现在继续保持全局生效，同时已经新增 ECH 全局偏好和统一的 `NetworkTransportPolicy` 接口；但标准 Android / Maven Central 可用的 Conscrypt Java API 当前没有公开 ECH 方法，真正的 ECH 传输仍受限于自定义 Conscrypt / JNI 分支能力。

这意味着后续阶段不应再把“保存视图导出导入”或“基础本地数据备份”当成待设计项，而应该继续往“跨页面复用、固定入口、搜索模板复用、类型化查询模型收敛”推进。

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

## 9. 现状校正与优先级修正

这一节是结合当前仓库代码、现有工作流和对 EhViewer / FreshRSS 的参考后，对上面方案做的校正，避免把已经完成的事情继续当成待办。

### 9.1 已经存在的能力

1. 全局内置 DoH 已经落地，而且默认值已经与本次目标一致。
   当前 [app/src/main/java/me/rerere/awara/util/ConfigurableDohDns.kt](../app/src/main/java/me/rerere/awara/util/ConfigurableDohDns.kt) 已经实现全局 DNS 解析器，默认 endpoint 是 `doh.opendns.com/dns-query`，默认上游是 `dns.alidns.com/dns-query`。

2. 设置页已经暴露了 DoH 开关、endpoint 和 upstream 配置。
   当前 [app/src/main/java/me/rerere/awara/ui/page/setting/SettingPage.kt](../app/src/main/java/me/rerere/awara/ui/page/setting/SettingPage.kt) 已经提供 UI 配置入口，所以这部分下一步重点不是“从零实现”，而是补验证、重置入口、输入校验和故障回退。

3. GitHub Actions 已经改成在 CI 中动态同步最新稳定版 Gradle。
   当前 [.github/workflows/build-apk.yml](../.github/workflows/build-apk.yml)、[.github/workflows/build-apk-docker.yml](../.github/workflows/build-apk-docker.yml) 都会执行 [scripts/sync-gradle-wrapper.sh](../scripts/sync-gradle-wrapper.sh)，因此远端构建已经不是固定死在 `9.1.0`。

4. `FeedQuery` 相关领域对象已经开始存在，只是还没有完成真正的模块迁移和持久化接线。
   当前 [app/src/main/java/me/rerere/awara/domain/feed/FeedQuery.kt](../app/src/main/java/me/rerere/awara/domain/feed/FeedQuery.kt) 已经有 `FeedQuery`、`FeedScope`、`FeedFilter`、`SavedFeedView`，并且 [app/src/main/java/me/rerere/awara/domain/feed/LegacyFeedQueryMapper.kt](../app/src/main/java/me/rerere/awara/domain/feed/LegacyFeedQueryMapper.kt) 也已经开始承担旧 `FilterValue` 到新查询模型的过渡。因此第一阶段更准确的目标不是“新增模型”，而是把这些模型迁到 `:core:model` / `:data`，再接上 Room 和页面状态。

### 9.2 当前不建议立刻改动的点

1. 本地 `gradle-wrapper.properties` 不建议直接改成每次都跟随最新版。
   当前顶层插件仍是 AGP `9.0.1`，而 CI 之所以可以动态同步，是因为它先执行验证，再用工作流兜住兼容性风险。仓库内本地 wrapper 继续保留一个已知可启动基线，会比“每次打开工程都追最新”更稳。这个点更适合作为 `build-logic` 和 AGP 升级完成后的第二步，而不是现在先动。

2. ECH 不能按“只加一个设置项”的量级理解。
   参考 UjuiUjuMandan 的 EhViewer patch，ECH 需要至少同时引入 Conscrypt、自定义 `SSLSocketFactory`、额外 DNS / HTTPS 记录解析、ECH 拒绝后的重试或缓存失效逻辑，以及额外的 ProGuard keep / dontwarn 规则。也就是说，它更像一条单独网络栈分支，而不是普通设置页选项。
   进一步确认后，当前公开可用的 Conscrypt 稳定版和公开源码里都没有可直接接入的 `setEchConfigList` / `getEchConfigList` / `setCheckDnsForEch` / `EchRejectedException` Java API，因此 awara 这条线如果要继续推进，基本前提已经不是“补几个依赖”，而是要接受自编译 Conscrypt / JNI 分支或平台私有实现方案。

### 9.3 建议提升优先级的两件事

1. 先补持久化行为日志，而不是先上 ECH。
   当前仓库大量使用 Android `Log`，但没有统一的本地持久化日志池。相比 ECH，先做一个默认上限 10000 条、带敏感字段脱敏和导出能力的日志模块，能更快帮助排查下载、登录、评论、解析失败和工作流回归问题。

2. 把 DoH 从“可配置”提升到“可运维”。
   下一步更有价值的是加 4 个配套能力：
   一是 endpoint / upstream 输入格式校验。
   二是恢复默认值按钮。
   三是最近一次解析失败原因展示。
   四是按域名区分策略的扩展位，为以后是否单独给 API / 视频 / 图片分流做准备。

### 9.4 从 EhViewer 和 FreshRSS 得到的更具体结论

1. EhViewer 值得借鉴的不是“模块数量多”，而是它先用 `build-logic` 和 `core:*` 把约定与基础层抽离，再持续升级 AGP / Gradle / Paging / Compose。Awara 现在最值得照搬的是这个迁移顺序，而不是先把页面全部拆碎。

2. FreshRSS 值得借鉴的不是单纯筛选器数量，而是“保存查询 -> 分享查询 -> 导出/导入 -> 扩展点”这一整条资产化思路。Awara 的订阅视图如果只停在本地弹窗状态，就拿不到它真正的长期价值。

3. 因此 Awara 的第一阶段目标应该明确调整为：
   先统一查询模型和保存视图。
   再补导出导入与恢复能力。
   然后才考虑 ECH、下载策略插件化、外部播放器策略等更深的网络或扩展能力。
