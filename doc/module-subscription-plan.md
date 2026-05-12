<!--
当前已确认事实：
1. 默认入口页面现在允许 subscription、video、image、forum；若未登录导致 subscription 不可用，只做运行时回退，不主动改写用户偏好。
2. 搜索页头部已经固定为三层结构：返回键+搜索框 / video-image-user 圆角标签 / 日期下拉或 user 模式提示。
3. forum 和用户页“留言”都已经切到浏览器回退页，真实页面路径不允许再出现 Still in developing。
4. 评论区已经切到高对比深色 panel；若后续要再做主题联动，必须先经过人工视觉验收。
5. 本仓库禁止本地构建 APK；所有构建验证都必须通过 .github/workflows/build-apk.yml 完成。

后续仍需人工决策的事项：
1. forum 是否继续保留浏览器回退，还是进入原生论坛流开发。
2. 用户页留言是否补真实 API，还是长期保留浏览器回退。
3. 搜索页 user 模式是否长期与 video/image 并列，还是未来拆成独立入口。
-->

# Awara 导航、搜索、评论与 CI 执行蓝图（2026-05-12）

这份文档面向三类读者：

1. 工程负责人：快速判断当前实现是否已经移除占位页、是否还有明显回退风险。
2. 开发者：直接定位控制默认入口、搜索头部、评论样式和用户回退页的关键文件。
3. GPT/Agent：拿到仓库上下文后，直接进入“定位首个阻塞点 -> 最小改动 -> push -> 触发 build-apk.yml -> watch -> 只修首个错误”的闭环。

## 1. 硬约束

### 1.1 构建与提交流程

1. 不在本地构建 APK。
2. 所有 APK 验证都必须通过 .github/workflows/build-apk.yml。
3. 每次只修当前首个阻塞错误，不顺手扩大改动面。
4. 每次推送后都要重新触发 workflow，并持续监控到成功或出现新的首个阻塞错误。

### 1.2 隐私与本地调试

1. 本地接口调试只允许在终端进行。
2. 不落盘 token、cookie、Authorization 头或账号密码。
3. 输出只保留脱敏摘要：接口是否通、状态码、字段是否存在、播放链路走到哪一步。
4. 文档、提交说明、错误摘要里都不能粘贴敏感凭据。

### 1.3 代码修改边界

1. 默认优先修根因，不做纯表面绕过。
2. 不允许把真实页面路径再导回 TodoStatus。
3. 如果只能提供过渡方案，必须是可操作回退页，而不是“still in developing”。
4. UI 微调优先在现有 Compose 组件层完成，不无故改 repo/API 层。

## 2. 当前基线

### 2.1 导航与默认入口

1. 首页壳已经只在真实内容页之间切换。
2. 默认入口现在支持 subscription、video、image、forum。
3. setting/history/download 继续走外部路由，不再作为首页壳内部 section。
4. 若恢复状态或旧偏好把首页壳带到外部入口名称，壳层会回退到真实内容页，不再落到占位页。
5. forum 仍然是浏览器回退页，但它已经是可操作页面，不再显示开发中占位文案。

### 2.2 搜索页

1. 搜索头部已经贴近 EhViewer 的三行结构。
2. 第一行是返回键 + 搜索框。
3. 第二行是 video、image、user 三个更厚实的圆角标签按钮。
4. 第三行在媒体搜索时显示日期下拉，在 user 搜索时显示 user 模式提示条，避免头部塌成两行。
5. SearchVM 的 user 搜索分支已经重新暴露到 UI，不再被强制退回 video。

### 2.3 评论区

1. 评论卡片、回复上下文、标题条和回复栏已经统一成高对比深色 panel。
2. 正文、昵称、次级信息现在都有显式前景色，不再依赖动态主题下的近色默认值。
3. 评论区目前的视觉目标是“优先可读”，不是继续追求强动态主题联动。

### 2.4 用户页与占位页收口

1. 用户页第三个标签“留言”不再显示 TodoStatus。
2. 当前实现使用浏览器回退页，指向 Iwara 个人页。
3. 仓库内 TodoStatus 仅保留在公共状态组件和预览中，不再被真实页面路径调用。

## 3. 关键文件地图

### 3.1 默认入口与首页壳

1. app/src/main/java/me/rerere/awara/ui/page/index/layout/IndexPagePhoneLayout.kt
2. app/src/main/java/me/rerere/awara/ui/page/index/layout/IndexPageTabletLayout.kt
3. app/src/main/java/me/rerere/awara/ui/page/index/IndexPage.kt
4. app/src/main/java/me/rerere/awara/ui/page/setting/SettingPage.kt

关注点：

1. defaultEntryNavigations 是否只包含真实内容页。
2. 当前选中项是否会被外部入口名称污染。
3. 默认入口偏好在未登录场景下是否只做运行时回退。

### 3.2 搜索页

1. feature/search/src/main/java/me/rerere/awara/ui/page/search/SearchPage.kt
2. feature/search/src/main/java/me/rerere/awara/ui/page/search/SearchVM.kt
3. feature/search/src/main/res/values/strings.xml

关注点：

1. user 模式不能再被 LaunchedEffect 强行改回 video。
2. 三行头部结构不能退回四行或两行。
3. 视觉细节只在 SearchPage 组件层收敛，除非确有数据或分页问题。

### 3.3 评论区

1. app/src/main/java/me/rerere/awara/ui/component/iwara/comment/CommentCard.kt
2. app/src/main/java/me/rerere/awara/ui/component/iwara/comment/CommentList.kt
3. app/src/main/java/me/rerere/awara/ui/page/video/pager/VideoCommentPage.kt

关注点：

