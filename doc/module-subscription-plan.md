# Awara 动态加载与模块执行蓝图

## 0. 使用方式

这份文档是当前 Awara 仓库的执行基线，不是愿景草案。新的 agent 或人工维护者接手时，应先把这里记录的事实当作默认上下文，再根据用户新需求做最小改动。

目标读者：

1. 人工维护者：确认当前交互基线、模块现状和剩余边界。
2. GPT/Agent：直接按任务提示进入定位、最小修复、验证、提交、Actions 闭环。
3. CI 修复者：只看首个 blocker，禁止本地构建 APK。

## 1. 当前最高优先级

Awara 当前阶段的核心要求：

1. 所有生产页面弃用页码、左右翻页按钮和 `PaginationBar`。
2. 列表类内容统一使用动态加载：第一页替换，滚动接近底部时追加下一批。
3. `详情/缩略图` 媒体列表显示模式只允许在设置页修改。
4. 页面内不再出现 `MediaListModeButton` 快捷切换。
5. 真实页面路径不能回到 `TodoStatus` 或 still in developing。
6. 所有 APK 构建验证只能走 `.github/workflows/build-apk.yml`。

## 2. 不可谈判的工程约束

### 2.1 构建与提交

1. 不在本地构建 APK。
2. 不运行本地 Gradle 构建来替代 CI。
3. 允许本地静态检查、IDE 文件错误检查、grep、`git diff --check`。
4. 每轮代码完成后必须提交、推送、触发 `.github/workflows/build-apk.yml`。
5. Actions 失败时，只修第一处 blocker，然后重新提交、推送、触发工作流。

### 2.2 隐私

1. 本地接口检查只允许在终端中做。
2. 不保存 token、cookie、Authorization 头、账号密码。
3. 不把敏感凭据写入文档、提交说明、日志摘要或 issue 描述。
4. 若必须描述接口结果，只写状态码、字段是否存在、链路阶段和脱敏结论。

### 2.3 改动边界

1. 优先修根因，不做表面绕过。
2. 保持改动贴近当前模块，不顺手重构无关页面。
3. 接口没有证据时，不猜写路径或回复路径。
4. UI 行为改动必须能被 grep、文件错误检查和 Actions 证明。

## 3. 产品风格基线

当前视觉方向是“紧凑、清晰、可读、接近 EhViewer 的信息密度”。不要把操作界面做成松散的营销页或 Material 示例页。

设计判断：

1. 列表滚动应自然连续，用户不需要理解页码。
2. 刷新代表重新加载第一页，触底代表追加下一批。
3. 页面内高频筛选可以保留，显示模式这类全局偏好放入设置页。
4. 评论、留言和社交列表优先保证正文可读和状态明确。
5. 空态、错误态和回退态要像真实产品状态，而不是开发占位。

## 4. 动态加载新基线

### 4.1 统一状态模型

新增或改造后的动态列表应遵循这一模式：

1. `page`：内部 API 页号，从 1 开始记录当前已加载页。
2. `count` / `total`：服务端总数，仅用于计算是否还有更多。
3. `loading`：第一页加载或刷新状态。
4. `loadingMore`：追加下一页状态。
5. `hasMore`：当前已加载数量是否小于总数。
6. `list` / `comments`：已加载的合并结果。

行为规则：

1. `replaceResults = true`：请求第一页，成功后替换列表。
2. `replaceResults = false`：请求 `page + 1`，成功后追加到现有列表。
3. 追加前必须检查 `loadingMore` 和 `hasMore`，避免重复请求。
4. `loadingMore` 必须同步置位，不能等 coroutine 启动后才置位。
5. 追加失败时保留已有列表，只停止尾部 loading。
6. 第一页失败时才进入全屏错误态。

### 4.2 统一 UI 触发

新增共享辅助文件：

1. `app/src/main/java/me/rerere/awara/ui/component/iwara/LoadMore.kt`

它提供：

1. `LoadMoreEffect(LazyListState, ...)`
2. `LoadMoreEffect(LazyStaggeredGridState, ...)`
3. `LazyListScope.loadMoreFooter(...)`
4. `LazyStaggeredGridScope.loadMoreFooter(...)`

