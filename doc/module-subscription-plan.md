# Awara 执行蓝图与当前事实

## 0. 文档定位

这份文档是当前 Awara 仓库的事实基线，服务对象包括：

1. 人工维护者：快速判断哪些行为已经落地，哪些仍然是受控边界。
2. GPT/Agent：在不重新铺陈全仓库的前提下，直接进入最小修复与验证闭环。
3. CI 修复者：严格按照“不本地构建 APK，只依赖 GitHub Actions”推进。

如果新的 agent 接手，请先把这里的内容当作默认事实，而不是愿景清单。

## 1. 当前产品基线

Awara 当前实现优先级不是扩张功能面，而是把已暴露给用户的页面稳定收口到以下状态：

1. 列表默认采用动态加载，避免用户理解页码与左右翻页。
2. 详情/缩略图媒体显示模式是全局设置，而不是页面内的临时按钮。
3. 搜索页维持接近 EhViewer 的紧凑三层头部与高信息密度。
4. 评论、留言、社交列表优先保证可读性和状态清晰。
5. 真实页面路径不允许回退到 `TodoStatus` 或 still in developing。
6. APK 构建验证只能通过 `.github/workflows/build-apk.yml`。

## 2. 不可谈判的约束

### 2.1 构建与提交流程

1. 不在本地构建 APK。
2. 不用本地 Gradle build 代替 CI。
3. 允许的本地验证只有：文件错误检查、grep、`git diff --check`、终端内的脱敏 API 检查。
4. 每轮代码完成后都必须提交、推送、触发 `build-apk.yml`。
5. Actions 失败时，只修第一处 blocker，然后重新提交、推送、触发工作流。

### 2.2 隐私与接口验证

1. 终端允许做本地 API 检查，但不能泄露或持久化 token、cookie、Authorization 头。
2. 文档、提交说明、错误摘要和回复里都不能回显敏感凭据。
3. 若要证明接口可用，只记录：状态码、响应结构、关键字段存在与否、是否需要认证。

### 2.3 改动边界

1. 优先修根因，不做表层绕过。
2. 改动必须尽量贴近真正控制行为的文件。
3. 未经证实的接口不要猜写路径、回复路径或隐藏参数。
4. 第一次实质编辑后，优先做最窄验证，不扩大改动面。

## 3. 风格与交互方向

当前视觉方向：紧凑、明确、可读、接近 EhViewer 的信息密度，而不是松散的 Material 示例页。

交互判断：

1. 列表滚动应连续，刷新表示重载第一页，触底表示加载更多。
2. 高频筛选可以留在页面内，全局偏好应回收到设置页。
3. 评论、留言和社交列表优先清晰度，不优先淡色装饰。
4. 错误态、空态、回退态应表现为产品态，而不是开发占位。

## 4. 当前已验证的实现事实

### 4.1 动态加载已成为生产基线

当前生产路径中的旧页码结构已经被移除：

1. `PaginationBar` 已删除，不再被生产代码引用。
2. 页面内 `MediaListModeButton` 已删除，不再被生产代码引用。
3. `jumpTo*Page`、`change*Page`、旧评论 `onPageChange` 回调已退出生产路径。
4. `HorizontalPager` 仍允许存在，但仅用于 tab 页面切换，不代表列表分页。

动态加载统一模式：

1. 第一页使用 `replaceResults = true`，覆盖现有列表。
2. 触底加载使用 `replaceResults = false`，追加下一页。
3. VM 层统一维护 `page`、`count/total`、`loading`、`loadingMore`、`hasMore`、`list/comments`。
4. 追加请求前必须检查 `loadingMore` 和 `hasMore`。
5. `loadingMore` 必须同步置位，避免滚动监听重复触发。
6. 追加失败只停止尾部 loading，不清空已加载内容。

共享辅助：

1. `app/src/main/java/me/rerere/awara/ui/component/iwara/LoadMore.kt`
2. 提供 `LoadMoreEffect(LazyListState, ...)`
3. 提供 `LoadMoreEffect(LazyStaggeredGridState, ...)`
4. 提供 `loadMoreFooter(...)` 用于列表尾部 loading 提示。

### 4.2 用户搜索已修正到真实接口

这是当前最重要的新事实：

1. 用户搜索不再走旧的 `/profiles`。
2. 真实可用接口是 `/search?type=users&page=0&query=<keyword>&sort=relevance`。
3. 当前实现已将 `IwaraAPI.searchUser` 修正为：
   - 路径：`/search`
   - 固定参数：`type=users`
   - 固定排序：`sort=relevance`
