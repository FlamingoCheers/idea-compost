# 实操规约 01 · 本地数据库 DDL（SQLite / Room）

> 路线图 ② 之一。依据 `01-认知本体论`（字段与不变量）、`02-Agent生态协议`、`03-思想园丁调度协议`（INV-8 checkpoint）、`08 §3`（九表映射）。
>
> 本文件是**权威 Schema**：Room `@Entity` 逐字段照此映射，列名一致。SQLite 方言；JSON 一律 TEXT 存储并注明 `/* json */`。

## 0. 通用约定

```text
主键        UUID 存 TEXT（App 侧 UUIDv7 生成，时间有序利于分页）
时间戳      INTEGER epoch millis（UTC）
布尔        INTEGER 0/1
向量        BLOB（FloatArray little-endian），存独立表便于重建
FTS5        ideas_fts 虚表，content=ideas，中文按 ICU 分词（Room 用 @Fts4(contentEntity) 或原生 @Database 注入）
迁移策略    Room fallbackToDestructiveMigration 禁用；每版本 Migration 手写
            （bed_events / ideas 为不可丢数据，破坏性迁移 = 事故）
JSON 校验   App 侧 kotlinx.serialization；库内不校验
```

## 1. 表总览（21 表 + 1 FTS）

```text
核心九表（08 §3）  ideas, composts, probiotics, agents, insights, topics,
                   topic_members, bed_events, feedback_events
联结表            idea_compost, compost_probiotics, compost_stages,
                   insight_ideas, insight_agents, insight_topics, topic_ideas,
                   topic_relations, insight_relations, insight_citations
支撑表            idea_embeddings, agent_contributions, bed_snapshots, llm_calls
全文检索          ideas_fts (FTS5)
```

------

## 2. ideas · 面包渣

```sql
CREATE TABLE ideas (
  id           TEXT PRIMARY KEY NOT NULL,
  content      TEXT NOT NULL,                    -- Markdown 原文，INV-1 永不压缩
  content_type TEXT NOT NULL DEFAULT 'text',     -- text | image | mixed
  title        TEXT,                             -- 懒生成，INV-3，NULL=未生成
  source       TEXT NOT NULL DEFAULT 'manual',   -- manual|share_extension|clipboard|screenshot|import
  status       TEXT NOT NULL DEFAULT 'raw',      -- raw | composted
  metadata     TEXT NOT NULL DEFAULT '{}',       /* json: {device, location_opt_in, auto_tags[]} */
  created_at   INTEGER NOT NULL,
  updated_at   INTEGER NOT NULL
);
CREATE INDEX idx_ideas_created ON ideas(created_at DESC);
CREATE INDEX idx_ideas_status  ON ideas(status);
```

```sql
CREATE VIRTUAL TABLE ideas_fts USING fts5(
  content, title,
  content='ideas', content_rowid='rowid',
  tokenize='icu zh_CN'
);
-- Room 侧用触发器同步（或 @Fts4 contentEntity 自动维护）
```

```sql
-- 向量独立成表：换 embedding 模型可整表重建，不动原文（08 §5 三档降级）
CREATE TABLE idea_embeddings (
  idea_id    TEXT PRIMARY KEY NOT NULL REFERENCES ideas(id),
  vector     BLOB NOT NULL,
  dim        INTEGER NOT NULL,
  model      TEXT NOT NULL,                      -- 'api:text-embedding-3-small' | 'local:bge-small-zh'
  updated_at INTEGER NOT NULL
);
```

------

## 3. composts · 堆肥

```sql
CREATE TABLE composts (
  id                TEXT PRIMARY KEY NOT NULL,
  depth             TEXT NOT NULL DEFAULT 'standard',   -- shallow | standard | deep
  status            TEXT NOT NULL DEFAULT 'pending',    -- pending|running|suspended|awaiting_feedback|done|failed
  gardener_plan     TEXT,                              /* json: 识别结果+召集名单(含w值/配额角色/否决记录) */
  output_struct     TEXT,                              /* json: 十节结构，唯一真源（05 §1，INV-9） */
  output_note       TEXT,                              -- 渲染 MD 视图缓存
  cost              TEXT NOT NULL DEFAULT '{}',        /* json: {tokens_in, tokens_out, calls, by_model} */
  nutrition_awarded TEXT NOT NULL DEFAULT '{}',        /* json: {agent_id: score} */
  feedback_summary  TEXT NOT NULL DEFAULT '{}',        /* json */
  suspend_deadline  INTEGER,                           -- 挂起保留 7 天的截止（03 §10）
  created_at        INTEGER NOT NULL,
  completed_at      INTEGER
);
CREATE INDEX idx_composts_status ON composts(status, created_at DESC);
```

