# Awara 模块执行蓝图与事实基线

## 0. 文档定位

本文是 Awara 当前实现的事实基线、Agent 执行手册和人工维护速查。后续任何 Agent 进入本仓库时，应先读本文，再读相关 Kotlin、资源、路由、DI 和最近 diff。

本文服务三个目标：

1. 让维护者快速理解当前产品方向、模块边界、接口事实和禁止回退点。
2. 让 Agent 按固定流程完成探索、修改、文档更新、提交、推送和 GitHub Actions 验证。
3. 明确 CI 规则：本地不构建 APK，不运行本地 Gradle build 作为通过依据；最终以 GitHub Actions 为准。

如果本文与代码冲突，以最新代码和 Actions 结果为准。确认本文过期后，必须完整重写本文，而不是只追加备注。

## 1. Agent 执行 Prompt

你正在维护 Awara，一个 Kotlin + Jetpack Compose 的 Iwara Android 客户端。产品风格是紧凑、高信息密度、接近 EhViewer 的实用型客户端，并借鉴 FeedMe 的设置分组和二级菜单组织。你应保留真实可用功能，避免把页面退回占位或外部浏览器替代。

每轮执行：

1. 先读 `doc/module-subscription-plan.md`。
2. 再读相关 Kotlin、资源、路由、DI、仓库和最近 diff。
3. 修改前确认现有实现边界。
4. 不使用、保存、输出或提交用户贴出的 Authorization、cookie、JWT、账号密码。
5. 公开 API 验证只使用无凭据请求和普通 User-Agent。
6. 不在本地运行 Gradle build。
7. 不在本地构建 APK。
8. 修改完成后完整重写本文。
9. 运行允许范围内验证：file errors、grep、无凭据 API、`git diff --check`。
10. 提交、推送、触发 `.github/workflows/build-apk.yml`。
11. 等待 Actions debug/release 完成。
12. Actions 失败时只修第一处 blocker，然后重复验证、提交、推送、触发工作流。

信心循环：

1. 写出不确定点。
2. 用代码阅读、file errors、grep、无凭据 API 和 diff 消除不确定点。
3. 只修被事实证明的问题。
4. 重复直到剩余风险只依赖 GitHub Actions 构建事实。

## 2. 不可谈判约束

允许的本地动作：

1. 阅读文件、搜索代码、查看 diff。
2. `get_errors` 或 IDE file errors。
3. `git diff --check`。
4. `git status --short --branch`。
5. 无凭据公开 API 最小验证。
6. `gh run list`、`gh run view`、`gh api` 查询 Actions 状态。

禁止的本地动作：

1. `./gradlew build`。
2. `./gradlew assemble...`。
3. 本地 APK 构建。
4. 带用户凭据的 curl 或 API 调用。
5. 输出、保存或提交 Authorization、cookie、JWT、账号密码。
6. 用假 UI 绕过接口不确定性。
7. 把真实页面退回 `TodoStatus` 或 still in developing。

提交流程：

1. 完整重写本文。
2. file errors 覆盖所有修改 Kotlin/XML。
3. 运行定向 grep。
4. 运行无凭据 API 验证。
5. `git diff --check`。
6. `git status --short --branch`。
7. `git add` 相关文件。
8. `git commit`。
9. `git push`。
10. `gh workflow run build-apk.yml --ref ci/privacy-gradle9-actions`。
11. 使用 `GH_PAGER=cat gh run view <run_id> --json status,conclusion,jobs,url,headSha` 查看结果。

## 3. 当前产品事实

当前核心状态：

1. 首页顶部栏右侧只保留搜索和视图两个按钮。
2. 首页顶部栏标题是视频/图片/论坛切换器。
3. 首页 phone 版保留左侧抽屉按钮；tablet 版保留左侧常驻抽屉。
4. 首页 video/image 内容区不再显示“日期 + 筛选”第二行。
5. 订阅页也不再显示顶部媒体类型切换行。
6. 顶部视图菜单只保留“查看”组：详情/缩略图。
7. 拦截图片功能只放在设置页，默认 false。
8. 搜索页支持 7 个分区：视频、图片、帖子、用户、播单、论坛帖子、论坛主题。
9. 视频/图片搜索保留 sort/rating/date/tag 控制栏。
10. 媒体 sort key `date` 的显示文案改为“最新”；API key 仍然是 `date`。
11. 搜索页新增日期筛选器，支持 `date=YYYY` 和 `date=YYYY-M`。
12. 搜索页标签浏览支持 A-Z/0-9 分页浏览，也支持按输入首个字母/数字快速加载并本地匹配。
13. 论坛已有原生只读页：板块、主题列表、帖子详情、动态加载。
14. 论坛写入、发帖、回帖发送不在当前范围。

