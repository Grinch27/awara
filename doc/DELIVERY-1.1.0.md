# Awara 1.1.0 改造交付记录

日期：2026-10-30。方向：保留 Material 3，采用更紧凑的信息呈现；保留既定导航、七类搜索及只读论坛边界。

## 构建与下载

主分支源码提交：`0faa3d9c9b9d00dd3b39e62763ec3309999d7282`。
主分支普通构建：[Build APK](https://github.com/Grinch27/awara/actions/runs/40936476313)。
主分支容器构建：[Build APK Docker](https://github.com/Grinch27/awara/actions/runs/40936476432)。
最终状态：普通 Debug、Release / R8、Docker Debug 及全部 JVM 回归均成功。

- [下载主分支 Debug APK Artifact](https://github.com/Grinch27/awara/actions/runs/40936476313/artifacts/12925597225)
- [Release 构建 Artifact（未配置正式签名）](https://github.com/Grinch27/awara/actions/runs/40936476313/artifacts/12925950989)
- [JVM 测试与 Room schema 报告](https://github.com/Grinch27/awara/actions/runs/40936476313/artifacts/12925148108)
- [Docker Debug Artifact](https://github.com/Grinch27/awara/actions/runs/40936476432/artifacts/12926056151)

APK Artifact 保留 30 天，测试报告保留 7 天。之后的文档和编译器 schema 归档不改变上述已构建的应用源码。

已通过的整体验证基线 `bb61739`：

| 路线 | 结果 | 证据 |
| --- | --- | --- |
| 普通 Debug 与 JVM 回归 | 成功 | [运行记录](https://github.com/Grinch27/awara/actions/runs/40935175120) |
| 普通 Release / R8 | 成功 | 同上 |
| Docker Debug | 成功 | [运行记录](https://github.com/Grinch27/awara/actions/runs/40935175091) |

从成功的普通构建页面下载 Artifacts 中的 `app-debug`，解压后安装 APK。
`app-release` 为尚未配置正式签名的发行构建，不作为可直接安装的正式发行包。
应用 ID 保持 `me.rerere.awara`；versionCode 10，versionName 1.1.0；minSdk 26、targetSdk 33、compileSdk 37；ABI 仍为 arm64-v8a / x86_64。

## 已落地的改进

| 范围 | 行为 | 对应实现 |
| --- | --- | --- |
| 信息密度 | 共用紧凑 M3 卡片；标题、作者、日期、计数层级；宽度适配 1–6 列；关键搜索入口至少 48dp | core:ui、媒体适配器、搜索与历史 |
| 封面隐私 | 阻止加载时不创建图片请求；模糊独立于阻止；播单和下载采用同一规则；会话缓存隔离 | MediaThumbnail、CompactMediaCard、UserStore |
| 搜索 | 七分区保持；请求快照与取消；拒绝迟到结果；稳定 ID 去重；空页和重复页停止；翻页失败可重试 | SearchVM、SearchControls、SearchResults |
| 搜索恢复 | 保存已提交条件；草稿不改变翻页条件；查询身份切换时重置列表状态，导航返回使用 Compose 状态保存；历史可暂停、逐条删除或清空 | SavedStateHandle、SearchPage |
| 登录会话 | 密码不落盘；迁移旧凭据；Keystore AES/GCM；退出代次保护；刷新合并和冷却；只读 401 最多重放一次 | SecureSession、SessionRepository、NetworkModule |
| 诊断与链接 | 限制日志中的凭据、媒体路径和原始异常；历史日志导出再脱敏；外部链接校验 | AppLogger、AppLogRepo、ContextUtil |
| 备份恢复 | 16 MiB / 50,000 条限制；严格结构与版本；v2 兼容、v3 明确格式；导入预览；合并或替换；恢复日志维护跨存储一致性 | LocalDataRepo、SettingPage、import_commits |
| 播放与缓存 | 切换质量保留进度、速度与播放意图；有限过期源刷新及显式重试；视频进度；按内容/会话区分缓存；音频焦点 | PlayerState、PlayerCache、VideoVM、VideoPage |
| 图片阅读 | 空态、失败重试、稳定页 ID、缩放与翻页协调、阅读位置、有限预加载；取色限制响应和解码规模 | ImageVM、ImagePage、DynamicTheme |
| 持久下载 | ID 队列、并发上限 2、重新解析地址、未知总长、200/206/416 与验证器、部分文件、存储失败和有限重试 | DownloadTask、DownloadRange、DownloadScheduler、DownloadWorker |
| 下载管理 | 暂停/继续/取消、批量暂停/继续、离线播放进度、缺失文件提示、批量移除记录、单独选择删除文件、SAF 导出 | DownloadPage、DownloadVM、ManagedDownloadFiles |
| 本地数据 | Room 6→7 显式迁移；编译器导出 schema 7；历史按稳定资源身份写入；自动备份排除本地私有数据 | DatabaseModule、TransferDao、HistoryDao、备份 XML |
| 其他体验 | 历史记录可暂停、批量移除、确认清空；可选截图和最近任务预览保护；DLNA 回调和销毁受生命周期约束 | SettingPage、HistoryPage、RouterActivity、DlnaCast |
| 构建维护 | 保留自动稳定工具链策略与两条路线；接入必要 JVM 回归；失败不上传 APK；MMKV 改用维护中的 64 位制品 | Gradle 配置与 Actions |

安全 JSON 仅导出历史、下载元数据及明确的设置白名单；不包含媒体本体、下载任务、搜索历史、登录密码或 Iwara Token。
合并保留当前设置；替换只恢复白名单设置，其余偏好保留当前值。

## 验证证据的边界

CI 已包含刷新并发/失败/退出、取消传播、严格备份、范围响应及搜索竞争/翻页等 JVM 回归。
Room schema 7 来自成功 CI 的编译器输出，不是手工推导。
构建和单元测试不代表真机、真实账号、CDN、DLNA 或所有迁移中断点通过；本次未执行真实设备安装和性能测量。

下载按创建时的登录会话隔离，进程重启可继续同一会话的任务；退出后旧私有任务不会自动绑定到新会话。
备份只恢复文件元数据，跨设备路径可能失效，界面会提示文件不可用。
尚未声明 16 KiB 设备兼容或任何性能提升百分比。

## 后续路线

1. 优先完成实际设备验收：窄屏/平板/大字体、登录迁移、Room 旧数据升级、导入中断、通知与后台下载、真实 CDN 的续传和源过期、播放焦点与 DLNA。
2. 使用既定长期签名完成正式发行配置，再做安装与升级验收；不临时创建用户未确认的永久签名身份。
3. 根据验收结果补充故障恢复测试和可测量的性能预算；验证长期队列与退出后遗留部分文件的清理策略，以及每个搜索分区的深度分页/滚动恢复。
4. 用户选择后才推进 HLS/DASH 离线包装、轻量预设、本地收藏、应用锁、后台播放/PiP 或其他 K6 扩展。

本轮不把原比较报告的全部 56 项标为完成。逐项实际状态与约束见 [实施基线](module-subscription-plan.md)，运行验收范围见 [设备验收记录](ANDROID-RUNTIME-SMOKE.md)。
