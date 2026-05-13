# Awara 模块执行蓝图与事实基线

## 0. 文档定位

本文是 Awara 当前实现的事实基线、Agent 执行手册和人工维护速查。后续任何 Agent 进入本仓库时，应先读本文，再读相关 Kotlin、资源文件和最近提交差异。

本文服务三个目标：

1. 让人工维护者快速理解 Awara 的产品风格、模块边界、接口事实和禁止回退点。
2. 让 GPT Agent 能按固定流程完成探索、修复、文档更新、提交、推送和 GitHub Actions 验证。
3. 让 CI 修复者明确：本地不构建 APK，不用本地 Gradle build 作为通过依据；最终构建只看 GitHub Actions。

如果本文与代码冲突：以最新代码、提交和 Actions 结果为准。确认本文过期后，必须完整重写本文，而不是只追加零散备注。

## 1. Agent Awesome Prompt

你正在维护 Awara，一个 Kotlin + Jetpack Compose 的 Iwara Android 客户端。产品方向是紧凑、高信息密度、接近 EhViewer 的实用型客户端，同时在设置、阅读式弹窗和列表组织上借鉴 FeedMe 的清晰分组方式。你应优先保留用户正在使用的真实功能，避免把页面退回占位状态。

执行要求：

1. 先读 `doc/module-subscription-plan.md`。
2. 再读相关 Kotlin、资源、路由和 DI 文件。
3. 修改代码前确认现有实现边界。
4. 不使用用户贴出的 Authorization token、cookie 或任何敏感凭据。
5. 公开 API 检查只使用无凭据请求。
6. 不在本地运行 Gradle build。
7. 不在本地构建 APK。
8. 修改完成后完整重写本文。
9. 提交、推送、触发 `.github/workflows/build-apk.yml`。
10. 等待 Actions debug/release 结果。
11. Actions 失败时只修第一处 blocker，然后重复提交、推送、触发工作流。

信心循环：

1. 写出不确定点。
2. 用代码阅读、file errors、grep、无凭据 API、`git diff --check` 消除不确定点。
3. 只修被事实证明的问题。
4. 重复直到剩余风险只依赖 GitHub Actions 构建事实。

当前核心事实：

1. 首页顶部栏右侧只保留搜索和视图两个按钮。
2. 首页顶部栏标题是视频/图片/论坛切换器。
3. 首页 phone 版仍保留左侧抽屉按钮；tablet 版仍保留左侧常驻抽屉。
4. 首页 video/image 内容区不再显示“日期 + 筛选”第二行。
5. 视图菜单分为“查看”和“拦截图片”两组。
6. “查看”复用 `setting.media_list_mode`，可切换详情/缩略图。
7. “拦截图片”使用 `setting.block_media_thumbnails`，开启后媒体列表卡片不请求缩略图。
8. 搜索页仍保留独立的 sort/rating/date/browse tags 控制栏。
9. 论坛已有原生只读功能页：板块、主题列表、帖子详情、动态加载。
10. 论坛写入、发帖、回帖发送不在当前实现范围内。

## 2. 不可谈判约束

### 2.1 允许的本地动作

允许：

1. 阅读文件。
2. 搜索代码。
3. 查看 diff。
4. IDE/file errors。
5. `git diff --check`。
6. `git status --short --branch`。
7. 无凭据公开 API 最小验证。
8. `gh run list`、`gh run view`、`gh api` 查询 Actions 状态。

禁止：

1. 本地 Gradle build。
2. 本地 APK 构建。
3. 用本地 build 结果替代 Actions。
4. 输出、保存或提交 Authorization、cookie、JWT、账号密码。
5. 使用用户贴出的 Authorization header。
6. 为绕过接口不确定性而做假 UI。
7. 把真实页面退回 `TodoStatus` 或 still in developing。

### 2.2 提交流程

每轮功能或修复完成后：

1. 完整重写本文。
2. 运行允许范围内验证。
3. `git diff --check`。
4. `git status --short --branch`。
5. `git add` 相关文件。
6. `git commit`。
7. `git push`。
8. `gh workflow run build-apk.yml --ref ci/privacy-gradle9-actions`。
9. 等待 Actions 完成。
10. 若失败，先看第一处失败 blocker，不做无关重构。

### 2.3 隐私规则

可以记录：

1. 公开 URL path。
2. 无敏感 query。
3. HTTP 状态码。
4. count、limit、page、results 数量。
5. 公开 section id、thread id。