产品风格：

1. 内容优先，控件紧凑。
2. 顶部栏不堆无关按钮。
3. 列表适合快速扫读。
4. 卡片只用于真实条目、弹窗或工具。
5. 不用大段说明文替代功能。
6. 错误态、加载态、空态都是产品态。
7. 首页和搜索页职责分离：首页浏览，搜索页筛选。

## 4. 模块地图

主要模块：

1. `:app`：应用壳、数据源、仓库、DI、首页、论坛、详情页、用户页、设置页。
2. `:feature:search`：搜索页 UI、SearchVM、搜索 repository contract 和搜索结果轻量模型。
3. `:core:model`：feed query、scope、filter 等共享模型。
4. `:data`：FeedQuery 到 API 参数映射。
5. `:feature:player`：播放器功能。

关键文件：

1. `app/src/main/java/me/rerere/awara/ui/page/index/layout/IndexTopBar.kt`
2. `app/src/main/java/me/rerere/awara/ui/page/index/layout/IndexPagePhoneLayout.kt`
3. `app/src/main/java/me/rerere/awara/ui/page/index/layout/IndexPageTabletLayout.kt`
4. `app/src/main/java/me/rerere/awara/ui/page/index/pager/IndexVideoPage.kt`
5. `app/src/main/java/me/rerere/awara/ui/page/index/pager/IndexImagePage.kt`
6. `app/src/main/java/me/rerere/awara/ui/page/index/pager/IndexSubscriptionPage.kt`
7. `app/src/main/java/me/rerere/awara/ui/component/iwara/MediaListMode.kt`
8. `app/src/main/java/me/rerere/awara/ui/component/iwara/MediaCard.kt`
9. `app/src/main/java/me/rerere/awara/ui/page/setting/SettingPage.kt`
10. `feature/search/src/main/java/me/rerere/awara/ui/page/search/SearchPage.kt`
11. `feature/search/src/main/java/me/rerere/awara/ui/page/search/SearchVM.kt`
12. `feature/search/src/main/java/me/rerere/awara/ui/page/search/SearchRepository.kt`
13. `app/src/main/java/me/rerere/awara/ui/page/search/AppSearchRepository.kt`
14. `app/src/main/java/me/rerere/awara/data/source/IwaraAPI.kt`
15. `app/src/main/java/me/rerere/awara/data/repo/MediaRepo.kt`
16. `app/src/main/java/me/rerere/awara/data/entity/Post.kt`
17. `app/src/main/java/me/rerere/awara/data/entity/Playlist.kt`
18. `app/src/main/java/me/rerere/awara/data/entity/Forum.kt`
19. `app/src/main/java/me/rerere/awara/ui/page/forum/ForumVM.kt`
20. `app/src/main/java/me/rerere/awara/ui/page/forum/ForumComponents.kt`
21. `app/src/main/java/me/rerere/awara/ui/page/forum/ForumSectionPage.kt`
22. `app/src/main/java/me/rerere/awara/ui/page/forum/ForumThreadPage.kt`
23. `app/src/main/java/me/rerere/awara/ui/RouterActivity.kt`
24. `app/src/main/res/values*/strings.xml`
25. `feature/search/src/main/res/values*/strings.xml`

## 5. 首页与顶栏事实

顶部栏：

1. Phone 布局左侧是抽屉按钮，标题是 `IndexTopBarTitle`，右侧是 `IndexTopBarActions`。
2. Tablet 布局左侧常驻 `IndexDrawer`，标题和右侧 action 与 phone 版一致。
3. 右侧只允许 `Search` 和 `IndexViewMenu`。
4. 不显示 lab、会话气泡、通知铃铛。
5. 标题切换器只列出 `video`、`image`、`forum`。
6. subscription/history/download/setting 仍通过抽屉进入。