> 注：`depth` 轮次语义以 03 §3/§6.1 为准（浅=轮1+轮3，标准=3 轮，深=3 轮+拆分+魔鬼轮）；01 §6.1 括号里的轮数是早期草稿，以 03 为准。

```sql
CREATE TABLE idea_compost (
  idea_id    TEXT NOT NULL REFERENCES ideas(id),
  compost_id TEXT NOT NULL REFERENCES composts(id),
  position   INTEGER NOT NULL,      -- 用户选择顺序，进上下文包保持一致
  PRIMARY KEY (idea_id, compost_id)
);
CREATE INDEX idx_ic_compost ON idea_compost(compost_id);

CREATE TABLE compost_probiotics (
  compost_id  TEXT NOT NULL REFERENCES composts(id),
  probiotic_id TEXT NOT NULL REFERENCES probiotics(id),
  PRIMARY KEY (compost_id, probiotic_id)   -- ≤2 由预检保证（03 §3）
);
```

### compost_stages · 阶段 checkpoint（INV-8 核心）

```sql
CREATE TABLE compost_stages (
  id          TEXT PRIMARY KEY NOT NULL,
  compost_id  TEXT NOT NULL REFERENCES composts(id),
  seq         INTEGER NOT NULL,              -- 场内序号（0 起，含每菌每轮）
  kind        TEXT NOT NULL,                 -- preflight|identify|convoke|ferment_r1|ferment_r2|ferment_r3|integrate|assess
  agent_id    TEXT REFERENCES agents(id),    -- null=园丁/代码阶段
  round_no    INTEGER,                       -- 发酵轮次（kind=ferment_* 时有值）
  input_ref   TEXT,                          -- 上下文包哈希（内容寻址，便于回放审计）
  output      TEXT NOT NULL,                 /* json: 该 stage 原始输出（含 claims/citations） */
  model       TEXT NOT NULL,
  tokens_in   INTEGER NOT NULL DEFAULT 0,
  tokens_out  INTEGER NOT NULL DEFAULT 0,
  latency_ms  INTEGER NOT NULL DEFAULT 0,
  status      TEXT NOT NULL DEFAULT 'done',  -- done | failed | degraded（引用校验失败降级）
  created_at  INTEGER NOT NULL,
  UNIQUE (compost_id, seq)
);
CREATE INDEX idx_stages_compost ON compost_stages(compost_id, seq);
```

------

## 4. agents · 菌

```sql
CREATE TABLE agents (
  id                   TEXT PRIMARY KEY NOT NULL,
  type                 TEXT NOT NULL,          -- domain | method | creative
  name                 TEXT NOT NULL,
  description          TEXT NOT NULL,
  capability_card      TEXT NOT NULL,          -- 活跃态完整 Prompt 卡（300-600 token）
  card_version         INTEGER NOT NULL DEFAULT 1,  -- 卡片即数据：只增不改，历史绑定版本
  specialties          TEXT NOT NULL DEFAULT '[]',  /* json: 习得专长，增殖时写入 */
  status               TEXT NOT NULL DEFAULT 'active', -- embryo|active|compressed|dormant|fused
  vitality             REAL NOT NULL DEFAULT 40,
  nutrition_buffer     REAL NOT NULL DEFAULT 0,      -- 02 §2.3，半衰期 14 天由夜间任务衰减
  parent_id            TEXT REFERENCES agents(id),
  fusion_of            TEXT,                          /* json: [agent_id]，融合菌填写 */
  compressed_memory    TEXT,                          -- 休眠压缩卡 150-250 token（02 §6）
  full_profile         TEXT NOT NULL,                 -- 完整档案，INV-7 永不删除
  participation_count  INTEGER NOT NULL DEFAULT 0,
  last_contribution_at INTEGER,
  created_at           INTEGER NOT NULL,
  updated_at           INTEGER NOT NULL
);
CREATE INDEX idx_agents_pool ON agents(status, vitality DESC);  -- 激活池查询：未休眠按活性排
```