禁止记录：

1. Authorization。
2. Cookie。
3. JWT。
4. 用户 token。
5. 账号密码。
6. 私人响应体全文。

## 3. 产品风格

Awara 应保持高密度工具型客户端风格：

1. 内容优先。
2. 操作靠近对象。
3. 列表信息可快速扫读。
4. 卡片只用于真实条目、弹窗或工具，不做装饰堆叠。
5. 顶部控件紧凑，不做营销式布局。
6. 错误态、加载态、空态都是产品态。
7. 不使用大面积说明文替代功能。

对标方向：

1. 类 EhViewer 的主导航密度、抽屉结构和媒体条目扫读体验。
2. 类 EhViewer 的“标题即当前分区”心智，但标题本身可切换视频/图片/论坛。
3. 类 FeedMe 的设置分组和操作菜单分层：一级按钮少，二级菜单清晰分组。
4. 移动端优先，但 tablet 宽屏保留常驻抽屉，不浪费空间。
5. 论坛和搜索都应能连续浏览，不被外部浏览器占位打断。

## 4. 模块地图

主要模块：

1. `:app`：应用壳、数据源、仓库、DI、首页、论坛、详情页、用户页、设置页。
2. `:feature:search`：搜索页 UI、SearchVM、搜索 repository contract。
3. `:core:model`：共享模型、UiState、feed query、sort/rating 常量。
4. `:data`：FeedQuery 到 API 参数映射。
5. `:feature:player`：播放器功能。

关键文件：

1. `app/src/main/java/me/rerere/awara/ui/page/index/layout/IndexTopBar.kt`
2. `app/src/main/java/me/rerere/awara/ui/page/index/layout/IndexPagePhoneLayout.kt`
3. `app/src/main/java/me/rerere/awara/ui/page/index/layout/IndexPageTabletLayout.kt`
4. `app/src/main/java/me/rerere/awara/ui/page/index/pager/IndexVideoPage.kt`
5. `app/src/main/java/me/rerere/awara/ui/page/index/pager/IndexImagePage.kt`
6. `app/src/main/java/me/rerere/awara/ui/page/index/pager/IndexForumPage.kt`
7. `app/src/main/java/me/rerere/awara/ui/component/iwara/MediaListMode.kt`
8. `app/src/main/java/me/rerere/awara/ui/component/iwara/MediaCard.kt`
9. `app/src/main/java/me/rerere/awara/ui/page/setting/SettingPage.kt`
10. `feature/search/src/main/java/me/rerere/awara/ui/page/search/SearchPage.kt`
11. `feature/search/src/main/java/me/rerere/awara/ui/page/search/SearchVM.kt`
12. `app/src/main/java/me/rerere/awara/ui/page/search/AppSearchRepository.kt`
13. `app/src/main/java/me/rerere/awara/data/source/IwaraAPI.kt`
14. `app/src/main/java/me/rerere/awara/data/repo/MediaRepo.kt`
15. `app/src/main/java/me/rerere/awara/data/repo/ForumRepo.kt`
16. `app/src/main/java/me/rerere/awara/data/entity/Forum.kt`
17. `app/src/main/java/me/rerere/awara/ui/page/forum/ForumVM.kt`
18. `app/src/main/java/me/rerere/awara/ui/page/forum/ForumComponents.kt`
19. `app/src/main/java/me/rerere/awara/ui/page/forum/ForumSectionPage.kt`
20. `app/src/main/java/me/rerere/awara/ui/page/forum/ForumThreadPage.kt`
21. `app/src/main/java/me/rerere/awara/ui/RouterActivity.kt`
22. `app/src/main/res/values*/strings.xml`

共享辅助：

1. `UiStateBox`：通用加载/错误/空状态容器。
2. `LoadMoreEffect(LazyListState, ...)`：普通列表触底加载。
3. `LoadMoreEffect(LazyStaggeredGridState, ...)`：瀑布流触底加载。
4. `loadMoreFooter(...)`：动态加载 footer。
5. `RichText`：Markdown 风格正文与链接点击。

## 5. 首页与顶栏事实

### 5.1 顶部栏结构

phone 布局：

1. 左侧是抽屉按钮。
2. 标题区域是 `IndexTopBarTitle`。
3. 右侧只有 `Search` 和 `IndexViewMenu` 两个按钮。
4. 不显示 lab、会话气泡、通知铃铛。