使用规则：

1. 普通列表用 `rememberLazyListState()`。
2. 瀑布流/网格用 `rememberLazyStaggeredGridState()`。
3. 当最后可见项进入末尾阈值时调用 `loadNext*`。
4. 尾部 loading 使用现有 `Spin`，不要重新引入页码 footer。

## 5. 当前模块实现现状

### 5.1 首页、订阅与搜索

现状：

1. 首页视频、首页图片、订阅动态已经使用动态加载。
2. 搜索页视频、图片、用户结果已经使用动态加载。
3. 本轮移除了首页与订阅页面内的 `详情/缩略图` 快捷按钮。
4. 搜索页保留三层 EhViewer 风格头部，不再定义页面内 `MediaListModeButton`。
5. 搜索页仍读取全局 `setting.media_list_mode` 来决定媒体卡片布局。

主要文件：

1. `app/src/main/java/me/rerere/awara/ui/page/index/IndexVM.kt`
2. `app/src/main/java/me/rerere/awara/ui/page/index/pager/IndexVideoPage.kt`
3. `app/src/main/java/me/rerere/awara/ui/page/index/pager/IndexImagePage.kt`
4. `app/src/main/java/me/rerere/awara/ui/page/index/pager/IndexSubscriptionPage.kt`
5. `feature/search/src/main/java/me/rerere/awara/ui/page/search/SearchPage.kt`
6. `feature/search/src/main/java/me/rerere/awara/ui/page/search/SearchVM.kt`

验收要求：

1. 生产路径中不能出现 `PaginationBar`。
2. 生产路径中不能出现 `MediaListModeButton`。
3. 搜索 user 模式必须继续可达。
4. 搜索头部不能退回松散多行布局。

### 5.2 用户资料页

现状：

1. 用户页视频投稿动态加载。
2. 用户页图片投稿动态加载。
3. 用户页留言动态加载。
4. 留言仍保持真实读取 + 只读展示。
5. 留言请求必须使用 `profile.user.id`，不能直接使用路由参数。
6. 用户页不再提供页面内详情/缩略图切换按钮，只读取设置页偏好。

主要文件：

1. `app/src/main/java/me/rerere/awara/ui/page/user/UserVM.kt`
2. `app/src/main/java/me/rerere/awara/ui/page/user/UserPage.kt`
3. `app/src/main/java/me/rerere/awara/data/repo/CommentRepo.kt`
4. `app/src/main/java/me/rerere/awara/data/source/IwaraAPI.kt`

关键决策：

1. 资料页留言 GET 链路已接入。
2. profile guestbook 写入和回复接口未被证实，不接生产 UI。
3. 留言回复数可以静态显示，但不展开 replies 线程。
4. 浏览器打开个人页只作为错误态回退。

### 5.3 评论模块

现状：

1. 视频评论根列表动态加载。
2. 视频评论 replies 栈动态加载。
3. `CommentStateItem` 增加 `loadingMore` 与 `hasMore`。
4. `CommentList` 不再显示页码 footer。
5. 评论回复、发送、push/pop 线程栈行为保留。
6. 资料页留言通过 `CommentCard` 只读模式展示，不开放未证实的回复行为。

主要文件：

1. `app/src/main/java/me/rerere/awara/ui/component/iwara/comment/CommentState.kt`
2. `app/src/main/java/me/rerere/awara/ui/component/iwara/comment/CommentList.kt`
3. `app/src/main/java/me/rerere/awara/ui/component/iwara/comment/CommentCard.kt`
4. `app/src/main/java/me/rerere/awara/ui/page/video/VideoVM.kt`
5. `app/src/main/java/me/rerere/awara/ui/page/video/pager/VideoCommentPage.kt`
6. `app/src/main/java/me/rerere/awara/ui/page/video/layout/VideoPagePhoneLayout.kt`

重要风险控制：

1. replies 请求回来时，只能更新仍然活跃的同一 parent 栈顶。
2. 用户在 replies 加载中返回时，旧请求不能把已 pop 的栈顶写回去。
3. 追加失败不能清空已加载评论。
4. 发送评论后重新加载当前线程第一页，避免本地顺序猜测。

