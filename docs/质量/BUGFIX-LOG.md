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

## 修复记录

- 2026-08-28 BUG-001/002/003/004 关闭：App 侧零改动，全部为测试脚本缺陷（maxY 排除/ESC 丢输入/未聚焦输入/正则不认 emoji 实体）；stage3c.ps1 证据链全通过（头像循环+昵称持久化实测）。

## 结论

（审计完成后填写总表：共 X 个缺陷，P0 x 个 / P1 x 个 / P2 x 个，全部修复后附回归测试结果）