tablet 布局：

1. 左侧常驻 `IndexDrawer`。
2. 顶部栏标题同样使用 `IndexTopBarTitle`。
3. 右侧只有 `Search` 和 `IndexViewMenu` 两个按钮。
4. 不显示 lab、会话气泡、通知铃铛。

标题切换器：

1. 可切换 `video`、`image`、`forum`。
2. 使用当前 navigation 的 `titleRes` 显示当前标题。
3. DropdownMenu 中使用现有 `IndexNavigation.icon`，避免重新定义图标体系。
4. 选择项后只更新当前首页内容区，不打开新 route。
5. 抽屉仍保留 subscription/history/download/setting 等入口。

### 5.2 视图菜单

`IndexViewMenu` 是顶部右侧第二个按钮：

1. 第一组标题是“查看”。
2. “查看”包含详情和缩略图。
3. 当前选中项使用 check icon。
4. 状态写入 `setting.media_list_mode`。
5. `IndexVideoPage`、`IndexImagePage`、`IndexSubscriptionPage`、用户页、收藏页等读取同一个媒体列表模式偏好。

第二组标题是“拦截图片”：

1. 包含显示图片和拦截图片。
2. 当前选中项使用 check icon。
3. 状态写入 `setting.block_media_thumbnails`。
4. 该偏好由 `rememberBlockMediaThumbnailsPreference()` 统一读取。
5. 开启后 `MediaCard` 传给 Coil 的 model 为 null，并显示 surfaceVariant 占位块。
6. 开启后媒体列表卡片不请求缩略图。
7. 这与旧 `setting.work_mode` 不同：work mode 是加载后模糊，block thumbnails 是不加载。

### 5.3 首页视频/图片列表

当前正确结构：

1. 顶部栏下面直接是媒体内容网格。
2. 不再显示第二行 `FilterAndSort`。
3. 不再显示首页“日期”按钮。
4. 不再显示首页筛选按钮和筛选数量 badge。
5. 动态加载逻辑保留：距离列表末尾 6 个 item 时请求下一页。
6. 加载更多 footer 使用现有 `Spin`。

设计决策：

1. 用户明确要求移除截图中的“日期”整行。
2. 首页不是搜索页；首页媒体筛选 UI 不应和搜索页媒体控制栏混在一起。
3. 视图相关动作放入顶栏菜单，符合 FeedMe 式二级菜单。
4. 媒体列表继续使用 EhViewer 式高密度卡片，不做大卡片装饰化布局。

## 6. 搜索模块事实

### 6.1 搜索类型

搜索页支持：

1. `video`
2. `image`
3. `user`

规则：

1. 视频和图片使用媒体列表接口。
2. 用户使用 `/search` 用户搜索接口。
3. 切换类型会重置页号。
4. 目标类型存在有效搜索条件时自动搜索。
5. 用户类型不显示媒体 sort/rating/date/tag 控件。

### 6.2 搜索页顶部布局

当前正确布局：

1. 第一行：返回按钮 + 搜索框。
2. 搜索框左侧：搜索图标。
3. 搜索框右侧：清除/搜索按钮。
4. 搜索框内不放 sort。
5. 第二行：视频 / 图片 / 用户类型切换。
6. 第三行：视频/图片媒体控制栏。
7. 用户类型第三行显示用户搜索提示。

视频/图片媒体控制栏当前控件：

1. Sort 下拉：date / trending / popularity / views / likes。
2. Rating 下拉：all / ecchi / general。
3. Date 下拉：Any Date 或最近 24 个月。
4. Browse Tags 下拉：A-Z/0-9 动态标签。

搜索页和首页的区别：

1. 搜索页仍保留 sort/rating/date/tag 控制栏。
2. 首页 video/image 已移除 `FilterAndSort` 整行。
3. 不要因为首页移除“日期”行而删除搜索页的日期控件。

### 6.3 浏览标签

当前实现：

1. Browse Tags 是 `DropdownMenu`。
2. 顶部是 A-Z/0-9 filter chip。
3. 默认 filter 是 `A`。
4. 打开下拉或切换 filter 时加载 page 0。
5. 标签列表触底加载下一页。
6. 接口是 `/tags?filter=<A-Z|0-9>&page=<0-based>`。
7. 返回 `Pager<Tag>`。
8. UI 使用 `SearchPageResult<String>`。
9. 下拉宽度收紧为 240dp 到 320dp，降低窄屏测量风险。
10. 标签 `LazyColumn` 使用 index-aware key，不再使用 tag 文本作为唯一 key。
11. 重复 tag 文本不会再触发 duplicate key crash。
12. 点击标签添加 `FilterValue("tags", tag)` 并重新搜索。
13. 点击已选 tag chip 移除 filter 并重新搜索。

