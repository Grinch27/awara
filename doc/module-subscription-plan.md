# Awara 模块执行蓝图与事实基线

## 0. 文档定位

这份文档是 Awara 当前代码与流程的事实基线。后续人工维护者或 GPT/Agent 接手时，应先读本文，再进入代码修改。

本文目标：

1. 帮人工维护者快速理解当前产品风格、模块边界和禁止回退点。
2. 帮 GPT/Agent 直接进入最小修复、验证、提交和 CI 闭环。
3. 帮 CI 修复者明确：不本地构建 APK，构建事实只以 GitHub Actions 为准。

本文不是愿景清单。若代码与本文冲突，先看最新提交和 Actions；确认本文过期后，必须同步重写本文。

## 1. Agent 快速执行 Prompt

你正在维护 Awara，一个 Kotlin + Jetpack Compose 的 Iwara 客户端。当前产品方向是紧凑、高信息密度、接近 EhViewer 的实用型客户端，不是松散的 Material 示例页。

必须遵守：

1. 不在本地构建 APK。
2. 不用本地 Gradle build 代替 CI。
3. 构建验证只认 `.github/workflows/build-apk.yml`。
4. 每轮完成代码后必须提交、推送、触发 GitHub Actions。
5. Actions 失败时，只修第一处 blocker，再重新提交、推送、触发工作流。
6. 不泄露 token、cookie、Authorization header 或其他敏感凭据。
7. 不把真实页面退回 `TodoStatus` 或 still in developing。
8. 不恢复旧页码 footer，不恢复页面内详情/缩略图切换按钮。

当前最重要事实：

1. 搜索页视频、图片、用户结果均采用动态加载。
2. 视频/图片搜索支持 sort、rating、date、tags。
3. sort 下拉值是 `date`、`trending`、`popularity`、`views`、`likes`。
4. rating 下拉值是 `all`、`ecchi`、`general`，默认 `all`。
5. 设置页提供全局视频/图片搜索 rating 默认值，key 为 `setting.media_search_rating`。
6. 详情页 tag chip 可以点击，视频详情进入 `search?type=video&tag=<tag>`，图片详情进入 `search?type=image&tag=<tag>`。
7. 浏览标签走 `/tags?filter=<A-Z|0-9>&page=<page>`，返回 `Pager<Tag>`。
8. 用户搜索必须走 `/search?type=users&page=<page>&query=<keyword>&sort=relevance`，不能回到旧 `/profiles`。

如果你没有 100% 把握：

1. 写出具体不确定点。
2. 用最窄的代码阅读、无凭据接口检查、IDE/file errors、grep 或 `git diff --check` 消除不确定性。
3. 只修确认存在的根因。
4. 重复这个循环，直到剩余风险只依赖 GitHub Actions 构建事实。

## 2. 不可谈判的工程约束

### 2.1 本地允许动作

允许：

1. 阅读文件、搜索代码、查看 diff。
2. IDE/file errors 检查。
3. `git diff --check`。
4. `git status --short --branch`。
5. 不带凭据的公开 API 最小验证。

禁止：

1. 本地 APK 构建。
2. 本地 Gradle build 作为通过依据。
3. 输出或持久化任何敏感 header、cookie、token。
4. 猜接口或写假 UI 绕过真实能力。

### 2.2 提交流程

每轮代码完成后：

1. 更新本文。
2. 做允许范围内的本地验证。
3. `git add` 相关文件。
4. `git commit`。
5. `git push`。
6. 触发 `.github/workflows/build-apk.yml`。
7. 等待 Actions 结果。
8. 若失败，只修第一处 blocker。

### 2.3 隐私规则

公开接口检查可以记录：

1. URL 路径和无敏感 query。
2. HTTP 状态码。
3. 响应结构字段是否存在。
4. `count`、`limit`、`page`、`results` 数量级事实。

禁止记录：

1. Cookie。
2. Authorization。
3. 用户 token。
4. 账号密码。
5. 私人响应体全文。

## 3. 产品与视觉基线

Awara 当前 UI 应该像一个高密度客户端工具：