```sql
-- contribution_history[] 独立成表（每场一条，营养评估写入）
CREATE TABLE agent_contributions (
  id          TEXT PRIMARY KEY NOT NULL,
  agent_id    TEXT NOT NULL REFERENCES agents(id),
  compost_id  TEXT NOT NULL REFERENCES composts(id),
  score       REAL NOT NULL DEFAULT 0,      -- S5 评估分
  evidence    TEXT NOT NULL DEFAULT '[]',   /* json: ["core_ideas[1] ← claim:S2"] */
  created_at  INTEGER NOT NULL,
  UNIQUE (agent_id, compost_id)
);
```

------

## 5. insights · 洞察

```sql
CREATE TABLE insights (
  id              TEXT PRIMARY KEY NOT NULL,
  content         TEXT NOT NULL,             -- 命题级，INV-4，1-3 句
  confidence      TEXT NOT NULL DEFAULT 'emerging',  -- emerging | supported | strong
  source_compost  TEXT REFERENCES composts(id),
  status          TEXT NOT NULL DEFAULT 'emerging_status', -- 见下方状态机（对齐 01/05）
  importance      INTEGER NOT NULL DEFAULT 0,   -- 0-100，反馈累积（05 §4）
  version_no      INTEGER NOT NULL DEFAULT 0,   -- revised 时 +1，原命题保留为版本 0
  created_at      INTEGER NOT NULL,
  updated_at      INTEGER NOT NULL
);
CREATE INDEX idx_insights_feed ON insights(importance DESC, updated_at DESC);
```

> 状态机统一（01 §2.3 与 05 §4 合并）：`emerging →(❤️⭐↗) adopted / (?) contested / (30天无反馈) archived / (改写) revised`；contested/archived 保留且可被引用唤醒。列值：`emerging|adopted|contested|revised|archived`。

```sql
CREATE TABLE insight_ideas (
  insight_id TEXT NOT NULL REFERENCES insights(id),
  idea_id    TEXT NOT NULL REFERENCES ideas(id),
  PRIMARY KEY (insight_id, idea_id)
);
CREATE TABLE insight_agents (
  insight_id TEXT NOT NULL REFERENCES insights(id),
  agent_id   TEXT NOT NULL REFERENCES agents(id),
  PRIMARY KEY (insight_id, agent_id)
);
CREATE TABLE insight_topics (
  insight_id TEXT NOT NULL REFERENCES insights(id),
  topic_id   TEXT NOT NULL REFERENCES topics(id),
  PRIMARY KEY (insight_id, topic_id)
);
-- 被引用记录（CitationScore 数据源，02 §7 后见之明）
CREATE TABLE insight_citations (
  insight_id      TEXT NOT NULL REFERENCES insights(id),
  citing_compost  TEXT NOT NULL REFERENCES composts(id),
  claimed_by      TEXT REFERENCES agents(id),  -- 谁引用的（营养归因）
  created_at      INTEGER NOT NULL,
  PRIMARY KEY (insight_id, citing_compost)
);
```

------

## 6. topics · 主题（涌现，INV-5）

