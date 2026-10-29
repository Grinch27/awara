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
- core:model：共享查询/过滤、媒体展示模型及搜索契约，不依赖 Compose。
- core:ui：UiState、紧凑 Material 3 媒体卡片、封面策略和自适应网格。
- data：FeedQuery 到 API 参数映射。
- feature:search：可取消搜索 ViewModel、控制面板、七分区结果 UI。
- feature:player：Media3 播放器、缓存和 DLNA。
- build-logic：Android、Compose、Room、序列化构建约定。

关键入口：
- App.kt：偏好初始化、加密会话初始化、Koin、Coil、通知渠道。
- data/session/SecureSession.kt：Keystore 加密的会话文件，位于 noBackupFilesDir。
- data/session/SessionRepository.kt：有限刷新、失败冷却及会话代次检查。
- di/NetworkModule.kt：严格 DoH 公共客户端、API 鉴权客户端及只读 401 重放。
- ui/stores/UserStore.kt：观察会话变化并加载资料，不负责保存明文 Token。
- data/source/APIResult.kt：取消传播、API 错误转换。
- data/repo/LocalDataRepo.kt：严格、限量安全备份校验、预览、合并/替换和导入恢复日志。
- feature/search/.../SearchVM.kt：不可变请求快照、代次隔离、取消、去重和停止空/重复页。
- worker/DownloadWorker.kt：持久任务 ID、公共下载客户端、响应关闭、有限重试及校验续传。

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
- K0：dev 与 main 规则已整合；首批安全提交 7f81e5f 的普通 Debug/Release、Docker 均构建成功，Debug JVM 回归通过。
- K1：加密会话、刷新隔离、取消传播已通过首批 CI；严格备份预览/合并/替换、数据库与偏好恢复日志已实现，等待完整批次 CI。
- K2：共享 Material 3 紧凑卡片、统一封面拦截、自适应 1–6 列、搜索竞争控制与条件保存、搜索历史暂停/单条删除已实现，等待完整批次 CI 和设备显示验收。
- K3：切换清晰度保留进度/速度/播放意图、有限媒体源刷新、视频和图集进度、图集空态/重试/缩放手势、缓存键和取色内存限制、DLNA 主线程回调已实现，等待完整 CI 与设备验证。
- K4：普通视频文件持久任务、并发上限 2、未知总长、Range/If-Range 校验、部分文件、有限重试、暂停/继续/取消、批量暂停/继续、离线浏览、缺失文件提示、批量本地删除、SAF 导出已实现，等待完整 CI 与真实服务端验证。
- K5：Room 6→7 显式迁移、自动备份隐私规则、可选屏幕保护、历史暂停/批量移除、MMKV 维护版本迁移与文档已实现，等待完整 CI；当前 minSdk/targetSdk/ABI 不变。
- K6：预设、本地收藏、应用锁、后台播放、PiP、VR、新远端写操作和 HLS/DASH 离线包装不默认实施。

新增安全行为：自动云备份/设备传输不再包含本地账号数据、日志、MMKV、数据库或媒体路径；迁移使用用户选择的安全 JSON 导出。合并保留当前安全设置，替换恢复备份中的安全设置。媒体本体、凭据与任务队列不进入 JSON。
下载账号绑定至创建任务的会话；退出或账号更换会阻止旧任务继续。重新登录后可从详情新建任务，不会把旧私有任务自动绑定到新会话。

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
- 首批会话安全提交已经构建成功；完整改造批次仍以最新 Actions 结果为准。
- Keystore 旧数据迁移与密钥失效、真实账号刷新、真实手机运行尚未设备验证。
- 严格 DoH 的已有单元用例将在 CI 执行；本地不进行带凭据线上请求。
- 目标设备、下载成品扩展、长期正式签名和性能预算未确认；不据此扩展 minSdk 或 ABI。

## 9. 新增数据和恢复边界
- Room schema 7 增加 download_tasks、media_progress、import_commits，不删除已有历史/下载记录。
- 导入先校验大小、结构、版本、类型与安全设置；Room 事务覆盖两个列表和提交凭证。
- 私有 AtomicFile 恢复日志记录导入前后安全设置；异常或下次启动按提交凭证恢复一致设置。
- 历史新增按 (type, resourceId) 事务去重；备份合并按时间和稳定身份去重。
- 下载 Worker 输入不保存临时媒体 URL；只有响应体完成并提交文件后才写入离线记录。
- 206 校验起始偏移、长度与验证器；200 重启；416 重置并有限重取，绝不直接判定成功。
- 续传没有验证器时从头下载；取消/暂停递增任务代次，旧任务不得写成完成。
- 导入路径不能授权删除、导出或播放任意应用文件，相关操作限于受管理下载目录。
- 进度和队列保存在应用数据库；搜索条件通过 SavedStateHandle 保存，不承诺任意深度进程死亡后恢复所有分页缓存。

## 10. CI 迭代记录
- 安全基线 7f81e5f：普通 Debug/Release、Docker Debug 与 JVM 回归成功。
- 完整批次 12e9599：三条构建均在共享卡片宽度的 Compose 接收器作用域处停止；已修正为外层捕获宽度，等待新 CI。
- 复查同时修复：输入未提交的草稿不改变翻页条件；加载态不触发提前翻页；下载完成后取消不覆盖提交结果；取消移除部分文件；磁盘空间检查；旧诊断导出和崩溃信息脱敏；刷新凭据确认为 401 时按会话代次失效。
