<!--
关键待你决策（请在评审时明确）：
1) forum 后续是继续保留“浏览器落地页 + 回到视频/图片”的最小方案，还是直接进入原生列表页开发。
2) subscription 是否在后续版本重新回到“默认入口页面”的可选项，还是永久退回抽屉内二级入口。
3) 搜索页的 user 搜索是否拆成独立入口，还是保留底层能力但继续隐藏主切换标签。

后续代码研究方向（建议优先级从高到低）：
1) Forum 数据链路补齐：API -> repo -> VM -> 页面最小闭环，替换当前浏览器落地页。
2) 播放诊断标准化：登录态 -> 视频详情 -> manifest -> stream URL -> ExoPlayer 事件，统一脱敏日志。
3) FeedQuery 全链路统一：Index/Search/Favorites/Follow 统一 mapper、分页、重试与错误文案。

可继续优化点（可并行推进）：
1) 搜索页三行头部继续贴近 EhViewer 的视觉细节，包括间距、圆角和标签按钮样式。
2) forum 落地页增加一次性埋点，统计默认入口为 forum 的真实使用频率和跳转去向。
3) GH Actions 失败日志抓取脚本化：run 级为空时自动切 job 级并提取首个阻塞错误。
-->

# Awara 导航、搜索、详情与播放链路执行蓝图（2026-05-12）

这份文档服务三类读者：

1. 工程负责人：快速判断当前状态是否可上线、可回滚、是否还存在显著占位风险。
2. 开发者：直接定位当前控制导航、搜索、详情页顶栏和播放链路的关键文件。
3. GPT/Agent：拿到上下文后直接进入“提取首个阻塞错误 -> 最小改动 -> push -> gh run watch -> 修首个错误”的闭环。

本版文档已经替换掉“home 聚合页优先”的旧叙事。当前约束是：

1. 启动后不再进入 home 聚合页，而是进入默认入口页面。
2. 默认入口页面只允许 video、image、forum。
3. 搜索页头部必须向 EhViewer 收敛为三行结构。
4. forum 当前不能再显示 Still in developing，占位页已替换为可操作的最小落地页。
5. 本地调试遵循隐私优先，不落盘凭据、不输出 token/cookie、不在本地构建 APK。

## 0. 当前状态

1. 设置页、历史页、下载页已经从首页壳分离，不再走页面内占位 section。
2. forum 默认入口不再显示开发中占位文案，而是进入可操作的最小落地页。
3. 搜索页已经删除顶部旧的“日期/详情”工具行，头部只保留三行结构。
4. 视频详情页手机布局已经移除重复顶栏，播放器顶栏统一负责状态栏避让。
5. forum 仍然不是原生数据页，特殊视频场景的播放链路诊断也还需要继续补齐。

## 1. 文档定位

这份文档同时服务三类读者：

1. 工程负责人：快速判断当前变更是否可上线、可回滚、可继续拆分。
2. 开发者：按模块边界定位代码，不再围绕“主页/默认分区/搜索头部”反复猜测。
3. GPT/Agent：拿到上下文后能直接进入“读首个阻塞错误 -> 最小改动 -> push -> gh run watch -> 修首个错误”的闭环。

## 2. 本次已确认结论

### 2.1 导航与设置

1. 设置页出现 Still in developing 的根因不是设置页本身，而是旧导航逻辑曾把 setting/history/download 当作页面内 section 处理，最终落入 TodoStatus。
2. 当前首页壳已经改成抽屉直接路由工具入口，setting/history/download 不再走占位 section。
3. 默认入口偏好语义已经从“首页默认分区”收敛为“默认入口页面”。
4. 设置页选项目前只保留 video、image、forum。
5. 旧用户如果本地还残留 subscription 等 legacy 偏好值，应用启动时会被迁移回当前允许的默认入口集合，避免继续落到非目标入口。
6. forum 当前使用的是最小可用落地页，而不是 TodoStatus：
   - 可直接打开 Iwara forum 浏览器页面。
   - 可直接回到 video 或 image，避免默认入口陷入死路。

### 2.2 搜索页

1. 搜索页头部已经收敛为三行：
   - 第一行：返回键 + 搜索框。
   - 第二行：Video / Image 标签切换。
   - 第三行：日期下拉。