1. 紧凑。
2. 明确。
3. 信息可扫读。
4. 少装饰，多内容。
5. 错误态、空态、回退态表现为产品态，而不是开发占位。

搜索页头部保持三层：

1. 第一层：返回按钮 + 搜索框。
2. 第二层：视频 / 图片 / 用户类型切换。
3. 第三层：媒体搜索控件；用户搜索显示用户搜索提示。

媒体搜索第三层当前控件：

1. 排列方式。
2. 评级。
3. 日期。
4. 浏览标签。

媒体显示模式仍是全局设置：

1. 详情模式。
2. 缩略图模式。
3. 唯一入口是 `setting.media_list_mode`。
4. 不允许恢复页面内 `MediaListModeButton`。

## 4. 当前模块地图

核心模块：

1. `:app`：Android 应用壳、DI、数据源、仓库适配、主页面、设置页、详情页。
2. `:core:model`：跨模块共享模型和轻量常量，例如 feed query、sort/rating keys。
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
7. `app/src/main/java/me/rerere/awara/ui/RouterActivity.kt`
8. `app/src/main/java/me/rerere/awara/ui/component/iwara/TagChip.kt`
9. `app/src/main/java/me/rerere/awara/ui/page/video/pager/VideoOverviewPage.kt`
10. `app/src/main/java/me/rerere/awara/ui/page/image/ImagePage.kt`
11. `app/src/main/java/me/rerere/awara/ui/page/setting/SettingPage.kt`
12. `core/model/src/main/java/me/rerere/awara/domain/feed/FeedQuery.kt`
13. `core/model/src/main/java/me/rerere/awara/ui/component/iwara/param/sort/MediaSort.kt`
14. `core/model/src/main/java/me/rerere/awara/ui/component/iwara/param/rating/MediaRating.kt`
15. `data/src/main/java/me/rerere/awara/data/feed/FeedQueryApiMapper.kt`

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

有效搜索条件：

1. 视频：关键词非空，或视频 filters 非空，或 rating/sort 偏离默认值。
2. 图片：关键词非空，或图片 filters 非空，或 rating/sort 偏离默认值。
3. 用户：关键词非空。

### 5.2 视频与图片搜索参数

视频和图片搜索使用 `FeedQuery`：

1. `FeedScope.SEARCH_VIDEO` 映射到视频列表请求。
2. `FeedScope.SEARCH_IMAGE` 映射到图片列表请求。
3. `keyword` 映射为 `query`。
4. `sort` 映射为 `sort`。
5. `rating` 由 `SearchVM.videoRating` 或 `SearchVM.imageRating` 显式追加。
6. `page` 使用 0-based API 页号。
7. `pageSize` 当前为 24，映射为 `limit`。
8. filters 按 key 分组，多个同 key value 用逗号拼接。
9. 旧 filters 中若存在 `rating` key，会在 VM 组装参数时被过滤，避免出现 `rating=ecchi,all` 这类重复参数。

sort 值：

1. `date`
2. `trending`
3. `popularity`
4. `views`
5. `likes`

rating 值：

1. `all`
2. `ecchi`
3. `general`

当前媒体筛选：

1. `date`：来自日期下拉。
2. `tags`：来自浏览标签或详情页 tag 直达搜索。
3. `rating`：来自显式 rating 状态，不再依赖旧 FilterValue。
4. `sort`：来自显式 sort 状态。

### 5.3 动态加载规则

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

### 5.4 浏览标签

浏览标签是媒体搜索头部第三层入口。

可选 filter：

1. A-Z。
2. 0-9。

接口：

1. 路径：`/tags`。
2. 参数：`filter=<A-Z|0-9>`。
3. 参数：`page=<0-based page>`。
4. 返回：`Pager<Tag>`。
5. `Tag` 当前只消费 `id` 和 `type`。
6. API 返回的 `sensitive` 字段由 `JsonInstance.ignoreUnknownKeys = true` 忽略。

UI 行为：

