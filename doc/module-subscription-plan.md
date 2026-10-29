# Awara 实施基线与 Agent 执行规则
更新：2026-10-30。当前任务：保留 Material 3，采用 EhViewer 信息密度，分批实施可靠性改进并通过 GitHub Actions 构建。
本文记录实际实现，不把待办写成已完成；进入仓库先读根 AGENTS.md、本文、相关源码和最新 diff。

## 1. 执行与验证
- 本地允许读取、编辑、定向搜索、XML/YAML/Shell 静态检查及 git diff --check。
- 不在本地运行 Gradle build、assemble 或构建 APK；Kotlin 编译、必要 JVM 回归和 APK 构建在 Actions 执行。
- 工作流失败按首个可信错误修复；未运行的设备或接口场景必须标记未验证。
- 遵守 AGENTS.md 的最小必要验证规则：不追加 SHA256、证书、DEX 或产物版本扫描。
- 不读取或提交用户账号秘密；公开接口验证只使用匿名请求。
- 每批变更更新本文，提交、推送至验证分支并检查 Actions；不得把旧 APK 当成本次产物。

## 2. 已确认产品约束
1. Material 3 与 Compose 保留；紧凑布局通过间距、卡片与元数据层级实现。
2. 首页标题仅切换视频、图片、论坛；右侧仅搜索和视图。手机抽屉、宽窗导航保留。
3. 首页视频/图片与订阅页不恢复第二行筛选、日期或媒体切换。
4. 顶部视图菜单仅详情/缩略图；缩略图拦截入口只在设置页。
5. 完全拦截封面与加载后模糊分别定义；不能加载后遮盖冒充拦截。
6. 搜索保留视频、图片、帖子、用户、播单、论坛帖子、论坛主题七分区。
7. 视频/图片保留 sort/rating/date/tag 控制；其他分区不继承媒体专属过滤。
8. date 排序文案为“最新”；日期值支持 YYYY 与 YYYY-M。
9. 标签保留 A–Z/0–9 请求和本地匹配，直到新接口经过验证。
10. 不恢复旧分页条、跳页函数、saved-view 表与首页管理条。
11. 论坛保留原生只读板块/主题/帖子；写入不在本次基础范围。
12. username 路由必须非空并 Uri.encode；用户资料接口使用 username。
13. 不增加 VR、账号自动化、私信、后台播放或画中画等可选功能。
14. 48dp 关键触控目标、字体缩放和动态窗口适配不能为密度让步。

## 3. 模块和入口
- app：应用壳、DI、API/DAO/repository、首页、详情、论坛、用户、下载和设置。
- core:model：共享查询/过滤模型；UI 状态历史上仍在此模块，后续提取时必须维护依赖边界。
- data：FeedQuery 到 API 参数映射。
- feature:search：搜索契约、ViewModel、七分区 UI。
- feature:player：Media3 播放器、缓存和 DLNA。
- build-logic：Android、Compose、Room、序列化构建约定。

关键入口：
- App.kt：偏好初始化、加密会话初始化、Koin、Coil、通知渠道。
- data/session/SecureSession.kt：Keystore 加密的会话文件，位于 noBackupFilesDir。
- data/session/SessionRepository.kt：有限刷新、失败冷却及会话代次检查。
- di/NetworkModule.kt：严格 DoH 公共客户端、API 鉴权客户端及只读 401 重放。
- ui/stores/UserStore.kt：观察会话变化并加载资料，不负责保存明文 Token。
- data/source/APIResult.kt：取消传播、API 错误转换。
- data/repo/LocalDataRepo.kt：安全字段备份与恢复，严格导入和预览仍待本任务后续实现。
- feature/search/.../SearchVM.kt：搜索与分页，竞争控制待本任务后续实现。
- worker/DownloadWorker.kt：现有普通视频下载，持久任务和续传待后续实现。

## 4. 本批已实现
- 从 dev 功能基线整合 main 的 CI 规范及当前 AGENTS.md，保留 SDK 安装与依赖升级策略。
- 登录密码仅保留内存；清除旧 login.password 与明文 Token 偏好。
- 保留凭据迁移到 Android Keystore AES/GCM 加密文件，文件不进入系统备份。
- 退出递增会话代次；迟到刷新不能重新登录账户。
- 会话层合并刷新，失败有冷却；只对 API HTTPS GET/HEAD 的 401 最多重放一次，写操作不自动重放。
- 资料加载校验会话代次，取消离开的 UI 请求。
- API 取消继续传播；媒体 manifest 关闭响应；媒体来源请求使用公共客户端。
- 日志不再附带未脱敏原始 Throwable，URL 和凭据字段受控脱敏。
- 新增并发刷新、刷新失败、刷新期间退出、API 取消边界测试；是否通过以对应 Actions 结果为准。
- 普通构建 debug 执行 JVM 回归；失败时只上传诊断，APK 只在对应 Job 成功后上传。

## 5. 发展路线与实际状态
- K0：分支整合、规则与验证入口。本批等待实际 CI。
- K1：安全/会话/备份/请求与身份。会话部分已写入，备份、下载和查询部分未完成。
- K2：统一 Material 3 紧凑卡片、七分区搜索、封面策略和状态恢复，待实施。
- K3：播放质量切换、媒体源恢复、观看/阅读进度、图集与缓存，待实施。
- K4：普通视频文件的持久任务、校验续传、后台控制和离线浏览，待实施。
- K5：必要迁移、兼容性和交付；真实设备性能与正式签名仍取决于实际条件。
- K6：轻量预设、本地收藏、应用锁及媒体扩展逐项另行选择，不默认实施。
下载首版暂按普通文件推进；HLS/DASH 离线不自动加入。

## 6. 接口和路由事实
- 视频/图片搜索使用 /videos 与 /images，API 页码从 0 开始。
- 通用搜索 /search 使用 posts、users、playlists、forum_posts、forum_threads。
- 标签使用 /tags?filter=<A-Z|0-9>&page=<0-based>。
- 论坛使用 GET /forum、/forum/{sectionId}、/forum/threads/{threadId}。
- 搜索 route 为 search?type={type}&tag={tag}。
- 用户资料 route 及 API 使用非空 username；播单和论坛 ID 均编码。
- 公开接口的历史成功不代表当前账号/网络行为已重新实测。

## 7. 提交前后检查
1. 检查改动相关源码、调用方、资源与 DI。
2. 定向搜索首页顶栏、筛选入口、路由和旧分页回归。
3. XML/YAML/Shell 做适用静态检查；没有 IDE file-errors 工具时如实记录，以 Actions 编译为准。
4. git diff --check、git status，检查无秘密。
5. 提交并推送本次验证分支，等待普通 debug/release 的必要构建与测试结果。
6. 有新失败证据才修正和重跑；不重复扫描成功产物。

## 8. 仍未验证
- 本批尚未获得新的 Actions 结果，不能宣称构建成功。
- Keystore 旧数据迁移与密钥失效、真实账号刷新、真实手机运行尚未设备验证。
- 严格 DoH 的已有单元用例将在 CI 执行；本地不进行带凭据线上请求。
- 目标设备、下载成品扩展、长期正式签名和性能预算未确认；不据此扩展 minSdk 或 ABI。
