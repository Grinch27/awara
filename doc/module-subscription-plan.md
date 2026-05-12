# Awara 模块执行蓝图与接手提示

## 0. 这份文档是给谁看的

这份文档同时服务三类角色：

1. 人工维护者：快速判断哪些功能已经真正落地，哪些只是受控回退。
2. GPT/Agent：在拿到仓库后，不需要重新铺陈全图，直接按已验证事实进入最小改动闭环。
3. CI 修复者：严格遵循“不本地构建 APK，只用 GitHub Actions 验证”的流程推进。

如果你是新的 agent，请先把这份文档当成当前事实基线，而不是当成愿景文档。

## 1. 核心目标

Awara 当前阶段的核心目标不是“加更多页面”，而是把已经对用户暴露的入口收口到以下状态：

1. 首页默认入口必须只落到真实内容页，不允许再次掉回开发中占位页。
2. 搜索页必须保持接近 EhViewer 的三层头部结构，并保留 user 搜索。
3. 评论区必须优先保证可读性，避免文字和底色接近。
4. 用户资料页的“留言”必须是应用内可用页面，而不是 still in developing。
5. 所有改动都必须通过 GitHub Actions 工作流闭环验证，不在本地构建 APK。

## 2. 不可谈判的硬约束

### 2.1 构建策略

1. 禁止本地构建 APK。
2. 唯一允许的构建验证路径是 .github/workflows/build-apk.yml。
3. 每次只修一个首要 blocker，不顺手扩大修复面。
4. 只有当前 blocker 消失后，才允许看下一个问题。

### 2.2 隐私策略

1. 本地 API 检查只允许在终端完成。
2. 不保存 token、cookie、Authorization 头、账号密码。
3. 不在输出、文档、提交说明、日志摘要里泄露敏感凭据。
4. 若必须验证接口，只记录状态码、字段是否存在、链路走到哪一层。

### 2.3 改动策略

1. 优先修根因，不接受纯视觉遮羞布式补丁。
2. 不允许真实路径重新回到 TodoStatus 或 still in developing。
3. 当接口证据不足时，优先提供可信的只读实现，不猜写接口。
4. 第一次实质编辑后，必须先做一轮最窄验证，再继续下一刀。

## 3. 当前产品风格基线

### 3.1 总体风格

当前实现追求的是“紧凑、明确、可读、接近 EhViewer 的信息密度”，而不是 Material 默认演示风格。

应继续保持的视觉取向：

1. 顶部控制区要有明确层次，不要散成松垮的多行空白布局。
2. 搜索、筛选、标签切换要有厚实的点击面积和清晰的选中状态。
3. 评论和留言区域优先强调文字对比度，而不是追求过淡的表面色。
4. 真实功能页即使暂时能力不足，也应表现为“可用但受限”，而不是“尚未开发”。

### 3.2 对 EhViewer 风格的具体借鉴

当前不是逐像素复刻 EhViewer，而是借鉴以下交互与视觉语言：

1. 搜索页头部采用强结构化三行布局。
2. 标签切换使用更饱满的圆角胶囊，而不是薄弱的默认 Tab。
3. 评论区采用更稳的面板层级，让正文阅读优先级高于装饰。
4. 空态和错误态都尽量像真实产品页，而不是工程占位页。

## 4. 当前模块实现现状

### 4.1 默认入口与首页壳

现状：

1. 默认入口已经恢复支持 subscription。
2. 其他可选默认入口包括 video、image、forum。
3. 首页壳只应承载真实内容页，不再容纳 setting、history、download 这类外部路由页。
4. 遇到旧状态或非法恢复状态时，应运行时回退到有效内容页，而不是污染用户偏好。

主要文件：

1. app/src/main/java/me/rerere/awara/ui/page/index/IndexPage.kt
2. app/src/main/java/me/rerere/awara/ui/page/index/layout/IndexPagePhoneLayout.kt
3. app/src/main/java/me/rerere/awara/ui/page/index/layout/IndexPageTabletLayout.kt
4. app/src/main/java/me/rerere/awara/ui/page/setting/SettingPage.kt

验收要求：

1. setting 中能看到 subscription 默认入口选项。
2. 重启或恢复后，首页壳不会跳进占位页。
3. forum 即使仍是回退方案，也必须是可操作的真实回退页。

### 4.2 搜索模块

现状：

1. 搜索页已经恢复 user 搜索功能。
2. 搜索头部已收敛为三行：
   - 第一行：返回键 + 搜索框
   - 第二行：video / image / user 标签
   - 第三行：日期下拉或 user 模式提示
3. 当前样式方向是“更像 EhViewer 的紧凑控制条”，不是标准 Material 搜索模板。

主要文件：

1. feature/search/src/main/java/me/rerere/awara/ui/page/search/SearchPage.kt
2. feature/search/src/main/java/me/rerere/awara/ui/page/search/SearchVM.kt
3. feature/search/src/main/res/values/strings.xml

验收要求：

