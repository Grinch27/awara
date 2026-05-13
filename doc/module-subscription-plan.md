# Awara 模块执行蓝图与事实基线

## 0. 文档用途

本文是 Awara 当前实现、约束、验证流程和后续 Agent 接手方式的事实基线。后续人工维护者或 GPT Agent 进入仓库时，应先阅读本文，再改代码。

本文目标：

1. 让人工维护者快速知道当前产品风格、模块边界和禁止回退点。
2. 让 GPT Agent 可以按固定流程完成代码修改、文档更新、提交、推送和 GitHub Actions 验证。
3. 让 CI 修复者明确：不在本地构建 APK，不用本地 Gradle build 代替 GitHub Actions。
4. 把已经验证过的 API 事实、UI 决策和风险点写成可复用上下文。

如果代码与本文冲突：先看最新提交和 Actions 结果；确认本文过期后，必须完整更新本文。

## 1. Agent 快速 Prompt

你正在维护 Awara，一个 Kotlin + Jetpack Compose 的 Iwara 客户端。当前产品方向是紧凑、高信息密度、接近 EhViewer 的实用型客户端。不要把 Awara 做成松散的 Material 示例页，也不要用大面积装饰遮蔽内容。

必须遵守：

1. 不在本地构建 APK。
2. 不运行本地 Gradle build 作为通过依据。
3. 构建验证只认 `.github/workflows/build-apk.yml`。
4. 每轮完成代码后必须更新本文、提交、推送、触发 GitHub Actions。
5. Actions 失败时，只修第一处 blocker，然后重新提交、推送、触发工作流。
6. 不泄露 token、cookie、Authorization header 或其他敏感凭据。
7. 不把真实页面退回 `TodoStatus` 或 still in developing。
8. 不恢复旧页码 footer。
9. 不恢复页面内详情/缩略图切换按钮。
10. 不恢复搜索页旧 `FilterAndSort` / `FilterBottomSheet` 入口。

本轮关键事实：

1. 搜索页媒体 sort 下拉已移动到搜索框内，位于搜索按钮左侧。
2. 媒体控件行不再包含 sort，也不包含旧筛选按钮。
3. 媒体控件行现在只包含 rating、date、browse tags。
4. browse tags 不再打开 bottom sheet，改为 A-Z/0-9 下拉菜单。
5. 标签下拉菜单动态加载 `/tags?filter=<A-Z|0-9>&page=<page>`。
6. 用户资料页留言板支持点击“共 N 条回复”进入回复线程。
7. 用户资料页留言回复使用 `/profile/{userId}/comments?page=<page>&parent=<commentId>`。
8. 用户搜索仍使用 `/search?type=users&page=<page>&query=<keyword>&sort=relevance`。
9. 用户搜索结果与通用用户卡片进入资料页时都对 username 做 URI 编码。
10. 视频/图片详情 tag 仍可点击直达搜索。
11. 视频/图片搜索 rating 仍支持 `all / ecchi / general`。
12. 视频/图片搜索 sort 仍支持 `date / trending / popularity / views / likes`。

如果没有 100% 把握：

1. 写出具体不确定点。
2. 用最窄的代码阅读、无凭据 API 检查、IDE/file errors、grep 或 `git diff --check` 消除不确定性。
3. 只修确认存在的根因。
4. 重复这个循环，直到剩余风险只依赖 GitHub Actions 构建事实。

## 2. 不可谈判约束

### 2.1 本地允许动作

允许：

1. 阅读文件。
2. 搜索代码。
3. 查看 diff。
4. IDE/file errors。
5. `git diff --check`。
6. `git status --short --branch`。
7. 不带凭据的公开 API 最小验证。
8. `gh run view` / `gh run list` / `gh api` 查询 Actions 状态。

禁止：

1. 本地 APK 构建。
2. 本地 Gradle build。
3. 将本地 build 结果当作通过依据。
4. 输出或写入任何 Authorization、cookie、token。
5. 猜接口并用假 UI 绕过真实能力。
6. 使用用户贴出的 Authorization header。

### 2.2 提交流程

每轮代码完成后必须：