4. 终端无凭据请求已验证该端点能返回正确结构：`page`、`count`、`limit`、`results`、`type`。
5. `results` 中的单项仍然能够映射为 `User`，未知字段如 `creatorProgram` 会被忽略。

相关文件：

1. `app/src/main/java/me/rerere/awara/data/source/IwaraAPI.kt`
2. `app/src/main/java/me/rerere/awara/data/repo/MediaRepo.kt`
3. `app/src/main/java/me/rerere/awara/ui/page/search/AppSearchRepository.kt`
4. `feature/search/src/main/java/me/rerere/awara/ui/page/search/SearchVM.kt`
5. `feature/search/src/main/java/me/rerere/awara/ui/page/search/SearchPage.kt`

当前判断：

1. 用户搜索的根因是旧接口路径失效，而不是 UI 切换逻辑失效。
2. 这次修复没有扩改搜索 UI 结构，因为证据显示问题点在 API 定义层。

### 4.3 搜索模块

现状：

1. 搜索页视频、图片、用户结果均采用动态加载。
2. 搜索头部保持三层结构：返回与搜索框、类型切换、日期或 user 提示。
3. 搜索页继续读取全局 `setting.media_list_mode` 决定媒体卡片布局。
4. 用户搜索当前通过真实 `/search` 端点返回结果。

验收要求：

1. `user` 搜索模式必须可切换、可请求、可展示结果。
2. 搜索头部不能退化成松散多行。
3. 页面内不再提供局部详情/缩略图切换按钮。

### 4.4 用户资料页与留言

现状：

1. 用户页视频动态加载。
2. 用户页图片动态加载。
3. 用户页留言动态加载。
4. 留言仍是真实读取 + 只读展示。
5. 留言请求必须基于 `profile.user.id`，不能直接使用当前路由参数。
6. 浏览器打开个人页现在只作为错误态回退。

关键决策：

1. 资料页留言只证实了 GET `/profile/{id}/comments`。
2. 写留言与回复接口仍未证实，不接生产 UI。
3. 留言回复数可以静态显示，但不展开 replies 线程。

相关文件：

1. `app/src/main/java/me/rerere/awara/ui/page/user/UserVM.kt`
2. `app/src/main/java/me/rerere/awara/ui/page/user/UserPage.kt`
3. `app/src/main/java/me/rerere/awara/data/repo/CommentRepo.kt`
4. `app/src/main/java/me/rerere/awara/data/source/IwaraAPI.kt`

### 4.5 评论模块

现状：

1. 视频评论根列表动态加载。
2. 视频评论 replies 栈动态加载。
3. `CommentStateItem` 维护 `loadingMore` 与 `hasMore`。
4. `CommentList` 不再展示页码 footer。
5. 评论发送、回复、push/pop 线程栈行为保留。
6. 资料页留言继续复用 `CommentCard` 的只读模式。

当前风险控制：

1. replies 请求完成时，只更新仍然活跃的同一 parent 栈顶。
2. 用户在 replies 加载时返回，旧请求不能覆盖当前线程栈。
3. 追加失败不能清空已加载评论。

### 4.6 收藏、关注、好友、播单

现状：

1. 收藏视频、收藏图片、following、follower、好友列表、好友请求、播单列表、播单详情都已采用动态加载。
2. 这些页面的旧页码 footer 已移除。
3. 收藏和播单详情不再提供页面内详情/缩略图切换按钮。

特殊规则：

1. 好友接受/拒绝操作后刷新第一页，避免脏数据残留。
2. 追加失败不清空已有社交列表。

### 4.7 历史与设置

现状：

1. 历史页本身使用 Paging 数据流，不属于旧页码 UI。
2. 历史页已移除页面内详情/缩略图按钮。
3. 设置页外观分组保留媒体列表显示模式选择。
4. `setting.media_list_mode` 是详情/缩略图唯一入口。
5. 页面仍通过 `rememberMediaListModePreference()` 读取该全局设置。

## 5. 当前明确不应恢复的旧结构

生产路径中不应重新出现：

1. `PaginationBar`
2. `MediaListModeButton`
3. `jumpTo*Page`
4. `change*Page`
5. 评论分页式 `onPageChange`

允许保留的内部概念：

1. API 请求仍可使用 `page` 参数向后端取下一批数据。
2. VM 仍可记录当前已加载页号。
3. `HorizontalPager` 仍可用于 tab 切换。

## 6. 当前剩余边界

仍然存在的边界：

