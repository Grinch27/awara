# Awara 模块执行蓝图与事实基线

## 0. 文档定位

本文是 Awara 当前实现的事实基线、Agent 执行手册和人工维护速查。后续任何 Agent 进入本仓库时，应先读本文，再读相关代码。

本文服务三个目标：

1. 让人工维护者快速理解 Awara 的产品风格、模块边界、接口事实和禁止回退点。
2. 让 GPT Agent 能按固定流程完成探索、修复、文档更新、提交、推送和 GitHub Actions 验证。
3. 让 CI 修复者明确：本地不构建 APK，不用本地 Gradle build 作为通过依据；最终构建只看 GitHub Actions。

如果本文与代码冲突：以最新代码、提交和 Actions 结果为准；确认本文过期后，必须完整重写本文，而不是只追加零散备注。

## 1. Agent Awesome Prompt

你正在维护 Awara，一个 Kotlin + Jetpack Compose 的 Iwara Android 客户端。产品方向是紧凑、高信息密度、接近 EhViewer 的实用型客户端。你应优先保留用户正在使用的真实功能，避免把页面退回占位状态。

执行要求：

1. 先读 `doc/module-subscription-plan.md`。
2. 再读相关 Kotlin/资源文件。
3. 修改代码前确认现有实现边界。
4. 不使用用户贴出的 Authorization token、cookie 或任何敏感凭据。
5. 公开 API 检查只使用无凭据请求。
6. 不在本地运行 Gradle build。
7. 不在本地构建 APK。
8. 修改完成后完整重写本文。
9. 提交、推送、触发 `.github/workflows/build-apk.yml`。
10. 等待 Actions debug/release 结果。
11. Actions 失败时只修第一处 blocker，然后重复提交、推送、触发工作流。

本轮已知目标和当前实现方向：

1. 搜索页 sort 不在搜索框内。
2. 搜索框右侧只保留清除/搜索按钮。
3. 视频/图片搜索顶部控制栏包含 sort、rating、date、browse tags。
4. 旧 sort 旁筛选按钮和旧 bottom sheet 不恢复。
5. 浏览标签是 A-Z/0-9 下拉菜单，动态请求 `/tags?filter=<A-Z|0-9>&page=<page>`。
6. 标签列表使用 index-aware key，避免重复 tag 文本导致 Compose LazyColumn 崩溃。
7. 用户搜索使用 `/search?type=users&page=<page>&query=<keyword>&sort=relevance`。
8. 用户搜索结果进入资料页时只在 username 非空时导航，并使用 `Uri.encode(username)`。
9. 所有通用 username 导航入口都应编码。
10. 论坛已有原生只读功能页：板块列表、主题列表、帖子详情、动态加载。
11. 论坛写入、发帖、回帖发送不在当前实现范围内。

信心循环：

1. 写出不确定点。
2. 用代码阅读、无凭据 API、file errors、grep、`git diff --check` 消除不确定点。
3. 只修被事实证明的问题。
4. 重复直到剩余风险只依赖 GitHub Actions 构建事实。

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

1. 类 EhViewer 的主导航密度。
2. 类 EhViewer 的元数据优先列表。
3. 移动端优先，但宽屏不浪费空间。
4. 论坛和搜索都应能连续浏览，不被外部浏览器占位打断。

## 4. 模块地图

主要模块：

1. `:app`：应用壳、数据源、仓库、DI、首页、论坛、详情页、用户页、设置页。
2. `:feature:search`：搜索页 UI、SearchVM、搜索 repository contract。
3. `:core:model`：共享模型、UiState、feed query、sort/rating 常量。
4. `:data`：FeedQuery 到 API 参数映射。
5. `:feature:player`：播放器功能。

关键文件：