2. 旧的“日期/详情”常驻工具行已经从顶部移除，不再构成第四行伪顶栏。
3. recent query 仍保留在搜索框展开内容内部，但不再常驻头部下方。
4. user 搜索能力仍留在 VM/数据层，但主入口不再暴露为并列搜索标签。

### 2.3 播放与隐私

1. 本地脱敏接口扫测已确认：测试视频详情接口可以拿到 fileUrl，manifest 请求可以返回 200。
2. 旧的播放失败高风险点之一是清晰度偏好默认值为 Source，而 manifest 常返回 360/540 等名称；该问题此前已通过 PlayerState 的可用清晰度回退逻辑规避。
3. 本轮 UI 修复里又补了一处详情页安全区问题：播放器顶栏统一负责状态栏避让，手机详情页不再重复叠一层返回栏。
4. 隐私与媒体链路修复继续保留：
   - 登录成功后不再把 token 打到标准输出。
   - MediaRepo 的 manifest 请求继续使用 Iwara 专用 OkHttpClient。

### 2.4 仍然存在的风险

1. forum 入口仍无原生真实数据链路，当前只是浏览器落地页。
2. 搜索页虽然已经满足三行头部要求，但视觉细节还没有完全像素级贴齐 EhViewer。
3. 私有视频、年龄限制视频、地区限制视频仍需补一轮专门的播放链路脱敏扫测。

## 3. 强约束

### 3.1 构建与 CI

1. 不在本地构建 APK。
2. 所有构建均通过 GH Actions 的 build-apk.yml 完成。
3. 每次只修首个阻塞错误，不顺手扩散改动面。
4. 触发工作流后必须使用 gh run watch --interval 10 --exit-status 持续监控。
5. 每隔 1 分钟必须确认一次当前 run 状态没有遗漏。
6. 若 gh run view --log-failed 无输出，必须降级到 gh run view --job <job_id> --log。

### 3.2 隐私与本地测试

1. 凭据只允许在本地终端临时使用，不允许写入仓库、脚本、文档或日志归档。
2. 本地接口扫测只输出脱敏摘要：接口是否通、状态码、字段是否存在、是否拿到媒体源。
3. 不在任何提交说明、文档、错误日志中粘贴 token、cookie、Authorization 头。
4. 本地排障记录只写“链路阶段 + 现象 + 状态码/异常类型”，不写私密载荷。

### 3.3 代码风格

