# 07 · 调研：多 Agent 协作 Harness 框架选型

> v0.2 设计文档集 · 回答 v0.1 文末问题：「控制一整个 Agent 集群，底层是不是应该抄一个支持多 Agent 协作的 Harness 框架？抄哪一个？」
>
> **v0.2.1 架构修订：纯本地 Agent APP。** 用户决策：Agent 集群与整个 APP 打包，跑在设备本地；云端只提供 AI 推理 API。本调研结论在新架构下**更加成立**——生态逻辑本来就是应用侧数据逻辑，编排层也只是 App 内一个薄模块，连服务端都不需要了。下文原调研内容保留，§4 按新架构修订。

------

# 1. 结论（先说答案）

> **不需要「抄」任何框架——因为本 APP 的核心机制（菌群生态）没有任何框架提供；而框架能提供的（多步编排），我们只需要很薄的一层。**

演进路径（v0.2.1 修订后）：

```text
MVP 阶段：App 内自研薄编排层（Kotlin 协程，≈500 行）+ 模型无关 LLM 客户端，
          直连 OpenAI 兼容 API（BYO Key）
规模化阶段：如未来需要多设备同步/私有云备份，再评估服务端引入 LangGraph
永不采用：Letta 做核心（记忆思想已借鉴，见 06）；AutoGen（已成历史）
```

------

# 2. 需求分析：堆肥管线到底需要框架提供什么

03 协议的执行模式拆解：

```text
✅ 需要：多阶段顺序编排（5 阶段）
✅ 需要：轮内并行（一轮 K 个 Agent 并发调用）
✅ 需要：交叉（轮 2 能看到轮 1 全部输出）
✅ 需要：检查点与恢复（INV-8，stage 粒度）
✅ 需要：LLM 调用预算控制（≤20 次/场）
✅ 需要：模型无关（Agent 卡与模型解耦，D3）
❌ 不需要：Agent 自主对话循环（菌群不自聊，园丁统调度）
❌ 不需要：框架级 Agent 注册（Agent 是数据记录非代码对象，D3）
❌ 不需要：工具调用（发酵是纯思考，无工具）
```

**关键洞察**：增殖/融合/休眠/活性在任何框架里都不存在——它们是应用侧数据逻辑（02 协议）。框架对「生态」零贡献；Agent 对框架而言只是一次带不同 system prompt 的 LLM 调用。所以选型问题从「抄哪个生态框架」退化为「用什么跑可靠的多步 LLM 流水线」。

------

# 3. 候选框架逐一评估（2026 年现状）

| 框架 | 许可 | 核心模型 | 对本项目的适配度 |
| --- | --- | --- | --- |
| **LangGraph 1.0** | MIT | 图状态机：durable state/checkpointer、interrupt() 人工在环、Send 动态 fan-out、子图 | ★★★★☆ 最合适的框架选项：检查点恢复=INV-8、interrupt=awaiting_feedback、Send=轮内并行。生产最成熟（Uber/LinkedIn/Klarna，90M 月下载，2.0 前无 breaking change）。缺点：学习曲线陡、样板代码多、依赖 langchain-core、无归档记忆 |
| **Microsoft Agent Framework** | MIT | 2025-10 合并 AutoGen+Semantic Kernel 而来，Workflow API、A2A、checkpointing，GA Q1 2026，Python+.NET | ★★★ 能力齐全但偏企业对话智能体场景；.NET 生态对本项目无用；AutoGen 本体已成历史，新项目不应再选 |
| **CrewAI** | MIT（open core） | 角色制 crew（Agent/Task/Crew/Process），快速原型 | ★★ 原型最快，但生产可靠性弱、对流程形态控制力差——我们需要 03 协议那种精确的阶段控制与停止条件 |
| **OpenAI Agents SDK** | MIT | 极简原语 agents/handoffs/guardrails + tracing | ★★ 线性 handoff 天然串行，无内建并行与状态管理，生态习惯锁定 OpenAI |
| **Letta（fka MemGPT）** | Apache 2.0 | 记忆优先有状态 Agent，core memory blocks 自编辑、sleep-time compute、agents-as-service REST | ★★ 单体长生命周期 Agent 的记忆最强，但多 Agent 编排弱，且需独占控制上下文窗口、官方承认难以与其他编排框架组合。其 sleep-time 思想已借鉴（06 D40） |
| **Google ADK 1.0** | Apache 2.0 | 2026-04 GA，四语言，A2A 协议（Linux Foundation，150+ 组织） | ★★★ 2026 三支柱（MCP/A2A/ADK）之一，但我们没有「跨系统 Agent 互操作」需求，A2A 用不上 |