1. user 模式必须可见、可切换、可返回结果。
2. 头部不能塌成两行，也不要退回零散的四行。
3. 如果继续微调视觉，只应在 SearchPage 这一层做，不要为视觉改动扰动搜索数据流。

### 4.3 评论模块

现状：

1. 评论卡片与评论列表已改成高对比深色 panel。
2. 昵称、正文、时间、次级信息都已有明确的前景色策略。
3. 评论组件现在支持只读模式：
   - 当不提供回复加载回调时，回复数显示为静态文本
   - 当不提供 reply 回调时，不显示 Reply CTA

主要文件：

1. app/src/main/java/me/rerere/awara/ui/component/iwara/comment/CommentCard.kt
2. app/src/main/java/me/rerere/awara/ui/component/iwara/comment/CommentList.kt
3. app/src/main/java/me/rerere/awara/ui/component/iwara/comment/CommentState.kt
4. app/src/main/java/me/rerere/awara/ui/page/video/pager/VideoCommentPage.kt

验收要求：

1. 评论内容在浅色、深色和动态主题下都不能出现低对比不可读。
2. 视频评论的可回复交互不能被本轮只读支持破坏。
3. 资料页留言能复用 CommentCard，但不能误暴露未实现的写交互。

### 4.4 用户资料页与留言模块

现状结论：

1. 用户资料页第三个分页“留言”现在已经从浏览器默认回退改为应用内真实留言页。
2. 该留言页当前是“真实读取 + 只读展示”的边界实现。
3. 已接入的可信接口是 GET /profile/{id}/comments?page=0。
4. 路由参数不能直接拿来请求留言接口；必须先调用 profile 接口，再使用 profile.user.id 访问留言接口。
5. 浏览器打开 Iwara 个人页现在只保留为错误态的回退按钮，不再是默认展示内容。

为什么是只读实现：

1. 当前只验证过 profile comments 的 GET 链路。
2. 没有经过证据确认 profile comments 的 POST/reply 路由、参数和行为。
3. 在“要求事实上高置信”的前提下，不应猜测写接口。
4. 因此，当前最稳的实现是：应用内读取、分页、展示；写入和线程展开先不乱接。

主要文件：

1. app/src/main/java/me/rerere/awara/ui/page/user/UserPage.kt
2. app/src/main/java/me/rerere/awara/ui/page/user/UserVM.kt
3. app/src/main/java/me/rerere/awara/data/repo/CommentRepo.kt
4. app/src/main/java/me/rerere/awara/data/source/IwaraAPI.kt
5. app/src/main/res/values/strings.xml
6. app/src/main/res/values-zh-rCN/strings.xml

实现细节：

1. UserVM 新增 guestbookCommentState、guestbookError、guestbookExceptionMessage。
2. 留言请求在 profile 成功加载后启动。
3. 请求时使用 profile.user.id，而不是路由中的 id 参数。
4. UserPage 使用 CommentCard 的只读模式渲染留言列表。
5. 当加载失败时，展示错误说明、重试按钮，以及浏览器回退按钮。
6. 当留言为空时，展示正常空态，不再误导到浏览器页。
7. 当总数大于 0 时，才显示分页条，避免出现无意义页码。

验收要求：

1. 用户页“留言”不能再出现 still in developing。
2. 资料页留言必须在应用内呈现，而不是默认跳浏览器。
3. 请求必须基于 profile.user.id。
4. 错误态必须允许重试。
5. 如果留言持续失败，仍要能退回浏览器查看。

### 4.5 Forum 模块

现状：

1. forum 仍然是浏览器回退页。
2. 这是当前可接受的受控回退，而不是占位页。
3. 不允许把 forum 重新做成 TodoStatus。

结论：

1. forum 不是本轮优先级。
2. 如果后续继续原生化，应单独开工作面，不要和搜索、评论、资料页留言混修。

## 5. 已验证事实与高置信结论

当前可以当作事实使用的内容：

1. 用户资料页留言接口至少存在 GET /profile/{id}/comments。
2. 该接口的 id 必须是 profile.user.id，而不能盲目使用当前路由参数。
3. CommentCard 通过可空回调已经能安全支持只读渲染。
4. 用户页留言在实现上已经具备：加载、刷新、分页、错误态、浏览器回退。
5. 本仓库的 APK 验证必须依赖 GitHub Actions，而不是本地构建。

当前刻意不当作事实的内容：

1. profile 留言的发送接口。
2. profile 留言的回复接口。
3. profile 留言 replies 的线程接口是否与视频评论完全同构。
4. 任何未被日志或接口证据确认的 profile guestbook 写行为。

## 6. 仍然存在的边界与改进方向

### 6.1 已知边界

1. 用户资料页留言当前只读，不支持发送留言。
2. 用户资料页留言当前不展开 replies 线程，只显示 reply 数量。
3. forum 仍是浏览器回退页。
4. 评论区虽已高对比，但最终视觉细节仍建议人工真机看一轮。

### 6.2 后续正确改进方向