1. Kotlin/Compose 遵循 [Google Android Kotlin Style Guide](https://developer.android.com/kotlin/style-guide)，优先显式状态流与最小副作用。
2. Shell 遵循 Google Shell Style，默认 set -euo pipefail。
3. Python 若用于工具脚本或排障，兼容 Python 3.8，复杂类型使用 Any 或省略复杂类型声明。
4. 关键待决事项、研究方向、可选优化点统一写在文件开头注释块，便于 Agent 与人工共读。
5. 提交说明保持一句话，格式为“修复点 + 触发场景 + 结果”。
6. markdown 遵循 [Google Markdown Style Guide](https://google.github.io/styleguide/docguide/style.html)。
7. JSON 遵循 [Google JSON Style Guide](https://google.github.io/styleguide/jsoncstyleguide.xml)。

## 4. 当前实现地图

### 4.1 app 模块

职责：

1. 路由装配。
2. DI 装配。
3. 首页壳、设置页、视频详情页等页面编排。

关键文件：

1. app/src/main/java/me/rerere/awara/ui/RouterActivity.kt
2. app/src/main/java/me/rerere/awara/ui/page/index/IndexPage.kt
3. app/src/main/java/me/rerere/awara/ui/page/index/layout/IndexPagePhoneLayout.kt
4. app/src/main/java/me/rerere/awara/ui/page/index/layout/IndexPageTabletLayout.kt
5. app/src/main/java/me/rerere/awara/ui/page/index/pager/IndexForumPage.kt
6. app/src/main/java/me/rerere/awara/ui/page/setting/SettingPage.kt
7. app/src/main/java/me/rerere/awara/ui/page/video/VideoPage.kt
8. app/src/main/java/me/rerere/awara/ui/page/video/layout/VideoPagePhoneLayout.kt
9. app/src/main/java/me/rerere/awara/ui/page/video/pager/VideoOverviewPage.kt

### 4.2 feature/search

职责：

1. 搜索页 Compose UI。
2. 搜索状态与分页。
3. 搜索过滤器、排序、最近搜索与结果列表渲染。

关键文件：

1. feature/search/src/main/java/me/rerere/awara/ui/page/search/SearchPage.kt
2. feature/search/src/main/res/values/strings.xml

### 4.3 feature/player

职责：

1. ExoPlayer 状态封装。
2. 媒体缓存。
3. 播放器控制层与 DLNA 投放。

关键文件：

1. feature/player/src/main/java/me/rerere/awara/ui/component/player/PlayerState.kt
2. feature/player/src/main/java/me/rerere/awara/ui/component/player/PlayerCache.kt
3. feature/player/src/main/java/me/rerere/awara/ui/component/player/Player.kt
4. feature/player/src/main/java/me/rerere/awara/ui/component/player/PlayerScaffold.kt

### 4.4 data 与 network

职责：

1. Iwara API 封装。
2. Repo 级参数转换、manifest 获取、搜索与媒体仓储。
3. Iwara 专用网络头部与日志脱敏。

关键文件：

1. app/src/main/java/me/rerere/awara/data/source/IwaraAPI.kt
2. app/src/main/java/me/rerere/awara/data/repo/MediaRepo.kt
3. app/src/main/java/me/rerere/awara/data/repo/UserRepo.kt
4. app/src/main/java/me/rerere/awara/di/NetworkModule.kt
5. app/src/main/java/me/rerere/awara/di/RepoModule.kt
6. app/src/main/java/me/rerere/awara/util/AppLogger.kt

## 5. 根因归纳

### 5.1 设置页为什么会落到 Still in developing

旧问题链路：

1. 抽屉项存在工具入口与页面 section 的混用。
2. setting/history/download 被当作 section name 回写到首页壳。
3. 首页壳的 when 分支对这些名称没有真实页面实现。
4. 最终落入 TodoStatus，占位文案显示 Still in developing。

当前修复策略：

1. 工具入口只走 router.navigate。
2. 首页壳只承接真实浏览页。
3. 默认入口集合只认 video/image/forum。
4. legacy subscription 偏好值在首页壳启动时主动迁移掉。

### 5.2 为什么搜索页要拆成“三行头部 + 头部外增强区”

原因：

1. 用户明确要求按 EhViewer 风格收敛为三行结构。
2. 原实现虽然已经有三行头部，但三行下方还常驻一行“日期/详情”工具区，用户体感上仍然不是 EhViewer 式头部。
3. 最小改法不是推翻搜索逻辑，而是先删除这条顶部旧工具行，并把 recent query 保留在搜索框展开内容内部。

结果：

1. 头部核心结构符合目标。
2. 旧的“日期/详情”视觉干扰已经消失。
3. 后续若要继续极简，只需要继续处理视觉细节，而不是再拆一层顶栏。

### 5.3 视频为什么曾经“能看到详情但不能播”

当前已知结论：

1. 测试视频详情接口可返回 fileUrl。
2. manifest 可通过 x-version 获取。
3. 当前公开样本上，manifest 获取本身不是主要阻塞点。
4. 历史上真实导致播放器空载的原因，是“偏好清晰度名称与 manifest 实际名称不匹配”。

当前补强：

1. PlayerState 以可用清晰度回退，避免 Source 不存在时空载。
2. MediaRepo 改为使用 Iwara 专用客户端，避免媒体链路与统一请求头策略脱节。
3. LoginVM 去掉 token stdout，防止排障时把敏感信息带入日志。

## 6. 下一阶段模块化目标

### 阶段 A：完成 EhViewer 风格收敛

目标：

1. 搜索页头部结构与视觉继续向 EhViewer 靠拢。
2. 详情页手机顶部不再出现重复顶栏或安全区错位。

验收：

1. 搜索页顶部不再出现旧的“日期/详情”工具行。
2. 手机详情页顶部控制区稳定显示在状态栏下方，不再超出屏幕。

### 阶段 B：完成 forum 最小可用页升级

目标：

1. forum 从“浏览器落地页”升级为“原生列表页”。
2. forum 作为默认入口时具有完整的加载、错误和空态。

验收：

1. forum 至少具备最小列表页。
2. 不再依赖外部浏览器作为主路径。

### 阶段 C：完成播放链路诊断化

目标：

1. 明确区分登录失败、视频详情失败、manifest 失败、stream 失败、ExoPlayer 失败。
2. 所有日志自动脱敏。

验收：

1. 播放失败时可以精确指出失败阶段。
2. 日志不包含 token/cookie/Authorization 原文。

### 阶段 D：完成 FeedQuery 统一

目标：

1. Index/Search/Favorites/Follow 使用统一 FeedQuery 语义。
2. UI 不再散落 queryMap 字符串拼装。

验收：

1. 同一组筛选条件在 Index/Search 下发出的 API 参数一致。
2. 分页和重试不重复拉相同页。

## 7. 本地接口扫测建议顺序

1. 登录接口：确认 refresh token 是否返回。
2. token 刷新接口：确认 access token 是否可换取。
3. 视频详情接口：确认 fileUrl、private、embedUrl、user 基本字段。
4. manifest 接口：确认 x-version 是否仍然有效、返回的 qualities 是否包含 Source。
5. stream URL：确认是否需要额外头部、是否存在 403/302/协议相对 URL。
6. ExoPlayer 事件：确认 STATE_BUFFERING、STATE_READY、STATE_IDLE、STATE_ENDED 的变化轨迹。

输出要求：

1. 只输出脱敏摘要。
2. 不输出原始 Authorization 头。
3. 不输出 cookie/token 明文。

## 8. GH Actions 执行闭环

标准流程：

1. git status 确认工作树。
2. git add + git commit，提交说明只写一行。
3. git push origin 当前分支。
4. gh workflow run build-apk.yml --ref 当前分支。
5. gh run watch --interval 10 --exit-status。
6. 若失败：gh run view --log-failed。
7. 若 run 级失败日志为空：gh run view --job <job_id> --log。
8. 只修首个阻塞错误，再提交、push、重跑。

观察要点：

1. 是否是 Kotlin 编译错误。
2. 是否是资源编译、字符串占位或 Compose API 兼容问题。
3. 是否是工作流环境、JDK、Gradle、wrapper 或 action 版本问题。

## 9. 回滚预案

1. 若搜索页风格收敛影响交互，优先回滚搜索页头部布局，不回滚底层搜索仓储与分页逻辑。
2. 若详情页顶部修复引发播放器控制异常，优先回滚手机详情页重复顶栏删除或播放器状态栏避让实现，不回滚媒体链路修复。
3. 若 forum 落地页引发默认入口争议，优先回滚 forum 入口页实现，不回滚 setting/history/download 的直接路由修复。

## 10. Awesome Prompt 模板

```text
你是仓库内执行型工程代理，请严格按以下闭环工作：

【硬约束】
1) 不在本地构建 APK，所有构建只走 build-apk.yml。
2) 只修当前首个阻塞错误，不扩散改动面。
3) 所有本地接口扫测只输出脱敏摘要，不输出 token/cookie/Authorization。
4) 关键待决问题、后续研究方向、可优化点，必须写在目标文件顶部注释块中。

【执行顺序】
1) 读取当前分支状态与最新失败 run。
2) 提取首个阻塞错误。
3) 只修改与首个错误直接相关的最小代码。
4) 本地只做静态检查、错误面检查、脱敏接口扫测，不做 APK 构建。
5) 提交并 push 到当前工作分支。
6) 触发 gh workflow run build-apk.yml。
7) 使用 gh run watch --interval 10 --exit-status 持续监控。
8) 若失败：优先看 gh run view --log-failed；若为空则切 gh run view --job <job_id> --log。
9) 回到步骤 2，直到工作流成功。

【输出要求】
1) 先给结论，再列文件与原因。
2) 不粘贴敏感凭据。
3) 日志只摘错误摘要。
4) 最后给出变更清单、影响面、残余风险和下一步建议。
```

## 11. 建议执行顺序

1. 先让当前搜索页、forum 落地页和详情页顶部修复通过 build-apk.yml。
2. 再补 forum 原生数据链路，彻底移除浏览器落地页过渡方案。
3. 再补播放链路的结构化脱敏日志，把“不可播”定位从猜测变成阶段化诊断。
4. 最后统一 FeedQuery mapper，把首页、搜索、收藏、关注页纳入同一查询协议。