行业背景：2026 年共识栈是 MCP（工具）+ A2A（Agent 间通信）+ 编排框架（ADK 或 LangGraph 等）。本 APP 是**单用户私域封闭系统**，Agent 全部内部自生（02 协议），不接外部 Agent 也不需要 Agent 间协议——三支柱中只有「编排」一件与我们有关。

------

# 4. 推荐方案详解

## 4.1 MVP：App 内自研薄编排层（强烈推荐）

```text
构成（App 内，Kotlin）：
  ① 模型无关客户端层（OpenAI 兼容协议，多供应商可配，BYO Key）
  ② 03 协议的 5 阶段状态机（Kotlin 协程实现，≈300 行）
     —— 状态即 Room 中的 compost 记录，天然持久化，
        无需框架 checkpointer
  ③ 轮内并发：coroutineScope + async（K 个 Agent 并行调用）
  ④ 预算守卫：调用计数器，超限熔断进阶段 4
  ⑤ 失败恢复：stage 粒度重试，Room 即 checkpoint 存储

为什么 MVP 不用框架（v0.2.1 下更成立）：
  - 纯本地架构下根本没有「服务端编排」的位置——框架选项物理上不存在
  - 我们的「状态」本来就要落库（compost 记录），框架 checkpointer 是冗余
  - 流程是静态五阶段（配额在代码里，D11），不需要动态图
  - MVP 最大的技术风险是产品逻辑不是编排可靠性——薄层够用且完全可控
```

## 4.2 规模化：何时才需要重新考虑框架

v0.2.1 修订：原「迁 LangGraph」路径基于服务端架构，现已作废。重新触发评估的条件变为：

```text
- 未来做多设备同步 / 云备份（需要服务端承载，届时服务端编排可评估 LangGraph）
- 堆肥模式分化出多种动态流程形态（本地也需要图路由时，评估 Compose 侧
  状态机库而非引入 Python 框架）
迁移成本低：03 协议的 5 阶段天然映射为 5 个节点；
  Agent 调用是无状态函数，换引擎不动业务逻辑
```

## 4.3 本地运行时的可行性边界（v0.2.1 新增）

```text
「纯本地 Agent APP + 云端纯推理」为何成立：
  - 一场堆肥 = 10-20 次带不同 system prompt 的 LLM API 调用（D44），
    编排只是顺序+并发+落库，Kotlin 协程绰绰有余
  - 生态逻辑（Vitality/营养/休眠/增殖）全部是本地数据计算，零 LLM 依赖
  - 付出的代价（均可接受，详见 08 §5/§7）：
      ① 堆肥中需保持 App 前台（等待态 UX 已专门设计，08 §7）
      ② API Key 存设备端（BYO Key 模式，顺带解决计费与滥用）
      ③ embedding 需本地小模型（ONNX）或降级 FTS5 / 调 embedding API
      ④ 夜间任务靠 WorkManager，可靠性略低于服务器 cron
```

------

# 5. 决策汇总

| # | 决策 | 理由 |
| --- | --- | --- |
| D42 | App 内自研薄编排层（Kotlin 协程），不引入框架 | v0.2.1 纯本地架构下框架选项不存在；状态本就落库；流程静态 |
| D43 | 「规模化迁 LangGraph」路径作废，改为触发式重评估 | 服务端已取消；多设备/云备份需求出现时再议 |
| D44 | Agent=带 Prompt 卡的一次 LLM 调用，非框架节点 | D3 的工程推论；菌群生态是数据逻辑 |
| D45 | 拒绝 Letta 做核心、AutoGen 系、A2A 依赖 | 各有硬伤或不匹配封闭私域场景 |
| D46 | 模型无关客户端层为第一块基础设施（App 内） | Agent 卡与模型解耦（D3），可换模型换供应商 |
| D54 | 纯本地 Agent 运行时：编排/生态/记忆全在设备，云端只做推理 | 用户决策；隐私最大化；省掉整个后端 |
