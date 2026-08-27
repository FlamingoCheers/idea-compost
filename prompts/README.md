# Prompt 卡全集 · 总则

> 实操设计 ①。本文集定义首启全部 17 个 Agent 的 capability_card、6 个内置益生菌、思想园丁系统 Prompt 与五阶段调度指令。
>
> 对齐：`docs/v0.2-设计文档/01 §4.2`（字段）、`03 §6.2`（调用契约）、`04 §1`（益生菌本体）。

## 目录

```text
agents/domains.md      7 领域菌（首启 7 选 N，选中 V=50，未选 dormant）
agents/methods.md      6 方法菌（通用底座，V=40，人人必有）
agents/creators.md     4 创造菌（通用底座，V=40，人人必有）
probiotics.md          6 内置益生菌（global_builtin，不可删除可隐藏）
gardener/system.md     思想园丁系统 Prompt（身份 + 禁止清单）
gardener/stages.md     五阶段调度指令（识别/召集/发酵三轮/整合/评估）
```

## 卡片格式

每张卡 = 元数据（YAML）+ capability_card 全文（text 块）：

```yaml
id / type / name / description
init_vitality / seed_status        # 首启语义（02 §1）
specialties                        # 初始为空，增殖时习得
```

capability_card 三段式：**系统提示**（我是谁，从什么世界看问题）→ **思考准则**（4-6 条，该菌的方法论人格）→ **范例**（一小段体现其声音的少样本示例）。

## 调用契约（03 §6.2）

```text
system = capability_card + 调度指令（轮次目标 + 输出 schema）
user   = 上下文包（同轮统一）+ 前轮输出（轮2起）
```

调度指令（含输出 schema）在 `gardener/stages.md`，不写进能力卡——能力卡只承载菌的「人格」，调度指令承载流程。增殖/融合生成新菌时，由园丁以父卡为模板合成新 capability_card（02 §5.2），本文集只维护种子菌群。

## claim 与引用规则（全菌通用，D21）

- 每场堆肥，编排层为每个召集菌分配唯一短码（如 `PHI` `REV` `HYP`），claims 编号 `{短码}{n}`（PHI1、REV2…）
- `citations` 只允许三类，且**必须真实存在于上下文**：`idea:{id}` / `insight:{id}` / `claim:{id}`
- 引用校验失败 → 该段输出整体丢弃并记质检指标（幻觉溯源=硬错误，03 §6.2）
- confidence 三档：`emerging | supported | strong`，措辞规则见 05 §2

## token 预算

| 项 | 预算 |
| --- | --- |
| 领域/方法/创造菌能力卡 | 300-600 token/张 |
| 园丁系统 Prompt | ≈800 |
| 每轮调度指令 | ≈300 |
| 益生菌注入（prompt_logic） | ≈200/个 |
| 压缩卡（休眠态） | 150-250（由活跃卡蒸馏，非本文集维护） |

## 语言与版本

- 所有卡片以中文撰写；面向用户面包渣为其他语言时，卡片原则不变，输出语言跟随面包渣
- 卡片即数据（INV-6）：入库 Room，随 App 版本只增不改；修改已有卡需评估与存量菌床的一致性（老菌的 contribution_history 绑定旧卡版本）