### 6.4 搜索分页

媒体与用户搜索动态加载规则：

1. 提交搜索时 page 重置为 1。
2. API 请求使用 0-based page。
3. 首次搜索替换列表。
4. 下一页追加列表。
5. 下一页开始前同步置 `loadingMore = true`。
6. 追加失败不能清空旧列表。
7. `hasMore = mergedList.size < pager.count`。
8. 空关键词但有媒体条件时允许继续加载。
9. 同一 itemCount 防重复触底请求。

### 6.5 用户搜索

用户搜索接口：

1. `GET /search`
2. `type=users`
3. `sort=relevance`
4. `query=<keyword>`
5. `page=<0-based>`

导航规则：

1. `SearchUserItem.hasNavigableProfile` 必须为 true 才能点击。
2. hasNavigableProfile 当前含义是 username 非空。
3. 路由值使用 `Uri.encode(username)`。
4. Iwara profile API 是 `/profile/{username}`，因此用户搜索结果进入资料页使用 username，而不是 UUID。
5. 评论卡片、作者卡片、用户卡片、论坛用户入口都使用 `Uri.encode(username)`。
6. 自己资料/关注页使用 user id 时也对 route segment 做 `Uri.encode`，并避免把 null 拼进 route。

## 7. 论坛模块事实

### 7.1 范围

当前论坛功能是原生只读浏览：

1. 首页论坛板块列表。
2. 板块主题列表。
3. 主题帖子详情。
4. 板块主题动态加载。
5. 帖子详情动态加载。
6. Markdown/RichText 正文渲染。
7. 用户头像/名称展示。
8. 可点击 forum 用户进入资料页。

明确排除：

1. 发帖。
2. 回帖。
3. 编辑帖子。
4. 删除帖子。
5. 管理审核。
6. 使用需要登录写入权限的接口。

### 7.2 公开 API

已无凭据验证过的论坛接口：

1. `GET /forum` 返回 section 数组。
2. `GET /forum/announcements?page=0` 返回 section page。
3. Section page 返回 `{ section, threads, count, limit, page }`。
4. `GET /forum/threads/e31f9aa9-acdd-4971-b094-cac96a1ef3f8?page=0` 返回 thread page。
5. Thread page 返回 `{ thread, results, count, limit, page, pendingCount }`。
6. `GET /forum/{threadId}` 返回 404，不用于主题详情。

### 7.3 数据模型与路由

论坛模型在 `data/entity/Forum.kt`：

1. `ForumSection`
2. `ForumThread`
3. `ForumLastPost`
4. `ForumPost`
5. `ForumUser`
6. `ForumSectionPage`
7. `ForumThreadPage`

设计决策：

1. 使用独立 `ForumUser`，不复用严格的 `User` 实体。
2. ForumUser 字段默认值更宽松，降低 forum payload 缺字段导致解析失败的风险。
3. 字符串字段使用 `EmptyStringSerializer`。
4. 时间字段使用 `InstantSerializer` 并允许 nullable。
5. 未建模字段依赖全局 JSON `ignoreUnknownKeys` 忽略。

路由：

1. `forum/section/{sectionId}`
2. `forum/thread/{threadId}`

导航规则：

1. sectionId 使用 `Uri.encode(section.id)`。
2. threadId 使用 `Uri.encode(thread.id)`。
3. forum 用户 username 使用 `Uri.encode(username)`。
4. Back 使用 `router.popBackStack()`。

## 8. 设置页事实

外观设置包含：

1. 暗色模式。
2. 动态色彩。
3. 工作模式。
4. 媒体列表模式。
5. 拦截图片。
6. 搜索评级。
7. 默认入口页面。

媒体列表模式：

1. key 是 `setting.media_list_mode`。
2. 可选 `detail` 和 `thumbnail`。
3. 首页视图菜单和设置页共享同一偏好。
4. 搜索、首页、用户页等读取全局设置。

拦截图片：

