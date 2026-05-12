# Awara 模块执行蓝图与事实基线

## 0. 文档定位

这份文档是 Awara 当前实现的事实基线，也是后续 GPT/Agent 接手时的首读文件。

它服务三类读者：

1. 人工维护者：快速判断产品风格、模块边界、已落地能力和禁止回退点。
2. GPT/Agent：不用重新全仓库摸索，就能直接进入最小修改、验证、提交、CI 闭环。
3. CI 修复者：明确本地不能构建 APK，只能通过 GitHub Actions 的 `build-apk.yml` 取得最终构建事实。

本文件记录的是“当前应该相信的实现事实”，不是愿景清单。若代码与本文冲突，先检查最新提交和 Actions 结果；若确认本文过期，必须同步重写本文。

## 1. Agent 快速执行 Prompt

你正在维护 Awara，一个 Kotlin + Jetpack Compose 的 Iwara 客户端。当前产品方向是紧凑、高信息密度、接近 EhViewer 的实用型客户端，而不是松散的示例页或营销页。

你必须遵守：

1. 不在本地构建 APK。
2. 不用本地 Gradle build 代替 CI。
3. 构建验证只认 `.github/workflows/build-apk.yml`。
4. 每次代码完成后必须提交、推送、触发 GitHub Actions。
5. Actions 失败时，只修第一处 blocker，重新提交、推送、触发工作流。
6. 不泄露 token、cookie、Authorization header 或其他敏感凭据。
7. 真实页面不允许退回 `TodoStatus` 或 still in developing。
8. 不恢复旧页码 footer，不恢复页面内详情/缩略图切换按钮。

本轮关键事实：

1. 搜索页视频、图片、用户结果均应动态加载。
2. 视频/图片搜索支持日期筛选和浏览标签筛选。
3. 浏览标签走 `/tags?filter=<A-Z|0-9>&page=<page>`，返回 `Pager<Tag>`。
4. 选择浏览标签后写入媒体搜索的 `tags` filter，再走 `/videos` 或 `/images` 的现有列表接口。
5. 空关键词但有日期或标签筛选时，搜索结果也必须能继续加载下一页。
6. 用户搜索必须走 `/search?type=users&page=<page>&query=<keyword>&sort=relevance`，不能回到旧 `/profiles`。

如果你没有 100% 把握：

1. 先列出你不确定的具体点。
2. 用最窄的代码阅读、无凭据接口检查、IDE/file errors、grep 或 `git diff --check` 消除不确定性。
3. 只修确认存在的根因。
4. 重复这个循环，直到剩余风险只依赖 GitHub Actions 构建事实。

## 2. 不可谈判的工程约束

### 2.1 构建与提交

允许的本地动作：

1. 阅读文件、搜索代码、查看 diff。
2. IDE/file errors 检查。
3. `git diff --check`。
4. `git status --short --branch`。
5. 不带凭据的公开 API 最小验证。

禁止的本地动作：

1. 本地 APK 构建。
2. 本地 Gradle build 作为通过依据。
3. 输出或持久化任何敏感 header、cookie、token。
4. 通过猜接口或写假 UI 绕过真实能力。

提交闭环：

1. 完成最小代码改动。
2. 更新本文件，使文档描述与代码一致。
3. 做允许范围内的本地验证。
4. `git add` 相关文件。
5. `git commit`。
6. `git push`。
7. 触发 `.github/workflows/build-apk.yml`。
8. 等待 Actions 结果。
9. 若失败，只修第一处 blocker。

### 2.2 隐私与接口检查

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

Awara 当前风格不是 Material 示例页，而是接近 EhViewer 的客户端工具界面：

1. 紧凑。
2. 高信息密度。
3. 明确的层级。
4. 少装饰，多可扫读内容。
5. 真实页面优先呈现产品态，而不是开发占位。

搜索页当前头部仍保持三层结构：

1. 第一层：返回按钮 + 搜索框。
2. 第二层：视频 / 图片 / 用户类型切换。
3. 第三层：媒体搜索筛选入口；用户搜索显示用户搜索提示。

媒体显示模式是全局设置：

1. 详情模式。
2. 缩略图模式。
3. 唯一入口是设置页中的 `setting.media_list_mode`。
4. 不允许恢复页面内 `MediaListModeButton`。

列表交互原则：