```sql
CREATE TABLE topics (
  id               TEXT PRIMARY KEY NOT NULL,
  name             TEXT NOT NULL,
  description      TEXT,
  status           TEXT NOT NULL DEFAULT 'emerging', -- emerging|active|declining|dormant|merged
  vitality         REAL NOT NULL DEFAULT 50,
  birth_ref_type   TEXT,                          -- idea | insight
  birth_ref_id     TEXT,
  parent_topic_id  TEXT REFERENCES topics(id),    -- 允许一层父子
  snapshot_summary TEXT,                          -- T2 主题卡（06 记忆分层）
  first_seen       INTEGER NOT NULL,
  last_active      INTEGER NOT NULL
);
CREATE INDEX idx_topics_status ON topics(status, vitality DESC);

CREATE TABLE topic_ideas (
  topic_id TEXT NOT NULL REFERENCES topics(id),
  idea_id  TEXT NOT NULL REFERENCES ideas(id),
  PRIMARY KEY (topic_id, idea_id)
);
CREATE TABLE topic_members (      -- 08 §3 命名的 insight 成员表（= insight_topics 的反向读法）
  topic_id    TEXT NOT NULL REFERENCES topics(id),
  insight_id  TEXT NOT NULL REFERENCES insights(id),
  PRIMARY KEY (topic_id, insight_id)
);
CREATE TABLE topic_relations (
  topic_a  TEXT NOT NULL REFERENCES topics(id),
  topic_b  TEXT NOT NULL REFERENCES topics(id),
  rel_type TEXT NOT NULL,             -- parent | adjacent
  strength REAL NOT NULL DEFAULT 1,
  PRIMARY KEY (topic_a, topic_b, rel_type)
);
```

------

## 7. insight_relations · 关系边（01 §8，只在堆肥中断言）

```sql
CREATE TABLE insight_relations (
  from_id            TEXT NOT NULL REFERENCES insights(id),
  to_id              TEXT NOT NULL REFERENCES insights(id),
  rel_type           TEXT NOT NULL,   -- supports | contradicts | extends | clarifies
  strength           REAL NOT NULL DEFAULT 1,  -- 共现对数+园丁+2+用户确认+3
  asserted_by_compost TEXT REFERENCES composts(id),
  user_confirmed     INTEGER NOT NULL DEFAULT 0,
  created_at         INTEGER NOT NULL,
  PRIMARY KEY (from_id, to_id, rel_type)
);
CREATE INDEX idx_ir_to ON insight_relations(to_id, rel_type);  -- 检索连接候选
```

------

## 8. probiotics · 益生菌

```sql
CREATE TABLE probiotics (
  id              TEXT PRIMARY KEY NOT NULL,
  name            TEXT NOT NULL,
  icon            TEXT,
  description     TEXT NOT NULL,
  prompt_logic    TEXT NOT NULL,               -- 注入调度指令（04 §1）
  target_types    TEXT NOT NULL DEFAULT '[]',  /* json: 偏好 Agent 类型 */
  domain_boosts   TEXT NOT NULL DEFAULT '{}',  /* json: {菌名: 倍率} */
  stage_emphasis  TEXT NOT NULL DEFAULT '{}',  /* json */
  diversity_shift TEXT,                        /* json: null=不改配比；{"quota":"50/30/20"} 或 {"wildcard":"0.2"} */
  scope           TEXT NOT NULL DEFAULT 'user_defined',  -- global_builtin | user_defined
  hidden          INTEGER NOT NULL DEFAULT 0,  -- 内置可隐藏（04 §2）
  usage_count     INTEGER NOT NULL DEFAULT 0,  -- ProbioticAffinity 数据源（02 §2.1）
  last_used       INTEGER,
  birth_context   TEXT,                        -- 自定义菌种用户原话
  created_at      INTEGER NOT NULL,
  updated_at      INTEGER NOT NULL
);
```

------

## 9. bed_events · 菌床事件流（append-only，INV-10）

```sql
CREATE TABLE bed_events (
  seq        INTEGER PRIMARY KEY AUTOINCREMENT,  -- 事件全序
  ts         INTEGER NOT NULL,
  event_type TEXT NOT NULL,   -- idea_created|compost_completed|insight_proposed|insight_adopted|
                              -- insight_revised|insight_contested|insight_cited|topic_emerged|
                              -- topic_status|agent_spawned|agent_compressed|agent_dormant|
                              -- agent_awakened|agent_fused|probiotic_used|feedback_given
  payload    TEXT NOT NULL,   /* json: 事件体（如 {agents, weights, nutrition}）*/
  compost_id TEXT             -- 幂等去重键（03 §10：落库重试按 compost_id 去重）
);
CREATE INDEX idx_bed_events_type ON bed_events(event_type, ts);
CREATE INDEX idx_bed_events_compost ON bed_events(compost_id);
-- 纯追加：App 层禁止 UPDATE/DELETE；时间序列分析（v0.1 §41）的唯一事实源
```