1. `feature/search/src/main/java/me/rerere/awara/ui/page/search/SearchPage.kt`
2. `feature/search/src/main/java/me/rerere/awara/ui/page/search/SearchVM.kt`
3. `feature/search/src/main/java/me/rerere/awara/ui/page/search/SearchRepository.kt`
4. `app/src/main/java/me/rerere/awara/ui/page/search/AppSearchRepository.kt`
5. `app/src/main/java/me/rerere/awara/data/source/IwaraAPI.kt`
6. `app/src/main/java/me/rerere/awara/data/repo/MediaRepo.kt`
7. `app/src/main/java/me/rerere/awara/data/repo/ForumRepo.kt`
8. `app/src/main/java/me/rerere/awara/data/entity/Forum.kt`
9. `app/src/main/java/me/rerere/awara/ui/page/forum/ForumVM.kt`
10. `app/src/main/java/me/rerere/awara/ui/page/forum/ForumComponents.kt`
11. `app/src/main/java/me/rerere/awara/ui/page/forum/ForumSectionPage.kt`
12. `app/src/main/java/me/rerere/awara/ui/page/forum/ForumThreadPage.kt`
13. `app/src/main/java/me/rerere/awara/ui/page/index/pager/IndexForumPage.kt`
14. `app/src/main/java/me/rerere/awara/ui/RouterActivity.kt`
15. `app/src/main/java/me/rerere/awara/di/RepoModule.kt`
16. `app/src/main/java/me/rerere/awara/di/ViewModelModule.kt`
17. `app/src/main/java/me/rerere/awara/ui/page/index/IndexDrawer.kt`
18. `app/src/main/java/me/rerere/awara/ui/component/iwara/UserCard.kt`
19. `app/src/main/java/me/rerere/awara/ui/component/iwara/AuthorCard.kt`
20. `app/src/main/java/me/rerere/awara/ui/component/iwara/comment/CommentCard.kt`
21. `app/src/main/res/values/strings.xml`
22. `app/src/main/res/values-zh-rCN/strings.xml`

共享辅助：

1. `UiStateBox`：通用加载/错误/空状态容器。
2. `LoadMoreEffect(LazyListState, ...)`：普通列表触底加载。
3. `LoadMoreEffect(LazyStaggeredGridState, ...)`：瀑布流触底加载。
4. `loadMoreFooter(...)`：动态加载 footer。
5. `RichText`：Markdown 风格正文与链接点击。

## 5. 搜索模块事实

### 5.1 搜索类型

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

有效搜索条件：

1. 视频：关键词非空，或 filters 非空，或 rating/sort 偏离默认值。
2. 图片：关键词非空，或 filters 非空，或 rating/sort 偏离默认值。
3. 用户：关键词非空。

### 5.2 搜索页顶部布局

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

布局策略：

1. 控制栏使用横向滚动 Row，避免移动端四个控件被挤压。
2. 每个控件有最小/最大宽度，文本超出省略。
3. 旧 `FilterAndSort`、旧 `FilterBottomSheet`、旧 search bar sort 不恢复。

### 5.3 媒体 sort

sort 值：

1. `date`
2. `trending`
3. `popularity`
4. `views`
5. `likes`

行为：

1. SortDropdown 位于媒体控制栏。
2. 切换 sort 后更新当前媒体类型状态。
3. 当前类型为 video 时更新 videoSort。
4. 当前类型为 image 时更新 imageSort。
5. 切换后立即重新搜索。
6. 用户搜索类型不显示该控件。

### 5.4 Rating 和 Date

rating 值：

1. `all`
2. `ecchi`
3. `general`

Date 规则：

1. 空值表示 Any Date。
2. 最近 24 个月用 `yyyy-M`。
3. Date filter key 是 `date`。
4. 修改日期会替换当前类型旧 date filter 并重新搜索。

### 5.5 浏览标签

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
14. 已删除旧 bottom sheet 标签浏览路径。

### 5.6 搜索分页

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

### 5.7 用户搜索

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
5. 已用无凭据公开请求验证：`/profile/bondage1_-` 返回 HTTP 200。
6. 评论卡片、作者卡片、用户卡片、论坛用户入口都使用 `Uri.encode(username)`。
7. 自己资料/关注页使用 user id 时也对 route segment 做 `Uri.encode`，并避免把 null 拼进 route。

## 6. 论坛模块事实

### 6.1 范围

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

### 6.2 公开 API

已无凭据验证：