1. 点击“浏览标签”打开 bottom sheet。
2. 顶部显示 A-Z 和 0-9 filter chip。
3. 默认 filter 为 `A`。
4. 切换 filter 后重新加载第 0 页。
5. 标签列表触底后请求下一页。
6. `loadingMore` 与 `hasMore` 阻止重复追加。
7. 点击标签会添加 `FilterValue("tags", tag)` 并重新搜索。
8. 已选标签以 chip 显示；点击已选 chip 会移除该标签并重新搜索。
9. 选择标签后不关闭 sheet，允许连续选择多个标签。

### 5.5 详情页 tag 直达搜索

详情页 tag 行为：

1. `TagChip` 和 `TagRow` 支持可选点击回调。
2. 视频详情页的 tag 点击进入 `search?type=video&tag=<encoded tag>`。
3. 图片详情页现在也显示 tag row，tag 点击进入 `search?type=image&tag=<encoded tag>`。
4. 路由使用 Android `Uri.encode(tag.id)` 编码 tag。
5. 搜索页接收可选 `type` 与 `tag` 参数。
6. 搜索页收到 tag 后会清空目标类型旧 filters，仅添加当前 tag，并立即搜索。
7. tag 直达搜索保留当前 sort 与默认 rating。

### 5.6 全局搜索 rating 设置

设置页新增全局视频/图片搜索 rating 默认值。

事实：

1. key：`setting.media_search_rating`。
2. 默认：`all`。
3. 可选：`all`、`ecchi`、`general`。
4. 设置项位于外观分组。
5. 搜索页初次打开时读取该默认值，并应用到 `videoRating` 与 `imageRating`。
6. 搜索页内 rating 下拉改变的是当前搜索页状态，不直接写回全局设置。

### 5.7 用户搜索

用户搜索真实端点：

1. 路径：`/search`。
2. 固定 `type=users`。
3. 固定 `sort=relevance`。
4. 使用 `query=<keyword>`。
5. 使用 0-based `page`。

禁止：

1. 不回到旧 `/profiles`。
2. 不把用户搜索伪装成媒体搜索。
3. 不猜用户接口隐藏参数。

## 6. 公开接口验证事实

已用无凭据公开请求验证：

1. `/tags?filter=C&page=0` 返回 HTTP 200 和分页结构。
2. `/tags?filter=C&page=1` 返回 `count/limit/page/results`。
3. `/videos?limit=24&page=0&sort=date&tags=catgirl` 返回分页字段。
4. `/videos?limit=24&page=1&sort=date&tags=catgirl` 返回分页字段。
5. `/images?limit=24&page=0&sort=date&tags=catgirl` 返回分页字段。
6. `/images?limit=24&page=1&sort=date&tags=catgirl` 返回分页字段。
7. `/videos?rating=ecchi&sort=likes&limit=1&page=0` 返回分页字段。
8. `/videos?rating=general&sort=likes&limit=1&page=0` 返回分页字段。
9. `/videos?rating=all&sort=date&limit=1&page=0` 返回分页字段。
10. `/images?rating=ecchi&sort=views&limit=1&page=0` 返回分页字段。
11. 视频接口 `date/trending/popularity/views/likes` sort 枚举在无凭据请求下均返回 HTTP 200。
12. 图片接口 `date/trending/popularity/views/likes` sort 枚举在无凭据请求下均返回 HTTP 200。
13. 图片接口 `all/ecchi/general` rating 枚举在无凭据请求下均返回 HTTP 200。

媒体接口的 `count` 可能随页变化，因此不要把第一页 count 视为绝对最终总数。当前实现使用“已合并数量 < 当前响应 count”的保守策略判断是否还有下一页。

## 7. 其他页面与模块事实

### 7.1 首页与订阅

现状：

1. 首页视频动态加载。
2. 首页图片动态加载。
3. 订阅列表动态加载。
4. API 仍使用 page 参数。
5. UI 不暴露旧页码 footer。
6. 刷新第一页替换列表。
7. 触底加载追加列表。

### 7.2 用户资料页与留言

现状：

