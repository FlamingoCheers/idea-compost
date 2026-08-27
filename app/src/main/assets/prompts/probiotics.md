# 内置益生菌 · Probiotics（global_builtin）

> 对齐 `docs/v0.2-设计文档/04 §1-2`。不可删除、可隐藏。组合 ≤2，语义叠加（domain_boosts 取乘积、diversity_shift 取更激进值、prompt_logic 拼接）。
>
> `prompt_logic` 是注入**园丁与菌群调度指令**的思考要求（不是普通 Prompt Template——三个结构化字段同时改变菌群组成与流程形态，这是益生菌的本体论差异）。

------

### 哲学益生菌

```yaml
id: probiotic_philosophy
name: 哲学益生菌
description: 从存在、自由、意义、异化等角度重新审视问题
scope: global_builtin
domain_boosts: {哲学菌: 2.5}
stage_emphasis: {}
diversity_shift: null
```

```text
prompt_logic:
本次堆肥从哲学的方向刺激发酵。请所有参与菌：把讨论中的核心概念放回
存在、自由、意义、异化、正义这些根本问题的坐标系中审视；对每个关键
主张追问「它隐含了什么样的人性观/价值观」；区分它作为事实主张与作为
价值主张的身份。哲学菌本场权重提升，但其他菌也应携带一个哲学问题
进入自己的领域视角。
```

------

### 科研益生菌

```yaml
id: probiotic_research
name: 科研益生菌
description: 将直觉转化为研究问题，识别研究边界与空白
scope: global_builtin
domain_boosts: {研究问题菌: 2.5, 假说菌: 1.5}
stage_emphasis: {}
diversity_shift: null
```

```text
prompt_logic:
本次堆肥向科研方向发酵。请所有参与菌：把用户直觉转译为可研究的问题；
明确指出现有知识走到哪里、断在哪里（具体到缺什么数据/什么设计）；
区分「经验问题」与「非经验问题」，不把价值判断硬塞给实证。本轮优先
产出：清晰的研究问题树 + 每个问题的第一句可检验表述。研究问题菌与
假说菌本场主导，领域菌负责为问题供给背景与边界。
```

------

### 反直觉益生菌

```yaml
id: probiotic_counterintuitive
name: 反直觉益生菌
description: 不接受默认前提，寻找最违反直觉的解释
scope: global_builtin
domain_boosts: {反事实菌: 2.0, 概念辨析菌: 1.5}
stage_emphasis: {}
diversity_shift: {意外菌配额: "10%→20%"}
```

```text
prompt_logic:
本次堆肥施加反直觉的选择压力。请所有参与菌：先显式列出「大家默认为真
但从未检验」的前提，再逐个挑战；优先提出让用户第一反应是「不对吧」
但细想有结构性理由的解释——惊奇必须来自逻辑而非语气；每个反直觉
主张附「如果它为真，我们应该观察到什么」。默认前提是本次发酵的
主要燃料，不是讨论的地基。
```

------

### 魔鬼代言人益生菌

```yaml
id: probiotic_devils_advocate
name: 魔鬼代言人益生菌
description: 假设用户核心判断是错的，为反方构建最强论证
scope: global_builtin
domain_boosts: {反驳菌: 3.0}
stage_emphasis: {}
diversity_shift: null
```

```text
prompt_logic:
本次堆肥的规则改写为：假设用户在这批面包渣中的核心判断是错误的。
请所有参与菌先各自独立概括「用户的核心判断是什么」，然后以最强形式
（钢人而非稻草人）为反方构建论证；反驳菌本场拥有最高权重与最后发言
位置。产物必须包含一个让用户感到真实压力的最强反方论证，以及
「用户要守住原判断，最需要补的证据是什么」。
```

------

### 跨学科益生菌

```yaml
id: probiotic_interdisciplinary
name: 跨学科益生菌
description: 从多个看似无关的学科寻找结构上的相似性
scope: global_builtin
domain_boosts: {类比菌: 2.0}
stage_emphasis: {}
diversity_shift: {配额: "70/20/10 → 50/30/20"}
```

```text
prompt_logic:
本次堆肥扩大异质性配额：召集时相邻领域菌与意外菌的比例上调，刻意引入
用户惯常视角之外的学科。请所有参与菌：至少一次从本学科之外的眼光
重述核心问题；类比菌主导寻找结构相似（只映射关系，不映射实体）；
每个跨学科连接标注映射表与断点。目标是让这次发酵产生用户自己
「不会被想到会相关」的连接。
```

------

### 苏格拉底益生菌

```yaml
id: probiotic_socratic
name: 苏格拉底益生菌
description: 不急于回答，不断追问判断成立所依赖的前提
scope: global_builtin
domain_boosts: {概念辨析菌: 2.5, 哲学菌: 1.5}
stage_emphasis: {}
diversity_shift: null
```

```text
prompt_logic:
本次堆肥延迟给出答案：在产出任何判断之前，先完成追问。请所有参与菌：
对每个主张连续追问其成立的前提（至少两层：「它依赖什么」以及「那个
依赖又依赖什么」）；概念辨析菌主导拆解关键词的歧义节点；产出的
open_questions 优先于 forming_judgments——本场允许的结论形态是
「更清晰的问题」而非「更完整的答案」。宁要锋利的问题，不要圆滑的
综合。
```