1. 更新本文。
2. 做允许范围内的本地验证。
3. `git diff --check`。
4. `git status --short --branch`。
5. `git add` 本轮相关文件。
6. `git commit`。
7. `git push`。
8. 触发 `.github/workflows/build-apk.yml`。
9. 等待 Actions 完成。
10. 若失败，只修第一处 blocker。

### 2.3 隐私规则

公开接口检查可以记录：

1. URL path 与无敏感 query。
2. HTTP 状态码。
3. `count`、`limit`、`page`、`results` 数量。
4. forum section 数量与公开 section id。

禁止记录：

1. Cookie。
2. Authorization。
3. 用户 token。
4. 账号密码。
5. 私人响应体全文。

## 3. 产品风格

Awara 当前 UI 应该像一个高密度客户端工具：

1. 紧凑。
2. 清晰。
3. 信息可扫读。
4. 少装饰，多内容。
5. 操作入口靠近对应内容。
6. 错误态、空态、加载态都是产品态，不是开发占位。

搜索页头部结构：

1. 第一层：返回按钮 + 搜索框。
2. 搜索框内部：左侧搜索图标，右侧媒体 sort 下拉 + 搜索/清除按钮。
3. 第二层：视频 / 图片 / 用户类型切换。
4. 第三层：媒体搜索控件；用户搜索显示用户搜索提示。

媒体搜索第三层当前控件：

1. 评级。
2. 日期。
3. 浏览标签。

媒体显示模式仍是全局设置：

1. 详情模式。
2. 缩略图模式。
3. 唯一入口是 `setting.media_list_mode`。
4. 不允许恢复页面内 `MediaListModeButton`。

## 4. 模块地图

核心模块：

1. `:app`：Android 应用壳、DI、数据源、仓库适配、主页面、设置页、详情页、用户页。
2. `:core:model`：跨模块共享模型和常量，例如 feed query、sort/rating keys。
3. `:data`：FeedQuery 到 API 参数的映射。
4. `:feature:search`：搜索页 UI、SearchVM、搜索 repository contract。
5. `:feature:player`：播放器相关功能。

关键文件：

1. `feature/search/src/main/java/me/rerere/awara/ui/page/search/SearchPage.kt`
2. `feature/search/src/main/java/me/rerere/awara/ui/page/search/SearchVM.kt`
3. `feature/search/src/main/java/me/rerere/awara/ui/page/search/SearchRepository.kt`
4. `app/src/main/java/me/rerere/awara/ui/page/search/AppSearchRepository.kt`
5. `app/src/main/java/me/rerere/awara/data/source/IwaraAPI.kt`
6. `app/src/main/java/me/rerere/awara/data/repo/MediaRepo.kt`
7. `app/src/main/java/me/rerere/awara/data/repo/CommentRepo.kt`
8. `app/src/main/java/me/rerere/awara/ui/RouterActivity.kt`
9. `app/src/main/java/me/rerere/awara/ui/component/iwara/TagChip.kt`
10. `app/src/main/java/me/rerere/awara/ui/component/iwara/comment/CommentCard.kt`
11. `app/src/main/java/me/rerere/awara/ui/component/iwara/AuthorCard.kt`
12. `app/src/main/java/me/rerere/awara/ui/component/iwara/UserCard.kt`
13. `app/src/main/java/me/rerere/awara/ui/page/video/pager/VideoOverviewPage.kt`
14. `app/src/main/java/me/rerere/awara/ui/page/image/ImagePage.kt`
15. `app/src/main/java/me/rerere/awara/ui/page/user/UserPage.kt`
16. `app/src/main/java/me/rerere/awara/ui/page/user/UserVM.kt`
17. `app/src/main/java/me/rerere/awara/ui/page/setting/SettingPage.kt`
18. `core/model/src/main/java/me/rerere/awara/domain/feed/FeedQuery.kt`
19. `core/model/src/main/java/me/rerere/awara/ui/component/iwara/param/sort/MediaSort.kt`
20. `core/model/src/main/java/me/rerere/awara/ui/component/iwara/param/rating/MediaRating.kt`
21. `data/src/main/java/me/rerere/awara/data/feed/FeedQueryApiMapper.kt`

