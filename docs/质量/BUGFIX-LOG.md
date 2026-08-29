# BUGFIX-LOG（缺陷汇总与修补记录）

> 规则：所有发现的 bug 先登记（编号/严重度/现象/根因/修复/验证）。
> 严重度：P0=核心功能不可用 / P1=功能异常但不崩溃 / P2=体验与工程质量 / P3= Cosmetic。

## 缺陷登记

### BUG-001 [P1→关闭·测试脚本缺陷] 昵称保存链路（App 无 bug）
- 现象：stage3b 实测——昵称对话框输入 GardenerQA 后界面无变化、profile.xml 未创建。
- 根因（stage3c 证据链定案，2026-08-28）：三处均为**测试脚本缺陷**：① 保存按钮实际在 y=1364，脚本 maxY=1200 将其排除；② keyevent 111(ESC) 直接关闭 Compose Dialog 丢弃输入；③ 未先点 EditText 聚焦即 input text。App 侧链条完整正常：点昵称值(318,310)→对话框→输入→保存(821,1012)→界面即时更新 + profile.xml 写入 nickname 成功。
- 修复：stage3c.ps1 采用"先点 EditText 再输入、不按 ESC、直接点保存、maxY 放宽"。
- 验证：stage3c PASS——profile.xml 含 nickname=园丁GardenerQA，界面即时显示。App 代码零改动。

### BUG-002 [P1→关闭·测试脚本缺陷] 头像循环（App 无 bug）
- 根因：① stage3 盲点(978,180)不是头像位置（头像在 ProfileBlock 左上 [81,325]）；② stage3b 探针正则不认 uiautomator 的 emoji 数字实体转义（🌱=&#127793;）。
- 修复：stage3c 用含 `&#\d+;` 的正则定位头像实体节点，点其中心。
- 验证：stage3c PASS——🌱→🍄 循环生效，profile.xml 写入 avatar_emoji=🍄。App 代码零改动。

### BUG-003 [P2] 测试脚本 CP() 函数报错噪声（测试侧）
- 根因：PS 5.1 中 `CP "abc" $bytes` 被解析为数组参数展开。改为内联 `[char]` 拼接后消失。
- 验证：stage2/3c 无 CP 报错。

### BUG-004 [P2] hint needle 未命中（测试侧，App 功能正常）
- 根因：提示文案「长按多选 → 堆肥」中箭头为 U+2192，脚本 needle 编码不匹配；且该 clickable 文本节点点击区域窄。
- 验证：M2 期间 App 功能本身已实测（点击提示可切换首张选中）；stage1 靠输入框兜底完成，不影响功能。

### BUG-005 [P1] design/fulltest 截图内容与命名可能不一致
- 现象：08-nickname.png 拍摄时昵称实际处于 FAIL 状态（违反"断言通过才拍"纪律）；01-home.png 在 stage1 第一次失败运行时拍的是首启页。历史 stage 截图可信度存疑。
- 状态：待全量重拍（断言后拍摄）并逐张核对。
- 修复：
- 验证：

### BUG-006 [P3] 测试脚本两次 back 会退出 App（导航栈行为）
- 现象：output→back→home→back→桌面。属 Android 标准行为（首页 back 退出），非 App bug；但自动化脚本需用 am start 重入而不是盲 back。
- 状态：记录备查，不改 App。

### BUG-007 [P3·已登记待v0.3] fallbackToDestructiveMigration 数据丢失风险
- 现象：AppModule.kt:31 Room 建库用 fallbackToDestructiveMigration()——未来 DB version 升级且未写 Migration 时会清空用户全部数据，违背「用户的思想首先属于用户」原则（specs/37）。
- 决策：v0.2.0（首个对外版本）前冻结 DB version=3 不再升版；v0.3 起逐版编写正式 Migration。已确认当前所有实体字段与 v3 schema 一致，风险暂不触发。

