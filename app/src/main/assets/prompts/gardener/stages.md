# 思想园丁 · 五阶段调度指令

> 对齐 `docs/v0.2-设计文档/03`（阶段流程）与 `05`（产物结构）。每阶段一次 LLM 调用（发酵轮除外），system = `gardener/system.md` + 本文件对应阶段指令；阶段产物即 checkpoint（D17）。
>
> 占位符 `{{ctx}}` 由编排层填充；`{{}}` 之外的文字原样进入 Prompt。

------

## S1 · 识别 Identify

```text
—— 本次任务：识别 ——

你要处理一批用户面包渣。不要评判，不要整理，不要建议。只做一件事：
理解这批碎片「在想什么」，以及它们之间尚未言明的张力。

重点：
- tensions：碎片之间未言明的矛盾与拉扯——这是后续冲突轮的弹药，
  找得越准，发酵越有劲。
- premises：某条碎片隐含了但从未检验的假设——挖到第二层
  （假设的假设优先）。
- bed_links：只允许引用上下文中真实存在的 insight 编号，宁缺勿滥。

输出严格 JSON，不要其他文字：
{
  "gist": "这批碎片共同指向的问题（1-2 句）",
  "potential_domains": ["…"],
  "potential_methods": ["…"],
  "tensions": ["碎片A与碎片C在X上存在未言明的张力"],
  "premises": ["碎片B隐含假设了Y，但Y未被检验"],
  "bed_links": [123],
  "suggest_depth": "shallow | standard | deep"
}
```

------

## S2 · 召集微调 Convoke

```text
—— 本次任务：召集复核 ——

代码已按激活权重与 70/20/10 配额（相关菌/相邻菌/意外菌）选出本场菌群：
{{gardener_plan}}

你的权限很小，这是刻意的：
- 核心配额（70% 相关菌）不许动。
- 你可以否决或替换至多 1 个意外菌，必须给出理由（理由写入
  gardener_plan 供审计）。
- 判断标准：这个意外菌与本次面包渣是否存在「表面无关、结构相关」的
  可能？若完全没有，可换；若只是「用户从没用过」，不构成否决理由——
  那正是它的职责。

输出严格 JSON：
{
  "vetoes": [{"replace": "菌名", "with": "菌名或null", "reason": "…"}],
  "note": "给编排层的一句话（可空）"
}
```

------

## S3 · 发酵 Ferment（三轮调度指令，附在各菌 capability_card 之后）

### 轮 1 · 理解拆解（全部召集菌并行）

```text
—— 发酵轮 1：理解与拆解 ——

你是本轮参与菌群之一，{{agent_name}}。以你自己的视角阅读上下文包中的
全部面包渣。不与其他菌商量（本轮互相不可见）。

从你的领域/方法出发回答：
1. 这批碎片里，你认为最核心的问题是什么？
2. 哪些关键概念在其中被使用？哪些被混用？
3. 有哪些隐含前提（至少挖两层）？

所有观点以带编号的 claim 输出（编号 {你的短码}{数字}），引用只允许
idea:{id} / insight:{id} / claim:{id} 且必须真实存在。

输出严格 JSON：
{
  "agent": "{{agent_name}}",
  "core_questions": ["…"],
  "key_concepts": ["…"],
  "implicit_premises": ["…"],
  "claims": [{"id": "{{CODE}}1", "text": "…", "confidence": "emerging|supported|strong"}],
  "citations": ["idea:3"]
}
```

### 轮 2 · 连接冲突（互看轮 1，方法菌主导）

```text
—— 发酵轮 2：连接与冲突 ——

本轮你能看到全部轮 1 输出（见上下文）。你的任务从「理解」切换为
「交叉」：

- 方法菌（尤其反驳菌）：对轮 1 中最强的主张构建钢人反驳；对识别阶段
  列出的 tensions 逐个开火。
- 领域菌：寻找轮 1 中未被连接的跨域结构；本领域有什么概念能让两个
  碎片的关系变清晰？
- 每个连接候选与冲突必须注明来源（引用轮 1 的 claim 编号）。
- 禁止和稀泥：发现矛盾就写矛盾，化解方向可选、裁决禁止。

输出严格 JSON：
{
  "agent": "{{agent_name}}",
  "connection_candidates": [{"text": "…", "source_claims": ["{{CODE}}1", "OTH2"]}],
  "conflicts": [{"a": "…", "b": "…", "nature": "事实层面|价值层面|概念界定"}],
  "recombinations": ["跨学科重组候选"],
  "claims": [{"id": "{{CODE}}5", "text": "…", "confidence": "…"}],
  "citations": ["claim:OTH2"]
}
```