1. 第一页刷新是替换结果。
2. 触底加载是追加结果。
3. 追加失败不清空已加载内容。
4. 追加请求前必须检查 `loadingMore` 与 `hasMore`。
5. `loadingMore` 必须尽早置位，避免同一底部位置重复触发。

## 4. 当前模块地图

核心模块：

1. `:app`：Android 应用壳、DI、数据源、仓库适配、主页面。
2. `:core:model`：跨模块共享模型，例如 feed 查询模型。
3. `:data`：FeedQuery 到 API 参数的映射。
4. `:feature:search`：搜索页 UI、SearchVM、搜索 repository contract。
5. `:feature:player`：播放器相关功能。

搜索相关关键文件：

1. `feature/search/src/main/java/me/rerere/awara/ui/page/search/SearchPage.kt`
2. `feature/search/src/main/java/me/rerere/awara/ui/page/search/SearchVM.kt`
3. `feature/search/src/main/java/me/rerere/awara/ui/page/search/SearchRepository.kt`
4. `app/src/main/java/me/rerere/awara/ui/page/search/AppSearchRepository.kt`
5. `app/src/main/java/me/rerere/awara/data/source/IwaraAPI.kt`
6. `app/src/main/java/me/rerere/awara/data/repo/MediaRepo.kt`
7. `core/model/src/main/java/me/rerere/awara/domain/feed/FeedQuery.kt`
8. `data/src/main/java/me/rerere/awara/data/feed/FeedQueryApiMapper.kt`

共享动态加载辅助：

1. `app/src/main/java/me/rerere/awara/ui/component/iwara/LoadMore.kt`
2. 提供 `LoadMoreEffect(LazyListState, ...)`。
3. 提供 `LoadMoreEffect(LazyStaggeredGridState, ...)`。
4. 提供 `loadMoreFooter(...)`。

## 5. 搜索模块事实

### 5.1 搜索类型

搜索页支持三种类型：

1. `video`
2. `image`
3. `user`

类型切换规则：

1. 视频和图片使用媒体列表接口。
2. 用户使用 `/search` 用户搜索接口。
3. 切换类型时重置页号为第一页。
4. 如果目标类型有有效搜索条件，应立即重新搜索。

有效搜索条件：

1. 视频：关键词非空，或视频 filters 非空。
2. 图片：关键词非空，或图片 filters 非空。
3. 用户：关键词非空。

### 5.2 视频与图片搜索

视频和图片搜索使用 `FeedQuery`：

1. `FeedScope.SEARCH_VIDEO` 映射到视频列表请求。
2. `FeedScope.SEARCH_IMAGE` 映射到图片列表请求。
3. `keyword` 映射为 `query`。
4. `sort` 映射为 `sort`。
5. `page` 使用 0-based API 页号。
6. `pageSize` 当前为 24，映射为 `limit`。
7. filters 按 key 分组，多个同 key value 用逗号拼接。

当前媒体筛选：

1. `date`：来自日期下拉。
2. `tags`：来自浏览标签或旧内部标签搜索组件。
3. `rating`：旧内部 bottom sheet 组件仍存在，但当前生产头部没有直接暴露。

媒体搜索动态加载规则：

1. `submitSearch()` 会 trim query、重置 `page = 1`、重置 `hasMore = true`。
2. 第一页请求使用 `replaceResults = true`。
3. 下一页请求先同步设置 `loadingMore = true`，再递增 `page`。
4. 追加成功后合并旧列表和新结果。
5. `hasMore = mergedList.size < pager.count`。
6. 追加失败时回滚页号，不清空旧列表。
7. 空关键词但有日期或标签筛选时允许继续加载下一页。
8. 搜索页触底判断使用可见最大 index，而不是 staggered grid 的最后一个元素顺序。
9. 同一 itemCount 只触发一次 load-more，避免追加失败后在底部立即重复请求。

### 5.3 浏览标签

浏览标签是媒体搜索头部第三层的新入口，与日期下拉并排显示。

可选 filter：

1. A-Z。
2. 0-9。

接口事实：

1. 路径：`/tags`。
2. 参数：`filter=<A-Z|0-9>`。
3. 参数：`page=<0-based page>`。
4. 返回：`Pager<Tag>`。
5. `Tag` 当前只消费 `id` 和 `type`。
6. API 返回的 `sensitive` 字段由 `JsonInstance.ignoreUnknownKeys = true` 忽略。