1. key 是 `setting.block_media_thumbnails`。
2. 默认 false。
3. 首页视图菜单和设置页共享同一偏好。
4. 当前实现作用于 `MediaCard`。
5. 开启后媒体列表卡片不加载缩略图，显示主题色占位。
6. 该功能不同于 `setting.work_mode` 的模糊效果。

搜索相关设置：

1. `setting.media_search_rating` 控制视频/图片搜索默认 rating。
2. 默认值是 `all`。
3. 可选 `all`、`ecchi`、`general`。
4. 搜索页初次打开时读取该默认值。
5. 搜索页内 rating 下拉只改当前页状态，不写回全局设置。

默认入口：

1. 首页默认入口支持 subscription/video/image/forum。
2. forum 是原生页面，不是浏览器落地页。
3. 顶部标题切换器只列出 video/image/forum；subscription 仍可通过抽屉进入。

## 9. 禁止回退清单

生产路径中不应重新出现：

1. 首页顶部栏 lab/lens 按钮。
2. 首页顶部栏会话气泡按钮。
3. 首页顶部栏通知铃铛按钮。
4. 首页顶部栏右侧超过搜索和视图两个按钮。
5. 首页 video/image 顶部 `FilterAndSort` 行。
6. 首页 video/image 顶部“日期”按钮。
7. 首页 video/image 顶部旧筛选按钮和筛选 badge。
8. 搜索框内 sort 下拉。
9. `SearchBarSortDropdown`。
10. 搜索页 tag bottom sheet。
11. `ModalBottomSheet` in SearchPage。
12. `items(tags, key = { it })`。
13. 未编码 username route。
14. blank username route。
15. `PaginationBar`。
16. `MediaListModeButton`。
17. `jumpTo*Page`。
18. `change*Page`。
19. `jump*Page`。
20. 真实 forum 页退回浏览器占位。
21. 真实页面 `TodoStatus`。

允许存在：

1. API 层 page 参数。
2. VM 内 page 状态。
3. LoadMoreEffect。
4. `HorizontalPager` 用于 tab。
5. `TodoStatus` 组件定义本身，只要真实页面不调用它。
6. `FilterAndSort` 组件定义本身，只要首页 video/image 和搜索页不恢复旧路径。

## 10. 验证清单

提交前必须做：

1. file errors 覆盖所有修改 Kotlin/XML。
2. grep 搜索首页顶部旧按钮回归。
3. grep 搜索首页 video/image `FilterAndSort` 回归。
4. grep 搜索旧 SearchPage 控件回归。
5. grep 搜索 unsafe user route。
6. grep 搜索 page footer 回归。
7. grep 搜索用户 token/JWT/cookie 是否落盘。
8. 无凭据 API 检查 forum/tags/user search/profile。
9. `git diff --check`。
10. `git status --short --branch`。
11. 提交推送。
12. GitHub Actions build-apk.yml。

建议 grep：

1. `BuildConfig|Icons\.Outlined\.(Lens|Message|Notifications)|navigate\("lab"\)|navigate\("conversations"\)` in `ui/page/index/layout`。
2. `FilterAndSort\(|sortOptions = MediaSortOptions|R\.string\.date` in `ui/page/index/pager`。
3. `SearchBarSortDropdown|FilterBottomSheet|ModalBottomSheet|items\(tags, key = \{ it \}\)`。
4. `PaginationBar|MediaListModeButton|jumpTo[A-Za-z]*Page|change[A-Za-z]*Page|jump[A-Za-z]*Page|onPageChange =`。
5. `navigate\("user/\$[A-Za-z]|navigate\("user/[^"$]`。
6. 用户 token 片段、JWT 前缀和 Authorization header 模式。

无凭据 API 检查：

1. `/forum`
2. `/forum/announcements?page=0`
3. `/forum/threads/e31f9aa9-acdd-4971-b094-cac96a1ef3f8?page=0`
4. `/tags?filter=A&page=0`
5. `/tags?filter=A&page=1`
6. `/search?type=users&page=0&query=latex&sort=relevance`
7. `/profile/bondage1_-`

禁止执行：

1. `./gradlew build`
2. `./gradlew assemble...`
3. 本地 APK 构建。
4. 带凭据 curl。
5. 使用用户贴出的 Authorization header。

## 11. 决策记录

### 11.1 为什么首页右侧只留搜索和视图

用户明确要求右上角只保留搜索和视图两个按钮。lab、通知和会话气泡入口会增加顶部噪音，也不符合 EhViewer/FeedMe 式主页面高密度但克制的操作模型。路由本身可保留，不在首页顶栏暴露。