### 轮 3 · 新假设（创造菌 + 权重最高 2 个领域菌）

```text
—— 发酵轮 3：新假设 ——

你能看到轮 1 + 轮 2 的全部输出。你的任务是「长出新的东西」：
基于已被反驳检验过的连接与冲突，提出新假设、新概念、研究问题候选。

要求：
- 每个候选必须是命题化的可检验/可讨论表述，附置信度（诚实标注，
  大多数应该是 emerging）。
- 标注它由哪些轮 1/轮 2 的 claim 生长而来（溯源）。
- 新概念须含定义与一个边界案例；新假说须含证伪条件。

输出严格 JSON：
{
  "agent": "{{agent_name}}",
  "new_hypotheses": [{"text": "…", "falsifier": "…", "confidence": "emerging"}],
  "new_concepts": [{"term": "…", "definition": "…", "boundary_case": "…"}],
  "research_questions": ["…"],
  "claims": [{"id": "{{CODE}}9", "text": "…", "confidence": "…"}],
  "citations": ["claim:REV3"]
}
```

> 停止条件由代码层判定（03 §7：信息饱和/冲突收敛/4 轮上限/复读检测），不进入 Prompt。浅模式 = 轮1+轮3；深模式 = 轮 2 拆两轮 + 独立魔鬼代言人轮（03 §6.1）。

------

## S4 · 整合 Integrate（产出 compost_output）

```text
—— 本次任务：整合 ——

输入：上下文包 + 全部轮次输出 + 识别阶段的 tensions/premises。
你要把它们组织成堆肥产物。铁律：

1. 只组织，不新增。产物中的每个观点必须能溯源到某个 Agent 的 claim
   （source_claims）。你自己没有观点——你是园丁，不是答案机器。
2. 不消除矛盾。conflicts 原样保留，你不裁决，resolution_hint 至多是
   「可能的化解方向」。
3. strongest_objection 无条件强制：有反驳菌取其最强论证；没有则你以
   魔鬼代言人身份补位（by 标 gardener_devils_advocate）。
4. 置信度规则：单一 Agent 单一来源最高 supported；strong 需跨视角交叉。
   标题含 emerging 核心思想时必须加「（初步）」前缀。
5. future_directions 每条附 as_crumbs：一句用户可以直接丢回来的面包渣。

输出严格 JSON（十节结构，字段顺序固定）：
{
  "title": "（初步）…",
  "core_ideas": [{"text": "命题化表述", "confidence": "…", "source_claims": ["S1"]}],
  "fragment_links": [{"from_idea": 3, "to_idea": 7, "relation": "…", "source_claims": ["…"]}],
  "forming_judgments": [{"text": "正在形成的判断", "confidence": "…", "source_claims": ["…"]}],
  "new_connections": [{"text": "…", "target": {"type": "insight", "id": 12}, "source_claims": ["…"]}],
  "conflicts": [{"a": "…", "b": "…", "nature": "…", "resolution_hint": "…"}],
  "strongest_objection": {"text": "…", "by": "反驳菌|gardener_devils_advocate", "source_claims": ["…"]},
  "open_questions": ["…"],
  "future_directions": [{"text": "…", "as_crumbs": "建议丢进来的新面包渣"}],
  "agents_used": [{"agent": "…", "weight": 0.8, "quota_role": "core|adjacent|wildcard", "key_contribution": "…"}],
  "source_crumbs": [3, 7]
}
```

------

## S5 · 评估反哺 Assess（营养评分）

```text
—— 本次任务：营养评估 ——

输入：本场全部轮次输出 + 最终产物。为每个参与菌评分。

评分通道（每个 Agent 独立计）：
1. 其 claims 被最终产物引用的段落比例 → base 0-4 分
2. 其贡献进入核心思想/正在形成的判断的命题数 → +1/条，封顶 +3
3. 反驳菌专属：其反驳被其他菌有效回应的次数 → +0.5/次

防作弊铁律：每一分都必须附引用证据（产物中哪一段来自该菌哪个 claim），
无证据不得给分。你的评分用户可见（透明性），也是菌床更新的依据——
评得准，菌床才长得对。

输出严格 JSON：
{
  "nutrition_awarded": [
    {"agent": "社会学菌", "score": 6.0,
     "evidence": ["core_ideas[1] ← claim:S2", "forming_judgments[0] ← claim:S5"]}
  ],
  "ecology_note": "本场菌群组成的一句观察（如：意外菌贡献了关键视角）（可空）"
}
```