1. `GET /forum` 返回 HTTP 200 和 section 数组。
2. `GET /forum/announcements?page=0` 返回 HTTP 200。
3. Section page 返回 `{ section, threads, count, limit, page }`。
4. `GET /forum/threads/e31f9aa9-acdd-4971-b094-cac96a1ef3f8?page=0` 返回 HTTP 200。
5. Thread page 返回 `{ thread, results, count, limit, page, pendingCount }`。
6. `GET /forum/thread/{id}` 也返回 HTTP 200，但当前实现优先使用复数 `/forum/threads/{id}`。
7. `GET /forum/{threadId}` 返回 404，不用于主题详情。

### 6.3 数据模型

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

### 6.4 API 与仓库

`IwaraAPI` 新增：

1. `getForumSections()` -> `/forum`
2. `getForumSection(sectionId, page)` -> `/forum/{sectionId}`
3. `getForumThread(threadId, page)` -> `/forum/threads/{threadId}`

`ForumRepo` 新增：

1. `getSections()`
2. `getSectionThreads(sectionId, page)`
3. `getThreadPosts(threadId, page)`

DI：

1. `RepoModule` 注册 `ForumRepo`。
2. `ViewModelModule` 注册 `ForumIndexVM`、`ForumSectionVM`、`ForumThreadVM`。

### 6.5 页面与路由

页面：

1. `IndexForumPage`：首页论坛板块列表，替换旧浏览器占位。
2. `ForumSectionPage`：板块主题列表。
3. `ForumThreadPage`：主题帖子详情。
4. `ForumComponents`：SectionCard、ThreadCard、ThreadHeader、PostCard、UserMeta。

路由：

1. `forum/section/{sectionId}`
2. `forum/thread/{threadId}`

导航规则：

1. sectionId 使用 `Uri.encode(section.id)`。
2. threadId 使用 `Uri.encode(thread.id)`。
3. forum 用户 username 使用 `Uri.encode(username)`。
4. Back 使用 `router.popBackStack()`。

### 6.6 动态加载

ForumSectionVM：

1. 初始加载 section page 0。
2. UI page 从 1 计数，API page 从 0 计数。
3. 触底调用 `loadNextPage()`。
4. `loadingMore` 阻止重复请求。
5. `hasMore = mergedThreads.size < count`。
6. 追加失败保留旧 threads。

ForumThreadVM：

1. 初始加载 posts page 0。
2. Header 使用 response.thread。
3. posts 使用 response.results。
4. pendingCount 只展示，不参与分页判断。
5. 追加失败保留旧 posts。

## 7. 用户资料留言板事实

现状：

1. 根留言接口：`/profile/{profileUserId}/comments?page=<page>`。
2. profileUserId 必须来自 `profile.user.id`。
3. 回复接口：`/profile/{profileUserId}/comments?page=<page>&parent=<commentId>`。
4. 用户资料留言保持只读。
5. 点击“共 N 条回复”进入回复线程。
6. `CommentState` stack 负责 push/pop。
7. Android Back 在回复线程中先返回上一层。
8. 旧请求返回时检查 active parent，避免覆盖当前线程。

## 8. 设置页事实

搜索相关设置：

1. `setting.media_search_rating` 控制视频/图片搜索默认 rating。
2. 默认值是 `all`。
3. 可选 `all`、`ecchi`、`general`。
4. 搜索页初次打开时读取该默认值。
5. 搜索页内 rating 下拉只改当前页状态，不写回全局设置。

媒体列表模式：

1. `setting.media_list_mode` 控制详情/缩略图模式。
2. 搜索、首页、用户页等读取全局设置。
3. 不恢复页面内模式切换按钮。

默认入口：

1. 首页默认入口支持 subscription/video/image/forum。
2. forum 现在是原生页面，不再是浏览器落地页。

## 9. 禁止回退清单

生产路径中不应重新出现：

1. 搜索框内 sort 下拉。
2. `SearchBarSortDropdown`。
3. `FilterAndSort`。
4. `FilterBottomSheet`。
5. 搜索页 tag bottom sheet。
6. `ModalBottomSheet` in SearchPage。
7. `items(tags, key = { it })`。
8. 未编码 username route。
9. blank username route。
10. `PaginationBar`。
11. `MediaListModeButton`。
12. `jumpTo*Page`。
13. `change*Page`。
14. `jump*Page`。
15. 真实 forum 页退回浏览器占位。
16. 真实页面 `TodoStatus`。

允许存在：