## 10. bed_snapshots · 物化快照

```sql
CREATE TABLE bed_snapshots (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  version     INTEGER NOT NULL,
  content     TEXT NOT NULL,   /* json: 01 §7.3 全部字段（active/dormant lists、
                                 diversity_index、perturbation_log、bed_summary 等）*/
  created_at  INTEGER NOT NULL
);
-- 只读最新一条；bed_summary(T3) 在快照 content 内，注入园丁上下文时标注
-- 「历史认知活动统计」（INV-11：摘要是画像不是指令）
```

------

## 11. feedback_events · 反馈事件（INV-12）

```sql
CREATE TABLE feedback_events (
  id           TEXT PRIMARY KEY NOT NULL,
  ts           INTEGER NOT NULL,
  target_type  TEXT NOT NULL,        -- insight | compost_section | agent
  target_id    TEXT NOT NULL,
  feedback     TEXT NOT NULL,        -- love|keep|develop|disagree|edit|cite（01 §9.1）
  section_ref  TEXT,                 -- 产物节引用（如 core_ideas[1]），营养归因用
  diff         TEXT,                 /* json: edit 时的编辑 diff */
  compost_id   TEXT REFERENCES composts(id),
  settled      INTEGER NOT NULL DEFAULT 0,   -- 延迟营养是否已结算（夜间任务清账）
  created_at   INTEGER NOT NULL
);
CREATE INDEX idx_feedback_settle ON feedback_events(settled, ts);
```

------

## 12. llm_calls · 调用台账

```sql
CREATE TABLE llm_calls (
  id          TEXT PRIMARY KEY NOT NULL,
  compost_id  TEXT,                 -- null=夜间任务/标题生成等场外调用
  purpose     TEXT NOT NULL,        -- ferment|identify|convoke|integrate|assess|distill|title|embed_api
  model       TEXT NOT NULL,
  tokens_in   INTEGER NOT NULL DEFAULT 0,
  tokens_out  INTEGER NOT NULL DEFAULT 0,
  latency_ms  INTEGER NOT NULL DEFAULT 0,
  status      TEXT NOT NULL,        -- ok | repaired | failed | budget_cut
  retry_count INTEGER NOT NULL DEFAULT 0,
  created_at  INTEGER NOT NULL
);
CREATE INDEX idx_llm_compost ON llm_calls(compost_id);
-- cost 聚合数据源；预算守卫（08 §5 ≤20 次）实时查本表 compost_id 计数
```

------

## 13. DAO 关键查询（Room @Query 预定）

```text
激活池        SELECT * FROM agents WHERE status IN ('active','embryo','compressed') ORDER BY vitality DESC
上下文包·洞察  SELECT * FROM insights WHERE status != 'archived' ORDER BY importance DESC LIMIT 10
连接候选      SELECT ir.* FROM insight_relations ir JOIN insights i ON ir.to_id=i.id
              WHERE ir.to_id IN (:activeInsightIds) AND ir.rel_type='extends'
场恢复        SELECT * FROM compost_stages WHERE compost_id=:id ORDER BY seq DESC LIMIT 1
事件回放      SELECT * FROM bed_events WHERE ts >= :since ORDER BY seq   -- §41 时间序列分析
引用营养      SELECT ie.claimed_by, COUNT(*) FROM insight_citations ie WHERE ie.created_at>=:since GROUP BY 1
```

## 14. 首启种子数据

安装后首次建库，从 `prompts/` 卡全集播种（02 §1）：

```text
用户勾选的 N 个领域菌   status=active,   vitality=50
未勾选的 (7-N) 个       status=dormant,  compressed_memory=卡内首行简介, full_profile=完整卡
6 方法菌 + 4 创造菌     status=active,   vitality=40
6 内置益生菌            scope=global_builtin
bed_events 追加 idea 无、agent_spawned ×（7+10）
```