共享动态加载辅助：

1. `app/src/main/java/me/rerere/awara/ui/component/iwara/LoadMore.kt`
2. 提供 `LoadMoreEffect(LazyListState, ...)`。
3. 提供 `LoadMoreEffect(LazyStaggeredGridState, ...)`。
4. 提供 `loadMoreFooter(...)`。

## 5. 搜索模块事实

### 5.1 搜索类型

搜索页支持：

1. `video`
2. `image`
3. `user`

规则：

1. 视频和图片使用媒体列表接口。
2. 用户使用 `/search` 用户搜索接口。
3. 切换类型时重置页号为第一页。
4. 如果目标类型有有效搜索条件，应立即重新搜索。
5. 用户类型不显示媒体 sort/rating/date/tag 控件。

有效搜索条件：

1. 视频：关键词非空，或视频 filters 非空，或 rating/sort 偏离默认值。
2. 图片：关键词非空，或图片 filters 非空，或 rating/sort 偏离默认值。
3. 用户：关键词非空。

### 5.2 媒体 sort 入口

当前实现：

1. sort 下拉位于搜索框 trailing 区域。
2. sort 下拉在搜索按钮左侧。
3. sort 下拉只在 `video` 和 `image` 类型显示。
4. sort 下拉修改当前媒体类型的 sort 并立即重新搜索。
5. 旧媒体控件行里的 sort button 已移除。
6. 旧 sort 旁边的 filter button / bottom sheet 已删除。

sort 值：

1. `date`
2. `trending`
3. `popularity`
4. `views`
5. `likes`

### 5.3 媒体 rating/date/tag 控件

媒体控件行当前只包含：

1. Rating 下拉。
2. Date 下拉。
3. Browse Tags 下拉。

rating 值：

1. `all`
2. `ecchi`
3. `general`

date 值：

1. 空值表示 Any Date。
2. 最近 24 个月按 `yyyy-M` 形式提供。

### 5.4 视频与图片搜索参数

视频和图片搜索使用 `FeedQuery`：

1. `FeedScope.SEARCH_VIDEO` 映射到视频列表请求。
2. `FeedScope.SEARCH_IMAGE` 映射到图片列表请求。
3. `keyword` 映射为 `query`。
4. `sort` 映射为 `sort`。
5. `rating` 由 `SearchVM.videoRating` 或 `SearchVM.imageRating` 显式追加。
6. `page` 使用 0-based API 页号。
7. `pageSize` 当前为 24，映射为 `limit`。
8. filters 按 key 分组，多个同 key value 用逗号拼接。
9. 旧 filters 中若存在 `rating` key，会在 VM 组装参数时被过滤，避免 `rating=ecchi,all`。

当前媒体筛选：

1. `date`：来自日期下拉。
2. `tags`：来自浏览标签或详情页 tag 直达搜索。
3. `rating`：来自显式 rating 状态。
4. `sort`：来自显式 sort 状态。

### 5.5 动态加载规则

媒体搜索动态加载：

1. `submitSearch()` 会 trim query、重置 `page = 1`、重置 `hasMore = true`。
2. 第一页请求使用 `replaceResults = true`。
3. 下一页请求先同步设置 `loadingMore = true`，再递增 `page`。
4. 追加成功后合并旧列表和新结果。
5. `hasMore = mergedList.size < pager.count`。
6. 追加失败时回滚页号，不清空旧列表。
7. 空关键词但有日期、标签、rating 或 sort 条件时允许继续加载下一页。
8. 搜索页触底判断使用可见最大 index。
9. 同一 itemCount 只触发一次 load-more，避免追加失败后在底部立即重复请求。
10. 提交搜索、切换类型、修改 date/tag/rating/sort 时重置触底防重复计数。

### 5.6 浏览标签

浏览标签入口现在是下拉菜单，不是 bottom sheet。

可选 filter：

1. A-Z。
2. 0-9。

接口：