浏览标签 UI 行为：

1. 点击“Browse Tags”打开 bottom sheet。
2. 顶部显示 A-Z 和 0-9 filter chip。
3. 默认 filter 为 `A`。
4. 切换 filter 后重新加载第 0 页。
5. 标签列表触底后请求下一页。
6. `loadingMore` 与 `hasMore` 会阻止重复追加。
7. 同一 tags itemCount 只触发一次追加，避免失败后循环请求。
8. 点击标签会添加 `FilterValue("tags", tag)`。
9. 已选标签以 chip 显示；点击已选 chip 会移除该标签并重新搜索。
10. 选择标签后不关闭 sheet，允许连续选择多个标签。

已验证公开接口事实：

1. `/tags?filter=C&page=0` 返回 HTTP 200。
2. `/tags?filter=C&page=1` 返回 `count=173 limit=32 page=1`。
3. `/videos?limit=24&page=0&sort=date&tags=catgirl` 返回分页字段。
4. `/videos?limit=24&page=1&sort=date&tags=catgirl` 返回分页字段。
5. `/images?limit=24&page=0&sort=date&tags=catgirl` 返回分页字段。
6. `/images?limit=24&page=1&sort=date&tags=catgirl` 返回分页字段。
7. `/images?limit=24&page=2&sort=date&tags=catgirl` 返回分页字段。

媒体接口的 `count` 可能随页变化，因此不能把第一页 count 视为绝对最终总数。当前实现使用“已合并数量 < 当前响应 count”的保守策略判断是否还有下一页。

### 5.4 用户搜索

用户搜索真实端点：

1. 路径：`/search`。
2. 固定 `type=users`。
3. 固定 `sort=relevance`。
4. 使用 `query=<keyword>`。
5. 使用 0-based `page`。

禁止回退：

1. 不允许回到旧 `/profiles`。
2. 不允许把用户搜索伪装成媒体搜索。
3. 不允许猜用户接口隐藏参数。

## 6. 其他页面与模块事实

### 6.1 首页与订阅

首页视频、图片和订阅列表使用动态加载。

关键点：

1. API 仍使用 page 参数。
2. UI 不暴露旧页码 footer。
3. 刷新第一页替换列表。
4. 触底加载追加列表。

### 6.2 用户资料页与留言

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

### 6.3 评论模块

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

### 6.4 收藏、关注、好友、播单

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

### 6.5 历史与设置

现状：

1. 历史页使用 Paging 数据流，不属于旧页码 UI。
2. 历史页不提供页面内详情/缩略图切换按钮。
3. 设置页保留媒体列表显示模式选择。
4. `setting.media_list_mode` 是详情/缩略图模式的唯一入口。

## 7. 禁止回退清单

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
4. 内部未暴露的筛选组件，只要不恢复旧交互。

## 8. 设计决策与原因

### 8.1 为什么浏览标签放在搜索头部

浏览标签是搜索条件，不是全局偏好。

放在日期旁边的理由：

1. 日期和 tags 都是媒体搜索 filter。
2. 用户切到 user 搜索时不应看到媒体 filter。
3. 头部第三层保持紧凑，不增加新的页面级 section。
4. 与 EhViewer 风格一致：高频筛选靠近搜索框。

### 8.2 为什么选择 bottom sheet

原因：

1. A-Z/0-9 filter 选项较多，dropdown 不适合承载分页列表。
2. bottom sheet 可容纳已选 tags、filter chips 和动态列表。
3. 不需要新页面路由。
4. 可以保留当前搜索上下文。

### 8.3 为什么选择多标签追加

原因：

1. 既有 `FeedQueryApiMapper` 已支持同 key 多 value 逗号拼接。
2. 旧 `SearchTagFilter` 也支持多个 `tags` filter。
3. 连续选择标签不关闭 sheet，更适合浏览式筛选。

### 8.4 为什么不重写为 Paging 3

当前目标是修复已暴露搜索行为，而不是重构所有网络列表。

不使用 Paging 3 的原因：

1. 改动范围过大。
2. 会牵动多个页面状态模型。
3. 当前 VM 层动态加载已覆盖主要需求。
4. CI blocker 排查会变复杂。

## 9. 剩余边界

仍然存在的边界：

