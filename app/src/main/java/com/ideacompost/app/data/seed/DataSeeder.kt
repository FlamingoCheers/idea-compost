package com.ideacompost.app.data.seed

import android.content.Context
import com.ideacompost.app.data.db.dao.AgentDao
import com.ideacompost.app.data.db.dao.BedEventDao
import com.ideacompost.app.data.db.dao.ProbioticDao
import com.ideacompost.app.data.db.entity.AgentEntity
import com.ideacompost.app.data.db.entity.BedEventEntity
import com.ideacompost.app.data.db.entity.ProbioticEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 首启播种（specs/01 §14）：
 *  - 用户勾选的 N 个领域菌   active, V=50
 *  - 未勾选的 (7-N) 个       dormant（压缩卡=描述行，full_profile=完整卡）
 *  - 方法菌 ×6 + 创造菌 ×4   active, V=40
 *  - 内置益生菌 ×6
 *  - bed_events += agent_spawned ×17
 */
@Singleton
class DataSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val agentDao: AgentDao,
    private val probioticDao: ProbioticDao,
    private val bedEventDao: BedEventDao
) {
    suspend fun seed(pickedDomainIds: Set<String>) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()

        val domainCards = parse("prompts/agents/domains.md", PromptCardParser::parseAgents)
        val methodCards = parse("prompts/agents/methods.md", PromptCardParser::parseAgents)
        val creatorCards = parse("prompts/agents/creators.md", PromptCardParser::parseAgents)
        val probiotics = parse("prompts/probiotics.md", PromptCardParser::parseProbiotics)

        val agents = buildList {
            addAll(domainCards.map { card ->
                val picked = card.id in pickedDomainIds
                AgentEntity(
                    id = card.id,
                    type = card.type,
                    name = card.name,
                    description = card.description,
                    capabilityCard = card.capabilityCard,
                    status = if (picked) "active" else "dormant",
                    vitality = if (picked) 50.0 else 0.0,
                    compressedMemory = if (picked) null else card.description,
                    fullProfile = card.capabilityCard,
                    createdAt = now,
                    updatedAt = now
                )
            })
            addAll((methodCards + creatorCards).map { card ->
                AgentEntity(
                    id = card.id,
                    type = card.type,
                    name = card.name,
                    description = card.description,
                    capabilityCard = card.capabilityCard,
                    status = "active",
                    vitality = card.initVitality,
                    fullProfile = card.capabilityCard,
                    createdAt = now,
                    updatedAt = now
                )
            })
        }
        agentDao.insertAll(agents)

        probioticDao.insertAll(probiotics.map { p ->
            ProbioticEntity(
                id = p.id,
                name = p.name,
                description = p.description,
                promptLogic = p.promptLogic,
                domainBoosts = p.domainBoosts,
                diversityShift = p.diversityShift,
                scope = "global_builtin",
                createdAt = now,
                updatedAt = now
            )
        })

        agents.forEach { agent ->
            bedEventDao.insert(
                BedEventEntity(
                    ts = now,
                    eventType = "agent_spawned",
                    payload = """{"agent_id":"${agent.id}","type":"${agent.type}","picked":${agent.status == "active"}}"""
                )
            )
        }
    }

    private fun <T> parse(path: String, parser: (String) -> List<T>): List<T> =
        context.assets.open(path).bufferedReader().use { it.readText() }.let(parser)
}