1. 用户页视频动态加载。
2. 用户页图片动态加载。
3. 用户页留言动态加载。
4. 留言只读展示。
5. 留言请求必须基于 `profile.user.id`。
6. 浏览器打开个人页只作为错误态回退。

禁止：

1. 不猜 profile guestbook 写接口。
2. 不猜 profile guestbook 回复接口。
3. 不展开未证实的 replies 线程。

### 7.3 评论模块

现状：

1. 视频评论根列表动态加载。
2. 视频评论 replies 栈动态加载。
3. `CommentStateItem` 维护 `loadingMore` 与 `hasMore`。
4. 评论发送、回复、线程 push/pop 行为保留。
5. 资料页留言复用 `CommentCard` 的只读模式。

风险控制：

1. replies 返回时只更新仍然活跃的同一 parent 栈顶。
2. 用户返回上一层后，旧 replies 请求不能覆盖当前线程栈。
3. 追加失败不能清空已加载评论。

### 7.4 收藏、关注、好友、播单

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

### 7.5 历史与设置

现状：

1. 历史页使用 Paging 数据流，不属于旧页码 UI。
2. 历史页不提供页面内详情/缩略图切换按钮。
3. 设置页保留媒体列表显示模式选择。
4. 设置页保留首页默认入口选择。
5. 设置页新增视频/图片搜索 rating 默认值选择。

## 8. 禁止回退清单

生产路径中不应重新出现：

1. `PaginationBar`
2. `MediaListModeButton`
3. `jumpTo*Page`
4. `change*Page`
5. `jump*Page`
6. 评论分页式 `onPageChange =`
7. 用户搜索旧 `/profiles`
8. 真实页面 `TodoStatus`
9. still in developing 占位

允许存在：

1. API 层 page 参数。
2. VM 层当前页号。
3. `HorizontalPager` 用于 tab 切换。
4. 内部旧筛选组件，只要生产路径不恢复旧交互。

## 9. 设计决策

### 9.1 为什么 sort/rating 放在搜索头部

原因：

1. sort、rating、date、tags 都是媒体搜索条件。
2. 用户搜索不使用这些媒体条件。
3. 控件靠近搜索框，符合高频筛选使用习惯。
4. 避免把筛选藏在深层 bottom sheet。

### 9.2 为什么 rating 是显式状态

原因：

1. rating 是核心搜索参数，应稳定传入 API。
2. 设置页需要提供全局默认值。
3. 旧 `FilterValue("rating", ...)` 容易和新默认值重复拼接。
4. VM 统一过滤旧 rating filter，只保留显式 rating。

### 9.3 为什么 tag 直达搜索走路由参数

原因：

1. 详情页不应直接依赖 SearchVM 实例。
2. 路由参数可表达用户意图：搜索某类型下的某个 tag。
3. 视频和图片 tag 可以分别进入对应 search type。
4. 参数经过 `Uri.encode`，避免 tag 中特殊字符破坏路由。

### 9.4 为什么不引入 Paging 3 重写搜索

原因：

1. 本轮目标是修复已暴露搜索行为。
2. 当前 VM 动态加载已覆盖需求。
3. Paging 3 会牵动多个页面状态模型。
4. CI blocker 排查会变复杂。

## 10. 剩余边界与改进方向

仍然存在：

1. profile guestbook 只读。
2. profile guestbook 不展开 replies。
3. forum 仍为浏览器回退页。
4. 动态加载会在内存中保留已加载项目。
5. 搜索页内部旧 `FilterBottomSheet` 组件仍在文件内，但当前生产头部直接展示 sort/rating/date/tag。

未来可评估：

1. 清理或拆分搜索页旧内部筛选组件。
2. 将搜索页 load-more 防重复策略推广到共享 `LoadMoreEffect`。
3. 为标签浏览错误态增加显式重试按钮。
4. 若列表规模继续扩大，再评估 Paging 3 或有限缓存。
5. 若后端公开更多搜索条件，再把筛选模型从 raw key/value 提升为 typed filter。

## 11. 默认检查顺序

接手任务时：