### 11.2 为什么标题切换只放视频/图片/论坛

用户要求“视频”变成可切换视频、图片、论坛。subscription/history/download/setting 仍属于抽屉导航，不属于这个标题切换器的主浏览分区。

### 11.3 为什么首页移除 FilterAndSort 行

截图中的第二行是首页 video/image 页面的 `FilterAndSort`，不是搜索页。用户明确要求整行去除，因此首页内容区直接显示列表。搜索页的媒体控制栏保留，因为它属于搜索功能。

### 11.4 为什么拦截图片是独立偏好

旧 work mode 只是把已加载缩略图模糊，不能满足“拦截图片”的字面行为。新 `setting.block_media_thumbnails` 让 `MediaCard` 不请求缩略图，行为更接近 FeedMe 的 block image 思路。

### 11.5 为什么拦截图片先作用于 MediaCard

`MediaCard` 是首页、订阅、用户媒体列表等主要媒体列表卡片的共用组件。先从该组件接入能覆盖核心列表，不需要逐页复制逻辑。搜索页若未来也要同一策略，应在 feature search 中复用同一偏好或抽出共享 UI。

### 11.6 为什么论坛仍只读

用户需求是完整原生浏览。公开 API 已验证 section、thread、posts 可读；写入接口未验证。为避免猜接口和引入权限问题，当前实现只做完整浏览。

### 11.7 为什么用户搜索仍用 username

`IwaraAPI.getProfile` 是 `/profile/{username}`。公开验证 `/profile/bondage1_-` 返回 HTTP 200。搜索结果跳转使用 username 是正确方向，关键是非空检查和 `Uri.encode`。

## 12. 当前不确定点与处理方式

当前仍需事实验证的点：

1. GitHub Actions 是否接受新增 `Icons.Outlined.Image`、`ViewAgenda`、`Check` 等图标引用。
2. `rememberAsyncImagePainter(model = null)` 在目标依赖版本下是否维持空 painter 而不发请求。
3. 顶部标题切换器在极窄屏是否会被两个右侧按钮挤压。

已采取的风险控制：

1. `Icons.Outlined.Image` 已在 `IndexPage.kt` 使用过，图标依赖已存在。
2. `Icons.Outlined.ViewAgenda` 已在 `SettingPage.kt` 使用过，图标依赖已存在。
3. `Icons.Outlined.Check` 已在 `Message.kt` 使用过，图标依赖已存在。
4. 标题按钮使用 `widthIn(max = 220.dp)`、`maxLines = 1`、`TextOverflow.Ellipsis`。
5. 图片拦截分支不渲染 Image composable，只渲染占位 Box。
6. file errors 必须覆盖新增和修改文件。
7. 最终以 GitHub Actions debug/release 构建结果为准。

## 13. 后续改进方向

1. 搜索页可接入同一个 `setting.block_media_thumbnails`，但需确认 feature search 模块依赖边界。
2. 顶部视图菜单可在未来增加“紧凑/舒适密度”，但必须先有真实列表模式支持。
3. Forum section 可增加 group 分段标题，但不要牺牲列表密度。
4. Forum thread 可增加搜索/section 内排序，但必须先验证 API 参数。
5. Forum post 可增加复制链接/打开浏览器，但不应替代原生浏览。
6. RichText 若遇到未支持 markdown，可先安全显示纯文本，再扩展 parser。
7. 标签浏览可增加搜索框，但 A-Z/0-9 动态加载是当前稳定基线。

## 14. 接手 Prompt

后续 Agent 可直接使用：

你正在维护 Awara。先阅读 `doc/module-subscription-plan.md`。本地不运行 Gradle build，不本地构建 APK。修复时保留高密度工具型 UI，主要参考 EhViewer，并借鉴 FeedMe 的二级菜单和设置分组。首页顶部栏右侧只能有搜索和视图两个按钮；标题切换器只切视频、图片、论坛；首页 video/image 不恢复“日期 + 筛选”第二行。搜索页仍保留自己的 sort/rating/date/browse tags 控制栏。论坛当前是只读原生页：`/forum`、`/forum/{sectionId}`、`/forum/threads/{threadId}`。所有 username route 必须 username 非空并 `Uri.encode`。完成后完整重写本文，运行 file errors、grep、公开 API 检查、`git diff --check`，提交推送并触发 `build-apk.yml`，等待 Actions debug/release 成功。