视图菜单：

1. `IndexViewMenu` 只包含“查看”组。
2. “查看”包含详情和缩略图。
3. 当前选中项使用 check icon。
4. 状态写入 `setting.media_list_mode`。
5. 不再在顶部视图菜单暴露拦截图片。
6. 顶部视图菜单不得重新出现 `index_view_menu_block_images` 相关 UI。

首页 video/image：

1. 顶部栏下面直接是媒体内容网格。
2. 不显示 `FilterAndSort`。
3. 不显示首页“日期”按钮。
4. 不显示首页筛选按钮或筛选数量 badge。
5. 动态加载保留，靠近末尾 6 个 item 时加载下一页。
6. 列表模式读取 `setting.media_list_mode`。

订阅页：

1. 不再显示顶部 `FilledTonalButton` 和 `DropdownMenu` 切换行。
2. 页面直接显示 `UiStateBox` 和瀑布流列表。
3. 订阅页仍读取全局媒体列表模式。
4. `IndexVM.SubscriptionType` 可以存在，但当前没有订阅页顶部切换 UI。

## 6. 设置页与拦截图片

设置页外观分组保留拦截图片：

1. key 是 `setting.block_media_thumbnails`。
2. 默认值是 false。
3. 设置页 `SettingBooleanItem` 是该功能的唯一 UI 入口。
4. `MediaCard` 读取 `rememberBlockMediaThumbnailsPreference()`。
5. 开启后 `MediaCard` 不把缩略图 URL 传给 Coil，而是显示 `surfaceVariant` 占位。
6. 该功能不同于 `setting.work_mode`：work mode 是加载后模糊，block thumbnails 是不加载媒体缩略图。

媒体列表模式：

1. key 是 `setting.media_list_mode`。
2. 可选 `detail` 和 `thumbnail`。
3. 首页视图菜单和设置页共享同一偏好。
4. 首页、订阅、用户媒体列表等读取同一设置。

搜索默认评级：

1. key 是 `setting.media_search_rating`。
2. 默认 `all`。
3. 可选 `all`、`ecchi`、`general`。
4. 搜索页首次打开时读取该默认值。
5. 搜索页内 rating 下拉只改当前页状态，不写回全局设置。

## 7. 搜索模块事实

### 7.1 七类分区

`SearchTypes` 位于 `feature/search/.../SearchRepository.kt`。

UI 类型与接口：

1. `video`：使用 `/videos`，保留媒体 sort/rating/date/tags。
2. `image`：使用 `/images`，保留媒体 sort/rating/date/tags。
3. `post`：使用 `/search?type=posts&sort=relevance`。
4. `user`：使用 `/search?type=users&sort=relevance`。
5. `playlist`：使用 `/search?type=playlists&sort=relevance`。
6. `forum_post`：使用 `/search?type=forum_posts&sort=relevance`。
7. `forum_thread`：使用 `/search?type=forum_threads&sort=relevance`。

公开无凭据验证过的 search type：

1. `videos` HTTP 200。
2. `images` HTTP 200。
3. `posts` HTTP 200。
4. `users` HTTP 200。
5. `playlists` HTTP 200。
6. `forum_posts` HTTP 200。
7. `forum_threads` HTTP 200。

`SearchTypes.normalize()` 接受复数 API type 和单数 UI type，并规范到 UI type。

### 7.2 搜索页顶部布局

当前正确布局：

1. 第一行：返回按钮 + `DockedSearchBar`。
2. 第二行：横向滚动的 7 个搜索分区 chip。
3. 第三行仅在 `video`/`image` 显示媒体控制栏。
4. `user` 分区显示用户搜索提示。
5. 其他通用分区不显示媒体控制栏。
6. 搜索框 placeholder 是“Search Iwara”/“搜索 Iwara”。
7. 搜索框内不放 sort。

媒体控制栏：

1. Sort 下拉：`date`、`trending`、`popularity`、`views`、`likes`。
2. `date` 的显示文案是“Latest”/“最新”。
3. Rating 下拉：`all`、`ecchi`、`general`。
4. Date 下拉：Any Date、最近年份、最近 24 个月。
5. Date 值使用 `YYYY` 或 `YYYY-M`，直接进入 API 参数 `date`。
6. Browse Tags 下拉：支持 A-Z/0-9 分页和快速输入匹配。