1. API 层 page 参数。
2. VM 内 page 状态。
3. LoadMoreEffect。
4. `HorizontalPager` 用于 tab。
5. `TodoStatus` 组件定义本身，只要真实页面不调用它。

## 10. 验证清单

提交前必须做：

1. file errors 覆盖所有修改 Kotlin/XML。
2. grep 搜索旧 SearchPage 控件回归。
3. grep 搜索 unsafe user route。
4. grep 搜索 page footer 回归。
5. grep 搜索用户 token/JWT/cookie 是否落盘。
6. 无凭据 API 检查 forum/tags/user search/profile。
7. `git diff --check`。
8. `git status --short --branch`。
9. 提交推送。
10. GitHub Actions build-apk.yml。

建议 grep：

1. `SearchBarSortDropdown|FilterAndSort|FilterBottomSheet|ModalBottomSheet|items\\(tags, key = \\{ it \\}\\)`
2. `PaginationBar|MediaListModeButton|jumpTo[A-Za-z]*Page|change[A-Za-z]*Page|jump[A-Za-z]*Page|onPageChange =`
3. `navigate\\("user/\\$[A-Za-z]|navigate\\("user/[^"$]`
4. 用户 token 片段、JWT 前缀和 Authorization header 模式。

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

### 11.1 Sort 为什么回到媒体控制栏

用户测试指出上轮把按钮移动错了。正确理解是：不是搜索框里的日期按钮，而是视频/图片顶部第二栏里的日期 + 筛选按钮区域。当前实现把 sort 放回媒体控制栏，搜索框保持纯搜索职责。

### 11.2 为什么控制栏横向滚动

四个控件同时存在：sort、rating、date、tags。移动端若用等分权重，文字容易挤压和溢出。横向滚动更符合高密度工具栏，也能保留完整功能。

### 11.3 为什么标签 key 使用 index-aware

Iwara tags API 可能返回重复 tag 文本。Compose LazyColumn 不允许同一层重复 key。使用 index-aware key 后，重复 tag 不再导致点击或加载时崩溃。

### 11.4 为什么论坛只读

用户提供的是浏览接口。公开 API 已验证 section、thread、posts 可读；写入接口未验证。为避免猜接口和引入权限问题，当前实现只做完整浏览。

### 11.5 为什么 ForumUser 独立于 User

论坛 user payload 可能比资料/搜索 user 更轻。复用严格 `User` 会放大缺字段解析风险。独立 ForumUser 用 nullable/default 字段更稳。

### 11.6 为什么用户搜索仍用 username

`IwaraAPI.getProfile` 是 `/profile/{username}`。公开验证 `/profile/bondage1_-` 返回 HTTP 200。搜索结果跳转使用 username 是正确方向，关键是非空检查和 `Uri.encode`。

## 12. 后续改进方向

1. Forum section 可增加 group 分段标题，但不要牺牲列表密度。
2. Forum thread 可增加搜索/section 内排序，但必须先验证 API 参数。
3. Forum post 可增加复制链接/打开浏览器，但不应替代原生浏览。
4. RichText 若遇到未支持 markdown，可先安全显示纯文本，再扩展 parser。
5. 标签浏览可增加搜索框，但 A-Z/0-9 动态加载是当前稳定基线。
6. 用户导航如发现 profile API 支持 UUID 和 username，应保持 route 语义清晰，不混用未验证入口。

## 13. 接手 Prompt

后续 Agent 可直接使用：

你正在维护 Awara。先阅读 `doc/module-subscription-plan.md`。本地不运行 Gradle build，不本地构建 APK。修复时保留高密度工具型 UI，不恢复旧页码、旧搜索 filter sheet、搜索框内 sort、论坛浏览器占位。搜索页当前正确布局是：返回 + 搜索框，搜索框右侧只有清除/搜索；视频/图片控制栏包含 sort/rating/date/browse tags；用户搜索无媒体控件。论坛当前是只读原生页：`/forum`、`/forum/{sectionId}`、`/forum/threads/{threadId}`。所有 username route 必须 username 非空并 `Uri.encode`。完成后完整重写本文，运行 file errors、grep、公开 API 检查、`git diff --check`，提交推送并触发 `build-apk.yml`，等待 Actions debug/release 成功。
