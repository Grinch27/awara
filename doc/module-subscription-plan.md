<!--
关键待你决策（请在评审时明确）：
1) subscription 是否在后续版本重新回到“默认入口页面”的可选项，还是永久退回抽屉内二级入口。
2) forum 是本阶段直接做最小可用列表页，还是继续保留入口但在设置页标明“尚未完成”。
3) 搜索页的 user 搜索是否拆成独立入口，还是保留底层能力但继续隐藏主切换标签。

后续代码研究方向（建议优先级从高到低）：
1) Forum 数据链路补齐：API -> repo -> VM -> 页面最小闭环，替换当前 forum 占位态。
2) FeedQuery 全链路统一：Index/Search/Favorites/Follow 统一 mapper、分页、重试与错误文案。
3) 播放诊断标准化：登录态 -> 视频详情 -> manifest -> stream URL -> ExoPlayer 事件，统一脱敏日志。

可继续优化点（可并行推进）：
1) 搜索页头部三行做折叠态，滚动时保留返回键、搜索框和日期入口。
2) 默认入口迁移增加一次性自修复埋点，统计 legacy subscription/home 偏好是否仍有残留。
3) GH Actions 失败日志抓取脚本化：run 级为空时自动切 job 级并提取首个阻塞错误。
-->

# Awara 导航、搜索与播放链路执行蓝图（2026-05-12）

## 1. 文档定位

这份文档同时服务三类读者：

1. 工程负责人：快速判断当前变更是否可上线、可回滚、可继续拆分。
2. 开发者：按模块边界定位代码，不再围绕“主页/默认分区/搜索头部”反复猜测。
3. GPT/Agent：拿到上下文后能直接进入“读首个阻塞错误 -> 最小改动 -> push -> gh run watch -> 修首个错误”的闭环。

本版文档替代“主页聚合页优先”的旧叙事，核心约束已经改为：

1. 启动后不再进入 home 聚合页，而是进入默认入口页面。
2. 默认入口页面当前仅允许 video、image、forum。
3. 搜索页头部向 EhViewer 收敛，固定为三行结构。
4. 本地调试遵循隐私优先，不落盘凭据、不输出 token/cookie、不在本地构建 APK。

## 2. 本次已确认结论

### 2.1 导航与设置

1. 设置页出现 Still in developing 的根因不是设置页本身，而是旧导航逻辑曾把 setting/history/download 当作页面内 section 处理，最终落入 TodoStatus。
2. 当前首页壳已经改成抽屉直接路由工具入口，setting/history/download 不再走占位 section。
3. 默认入口偏好语义已经从“首页默认分区”收敛为“默认入口页面”。
4. 设置页选项目前只保留 video、image、forum。
5. 旧用户如果本地还残留 subscription 等 legacy 偏好值，应用启动时会被迁移回当前允许的默认入口集合，避免继续落到非目标入口。

### 2.2 搜索页

1. 搜索页头部已经收敛到三行：
   - 第一行：返回键 + 搜索框。
   - 第二行：Video / Image 标签切换。
   - 第三行：日期下拉。
2. 排序、过滤器和列表模式仍保留，但已移出头部三行主体，避免头部结构继续偏离 EhViewer 风格。
3. user 搜索能力仍留在 VM/数据层，但主入口不再暴露为并列搜索标签。

### 2.3 播放与隐私

1. 本地脱敏接口扫测已确认：测试视频详情接口可以拿到 fileUrl，manifest 请求可以返回 200。
2. 旧的播放失败高风险点之一是清晰度偏好默认值为 Source，而 manifest 常返回 360/540 等名称；该问题此前已通过 PlayerState 的可用清晰度回退逻辑规避。
3. 本轮又补了一处隐私修复：登录成功后不再把 token 打到标准输出。
4. MediaRepo 的 manifest 请求已切换到 Iwara 专用 OkHttpClient，避免媒体链路绕开统一的 Iwara 头部策略。

### 2.4 仍然存在的风险

1. forum 入口仍无真实数据链路，当前仍不是最小可用页。
2. 搜索页虽然已满足三行头部要求，但底部增强区仍比 EhViewer 更重。
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

1. Kotlin/Compose 遵循 Google Android 风格，优先显式状态流与最小副作用。
2. Shell 遵循 Google Shell Style，默认 set -euo pipefail。
3. Python 若用于工具脚本或排障，兼容 Python 3.8，复杂类型使用 Any 或省略复杂类型声明。
4. 关键待决事项、研究方向、可选优化点统一写在文件开头注释块，便于 Agent 与人工共读。
5. 提交说明保持一句话，格式为“修复点 + 触发场景 + 结果”。

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
5. app/src/main/java/me/rerere/awara/ui/page/setting/SettingPage.kt
6. app/src/main/java/me/rerere/awara/ui/page/video/VideoPage.kt
7. app/src/main/java/me/rerere/awara/ui/page/video/VideoVM.kt

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
2. 现有搜索功能比 EhViewer 更重，无法直接删掉排序/过滤器而不伤功能。
3. 最小改法是在不破坏数据逻辑的前提下，仅重排布局层。

结果：

1. 头部核心结构符合目标。
2. 排序、过滤器、列表模式仍可用。
3. 后续若需要再极简，可以继续把增强区折叠或抽屉化。

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

### 阶段 A：完成导航与入口语义收敛

目标：

1. 首页只承接浏览内容，不承接工具页占位态。
2. 默认入口配置与实际落地页面完全一致。

验收：

1. 从抽屉进入 setting/history/download 不再出现 Still in developing。
2. 启动后落点仅可能是 video、image、forum。

### 阶段 B：完成查询模型统一

目标：

1. Index/Search/Favorites/Follow 使用统一 FeedQuery 语义。
2. UI 不再散落 queryMap 字符串拼装。

验收：

1. 同一组筛选条件在 Index/Search 下发出的 API 参数一致。
2. 分页和重试不重复拉相同页。

### 阶段 C：完成播放链路诊断化

目标：

1. 明确区分登录失败、视频详情失败、manifest 失败、stream 失败、ExoPlayer 失败。
2. 所有日志自动脱敏。

验收：

1. 播放失败时可以精确指出失败阶段。
2. 日志不包含 token/cookie/Authorization 原文。

### 阶段 D：补齐 forum 最小可用页

目标：

1. forum 不再是空占位。
2. 允许把 forum 作为真实默认入口。

验收：

1. forum 至少具备最小列表页。
2. 论坛页加载失败时提供真实错误态而不是开发中占位态。

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

1. 若导航改动引发入口错乱，优先回滚首页壳路由分发，不回滚底层 FeedQuery 与数据层整理。
2. 若搜索页布局收敛影响交互，优先回滚搜索页头部布局，不回滚底层搜索仓储与分页。
3. 若播放链路修复引发额外副作用，优先回滚 manifest 客户端选择或播放器选择策略，不回滚脱敏日志与 token 输出修复。

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

1. 先让当前导航、搜索、隐私和媒体客户端修复通过 build-apk.yml。
2. 再补 forum 最小可用页，消除默认入口的最后一个占位风险。
3. 再做播放链路的结构化脱敏日志，把“不可播”定位从猜测变成阶段化诊断。
4. 最后统一 FeedQuery mapper，把首页、搜索、收藏、关注页纳入同一查询协议。