1. 若要继续做资料页留言增强，先证实 profile guestbook 的写接口，再考虑输入框和发送行为。
2. 若要支持留言线程展开，先验证 profile comments 是否支持 parent 或专门 replies 端点，再复用 CommentState 栈。
3. 若要继续向 EhViewer 风格靠拢，应优先打磨搜索页与评论区的间距、圆角和选中态，不先改数据层。
4. 若要原生化 forum，应单独整理 forum 的接口、分页、详情和权限边界。

## 7. Agent 接手时应直接遵循的 Prompt

下面这段文字是给新 agent 的直接行动提示。接手时可以原样当作系统化工作提示使用。

### 7.1 Agent Prompt

你正在维护 Awara，一个基于 Kotlin + Compose 的 Iwara 客户端。当前阶段目标不是做大规模重构，而是在不做本地 APK 构建的前提下，把已暴露入口收口为稳定、可读、可验证的真实页面体验。

你的硬约束：

1. 不在本地构建 APK。
2. 所有构建验证只能走 .github/workflows/build-apk.yml。
3. 每次只修当前首个 blocker。
4. 不泄露 token、cookie、Authorization 头。
5. 不让真实页面再回到 TodoStatus 或 still in developing。
6. 若接口证据不足，优先做可信只读实现，不猜写接口。

你的局部判断规则：

1. 从最具体的锚点开始，只读能直接控制行为的本地文件。
2. 形成一个可被快速证伪的局部假设后，立刻做最小改动。
3. 第一次实质编辑后，下一步必须先做最窄验证。
4. 如果验证失败，只修该切片的首个本地问题，不扩散。
5. 如果验证通过，再处理相邻最小后续改动。

当前必须知道的产品事实：

1. 默认入口支持 subscription、video、image、forum。
2. 搜索页必须保留 EhViewer 风格的三层头部与 user 搜索。
3. 评论区已切到高对比深色 panel。
4. 用户资料页留言已改为应用内真实只读页。
5. 用户资料页留言接口应通过 profile.user.id 调用 GET /profile/{id}/comments。
6. 资料页留言写接口没有被证实，不要猜。

当前高优先级检查顺序：

1. 先看用户报错所对应的直接文件。
2. 若是资料页留言问题，先看 UserVM.kt、UserPage.kt、CommentRepo.kt、IwaraAPI.kt。
3. 若是搜索页问题，先看 SearchPage.kt，再决定是否进 SearchVM.kt。
4. 若是 still in developing 回归，先全文查 TodoStatus 的真实页面引用。
5. 代码改完后先做文件级错误检查，再提交、push、跑 workflow。

禁止动作：

1. 不要为了“可能有帮助”去做本地 APK 构建。
2. 不要一边有红色 blocker，一边继续顺手改第二个模块。
3. 不要把浏览器回退页误报成原生实现。
4. 不要把未证实的 API 写路径硬接进生产 UI。

## 8. 最小验证与 CI 闭环

### 8.1 本地最小验证顺序

在不做本地 APK 构建的前提下，优先按这个顺序：

1. get_errors 检查改动文件。
2. 全文搜索确认真实页面不再调用 TodoStatus。
3. git diff --check 检查补丁完整性。
4. git status --short --branch 确认改动面。

如果用户要求接口连通性排查，可做脱敏终端检查，但仍不得泄露敏感凭据。

### 8.2 GitHub Actions 标准闭环

固定执行顺序：

1. git add 相关文件
2. git commit -m "一句准确的提交说明"
3. git push origin 当前分支
4. gh workflow run build-apk.yml --ref 当前分支
5. gh run watch --interval 10 --exit-status
6. 若失败，先看 gh run view --log-failed
7. 若失败日志为空，再看 gh run view --job <job_id> --log
8. 只修第一个 blocker，然后重新走 1 到 7

关键注意点：

1. workflow 触发后，不要想当然地看最新列表；要核对当前分支和提交 SHA。
2. watch 未结束前，不要同时展开第二个问题。
3. 当 run 成功后，再决定是否需要补文档或做下一轮小改动。

## 9. 本轮完成定义

满足以下条件，才算本轮真正完成：

1. 用户资料页“留言”已在应用内显示真实留言列表。
2. 留言页面具备分页、刷新、错误重试和浏览器回退。
3. 留言请求基于 profile.user.id，而不是错误路由参数。
4. CommentCard 的只读模式没有破坏视频评论现有交互。
5. 修改已提交、已推送，并通过 build-apk.yml 验证。
6. 这份文档已反映当前真实状态，而不是旧版浏览器回退状态。

## 10. 下一轮建议优先级

1. 若 CI 失败，继续按“只修第一个 blocker”的规则收敛到成功。
2. 若产品继续追求 EhViewer 风格，优先打磨搜索页头部细节和评论区视觉层次。
3. 若用户继续要求资料页留言增强，先验证写接口与 replies 接口，再决定是否原生支持发送和线程展开。
4. 若用户要求彻底去掉 forum 浏览器回退，再单独为 forum 开新的实现与验证闭环。