### BUG-008 [P3·已登记] WaitScreen 残留调试 Log.d
- 现象：WaitScreen.kt:79 有 Log.d("WaitScreen",...) 调试日志；CompostEngine/EcoEngine 亦有多处 Log.d。
- 决策：release 打包统一靠 ProGuard/R8 移除（或 v0.3 收敛到 BuildConfig.DEBUG 条件），不影响功能，不单独修。


### BUG-009 [P1→已修复] 产物页返回落入过期设置页
- 现象：堆肥完成后自动进入产物页，按返回键落到「堆肥设置页」（携带已过期的一次性参数），再返回才是首页；导航栈语义错误。
- 根因：AppNavHost.kt wait 路由 onDone 的 popUpTo(WAIT){inclusive=true} 只弹到等待页，产物页压在设置页之上。
- 修复：AppNavHost.kt:82-84 popUpTo(Routes.CRUMBS)——产物页返回直接回首页。
- 验证：full_test P8 场景 back→首页断言 PASS（run15/16）。

### BUG-010 [P2→已修复] 管理对话框文案与行为不符（「内置可删除」）
- 现象：文案写「内置菌种可删除」，实际内置菌执行的是隐藏（数据保留，可恢复）。
- 根因：M5 文案笔误；行为本身正确（SetupViewModel.deleteProbiotic 分流 deleteUserDefined/hideBuiltin）。
- 修复：SetupScreen.kt:290 文案改「内置菌种可隐藏；自定义菌种可编辑、可删除」；行内按钮 builtin 显示「隐藏」、custom 显示「删除」。
- 验证：截图 04-益生菌管理.png 新文案生效；隐藏/删除行为复测 PASS（run15/16 P4/P5）。

### BUG-011 [P1→已修复] 开炉后多选残留，选区条遮挡底栏
- 现象：面包渣多选后点「去堆肥」，堆肥已开始但首页选中状态未清；返回首页时选区条仍悬浮，遮挡底栏，点「堆肥」tab 误触选区条（run13 P8 失败根因，debug-p8 截图实锤）。
- 根因：CrumbsScreen.kt 去堆肥 onClick 未调用 exitSelection()。
- 修复：CrumbsScreen.kt:259-263 onClick 先 vm.exitSelection() 再 onSetup(state.selected.toList())（闭包先读后清，取值安全）。
- 验证：run15/16 P8 PASS（去堆肥→back→首页无选区条，tab 可正常切换）。

### BUG-012 [P3→已修复] 演示模式产物标题含引擎格式标记乱码
- 现象：mock 堆肥产物标题出现「idea:0（①}）」等碎片。
- 根因：MockLLM 标题 gist=用户输入首行原样回显，而引擎传入的首行含「面包渣清单」格式标记。
- 修复：AiRouter.kt:118 gist 清洗：去 idea:\d+ 标记与 {}①-⑤（） 等符号后 trim。仅影响演示模式；真实 API 走模型自拟标题不受影响。
- 验证：run16 截图 08/11 标题干净（「（初步）NoteToSelf-entropy-taste：机制还是叙事」）。

### BUG-013 [P1→已修复] 「测试连接」读取已保存配置而非对话框当前输入
- 现象：AI 服务商对话框内填好三字段直接点「🔌 测试连接」，VM 校验的是**已保存的** provider（空）→ 永远提示「请先填写接口地址、Key 和模型名」，对话框内新填的值未保存时无法测试。
- 根因：SettingsViewModel.testConnection() 无参读 _state；对话框本地 remember 字段未传入。
- 修复：签名改为 testConnection(baseUrl, apiKey, model, onResult)；SettingsScreen onTest 回调传对话框当前值（SettingsScreen.kt:121、574、607）。
- 验证：假配置 https://fake.local/v1 → ❌ 连接失败：Unable to resolve host "fake.local"（行内显示）；空字段按钮禁用态即引导；llm_calls 表写入 test_connection/error 遥测。