1. 路径：`/tags`。
2. 参数：`filter=<A-Z|0-9>`。
3. 参数：`page=<0-based page>`。
4. 返回：`Pager<Tag>`。
5. `Tag` 当前只消费 `id` 和 `type`。
6. API 返回的额外字段由 `JsonInstance.ignoreUnknownKeys = true` 忽略。

UI 行为：

1. 点击“浏览标签”打开 `DropdownMenu`。
2. 顶部是 A-Z/0-9 横向 filter chip。
3. 默认 filter 是 `A`。
4. 打开下拉或切换 filter 时加载第 0 页。
5. 标签列表触底后请求下一页。
6. `loadingMore` 与 `hasMore` 阻止重复追加。
7. 点击标签会添加 `FilterValue("tags", tag)` 并重新搜索。
8. 已选标签以 chip 显示；点击已选 chip 移除该标签并重新搜索。
9. 选择标签后下拉保持打开，允许连续选择。
10. 旧 `ModalBottomSheet` 标签浏览实现已删除，避免点击浏览标签时的崩溃路径。

### 5.7 详情页 tag 直达搜索

详情页 tag 行为：

1. `TagChip` 和 `TagRow` 支持可选点击回调。
2. 视频详情页 tag 点击进入 `search?type=video&tag=<encoded tag>`。
3. 图片详情页 tag 点击进入 `search?type=image&tag=<encoded tag>`。
4. 路由使用 Android `Uri.encode(tag.id)` 编码 tag。
5. 搜索页接收可选 `type` 与 `tag` 参数。
6. 搜索页收到 tag 后会清空目标类型旧 filters，仅添加当前 tag，并立即搜索。
7. tag 直达搜索保留当前 sort 与默认 rating。

### 5.8 用户搜索

用户搜索真实端点：

1. 路径：`/search`。
2. 固定 `type=users`。
3. 固定 `sort=relevance`。
4. 使用 `query=<keyword>`。
5. 使用 0-based `page`。

崩溃修复事实：

1. 用户搜索结果点击进入资料页时使用 `Uri.encode(user.username)`。
2. `CommentCard` 中头像与用户名点击进入资料页时使用 `Uri.encode(username)`。
3. `AuthorCard` 点击进入资料页时使用 `Uri.encode(username)`。
4. `UserCard` 点击进入资料页时使用 `Uri.encode(username)`。
5. 用户搜索结果仍会跳过没有 username 的不可导航用户。

禁止：

1. 不回到旧 `/profiles`。
2. 不把用户搜索伪装成媒体搜索。
3. 不猜用户接口隐藏参数。
4. 不直接拼接未编码 username 到 route。

## 6. 用户资料页留言板

### 6.1 根留言列表

根留言接口：

1. `GET /profile/{userId}/comments?page=<page>`。
2. `userId` 必须来自 `profile.user.id`。
3. 不能直接把 route 参数当作 profile comments id。
4. 根留言列表动态加载。
5. 追加失败不清空已加载留言。

### 6.2 回复线程

回复接口：

1. `GET /profile/{userId}/comments?page=<page>&parent=<commentId>`。
2. 返回同样的 `Pager<Comment>`。
3. `Comment.parent` 可用于展示回复上下文。

UI 行为：

1. 根留言中 `numReplies > 0` 的 `CommentCard` 显示可点击“共 N 条回复”。
2. 点击后调用 `UserVM.pushGuestbookComment(comment.id)`。
3. VM 把 `CommentStateItem(parent = comment.id)` push 到 guestbook stack。
4. 回复线程加载第一页 parent replies。
5. 回复线程顶部显示返回 header。
6. Android Back 在回复线程内先 pop 回根留言，不直接离开用户页。
7. 回复线程中继续点击回复数可以进入下一层 parent。
8. 请求返回时会检查当前 active parent，避免旧请求覆盖当前线程。
9. pop 回复线程会清理回复层错误，避免污染根留言。
10. 留言板仍是只读，不添加留言或回复发送入口。

## 7. 公开 API 验证事实

已用无凭据公开请求验证：