### 5.4 收藏、关注、好友、播单

现状：

1. 收藏视频动态加载。
2. 收藏图片动态加载。
3. following 动态加载。
4. follower 动态加载。
5. 好友列表动态加载。
6. 好友请求动态加载。
7. 播单列表动态加载。
8. 播单详情视频动态加载。
9. 这些页面的 `PaginationBar` 全部移除。
10. 收藏和播单详情页面不再提供页面内详情/缩略图按钮。

主要文件：

1. `app/src/main/java/me/rerere/awara/ui/page/favorites/FavoritesVM.kt`
2. `app/src/main/java/me/rerere/awara/ui/page/favorites/FavoritesPage.kt`
3. `app/src/main/java/me/rerere/awara/ui/page/follow/FollowVM.kt`
4. `app/src/main/java/me/rerere/awara/ui/page/follow/FollowPage.kt`
5. `app/src/main/java/me/rerere/awara/ui/page/friends/FriendsVM.kt`
6. `app/src/main/java/me/rerere/awara/ui/page/friends/FriendsPage.kt`
7. `app/src/main/java/me/rerere/awara/ui/page/playlist/PlaylistsVM.kt`
8. `app/src/main/java/me/rerere/awara/ui/page/playlist/PlaylistsPage.kt`
9. `app/src/main/java/me/rerere/awara/ui/page/playlist/PlaylistDetailVM.kt`
10. `app/src/main/java/me/rerere/awara/ui/page/playlist/PlaylistDetailPage.kt`

好友请求特殊规则：

1. 接受请求后刷新好友与请求第一页。
2. 拒绝/移除后刷新好友与请求第一页。
3. 不在操作后继续追加旧列表，避免脏数据残留。

### 5.5 历史记录

现状：

1. 历史记录本身使用 Paging 数据流，不属于旧页码 UI。
2. 本轮移除了历史页 top bar 内的详情/缩略图快捷按钮。
3. 历史页继续读取设置页的全局媒体列表显示模式。

主要文件：

1. `app/src/main/java/me/rerere/awara/ui/page/history/HistoryPage.kt`
2. `app/src/main/java/me/rerere/awara/ui/page/history/HistoryVM.kt`

### 5.6 设置页

现状：

1. 设置页外观分组保留媒体列表显示模式选择。
2. `setting.media_list_mode` 是详情/缩略图唯一设置入口。
3. 页面内快捷切换函数已经删除。
4. 页面仍通过 `rememberMediaListModePreference()` 读取该全局设置。

主要文件：

1. `app/src/main/java/me/rerere/awara/ui/page/setting/SettingPage.kt`
2. `app/src/main/java/me/rerere/awara/ui/component/iwara/MediaListMode.kt`

决策：

1. 当前不拆分视频、图片、搜索、历史的独立显示模式 key。
2. 保持一个全局媒体列表模式，降低用户认知成本。
3. 后续若要分页面记忆，需要另开偏好迁移，不应混入本轮。

## 6. 已删除或弃用的旧结构

本轮后不应再使用：

1. `app/src/main/java/me/rerere/awara/ui/component/iwara/PaginationBar.kt`
2. `MediaListModeButton`
3. `jumpTo*Page` UI 跳页函数
4. `change*Page` UI 跳页函数
5. `onPageChange` 评论分页回调

允许保留的内部概念：

1. API 请求仍可使用 `page` query 参数。
2. VM 仍可记录当前已加载页号，用于计算下一批。
3. `HorizontalPager` 可继续用于 tab 页面切换，例如视频/图片、关注/粉丝。这不是列表页码翻页。

## 7. Agent 直接执行 Prompt

你正在维护 Awara，一个 Kotlin + Jetpack Compose 的 Iwara 客户端。当前代码基线已经弃用生产页面中的页码 footer 和页面内详情/缩略图切换按钮。你的任务是保持动态加载、设置页偏好和 CI 验证闭环稳定。

硬约束：