### BUG-014 [P2·测试侧备忘] 冷启动后底部手势排斥区高度不定
- 现象：emulator-5554 冷启动后 adb input tap 点 y≥2270 的按钮（去堆肥 2302/播下菌种 2255 边缘）间歇无效；同坐标另一时刻有效。上次会话 y=2282 可点，本次 2278/2288/2301 均无效、2255 有效。
- 根因：系统手势导航排斥区高度随启动状态变化，非 App bug（按钮 clickable/enabled 均正常，同屏其余按钮可点）。
- 对策：自动化点底部按钮前先 dump 取 bounds，优先 tap y=bounds.top+15；人工使用不受影响（手指按压面积大）。



### BUG-015 [P1·已修复] 冲突节横向排版灾难 + idea:N 标记泄漏
- 现象（用户真机截图）：冲突以两个 take(14) 截断 chip 横排（「idea:2 内部：人类'迫不」⚡「idea:2 内部：'很多人喜」），长文本被挤成竖排单字瀑布，小屏不可读；模型把引用标记写进了冲突文本。
- 修复：OutputScreen 冲突节重做为纵向 ConflictCard——nature 标签、甲/乙两方上下对垒（20dp 圆徽 + weight(1f) 多行文本）、居中 ⚡ 分隔线、化解线索；cleanConflictText 剥离 idea:/insight:/claim: 标记与「内部：」前缀。适配任意屏宽与文本长度。
- 验证：fake server 产物故意携带「idea:0 内部：」前缀，产物页渲染为清洗后的自然语言（截图 25-v03冲突卡.png）。

### BUG-016 [P1·已修复] S3 发酵无并发上限、失败静默丢弃
- 现象：S3 已是 async 扇出（700ms 错峰）但无并发上限（7 菌全开易触发供应商限流）；调用失败被 mapNotNull 静默吞掉（无重试、无记录、整合不知情）。
- 修复（specs/41）：Semaphore(4) 硬上限 + 完成补位；失败重试 2 次（指数退避 2s/4s + 抖动）后落库 {"ok":false,"agent","error"} 缺席标记，本轮继续；逐菌进度经 CompostProgressBus 上报，等待页显示「第 n 轮 · x/y 菌已归位」。
- 验证（fake OpenAI + adb reverse）：r1 四菌错峰并发、第 5 菌在任一完成 200ms 内补位；注入 6×500 后第 7 次（第 3 次引擎尝试）成功；另一菌 9×500 后跳过、轮次照常完成；fake_server.log 全程留痕。

### BUG-017 [P2·已修复] deep 深度与 standard 实现完全相同
- 现象：引擎 rounds 只区分 shallow（2 轮），standard 与 deep 同为 r1-r3；设计文档（03 §6.1/stages.md）明确深模式=标准 3 轮+独立魔鬼代言人轮；等待页却按 deep=4 轮提示。
- 修复：deep = r1,r2,r3,r4；新增「轮 4 · 魔鬼代言人」阶段 prompt（participantsFor(r4)=method 菌）；SetupScreen 深度选项改为轮数（2/3/4 轮），时长估计删除（用户实测中度 20 分钟与旧提示不符）；等待页轮数按深度精确映射（r 编号是 prompt 标识非序号，浅度 r3 显示为「第 2 轮」）。
- 验证：standard 全程 r1/r2/r3 无 r4；setup 页显示 2 轮/3 轮/4 轮。

### BUG-018 [P1·已修复] parseStages 轮 4 小节覆盖 r3 prompt
- 现象：GardenerPrompts.parseStages 只识别「### 轮 1/2/3」，新增轮 4 小节后 sub 停留在 r3，轮 4 的 fence 文本被写入 out["r3"]——标准模式第 3 轮错误使用魔鬼代言人 prompt（首次 fake 验证轮 r3 请求 system 含「发酵轮 4」暴露）。
- 修复：补 `### 轮 4 -> sub = "r4"` 分支。
- 验证：重跑全流程，r3 请求正确携带轮 3 prompt，server 判定 r1/r2/r3 序列无误。