### 7.3 搜索结果模型

`SearchRepository` 定义搜索结果轻量模型：

1. `SearchMediaItem`
2. `SearchUserItem`
3. `SearchPostItem`
4. `SearchPlaylistItem`
5. `SearchForumPostItem`
6. `SearchForumThreadItem`

App 层映射：

1. `Video`/`Image` 映射到 `SearchMediaItem`。
2. `User` 映射到 `SearchUserItem`。
3. `Post` 映射到 `SearchPostItem`。
4. `Playlist` 映射到 `SearchPlaylistItem`。
5. `ForumPost` 映射到 `SearchForumPostItem`。
6. `ForumThread` 映射到 `SearchForumThreadItem`。

新增实体事实：

1. `Post.kt` 是 `/search?type=posts` 的轻量实体。
2. `Playlist` 字段有默认值，并新增 `PlaylistThumbnail`，以兼容搜索结果缺少 `added`、包含 thumbnail 对象的情况。
3. `ForumPost` 新增 nullable `thread` 字段，用于 forum post 搜索结果显示和跳转。
4. 全局 JSON 已设置 `ignoreUnknownKeys = true`、`coerceInputValues = true`、`explicitNulls = false`。

### 7.4 搜索分页

分页规则：

1. ViewModel 内 `state.page` 从 1 开始。
2. API 请求 page 使用 `state.page - 1`。
3. 首次搜索替换当前分区列表。
4. 下一页追加当前分区列表。
5. 追加失败不能清空旧列表。
6. `hasMore = mergedList.size < pager.count`。
7. `loadingMore` 防止重复请求。
8. 搜索分区切换会重置 page、count、loadingMore、uiState。
9. 只有目标分区有有效搜索条件时才自动搜索。

### 7.5 日期与标签筛选

日期筛选：

1. 仅视频/图片分区显示。
2. 以 `FilterValue("date", value)` 存入对应 media filter list。
3. 切换日期会移除旧 date filter，只保留一个 date 值。
4. 支持 `date=2026`。
5. 支持 `date=2026-5`。
6. 已无凭据验证 `/videos` 和 `/images` 的年份/年月参数均返回 HTTP 200。

标签筛选：

1. 仅视频/图片分区显示。
2. `Browse Tags` 使用 `/tags?filter=<A-Z|0-9>&page=<0-based>`。
3. 打开下拉或切换 filter 时加载 page 0。
4. 触底或点击加载更多请求下一页。
5. 输入框取第一个字母/数字作为 filter，例如 `latex` 使用 `L`。
6. 本地对当前已加载 tags 做 ignore-case contains 匹配。
7. 标签列表 key 必须包含 index，不能只用 tag 文本。
8. 点击标签添加 `FilterValue("tags", tag)` 并重新搜索。
9. 点击已选 tag chip 移除 filter 并重新搜索。

## 8. 路由事实

搜索页 route：

1. `search?type={type}&tag={tag}`。
2. `type` 默认 `video`。
3. `tag` 默认空字符串。
4. 详情标签入口可传 `type=image` 或 `type=video`。
5. `SearchTypes.normalize()` 兼容复数 type。

搜索结果跳转：

1. `SearchMediaType.VIDEO` 跳 `video/{id}`。
2. `SearchMediaType.IMAGE` 跳 `image/{id}`。
3. `SearchUserItem` 只有 `hasNavigableProfile` 为 true 才跳 `user/{Uri.encode(username)}`。
4. `SearchPlaylistItem` 跳 `playlist/{Uri.encode(id)}`。
5. `SearchForumThreadItem` 跳 `forum/thread/{Uri.encode(id)}`。
6. `SearchForumPostItem` 如果有 threadId，跳 `forum/thread/{Uri.encode(threadId)}`。
7. `SearchPostItem` 当前只展示，不接原生详情页。

用户路由规则：

1. Iwara profile API 是 `/profile/{username}`。
2. 用户搜索结果进入资料页必须使用 username，不用 UUID。
3. username 必须非空。
4. route segment 必须 `Uri.encode`。
5. forum 用户、评论作者、搜索用户入口都遵循该规则。