1. `/tags?filter=A&page=0` 返回 HTTP 200，`count=179 limit=32 page=0 results=32`。
2. `/tags?filter=A&page=1` 返回 HTTP 200，`count=179 limit=32 page=1 results=32`。
3. `/search?type=users&page=0&query=latex&sort=relevance` 返回 HTTP 200，`count=8 limit=32 page=0 results=8`。
4. `/profile/<admin-user-id>/comments?page=0&parent=<comment-id>` 返回 HTTP 200，并返回分页结构。
5. `/forum` 返回 HTTP 200，公开 section 数量为 16，首个 section id 为 `announcements`。
6. `/videos?rating=ecchi&sort=likes&limit=1&page=0` 返回分页字段。
7. `/videos?rating=general&sort=likes&limit=1&page=0` 返回分页字段。
8. `/videos?rating=all&sort=date&limit=1&page=0` 返回分页字段。
9. `/images?rating=ecchi&sort=views&limit=1&page=0` 返回分页字段。
10. 视频接口 `date/trending/popularity/views/likes` sort 枚举均返回 HTTP 200。
11. 图片接口 `date/trending/popularity/views/likes` sort 枚举均返回 HTTP 200。
12. 图片接口 `all/ecchi/general` rating 枚举均返回 HTTP 200。

注意：

1. 所有验证都不使用用户贴出的 Authorization header。
2. 媒体接口的 `count` 可能随页变化，不把第一页 count 视为最终总数。
3. `hasMore` 使用“已合并数量 < 当前响应 count”的保守策略。
4. forum 顶层接口已记录为事实，但本轮没有实现 forum UI 改造。

## 8. 设置页事实

搜索相关设置：

1. 全局视频/图片搜索 rating 默认值 key 是 `setting.media_search_rating`。
2. 默认值是 `all`。
3. 可选值是 `all`、`ecchi`、`general`。
4. 设置项位于外观分组。
5. 搜索页初次打开时读取该默认值，并应用到 `videoRating` 与 `imageRating`。
6. 搜索页内 rating 下拉改变的是当前搜索页状态，不直接写回全局设置。

媒体显示设置：

1. key 是 `setting.media_list_mode`。
2. 可选详情模式和缩略图模式。
3. 搜索、首页、用户页等媒体列表读取全局设置。
4. 不恢复页面内显示模式按钮。

## 9. 其他页面现状

### 9.1 首页与订阅

现状：

1. 首页视频动态加载。
2. 首页图片动态加载。
3. 订阅列表动态加载。
4. API 仍使用 page 参数。
5. UI 不暴露旧页码 footer。
6. 刷新第一页替换列表。
7. 触底加载追加列表。

### 9.2 评论模块

现状：

1. 视频评论根列表动态加载。
2. 视频评论 replies 栈动态加载。
3. 用户资料留言 replies 栈动态加载。
4. `CommentStateItem` 维护 `loadingMore` 与 `hasMore`。
5. `CommentCard` 负责展示回复数量、回复上下文和 thread rail。
6. 视频评论保留发送和回复能力。
7. 用户资料留言保持只读。

风险控制：

1. replies 返回时只更新仍然活跃的同一 parent 栈顶。
2. 用户返回上一层后，旧 replies 请求不能覆盖当前线程栈。
3. 追加失败不能清空已加载评论。
4. 用户资料留言 pop 时清理回复层错误。

### 9.3 收藏、关注、好友、播单

现状：

1. 收藏视频动态加载。
2. 收藏图片动态加载。
3. following 动态加载。
4. follower 动态加载。
5. 好友列表动态加载。
6. 好友请求动态加载。
7. 播单列表动态加载。
8. 播单详情动态加载。

特殊规则：

1. 好友接受或拒绝后刷新第一页。
2. 追加失败不清空已有社交列表。
3. 收藏和播单详情不恢复页面内媒体显示模式按钮。

### 9.4 历史与论坛

现状：

1. 历史页使用 Paging 数据流，不属于旧页码 UI。
2. 历史页不提供页面内详情/缩略图切换按钮。
3. `/forum` 顶层公开接口已验证可用。
4. forum UI 改造不是本轮代码改动范围。
5. 若后续实现 forum，需要先建 typed DTO，再做 section/thread/post 分层，不要直接把示例 JSON 硬编码到 UI。