1. 读用户最新请求。
2. 读本文。
3. 若涉及搜索，读 `SearchVM.kt` 与 `SearchPage.kt`。
4. 若涉及详情 tag，读 `TagChip.kt`、`VideoOverviewPage.kt`、`ImagePage.kt`、`RouterActivity.kt`。
5. 若涉及设置，读 `SettingPage.kt` 和 string resources。
6. 若涉及 API 参数，读 `FeedQuery.kt` 与 `FeedQueryApiMapper.kt`。
7. 修改后先跑 file errors。
8. 再跑禁止回退 grep。
9. 再跑 `git diff --check`。
10. 提交、推送、触发 Actions。

## 12. 本地验证清单

允许执行：

1. IDE/file errors。
2. `rg -n "PaginationBar|MediaListModeButton|jumpTo[A-Za-z]*Page|change[A-Za-z]*Page|jump[A-Za-z]*Page|onPageChange =" app feature`
3. `git diff --check`
4. `git status --short --branch`
5. 无凭据公开 API 检查。

本轮接口检查建议：

1. `/videos?rating=ecchi&sort=likes&limit=1&page=0`
2. `/videos?rating=general&sort=likes&limit=1&page=0`
3. `/videos?rating=all&sort=date&limit=1&page=0`
4. `/images?rating=ecchi&sort=views&limit=1&page=0`
5. `/tags?filter=C&page=0`

禁止执行：

1. `./gradlew assemble...`
2. 本地 APK 构建。
3. 用本地 build 替代 Actions。
4. 带凭据 curl。

## 13. 置信度循环

提交前逐项确认：

1. tag 点击是否能表达目标类型和 tag。
2. tag 是否经过 URI 编码。
3. 搜索页是否读取 route 参数并自动搜索。
4. sort 是否只使用 `date/trending/popularity/views/likes`。
5. rating 是否只使用 `all/ecchi/general`。
6. 媒体请求是否始终带一个 rating 参数。
7. 旧 rating filter 是否被过滤，避免重复拼接。
8. 设置页全局 rating 是否只作为默认值，不覆盖页面内临时选择。
9. 用户搜索是否仍走 `/search?type=users&sort=relevance`。
10. 追加失败是否不清空旧列表。
11. 禁止回退 grep 是否干净。
12. 本文是否已经按当前事实重写。
13. 是否已提交、推送、触发 Actions。
14. Actions debug/release 是否通过。

只有这些问题都有事实证据支撑，才可以最终答复。

## 14. 后续 Agent 可直接使用的任务模板

```text
你正在维护 Awara。先阅读 doc/module-subscription-plan.md，并把它当作当前事实基线。

目标：在不本地构建 APK 的前提下，完成用户指定的最小功能或修复，更新文档，提交推送，并用 .github/workflows/build-apk.yml 验证。

硬约束：
1. 不运行本地 Gradle build。
2. 不本地构建 APK。
3. 不泄露 token、cookie、Authorization header。
4. 不恢复 PaginationBar、MediaListModeButton、jump/change page、旧用户搜索 /profiles。
5. 不把真实页面退回 TodoStatus 或 still in developing。
6. 不猜未证实接口。

搜索当前事实：
1. 视频/图片搜索使用 FeedQuery 和 /videos、/images。
2. 用户搜索使用 /search?type=users&sort=relevance。
3. sort 值是 date/trending/popularity/views/likes。
4. rating 值是 all/ecchi/general，默认 all。
5. 设置 key setting.media_search_rating 控制视频/图片搜索默认 rating。
6. 浏览标签使用 /tags?filter=<A-Z|0-9>&page=<page>。
7. 详情 tag 直达 search?type=<video|image>&tag=<tag>。
8. 空关键词但有媒体条件时也必须支持动态加载。
9. 追加加载必须同步置位 loadingMore，追加失败不能清空已有列表。

默认验证：
1. file errors。
2. 禁止回退 grep。
3. git diff --check。
4. git status。
5. 无凭据公开 API 检查。
6. commit + push + workflow_dispatch build-apk.yml。

失败处理：
Actions 失败时，只修第一处 blocker；不要顺手重构；修完后重新提交、推送、触发 workflow。
```