## 9. 论坛模块事实

当前论坛是原生只读浏览：

1. 首页论坛板块列表。
2. 板块主题列表。
3. 主题帖子详情。
4. 板块主题动态加载。
5. 帖子详情动态加载。
6. RichText 正文渲染。
7. 用户头像和名称展示。
8. 可点击 forum 用户进入资料页。

公开 API：

1. `GET /forum` 返回 section 数组。
2. `GET /forum/{sectionId}?page=0` 返回 section page。
3. Section page 返回 `{ section, threads, count, limit, page }`。
4. `GET /forum/threads/{threadId}?page=0` 返回 thread page。
5. Thread page 返回 `{ thread, results, count, limit, page, pendingCount }`。
6. `GET /forum/{threadId}` 返回 404，不用于主题详情。

论坛模型：

1. `ForumSection`
2. `ForumThread`
3. `ForumLastPost`
4. `ForumPost`
5. `ForumUser`
6. `ForumSectionPage`
7. `ForumThreadPage`

设计决策：

1. 使用独立 `ForumUser`，不复用严格 `User` 实体。
2. 字符串字段默认空字符串。
3. 时间字段 nullable。
4. 未建模字段交给全局 JSON 忽略。
5. 写入、发帖、回帖、审核不在当前范围。

## 10. 禁止回退清单

生产路径中不得重新出现：

1. 首页顶部栏 lab/lens 按钮。
2. 首页顶部栏会话气泡按钮。
3. 首页顶部栏通知铃铛按钮。
4. 首页顶部栏右侧超过搜索和视图两个按钮。
5. 首页顶部视图菜单中的拦截图片组。
6. 首页 video/image 顶部 `FilterAndSort` 行。
7. 首页 video/image 顶部“日期”按钮。
8. 订阅页顶部 `FilledTonalButton`/`DropdownMenu` 切换行。
9. 搜索框内 sort 下拉。
10. `SearchBarSortDropdown`。
11. 搜索页 tag bottom sheet。
12. `ModalBottomSheet` in SearchPage。
13. `items(tags, key = { it })`。
14. 未编码 username route。
15. blank username route。
16. `PaginationBar`。
17. `MediaListModeButton`。
18. `jumpTo*Page`、`change*Page`、`jump*Page`。
19. 真实 forum 页退回浏览器占位。
20. 真实页面调用 `TodoStatus`。
21. 把 `sort_date` 文案改回“日期”。

允许存在：

1. API 层 page 参数。
2. VM 内 page 状态。
3. LoadMoreEffect。
4. `HorizontalPager` 用于 tab。
5. `TodoStatus` 组件定义本身，只要真实页面不调用它。
6. `FilterAndSort` 组件定义本身，只要首页 video/image 不恢复旧路径。
7. `setting.block_media_thumbnails` 设置项和 `MediaCard` 行为。

## 11. 验证清单

提交前必须做：

1. file errors 覆盖所有修改 Kotlin/XML。
2. grep 搜索顶部栏旧按钮回归。
3. grep 搜索顶部视图菜单拦截图片回归。
4. grep 搜索首页 video/image `FilterAndSort` 回归。
5. grep 搜索订阅页顶部切换行回归。
6. grep 搜索旧 SearchPage 控件回归。
7. grep 搜索 unsafe user route。
8. grep 搜索用户 token/JWT/cookie 是否被新增到代码或文档。
9. 无凭据 API 检查 forum、tags、date filter、7 个 search type。
10. `git diff --check`。
11. `git status --short --branch`。
12. 提交推送。
13. GitHub Actions build-apk.yml。

建议 grep：

1. `BuildConfig|Icons\.Outlined\.(Lens|Message|Notifications)|navigate\("lab"\)|navigate\("conversations"\)` in `ui/page/index/layout`。
2. `rememberBlockMediaThumbnailsPreference|index_view_menu_block_images` in `IndexTopBar.kt`。
3. `FilledTonalButton|DropdownMenu|SubscriptionType\.values|changeSubscriptionType|Row\(` in `IndexSubscriptionPage.kt`。
4. `FilterAndSort\(|sortOptions = MediaSortOptions|R\.string\.date` in `ui/page/index/pager`。
5. `SearchBarSortDropdown|FilterBottomSheet|ModalBottomSheet|items\(tags, key = \{ it \}\)`。
6. `PaginationBar|MediaListModeButton|jumpTo[A-Za-z]*Page|change[A-Za-z]*Page|jump[A-Za-z]*Page|onPageChange =`。
7. `navigate\("user/\$[A-Za-z]|navigate\("user/[^"$]`。
8. `Authorization|Bearer|eyJ|access_token|refresh_token`，只允许既有 auth 存储/拦截器和脱敏日志，不允许用户贴出的凭据。

