# BUGFIX-LOG（缺陷汇总与修补记录）

> 规则：所有发现的 bug 先登记（编号/严重度/现象/根因/修复/验证）。
> 严重度：P0=核心功能不可用 / P1=功能异常但不崩溃 / P2=体验与工程质量 / P3= Cosmetic。

## 缺陷登记

### BUG-001 [P1] 个人页昵称保存后不生效（profile.xml 未创建）
- 现象：stage3b 实测——点开昵称对话框→输入 GardenerQA→按保存→界面无 GardenerQA、`shared_prefs/profile.xml` 不存在 → 昵称从未持久化。
- 定位：ProfileStore.kt 代码正确（写 "profile" prefs）；SettingsViewModel.kt:73 updateNickname 写 profile.nickname 正确；昵称对话框 UI 接线（SettingsScreen.kt:107-112）看似正确。疑点：测试脚本 keyevent 111（ESC）在 Compose Dialog 中可能直接 dismiss 对话框（丢弃输入），或"保存"按钮点到了别处。
- 状态：待证据链调试。
- 修复：
- 验证：

### BUG-002 [P1] 个人页头像点击循环未验证通过
- 现象：stage3 点 (978,180) 无反应（头像实际在 ProfileBlock 左上角）；stage3b 探针正则 `[^"\u4E00-\u9FFF]{1,4}` 无法匹配 uiautomator 输出中 emoji 的 `&#127793;` 数字实体转义 → 没找到节点、没点成。App 侧 cycleAvatar 逻辑未实测。
- 状态：待修（App 侧待验证，测试侧正则需容忍 &#nnnnnn; 实体）。
- 修复：
- 验证：

### BUG-003 [P2] 测试脚本 CP() 函数报错噪声
- 现象：CP @(0x....) 每次调用产生"找不到路径"错误输出；needle 仍构造成功（9/9 断言通过），但污染日志。
- 根因：PS 5.1 中函数名 CP 与内置解析冲突/数组字面量被当作命令参数展开（未细查，测试侧问题，不影响 App）。
- 状态：待修（改用 JoinChars 或直接内联 [char] 拼接）。
- 修复：
- 验证：

### BUG-004 [P2] stage1 fail=2：长按多选提示文案 needle 未命中
- 现象：home 页提示节点真实文案为 `长按多选 → 堆肥`（dump 实证），测试用 needle "多选" 应该能匹配…但 TapText 返回 false（fail 计数），最终靠盲点 (540,647)+丢进去兜底成功。
- 根因：待查（可能是可点击节点与文本节点分离，TapText 点中文本节点无效果或超时；属测试侧）。
- 状态：待修（测试侧；App 侧"提示可点击切换首张选中"功能本身此前 M2 实测通过）。
- 修复：
- 验证：

### BUG-005 [P1] design/fulltest 截图内容与命名可能不一致
- 现象：08-nickname.png 拍摄时昵称实际处于 FAIL 状态（违反"断言通过才拍"纪律）；01-home.png 在 stage1 第一次失败运行时拍的是首启页。历史 stage 截图可信度存疑。
- 状态：待全量重拍（断言后拍摄）并逐张核对。
- 修复：
- 验证：

### BUG-006 [P3] 测试脚本两次 back 会退出 App（导航栈行为）
- 现象：output→back→home→back→桌面。属 Android 标准行为（首页 back 退出），非 App bug；但自动化脚本需用 am start 重入而不是盲 back。
- 状态：记录备查，不改 App。

## 修复记录

（每次修复追加：日期 / 改动文件 / 改动内容 / 验证方式与结果）

## 结论

（审计完成后填写总表：共 X 个缺陷，P0 x 个 / P1 x 个 / P2 x 个，全部修复后附回归测试结果）
