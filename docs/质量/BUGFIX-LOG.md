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


## 修复记录

- 2026-08-28 BUG-001/002/003/004 关闭：App 侧零改动，全部为测试脚本缺陷（maxY 排除/ESC 丢输入/未聚焦输入/正则不认 emoji 实体）；stage3c.ps1 证据链全通过（头像循环+昵称持久化实测）。
- 2026-08-28（发布版烟测）BUG-013 修复并验证；BUG-014 登记为测试侧备忘。真 AI 直连路径全链路实测：无 Key 开炉引导 → 假配置 ❌ DNS 报错行内显示 → 发酵中断页（错误详情+重新点火）→ 遥测 test_connection/error 落库。

## 结论

**审计终版（2026-08-28）**：共登记 14 个缺陷——P0 0 个 / P1 5 个（BUG-001/002 测试侧、BUG-005 已由回归重拍关闭、BUG-009/011/013 已修复）/ P2 5 个（BUG-003/004/014 测试侧、BUG-010 已修复、BUG-006 记录备查）/ P3 4 个（BUG-007/008 登记待v0.3、BUG-012 已修复）。
- **App 真 bug 共 5 个（009/010/011/012/013），全部修复并经回归验证**；其余 9 个为测试脚本缺陷或已登记的工程决策。
- 回归验证：full_test.py 36 项断言 36/0 全绿（连续两轮 run15/run16）；截图 14/14 名实相符（design/回归截图/01-14）。
- 发布版烟测（演示模式删除后）：9/9 通过——图片 chip 移除 / 瀑布流等高卡片+badge 单行 / 无 Key 开炉引导 / 测试连接（禁用+❌DNS 报错） / 导出 zip（manifest+profile+provider+8 表） / pm clear 后导入全量恢复（昵称·菌群活力·面包渣·堆肥产物） / 捐赠对话框（无图态+放入收款码入口） / 等待页（诚实暂停文案+冥想一句+屏幕常亮） / 真 AI 直连失败链路（发酵中断页+重新点火）。
- 构建产物：app/build/outputs/apk/debug/app-debug.apk + apk/release/app-release.apk（12.3MB，签名 keystore 已配置）。