## 10. 禁止回退清单

生产路径中不应重新出现：

1. `PaginationBar`
2. `MediaListModeButton`
3. `jumpTo*Page`
4. `change*Page`
5. `jump*Page`
6. 评论分页式 `onPageChange =`
7. 用户搜索旧 `/profiles`
8. 搜索页旧 `FilterAndSort`
9. 搜索页旧 `FilterBottomSheet`
10. 真实页面 `TodoStatus`
11. still in developing 占位
12. 未编码的 `user/${username}` 导航

允许存在：

1. API 层 page 参数。
2. VM 层当前页号。
3. `HorizontalPager` 用于 tab 切换。
4. 内部分页状态字段。
5. `LoadMoreEffect` 的触底策略。

## 11. 设计决策

### 11.1 为什么 sort 放进搜索框

原因：

1. 用户要求把旧 `日期 / 趋势 / 流行 / 播放量 / 喜欢` 按钮移动到搜索按钮左侧。
2. sort 是搜索行为的一部分，放在搜索框 trailing 区域更贴近提交搜索动作。
3. 媒体控件行腾出空间给 rating/date/tags，移动端更紧凑。
4. 用户搜索不使用媒体 sort，因此用户模式隐藏该入口。

### 11.2 为什么删除旧 filter sheet

原因：

1. 用户明确要求去掉旧 sort 旁边的筛选按钮。
2. rating/date/tag 已经是显式控件，不需要旧 sheet 再包一层。
3. 旧 sheet 里仍有旧 rating filter 模型，容易与显式 rating 状态重复。
4. 删除旧入口降低崩溃面和回退风险。

### 11.3 为什么标签浏览改为下拉

原因：

1. 用户报告“浏览标签”点击崩溃。
2. 用户明确要求“先弄成下拉框 A-Z0-9，然后动态加载”。
3. DropdownMenu 足够承载 A-Z/0-9 filter 与动态结果列表。
4. 下拉比 bottom sheet 更轻，不打断搜索页上下文。

### 11.4 为什么用户路由统一编码

原因：

1. Navigation route 不能安全承载未编码的特殊字符。
2. 用户搜索结果来自公开 API，username 不能假设永远只含安全字符。
3. 同类入口包括搜索结果、评论卡片、作者卡片、用户卡片，应统一修根因。
4. `Uri.encode` 与详情 tag 路由编码策略一致。

### 11.5 为什么留言回复复用 CommentState stack

原因：

1. 视频评论已经有根列表 / replies 栈模型。
2. profile comments parent 接口返回同样的 `Pager<Comment>`。
3. 复用 stack 能自然支持多层进入、返回和动态加载。
4. 用户资料留言保持只读，避免猜写入接口。

## 12. 默认检查顺序

接手任务时：

1. 读用户最新请求。
2. 读本文。
3. 若涉及搜索，读 `SearchVM.kt` 与 `SearchPage.kt`。
4. 若涉及标签，读 `SearchPage.kt` 的 `TagBrowseButton` 与 API `getTags`。
5. 若涉及用户搜索，读 `AppSearchRepository.kt`、`IwaraAPI.searchUser`、`RouterActivity.kt`。
6. 若涉及用户导航，grep `navigate("user/`。
7. 若涉及留言回复，读 `UserPage.kt`、`UserVM.kt`、`CommentRepo.kt`、`CommentCard.kt`。
8. 若涉及设置，读 `SettingPage.kt` 和 string resources。
9. 修改后先跑 file errors。
10. 再跑禁止回退 grep。
11. 再跑 `git diff --check`。
12. 提交、推送、触发 Actions。

## 13. 本地验证清单

允许执行：

1. IDE/file errors。
2. `rg -n "PaginationBar|MediaListModeButton|jumpTo[A-Za-z]*Page|change[A-Za-z]*Page|jump[A-Za-z]*Page|onPageChange =" app feature`
3. `rg -n "FilterAndSort|FilterBottomSheet|ModalBottomSheet" feature/search/src/main/java/me/rerere/awara/ui/page/search/SearchPage.kt`
4. 凭据模式 grep，确认仓库里没有真实 token、cookie 或 Authorization header。
5. `git diff --check`
6. `git status --short --branch`
7. 无凭据公开 API 检查。