1. 不在本地构建 APK。
2. 只用 `.github/workflows/build-apk.yml` 验证构建。
3. 每次只修第一个 blocker。
4. 不泄露 token、cookie、Authorization 头。
5. 不让真实页面回到 `TodoStatus` 或 still in developing。
6. 不猜未证实的 profile guestbook 写接口或回复接口。

接手检查顺序：

1. 读用户最新请求，不要被旧任务牵着走。
2. 运行 grep 确认是否出现旧结构：`PaginationBar`、`MediaListModeButton`、`jumpTo*Page`、`change*Page`、`onPageChange`。
3. 若用户报动态加载问题，先看对应 VM 的 `loadingMore`、`hasMore`、`replaceResults`。
4. 若用户报设置页显示模式问题，先看 `MediaListMode.kt` 和 `SettingPage.kt`。
5. 若用户报评论问题，先看 `CommentState.kt`、`CommentList.kt`、`VideoVM.kt`。
6. 修改后先做文件错误检查，再做 `git diff --check`，最后提交推送跑 Actions。

禁止动作：

1. 不要恢复 `PaginationBar`。
2. 不要恢复页面内 `MediaListModeButton`。
3. 不要把 tab `HorizontalPager` 误删成无导航页面。
4. 不要在追加加载失败时清空已有列表。
5. 不要在 replies 请求返回后无条件覆盖当前评论栈。

## 8. 验证清单

本地允许验证：

1. IDE / file errors 检查所有改动 Kotlin 文件。
2. `rg -n "PaginationBar|MediaListModeButton|jumpTo[A-Za-z]*Page|change[A-Za-z]*Page|jump[A-Za-z]*Page|onPageChange =" app feature`
3. `git diff --check`
4. `git status --short --branch`

禁止本地验证：

1. 本地 APK 构建。
2. 本地 Gradle build 代替 Actions。
3. 带敏感凭据的持久化接口日志。

Actions 流程：

1. `git add` 相关文件。
2. `git commit -m "一句准确提交说明"`。
3. `git push origin ci/privacy-gradle9-actions`。
4. `gh workflow run build-apk.yml --ref ci/privacy-gradle9-actions`。
5. 用当前 head SHA 定位 run。
6. `gh run watch <run_id> --interval 10 --exit-status`。
7. 失败时先看 `gh run view <run_id> --log-failed`。
8. 若失败日志为空，看 failed job log。
9. 只修第一处 blocker，重复提交和 Actions。

## 9. 当前剩余边界

仍然存在的边界：

1. profile guestbook 当前只读，不支持写留言。
2. profile guestbook 不展开 replies 线程。
3. forum 仍是浏览器回退页，不是原生论坛流。
4. 动态加载会在内存中保留已加载项目；极大列表未来可考虑 Paging 3 或有限缓存。
5. 手机视频详情中的嵌入评论依赖外层详情流滚动触发更多评论；全屏评论页是更完整的动态评论体验。

明确不属于本轮：

1. 不引入 Android Paging 3 重写所有网络列表。
2. 不拆分多个媒体显示模式偏好 key。
3. 不改 API 协议，只把 UI 从页码翻页改为动态追加。
4. 不原生化 forum。

## 10. 事实置信标准

只有同时满足以下条件，才能声称本轮达到事实上的高置信：

1. grep 中没有 `PaginationBar`。
2. grep 中没有 `MediaListModeButton`。
3. grep 中没有生产路径 `jumpTo*Page`、`change*Page`、`onPageChange`。
4. 改动文件错误检查无错误。
5. `git diff --check` 无输出。
6. 改动已提交并推送。
7. `.github/workflows/build-apk.yml` 对当前 head SHA 成功。

## 11. 下一轮改进方向

优先级从高到低：

1. 如果 CI 失败，继续只修第一处 blocker。
2. 如果用户反馈某个列表触底不加载，先看该 VM 的 `hasMore` 和 UI 的 `LoadMoreEffect` itemCount。
3. 如果用户反馈重复加载，先看 `loadingMore` 是否同步置位。
4. 如果用户希望更像 EhViewer，优先微调列表密度、评论面板和搜索头部，不改数据层。
5. 如果用户要求 profile 留言写入，先验证真实写接口，再设计输入框和错误态。
