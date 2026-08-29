package com.ideacompost.app.data.ai

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** 从 assets/prompts 加载园丁系统 Prompt 与五阶段调度指令（prompts/ 是单一真源）。 */
@Singleton
class GardenerPrompts @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val cache = mutableMapOf<String, String>()

    val system: String by lazy { read("prompts/gardener/system.md").substringAfter("```text").substringBefore("```").trim() }

    private val stages: Map<String, String> by lazy { parseStages() }

    fun stage(key: String): String = stages[key] ?: error("unknown stage prompt: $key")

    private fun read(path: String): String =
        cache.getOrPut(path) { context.assets.open(path).bufferedReader().use { it.readText() } }

    private fun parseStages(): Map<String, String> {
        val text = read("prompts/gardener/stages.md")
        val out = mutableMapOf<String, String>()
        var section = ""
        var sub = ""
        val fence = StringBuilder()
        var inFence = false
        text.lines().forEach { line ->
            when {
                line.startsWith("## S1") -> { section = "s1"; sub = "" }
                line.startsWith("## S2") -> { section = "s2"; sub = "" }
                line.startsWith("## S3") -> section = "s3"
                line.startsWith("### 轮 1") -> sub = "r1"
                line.startsWith("### 轮 2") -> sub = "r2"
                line.startsWith("### 轮 3") -> sub = "r3"
                line.startsWith("### 轮 4") -> sub = "r4"
                line.startsWith("## S4") -> { section = "s4"; sub = "" }
                line.startsWith("## S5") -> { section = "s5"; sub = "" }
            }
            if (line.trim() == "```text") {
                inFence = true
                fence.clear()
                return@forEach
            }
            if (inFence && line.trim() == "```") {
                inFence = false
                val key = when (section) {
                    "s3" -> sub
                    else -> section
                }
                if (key.isNotBlank()) out[key] = fence.toString().trim()
                return@forEach
            }
            if (inFence) fence.appendLine(line)
        }
        return out
    }
}