1. profile guestbook 只读。
2. profile guestbook 不展开 replies。
3. forum 仍为浏览器回退页。
4. 动态加载会在内存中保留已加载项目。
5. 搜索页旧内部 `FilterBottomSheet` 组件仍在文件内，但当前头部生产路径只直接展示日期和浏览标签。
6. 构建事实必须以最新 GitHub Actions 结果为准，本文件不记录单次 run 号，避免每次 CI 造成文档 churn。

未来可评估：

1. 将搜索页旧内部筛选组件拆出或删减。
2. 将 `LoadMoreEffect` 的可见最大 index 与同 itemCount 防重复策略推广到共享 helper。
3. 为标签浏览补充更细的空态、错误态和重试入口。
4. 若列表规模继续扩大，再评估 Paging 3 或有限缓存。

## 10. 默认检查顺序

新 agent 接手时按这个顺序排查：

1. 最新用户请求指向哪个模块。
2. 是否涉及搜索页。
3. 若是搜索页，先读 `SearchVM.kt` 和 `SearchPage.kt`。
4. 若涉及 API，读 `IwaraAPI.kt`、`MediaRepo.kt`、`AppSearchRepository.kt`。
5. 若涉及媒体查询参数，读 `FeedQuery.kt` 和 `FeedQueryApiMapper.kt`。
6. 若涉及动态加载，检查 `page`、`count`、`loadingMore`、`hasMore`、`replaceResults`。
7. 若涉及 UI 回退，grep 禁止回退清单。
8. 修改后先跑 file errors。
9. 再跑 grep 禁止回退清单。
10. 再跑 `git diff --check`。
11. 提交、推送、触发 Actions。

## 11. 本地验证清单

允许执行：

1. IDE/file errors。
2. `rg -n "PaginationBar|MediaListModeButton|jumpTo[A-Za-z]*Page|change[A-Za-z]*Page|jump[A-Za-z]*Page|onPageChange =" app feature`
3. `git diff --check`
4. `git status --short --branch`
5. 无凭据公开 API 检查。

本轮应覆盖的无凭据接口检查：

1. `/tags?filter=C&page=0`
2. `/tags?filter=C&page=1`
3. `/videos?limit=24&page=0&sort=date&tags=catgirl`
4. `/videos?limit=24&page=1&sort=date&tags=catgirl`
5. `/images?limit=24&page=0&sort=date&tags=catgirl`
6. `/images?limit=24&page=1&sort=date&tags=catgirl`

禁止执行：

1. `./gradlew assemble...`
2. 本地 APK 构建。
3. 用本地 build 替代 Actions。
4. 带凭据 curl。

## 12. 置信度循环

提交前问自己：

1. API 路径是否真实验证过？
2. 返回结构是否能被当前 serializer 消费？
3. 新 UI 是否只出现在视频/图片搜索，不污染用户搜索？
4. 空关键词 + tags/date 是否能加载下一页？
5. `loadingMore` 是否足够早置位？
6. 追加失败是否不清空旧列表？
7. 触底 effect 是否会在同一 itemCount 上重复触发？
8. 是否恢复了禁止回退清单中的旧结构？
9. 是否新增了未验证接口猜测？
10. 是否已更新本文？
11. 是否已提交、推送、触发 Actions？

只有当这些问题都有事实证据支撑时，才可以进入最终答复。

## 13. 面向后续 Agent 的完整任务模板

```text
你正在维护 Awara。请先阅读 doc/module-subscription-plan.md，并把它当作当前事实基线。

目标：在不本地构建 APK 的前提下，完成用户指定的最小功能或修复，更新文档，提交推送，并用 .github/workflows/build-apk.yml 验证。

硬约束：
1. 不运行本地 Gradle build。
2. 不本地构建 APK。
3. 不泄露 token、cookie、Authorization header。
4. 不恢复 PaginationBar、MediaListModeButton、jump/change page、旧用户搜索 /profiles。
5. 不把真实页面退回 TodoStatus 或 still in developing。
6. 不猜未证实接口。

搜索模块当前事实：
1. 视频/图片搜索使用 FeedQuery 和 /videos、/images。
2. 用户搜索使用 /search?type=users&sort=relevance。
3. 浏览标签使用 /tags?filter=<A-Z|0-9>&page=<page>。
4. 标签选择写入 FilterValue("tags", tag)。
5. 空关键词但有媒体 filters 时也必须支持动态加载。
6. 追加加载必须同步置位 loadingMore，并且追加失败不能清空已有列表。

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