1. profile guestbook 当前只读，不支持写留言。
2. profile guestbook 不展开 replies 线程。
3. forum 仍为浏览器回退页，不是原生论坛流。
4. 动态加载会在内存中保留已加载项目；超大列表未来可再评估 Paging 3 或有限缓存。

本轮明确不做：

1. 不引入 Android Paging 3 重写全部网络列表。
2. 不拆分多个媒体显示模式偏好 key。
3. 不猜测 profile 留言写接口或回复接口。
4. 不原生化 forum。

## 7. Agent 直接执行 Prompt

你正在维护 Awara，一个 Kotlin + Jetpack Compose 的 Iwara 客户端。当前代码基线已经弃用生产页面中的页码 footer 和页面内详情/缩略图切换按钮；用户搜索必须走 `/search?type=users&sort=relevance`，而不是旧的 `/profiles`。你的任务是保持动态加载、搜索正确性、设置页偏好和 CI 闭环稳定。

硬约束：

1. 不在本地构建 APK。
2. 只用 `.github/workflows/build-apk.yml` 验证构建。
3. 每次只修第一个 blocker。
4. 不泄露 token、cookie、Authorization 头。
5. 不让真实页面回到 `TodoStatus` 或 still in developing。
6. 不猜未证实的 profile guestbook 写接口或回复接口。

接手时的默认检查顺序：

1. 先看用户最新请求指向哪个具体模块。
2. 若是用户搜索问题，先看 `IwaraAPI.searchUser`、`MediaRepo.searchUser`、`AppSearchRepository.searchUsers`。
3. 若是动态加载问题，先看对应 VM 的 `loadingMore`、`hasMore`、`replaceResults`。
4. 若是显示模式问题，先看 `MediaListMode.kt` 和 `SettingPage.kt`。
5. 若是评论问题，先看 `CommentState.kt`、`CommentList.kt`、`VideoVM.kt`。
6. 修改后先做文件错误检查，再做 grep 与 `git diff --check`，最后提交推送并跑 Actions。

禁止动作：

1. 不要恢复 `PaginationBar`。
2. 不要恢复页面内 `MediaListModeButton`。
3. 不要把 tab `HorizontalPager` 误删为分页回归。
4. 不要在追加加载失败时清空已有列表。
5. 不要在 replies 请求返回后无条件覆盖当前评论栈。
6. 不要把用户搜索重新指回旧的 `/profiles`。

## 8. 验证清单

本地允许验证：

1. 改动文件的 IDE / file errors 检查。
2. `rg -n "PaginationBar|MediaListModeButton|jumpTo[A-Za-z]*Page|change[A-Za-z]*Page|jump[A-Za-z]*Page|onPageChange =" app feature`
3. `git diff --check`
4. `git status --short --branch`
5. 在不使用凭据的前提下，用终端最小化请求验证公开接口是否可达，例如用户搜索端点。

禁止本地验证：

1. 本地 APK 构建。
2. 本地 Gradle build 代替 Actions。
3. 输出带凭据的终端日志。

Actions 固定流程：

1. `git add` 相关文件。
2. `git commit -m "一句准确提交说明"`。
3. `git push origin ci/privacy-gradle9-actions`。
4. `gh workflow run build-apk.yml --ref ci/privacy-gradle9-actions`。
5. 用当前 head SHA 定位 run。
6. `gh run watch <run_id> --interval 10 --exit-status`。
7. 若失败，先看 `gh run view <run_id> --log-failed`。
8. 若仍为空，查看 failed job log。
9. 只修第一处 blocker，然后重复提交与 Actions。

## 9. 事实置信标准

只有同时满足以下条件，才能声称本轮达到事实上的高置信：

1. 相关根因已被具体证据锁定，而不是猜测。
2. 代码改动贴近真正控制行为的本地文件。
3. 改动文件错误检查无错误。
4. `git diff --check` 无输出。
5. 生产代码残留 grep 结果符合当前基线。
6. 改动已提交并推送。
7. `.github/workflows/build-apk.yml` 对当前 head SHA 成功。

## 10. 下一轮优先级

优先级从高到低：

1. 若 CI 失败，继续只修第一处 blocker。
2. 若用户反馈用户搜索仍异常，先检查返回结构是否有新的字段兼容问题，而不是先改 UI。
3. 若用户反馈某个列表触底不加载，先看 `hasMore` 与 `LoadMoreEffect` 的 itemCount。
4. 若用户反馈重复加载，先看 `loadingMore` 是否同步置位。
5. 若后续继续追 EhViewer 风格，优先微调搜索头部、列表密度和评论视觉层级，不先改数据层。