1. 评论正文、昵称、时间、回复按钮的前景色是否始终可读。
2. 深色 panel 是否和回复栏、空态、线程上下文保持同一套层级。
3. 不要为了评论样式去动评论 repo 或 VM。

### 3.4 用户页浏览器回退

1. app/src/main/java/me/rerere/awara/ui/page/user/UserPage.kt
2. app/src/main/res/values/strings.xml
3. app/src/main/res/values-zh-rCN/strings.xml

关注点：

1. 留言标签页必须是可操作回退页。
2. 没有 username 时不能生成错误 URL。
3. 不要重新引入 TodoStatus。

### 3.5 播放链路

只有在用户反馈“详情正常但不能播”时再进入这一层：

1. app/src/main/java/me/rerere/awara/data/repo/MediaRepo.kt
2. app/src/main/java/me/rerere/awara/di/RepoModule.kt
3. app/src/main/java/me/rerere/awara/ui/page/login/LoginVM.kt
4. feature/player/src/main/java/me/rerere/awara/ui/component/player/Player.kt
5. feature/player/src/main/java/me/rerere/awara/ui/component/player/PlayerState.kt

## 4. 当前验收标准

### 4.1 必须满足

1. 设置页可以选择 subscription 作为默认入口。
2. 首页壳不会因为旧状态或恢复状态落到 still in developing。
3. 搜索页能直接切换到 user，并展示用户结果。
4. 搜索头部保持三层结构。
5. 评论区在浅色/深色/动态主题下都能保持可读。
6. 用户页留言标签不再显示 TodoStatus。

### 4.2 可接受的过渡状态

1. forum 仍然是浏览器回退页。
2. 用户页留言仍然是浏览器回退页。
3. 搜索页视觉可继续微调，但不能牺牲结构稳定性和 user 搜索可达性。

### 4.3 不可接受的回归

1. 再次把 subscription 从默认入口选项中移除。
2. 再次隐藏 user 搜索入口。
3. 任何真实页面路径重新调用 TodoStatus。
4. 评论区回到“文字和底色相近”的状态。

## 5. 本地验证顺序

在不做本地 APK 构建的前提下，优先使用以下顺序：

1. 对改动文件执行 IDE 错误检查。
2. 用全文搜索确认 TodoStatus 不再被真实页面引用。
3. 只在需要排查播放/接口问题时做本地脱敏 API 检查。
4. 本地验证结束后立即提交并走 GH Actions。

如果要做播放相关脱敏检查，建议顺序如下：

1. 登录接口。
2. token 刷新接口。
3. 视频详情接口。
4. manifest 接口。
5. 实际 stream URL。
6. ExoPlayer 事件阶段。

输出要求：

1. 只写状态码、链路阶段、是否拿到关键字段。
2. 不写原始凭据。
3. 不记录可复用敏感头部。

## 6. GH Actions 执行闭环

标准流程：

1. git status --short --branch
2. git add 相关文件
3. git commit -m "一句话提交说明"
4. git push origin 当前分支
5. gh workflow run build-apk.yml --ref 当前分支
6. gh run watch --interval 10 --exit-status
7. 若失败，先看 gh run view --log-failed
8. 若 run 级日志为空，降级到 gh run view --job <job_id> --log
9. 只修首个阻塞错误，然后重复步骤 2 到 8

重要细节：

1. gh workflow run 之后，gh run list --limit 1 可能仍返回旧 run。
2. 必须用 headSha 或最新提交 SHA 确认自己看的就是新 run。
3. watch 过程中不要并行修第二个问题；先让当前 run 的首个 blocker 消失。

## 7. 推荐的 Agent 行为

拿到这份文档后，默认执行策略应当是：

1. 先确认用户报的是结构回归、视觉回归还是 CI 回归。
2. 只读直接控制行为的本地文件，不先做大范围地图式搜索。
3. 形成一个可被快速证伪的局部假设后，立刻做最小改动。
4. 第一次实质改动后，先做文件级错误检查，再继续下一刀。
5. 代码改完后必须走 GH Actions 验证。

遇到以下情况时的默认动作：

1. 搜索页切不到 user：先看 SearchPage.kt，不要先动 VM。
2. 默认入口又出问题：先看 IndexPagePhoneLayout.kt、IndexPageTabletLayout.kt、SettingPage.kt。
3. 评论区看不清：先看 CommentCard.kt、CommentList.kt，不要先动主题系统。
4. 又出现 still in developing：先全文搜索 TodoStatus 的真实页面调用点。

## 8. 当前剩余风险

1. forum 和用户留言仍是浏览器回退，不是原生 feed。
2. 搜索页和评论区的最终视觉质量，仍建议人工真机看一轮。
3. 本地没有做 APK 构建，因此最终稳定性仍以 GH Actions 结果为准。
4. 如果后续重新引入强动态主题联动，评论区高对比方案可能需要再次锁定局部固定色。

## 9. 下一轮优先级

1. 让当前改动先通过 build-apk.yml，并以首个阻塞错误为唯一修复目标循环到成功。
2. 若用户继续要求“更像 EhViewer”，优先做搜索头部和评论区的视觉微调，不先改数据层。
3. 若用户要求彻底去掉浏览器回退，优先补 forum 原生流，再评估用户留言 API。
4. 只有在再次出现“详情正常但不能播”时，才重新打开播放链路诊断工作面。

## 10. 交付时必须汇报的内容

1. 这次改了什么用户可感知行为。
2. 哪些 still in developing 路径已经被彻底收口。
3. 做了哪些本地静态验证。
4. GH Actions run 编号、结果、首个阻塞错误是否已清零。
5. 还剩下哪些真实风险，而不是泛泛而谈的“可能有问题”。
