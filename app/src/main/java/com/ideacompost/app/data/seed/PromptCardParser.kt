package com.ideacompost.app.data.seed

/**
 * 解析 assets/prompts/ 卡全集（与仓库 prompts/ 目录同一真源）。
 *
 * 文件格式（prompts 下 agents 目录的 Markdown）：
 *   ### 菌名
 *   ```yaml
 *   id / type / name / description / init_vitality / seed_status / specialties
 *   ```
 *   ```text
 *   capability_card 全文
 *   ```
 */
data class ParsedAgentCard(
    val id: String,
    val type: String,
    val name: String,
    val description: String,
    val initVitality: Double,
    val seedStatus: String,
    val capabilityCard: String
)

data class ParsedProbiotic(
    val id: String,
    val name: String,
    val description: String,
    val promptLogic: String,
    val domainBoosts: String,
    val diversityShift: String?
)

object PromptCardParser {

    fun parseAgents(markdown: String): List<ParsedAgentCard> {
        val cards = mutableListOf<ParsedAgentCard>()
        val sections = markdown.split(Regex("\n### ")).drop(1) // 首段是文件头
        for (s in sections) {
            val yaml = blockAfter(s, "yaml") ?: continue
            val text = blockAfter(s, "text") ?: ""
            val field = { key: String ->
                Regex("$key:\\s*(.+)").find(yaml)?.groupValues?.get(1)?.trim()
            }
            val id = field("id") ?: continue
            cards += ParsedAgentCard(
                id = id,
                type = field("type") ?: "domain",
                name = field("name") ?: id,
                description = field("description") ?: "",
                initVitality = field("init_vitality")?.toDoubleOrNull() ?: 40.0,
                seedStatus = field("seed_status") ?: "universal",
                capabilityCard = text.trim()
            )
        }
        return cards
    }

    fun parseProbiotics(markdown: String): List<ParsedProbiotic> {
        val out = mutableListOf<ParsedProbiotic>()
        val sections = markdown.split(Regex("\n### ")).drop(1)
        for (s in sections) {
            val yaml = blockAfter(s, "yaml") ?: continue
            val text = blockAfter(s, "text") ?: ""
            val field = { key: String ->
                Regex("$key:\\s*(.+)").find(yaml)?.groupValues?.get(1)?.trim()
            }
            val id = field("id") ?: continue
            val boosts = field("domain_boosts") ?: "{}"
            val shift = field("diversity_shift")?.takeIf { it != "null" }
            out += ParsedProbiotic(
                id = id,
                name = field("name") ?: id,
                description = field("description") ?: "",
                promptLogic = text.trim(),
                domainBoosts = normalizeJsonish(boosts),
                diversityShift = shift?.let { normalizeJsonish(it) }
            )
        }
        return out
    }

    /** 取 ```lang ... ``` 第一个匹配块的内容。 */
    private fun blockAfter(section: String, lang: String): String? {
        val regex = Regex("```$lang\\n(.*?)```", RegexOption.DOT_MATCHES_ALL)
        return regex.find(section)?.groupValues?.get(1)
    }

    /** YAML 形如 {哲学菌: 2.5} → JSON {"哲学菌": 2.5}（未加引号的键补引号，值保持原样）。 */
    private fun normalizeJsonish(s: String): String =
        s.replace(Regex("([\\{,]\\s*)([^:{}\"\\[\\],]+?)(\\s*:)")) { m ->
            m.groupValues[1] + "\"" + m.groupValues[2].trim() + "\"" + m.groupValues[3]
        }
}