无凭据 API 检查：

1. `/videos?rating=all&sort=date&date=2026&page=0&limit=1`。
2. `/videos?rating=all&sort=date&date=2026-5&page=0&limit=1`。
3. `/images?rating=all&sort=date&date=2026&page=0&limit=1`。
4. `/images?rating=all&sort=date&date=2026-5&page=0&limit=1`。
5. `/tags?filter=L&page=0`。
6. `/search?type=videos&query=test&page=0&sort=relevance`。
7. `/search?type=images&query=test&page=0&sort=relevance`。
8. `/search?type=posts&query=test&page=0&sort=relevance`。
9. `/search?type=users&query=test&page=0&sort=relevance`。
10. `/search?type=playlists&query=test&page=0&sort=relevance`。
11. `/search?type=forum_posts&query=test&page=0&sort=relevance`。
12. `/search?type=forum_threads&query=test&page=0&sort=relevance`。

## 12. 当前不确定点与风险控制

仍需 Actions 验证的点：

1. 新增资源文件和字符串是否被所有 variant 正确合并。
2. 新增搜索实体在 release minify/serialization 下是否正常。
3. Compose 搜索卡片在小屏上的最终视觉表现。
4. `PlaylistThumbnail` 对不同 playlist payload 的兼容性。

已做的风险控制：

1. file errors 已覆盖新增和修改 Kotlin 文件。
2. 7 个 search type 均用无凭据公开请求验证 HTTP 200。
3. 视频/图片年份和年月 date filter 均用无凭据公开请求验证 HTTP 200。
4. `/tags?filter=L&page=0` 用无凭据公开请求验证 HTTP 200。
5. 标签 key 保持 index-aware。
6. username route 保持非空检查和 `Uri.encode`。
7. 顶部拦截图片入口从 `IndexTopBar.kt` 移除，但设置页入口保留。
8. 最终通过标准仍是 GitHub Actions debug/release。

## 13. 后续改进方向

1. 可为 `SearchPostItem` 增加原生 post 详情页，但必须先验证公开详情接口。
2. 搜索页媒体卡片未来可接入 `setting.block_media_thumbnails`，需要先确认 feature search 依赖边界。
3. 标签快速匹配可增加后端 autocomplete 合并，但当前稳定基线是 `/tags?filter=<first char>`。
4. 论坛主题搜索结果可增加 section 入口跳转。
5. Forum post 搜索结果可定位楼层，但需要先验证 thread page 支持定位参数。
6. Playlist 搜索卡片可展示更多缩略图，但不要牺牲列表密度。

## 14. 接手 Prompt

后续 Agent 可直接使用：

你正在维护 Awara。先阅读 `doc/module-subscription-plan.md`。本地不运行 Gradle build，不本地构建 APK。保留高密度工具型 UI，主要参考 EhViewer，并借鉴 FeedMe 的二级菜单和设置分组。首页顶部栏右侧只能有搜索和视图两个按钮；标题切换器只切视频、图片、论坛；首页 video/image 和订阅页不恢复第二行筛选/切换控件；顶部视图菜单只保留“查看”，拦截图片只在设置页。搜索页支持 7 个分区：视频、图片、帖子、用户、播单、论坛帖子、论坛主题；视频/图片保留 sort/rating/date/tag 控制栏；`date` sort 文案是“最新”；日期筛选支持 `YYYY` 和 `YYYY-M`；标签浏览按首字符请求 `/tags` 并本地匹配。论坛当前是只读原生页。所有 username route 必须 username 非空并 `Uri.encode`。完成后完整重写本文，运行 file errors、grep、公开 API 检查、`git diff --check`，提交推送并触发 `build-apk.yml`，等待 Actions debug/release 成功。