### BUG-019 [P2·已修复] cleartext HTTP 被系统拦截
- 现象：用户填 http:// 本地/内网接口时测试连接报「CLEARTEXT communication not permitted」（Android 9+ 默认禁明文）；v0.2 烟测用的 https 假域名未暴露此问题。
- 修复：Manifest application 加 usesCleartextTraffic="true"（用户自填自担；主流服务商均为 https）。
- 验证：http://127.0.0.1:8765（adb reverse）测试连接 ✅「连接成功：模型回复「成功」」。


## 修复记录

- 2026-08-28 BUG-001/002/003/004 关闭：App 侧零改动，全部为测试脚本缺陷（maxY 排除/ESC 丢输入/未聚焦输入/正则不认 emoji 实体）；stage3c.ps1 证据链全通过（头像循环+昵称持久化实测）。
- 2026-08-28（发布版烟测）BUG-013 修复并验证；BUG-014 登记为测试侧备忘。真 AI 直连路径全链路实测：无 Key 开炉引导 → 假配置 ❌ DNS 报错行内显示 → 发酵中断页（错误详情+重新点火）→ 遥测 test_connection/error 落库。

## 结论

**审计终版（2026-08-28）**：共登记 14 个缺陷——P0 0 个 / P1 5 个（BUG-001/002 测试侧、BUG-005 已由回归重拍关闭、BUG-009/011/013 已修复）/ P2 5 个（BUG-003/004/014 测试侧、BUG-010 已修复、BUG-006 记录备查）/ P3 4 个（BUG-007/008 登记待v0.3、BUG-012 已修复）。
- **App 真 bug 共 5 个（009/010/011/012/013），全部修复并经回归验证**；其余 9 个为测试脚本缺陷或已登记的工程决策。
- 回归验证：full_test.py 36 项断言 36/0 全绿（连续两轮 run15/run16）；截图 14/14 名实相符（design/回归截图/01-14）。
- 发布版烟测（演示模式删除后）：9/9 通过——图片 chip 移除 / 瀑布流等高卡片+badge 单行 / 无 Key 开炉引导 / 测试连接（禁用+❌DNS 报错） / 导出 zip（manifest+profile+provider+8 表） / pm clear 后导入全量恢复（昵称·菌群活力·面包渣·堆肥产物） / 捐赠对话框（无图态+放入收款码入口） / 等待页（诚实暂停文案+冥想一句+屏幕常亮） / 真 AI 直连失败链路（发酵中断页+重新点火）。
- 构建产物：app/build/outputs/apk/debug/app-debug.apk + apk/release/app-release.apk（12.3MB，签名 keystore 已配置）。

## v0.3 回归与发布（2026-08-29）

- 2026-08-29 BUG-015/016/017/018/019 修复并经 fake OpenAI 服务端到端验证（adb reverse tcp:8765）：并行并发 4+补位 / 重试 2 次退避后成功 / 重试耗尽跳过不整轮失败 / 熄屏 12s 发酵续跑（前台服务+进度通知）/ 冲突卡纵向排版+文本清洗 / 轮数提示准确 / 深度=轮数对齐设计 / 本地 http 可连。
- 输入页三修（用户 P3）：粘贴 chip 移除；未发酵 badge 独占底行（不再与日期同行挤压截断）；日期移至卡片最上方。
- 捐赠语义修正（用户 P2）：收款码为作者内置只读资产，用户仅可「保存收款码图片」到相册 Pictures/IdeaCompost（MediaStore，实测文件落盘）。
## v0.3.1 回归与发布（2026-08-29）

### BUG-020 [P1·已修复] 已发酵 badge 显示不出来（胶囊被裁成空壳）
- 现象：v0.3 将 badge 改为独占底行后，168dp 固定卡高装不下全部内容（28 padding + 16 日期 + 6 间距 + 105 正文5×21 + 22 badge = 177dp > 168dp），badge 行被压缩、文字垂直裁切——视觉上只剩空胶囊；「未发酵」恰好看似正常（用户实测截图实锤）。
- 修复：CrumbCard 高度 168→190dp（等高长矩形，内容+badge 完整露出）。
- 验证：导入桩数据（raw+composted），两种 badge 节点树与截图均完整（design/回归截图/29）。