本轮 API 检查建议：

1. `/tags?filter=A&page=0`
2. `/tags?filter=A&page=1`
3. `/search?type=users&page=0&query=latex&sort=relevance`
4. `/profile/<userId>/comments?page=0&parent=<commentId>`
5. `/forum`

禁止执行：

1. `./gradlew assemble...`
2. 本地 APK 构建。
3. 本地 Gradle build。
4. 带凭据 curl。
5. 使用用户贴出的 Authorization header。

## 14. 置信度循环

提交前逐项确认：

1. sort 是否只在搜索框搜索按钮左侧显示。
2. 媒体控件行是否只有 rating/date/tags。
3. 旧 `FilterAndSort` 是否删除。
4. 旧 `FilterBottomSheet` 是否删除。
5. Browse Tags 是否是 DropdownMenu 而不是 bottom sheet。
6. A-Z/0-9 切换是否会重新加载第 0 页。
7. 标签列表触底是否会请求下一页。
8. 用户搜索是否仍走 `/search?type=users&sort=relevance`。
9. 用户搜索结果导航是否 URI encode。
10. 通用用户卡片和评论用户点击是否 URI encode。
11. 用户资料留言 root 是否仍从 `profile.user.id` 请求。
12. 用户资料留言回复是否带 `parent` 参数。
13. 回复线程返回是否不会污染根列表错误态。
14. 无凭据 API 检查是否通过。
15. IDE/file errors 是否干净。
16. 禁止回退 grep 是否干净。
17. `git diff --check` 是否通过。
18. 本文是否完整重写。
19. 是否提交、推送、触发 Actions。
20. Actions debug/release 是否通过。

只有这些问题都有事实证据支撑，才可以最终答复。

## 15. 后续 Agent 可直接使用的任务模板

```text
你正在维护 Awara。先阅读 doc/module-subscription-plan.md，并把它当作当前事实基线。

目标：在不本地构建 APK、不运行本地 Gradle build 的前提下，完成用户指定的最小功能或修复，更新文档，提交推送，并用 .github/workflows/build-apk.yml 验证。

硬约束：
1. 不运行本地 Gradle build。
2. 不本地构建 APK。
3. 不泄露 token、cookie、Authorization header。
4. 不使用用户贴出的 Authorization header。
5. 不恢复 PaginationBar、MediaListModeButton、jump/change page、旧用户搜索 /profiles。
6. 不恢复搜索页旧 FilterAndSort / FilterBottomSheet。
7. 不把真实页面退回 TodoStatus 或 still in developing。
8. 不猜未证实接口。

搜索当前事实：
1. 视频/图片搜索使用 FeedQuery 和 /videos、/images。
2. 用户搜索使用 /search?type=users&sort=relevance。
3. sort 值是 date/trending/popularity/views/likes。
4. sort 下拉位于搜索框搜索按钮左侧。
5. rating 值是 all/ecchi/general，默认 all。
6. 设置 key setting.media_search_rating 控制视频/图片搜索默认 rating。
7. 浏览标签使用下拉菜单和 /tags?filter=<A-Z|0-9>&page=<page>。
8. 详情 tag 直达 search?type=<video|image>&tag=<tag>。
9. 用户资料留言回复使用 /profile/{userId}/comments?parent=<commentId>。
10. 用户 route 必须 Uri.encode(username)。
11. 空关键词但有媒体条件时也必须支持动态加载。
12. 追加加载必须同步置位 loadingMore，追加失败不能清空已有列表。

默认验证：
1. file errors。
2. 禁止回退 grep。
3. 敏感凭据 grep。
4. git diff --check。
5. git status。
6. 无凭据公开 API 检查。
7. commit + push + workflow_dispatch build-apk.yml。

失败处理：
Actions 失败时，只修第一处 blocker；不要顺手重构；修完后重新提交、推送、触发 workflow。
```