### 面包渣页滚动重构 + 回到顶部（用户需求）
- 顶栏（思想堆肥 + IDEA COMPOST + 头像）固定不随滚动；大标题/输入框/节标题并入 LazyVerticalStaggeredGrid 首个整行 item（FullLine span），跟随卡片一起滚动；空态保持原布局。
- 输入框完全滚出视野（firstVisibleItemIndex ≥ 1）后，底部居中浮现「↑ 回到顶部」胶囊按钮（BottomBar 上方 92dp），点击 animateScrollToItem(0) 平滑回顶；多选操作条显示时自动隐藏。
- 验证（10 颗桩渣）：上滑后输入框滚出、顶栏固定、按钮出现；点按钮回到顶部且按钮消失（截图 30）。

### 安全事件：签名密钥泄露与轮换（仓库公开后发现）
- 事件：ideacompost-release.keystore 曾于审计收尾 commit 提交入库；仓库转公开后全历史扫描（git log -S）确认 keystore 文件与同期 build.gradle.kts 中硬编码的签名密码均在公开历史中可见。
- 处置：①git filter-repo 从全部历史抹除 keystore 并强推重写；②旧 keystore 作废（本地删除）；③生成全新 ideacompost-v2.keystore（随机 18 位密码，仅存 local.properties，gitignore 覆盖）；④v0.3.1 起 Release 用新签名——**旧版本升级需卸载重装一次（签名不同不可覆盖安装）**。
- 其余敏感项复核结论：API Key 仅存设备侧（不进仓库）；fake server 的 sk-fake 为假值；local.properties 与 private/ 从未被跟踪；收款码为作者主动公开资产，无风险。
- 备注：GitHub 侧旧 commit 对象在缓存过期前或仍可按 hash 直达，但密钥已作废，无实际风险。

- 构建产物：versionName 0.3.1 / versionCode 4；debug + release APK（新签名）。

## v0.3.2 回归与发布（2026-08-29）

### BUG-021 [P0·已修复] 非空面包渣页整页布局坍塌（header 元素互叠）
- 现象：v0.3.1 起，面包渣列表非空时，大标题/输入卡/节标题全部叠绘在 grid 视口顶部（用户真机截图实锤）；空态布局正常。
- 根因：`InputHeader` 没有根容器——Spacer/Text/输入卡/节标题直接平铺为多个顶层子元素。空态分支中它恰处于外层 Column（顺序布局语义）故侥幸正确；非空分支进入 Lazy 布局的 item（Box 语义）后，所有顶层子元素全部以 cell 原点为基准叠放。与 staggered/grid 具体实现无关（换 LazyVerticalGrid 后坐标逐像素复现）。
- 修复：InputHeader 整体包 `Column(Modifier.fillMaxWidth())`；顺带从 LazyVerticalStaggeredGrid 迁移到 LazyVerticalGrid（Fixed(2) + GridItemSpan(maxLineSpan) 整行 item），瀑布视觉不变、行为更可控。
- 验证（模拟器，30 颗桩渣经备份导入通道灌入）：非空页排版正常（31-debug_grid3）；滚动+顶栏固定（32）；回顶按钮浮现/点击回顶/按钮消失（33）；删除断点：卡片→对话框→「🗑 删除本条」→「再点一次，确认删除」两步确认→条目消失列表即时刷新（34/35/36）。

### 新功能：面包渣删除
- 点击面包渣卡片打开「修一修这颗面包渣」对话框，新增「🗑 删除本条」入口，两步确认防误删（第一次点击变为「再点一次，确认删除」，重开对话框自动复位）。
- 数据链路：IdeaDao.deleteById（DELETE FROM ideas WHERE id=:id）+ CrumbsViewModel.deleteCrumb 落 bed_events（action=idea_deleted）。
- 验证：删除后瀑布流即时刷新、已删条目不再出现。

- 构建产物：versionName 0.3.2 / versionCode 5；release APK（v2 签名，可直接覆盖安装 v0.3.1）。
