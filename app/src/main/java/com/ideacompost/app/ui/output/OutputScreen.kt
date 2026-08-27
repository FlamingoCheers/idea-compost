package com.ideacompost.app.ui.output

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ideacompost.app.ui.theme.Amber
import com.ideacompost.app.ui.theme.AmberSoft
import com.ideacompost.app.ui.theme.Clay
import com.ideacompost.app.ui.theme.ClayDeep
import com.ideacompost.app.ui.theme.ClaySoft
import com.ideacompost.app.ui.theme.Ink
import com.ideacompost.app.ui.theme.InkFaint
import com.ideacompost.app.ui.theme.InkSoft
import com.ideacompost.app.ui.theme.Line
import com.ideacompost.app.ui.theme.Moss
import com.ideacompost.app.ui.theme.MossDeep
import com.ideacompost.app.ui.theme.MossSoft
import com.ideacompost.app.ui.theme.Paper
import com.ideacompost.app.ui.theme.PaperWarm
import com.ideacompost.app.ui.theme.Sand
import com.ideacompost.app.ui.theme.SansNote
import com.ideacompost.app.ui.theme.SansTiny
import com.ideacompost.app.ui.theme.SerifSection
import com.ideacompost.app.ui.theme.SerifTitle
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 堆肥产物阅读页（设计稿 output 屏）：十节 + 置信徽章 + 反方 + 菌群营养 + 反馈四键。 */
@Composable
fun OutputScreen(
    onBack: () -> Unit,
    vm: OutputViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val c = state.compost
    val ctx = LocalContext.current

    if (c == null || c.outputJson == null) {
        Box(Modifier.fillMaxSize().background(Paper))
        return
    }
    val output = remember(c.outputJson) { runCatching { JSONObject(c.outputJson!!) }.getOrNull() }
    if (output == null) {
        Box(Modifier.fillMaxSize().background(Paper))
        return
    }

    val nutrition = remember(c.nutritionJson) {
        c.nutritionJson?.let { runCatching { JSONObject(it) }.getOrNull() }
    }
    val rounds = if (c.depth == "shallow") 2 else 3
    val depthLabel = when (c.depth) {
        "shallow" -> "浅层发酵"
        "deep" -> "深度发酵"
        else -> "标准发酵"
    }
    val agentsUsed = output.optJSONArray("agents_used")?.toObjList() ?: emptyList()
    val emergingCount = listOf("core_ideas", "forming_judgments")
        .sumOf { key -> output.optJSONArray(key)?.toObjList()?.count { it.optString("confidence") != "strong" } ?: 0 }

    Column(
        Modifier.fillMaxSize().background(Paper).verticalScroll(rememberScrollState())
    ) {
        // 顶栏
        Row(
            Modifier.fillMaxWidth().padding(start = 10.dp, end = 18.dp, top = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("←", fontSize = 20.sp, color = InkSoft, modifier = Modifier.clickable(onClick = onBack).padding(10.dp))
            Text("🌱 堆肥", fontSize = 13.sp, color = InkSoft)
        }

        Column(Modifier.padding(horizontal = 22.dp)) {
            Text(
                output.optString("title", "（初步）一次堆肥"),
                style = SerifTitle, color = Ink, lineHeight = 30.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(Date(c.createdAt)) + " · 这批思想长出来的样子",
                fontSize = 11.sp, color = InkFaint
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(if (emergingCount > 0) "🟡 初步判断" else "🌿 发酵产物", AmberSoft, Amber)
                Chip("${agentsUsed.size} 位菌群参与", MossSoft, MossDeep)
                Chip("$depthLabel · ${rounds}轮", Sand, InkSoft)
            }
        }

        Spacer(Modifier.height(18.dp))

        // ① 核心思想
        Section("🌱 核心思想") {
            output.optJSONArray("core_ideas")?.toObjList()?.forEach { item ->
                ClaimRow(
                    text = item.optString("text"),
                    confidence = item.optString("confidence", "emerging"),
                    sources = item.optJSONArray("source_claims")?.toStringList() ?: emptyList()
                )
            }
        }

        // ② 碎片联系
        output.optJSONArray("fragment_links")?.toObjList()?.takeIf { it.isNotEmpty() }?.let { links ->
            Section("🧩 原始碎片之间的联系") {
                links.forEach { l ->
                    InfoCard(PaperWarm) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Tag("碎片${circ(l.optInt("from_idea", 0))}", MossSoft, MossDeep)
                            Text(" → ", fontSize = 12.sp, color = InkFaint)
                            Tag("碎片${circ(l.optInt("to_idea", 1))}", MossSoft, MossDeep)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(l.optString("relation"), fontSize = 13.sp, color = Ink, lineHeight = 20.sp)
                    }
                }
            }
        }

        // ③ 正在形成的判断
        output.optJSONArray("forming_judgments")?.toObjList()?.takeIf { it.isNotEmpty() }?.let { js ->
            Section("🌿 正在形成的判断") {
                js.forEach { item ->
                    ClaimRow(
                        text = item.optString("text"),
                        confidence = item.optString("confidence", "emerging"),
                        sources = item.optJSONArray("source_claims")?.toStringList() ?: emptyList()
                    )
                }
            }
        }

        // ④ 新连接
        output.optJSONArray("new_connections")?.toObjList()?.takeIf { it.isNotEmpty() }?.let { cs ->
            Section("🧬 新产生的连接") {
                cs.forEach { InfoCard(PaperWarm) { Text(textOf(it), fontSize = 13.sp, color = Ink, lineHeight = 20.sp) } }
            }
        }

        // ⑤ 冲突
        output.optJSONArray("conflicts")?.toObjList()?.takeIf { it.isNotEmpty() }?.let { cs ->
            Section("🔥 可能存在的冲突") {
                cs.forEach { cf ->
                    InfoCard(PaperWarm) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Tag(cf.optString("a").take(14), ClaySoft, ClayDeep)
                            Text(" ⚡ ", fontSize = 12.sp, color = InkFaint)
                            Tag(cf.optString("b").take(14), ClaySoft, ClayDeep)
                            Spacer(Modifier.weight(1f))
                            Text(cf.optString("nature", ""), fontSize = 10.5.sp, color = InkFaint)
                        }
                        cf.optString("resolution_hint")?.takeIf { it.isNotBlank() }?.let {
                            Spacer(Modifier.height(8.dp))
                            Text("化解线索：$it", fontSize = 12.sp, color = InkSoft, lineHeight = 18.sp)
                        }
                    }
                }
            }
        }

        // ⑥ 最强反方
        output.optJSONObject("strongest_objection")?.let { ob ->
            Section("👿 最强反方观点") {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(width = 3.dp, color = Clay, shape = RoundedCornerShape(10.dp))
                        .background(ClaySoft)
                        .padding(14.dp)
                ) {
                    Text("「${ob.optString("text")}」", fontSize = 13.5.sp, color = ClayDeep, lineHeight = 21.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "—— ${ob.optString("by", "反驳菌")} 构建" +
                            (ob.optJSONArray("source_claims")?.toStringList()?.takeIf { it.isNotEmpty() }?.let { " · 依据 ${it.joinToString("、")}" } ?: ""),
                        fontSize = 11.sp, color = ClayDeep
                    )
                }
            }
        }

        // ⑦ 未解决的问题
        output.optJSONArray("open_questions")?.toStringList()?.takeIf { it.isNotEmpty() }?.let { qs ->
            Section("❓ 尚未解决的问题") {
                qs.forEach { q ->
                    Row(Modifier.padding(vertical = 5.dp)) {
                        Text("· ", fontSize = 13.sp, color = InkFaint)
                        Text(q, fontSize = 13.sp, color = Ink, lineHeight = 20.sp)
                    }
                }
            }
        }

        // ⑧ 继续发展的方向（可转面包渣）
        output.optJSONArray("future_directions")?.toObjList()?.takeIf { it.isNotEmpty() }?.let { ds ->
            Section("🌱 可以继续发展的方向") {
                var savedIdx by remember { mutableStateOf(-1) }
                ds.forEachIndexed { i, d ->
                    InfoCard(if (savedIdx == i) MossSoft else PaperWarm) {
                        Text(d.optString("text"), fontSize = 13.sp, color = Ink, lineHeight = 20.sp)
                        d.optString("as_crumbs")?.takeIf { it.isNotBlank() }?.let { crumb ->
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (savedIdx == i) "✓ 已丢进面包渣" else "→ 变成面包渣，继续养",
                                fontSize = 12.sp,
                                color = if (savedIdx == i) MossDeep else Clay,
                                modifier = Modifier.clickable(enabled = savedIdx != i) {
                                    vm.saveAsCrumb(crumb) {
                                        savedIdx = i
                                        android.widget.Toast.makeText(ctx, "已丢进堆肥场 🍞", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // ⑨ 本次菌群 + 营养
        Section("🧠 本次堆肥使用的菌群") {
            val awards = nutrition?.optJSONArray("nutrition_awarded")?.toObjList() ?: emptyList()
            agentsUsed.forEach { a ->
                val name = a.optString("agent")
                val award = awards.firstOrNull { it.optString("agent") == name }
                val isWildcard = a.optString("quota_role") == "wildcard"
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(30.dp).clip(CircleShape).background(if (isWildcard) AmberSoft else MossSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(name.take(1), fontSize = 14.sp, color = if (isWildcard) Amber else MossDeep)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(name, fontSize = 13.5.sp, color = Ink, fontWeight = FontWeight.Medium)
                            if (isWildcard) {
                                Spacer(Modifier.width(6.dp))
                                Tag("意外菌", AmberSoft, Amber)
                            }
                        }
                        Text(a.optString("key_contribution", ""), fontSize = 11.5.sp, color = InkSoft, lineHeight = 17.sp)
                    }
                    award?.let {
                        Text(
                            "+${it.optDouble("score", 0.0)}",
                            fontSize = 13.sp, color = MossDeep, fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            if (nutrition == null) {
                Text("营养结算中……", fontSize = 11.5.sp, color = InkFaint, modifier = Modifier.padding(top = 4.dp))
            }
            nutrition?.optString("ecology_note")?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(10.dp))
                Text("💭 $it", fontSize = 11.5.sp, color = InkSoft, lineHeight = 18.sp,
                    modifier = Modifier.background(MossSoft).padding(10.dp).fillMaxWidth())
            }
        }

        // ⑩ 来源面包渣
        Section("🍞 来源面包渣") {
            state.crumbs.forEachIndexed { i, crumb ->
                Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
                    Tag("碎片${circ(i)}", Sand, InkSoft)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        crumb.content.replace("\n", " ").take(60) + if (crumb.content.length > 60) "……" else "",
                        fontSize = 12.5.sp, color = InkSoft, lineHeight = 19.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // 反馈四键
        val given = state.feedbackGiven
        Column(
            Modifier.fillMaxWidth().background(PaperWarm).padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Text(
                "这次发酵怎么样？你的反应会成为菌床的养分。",
                fontSize = 11.5.sp, color = InkSoft, modifier = Modifier.padding(bottom = 10.dp)
            )
            if (given != null) {
                Text(
                    when (given) {
                        "heart" -> "❤️ 已记录——菌群收到了启发"
                        "star" -> "⭐ 已存入菌床，值得保留"
                        "develop" -> "↗ 已加入继续发展清单"
                        else -> "? 已记录分歧——下次堆肥会引入对立菌"
                    },
                    fontSize = 12.5.sp, color = MossDeep,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FbButton(Modifier.weight(1f), "❤️", "有启发", "heart", vm, ctx)
                    FbButton(Modifier.weight(1f), "⭐", "值得保留", "star", vm, ctx)
                    FbButton(Modifier.weight(1f), "↗", "继续发展", "develop", vm, ctx)
                    FbButton(Modifier.weight(1f), "?", "我不同意", "disagree", vm, ctx)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FbButton(
    modifier: Modifier = Modifier,
    emoji: String, label: String, kind: String,
    vm: OutputViewModel, ctx: android.content.Context
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Paper)
            .border(1.dp, Line, RoundedCornerShape(10.dp))
            .clickable { vm.feedback(kind) { msg -> android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_SHORT).show() } }
            .padding(vertical = 9.dp)
    ) {
        Text(emoji, fontSize = 17.sp)
        Spacer(Modifier.height(3.dp))
        Text(label, fontSize = 10.5.sp, color = InkSoft)
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp)) {
        Text(title, style = SerifSection, color = Ink)
        Spacer(Modifier.height(10.dp))
        content()
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ClaimRow(text: String, confidence: String, sources: List<String>) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row {
            Text("· ", fontSize = 13.5.sp, color = InkFaint)
            Text(text, fontSize = 13.5.sp, color = Ink, lineHeight = 21.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 5.dp)) {
            if (confidence == "emerging") Tag("初步", AmberSoft, Amber) else Tag("较可靠", MossSoft, MossDeep)
            if (sources.isNotEmpty()) {
                Spacer(Modifier.width(6.dp))
                Text("依据 ${sources.joinToString("、")}", fontSize = 10.5.sp, color = InkFaint)
            }
        }
    }
}

@Composable
private fun Chip(text: String, bg: androidx.compose.ui.graphics.Color, fg: androidx.compose.ui.graphics.Color) {
    Text(
        text, fontSize = 10.5.sp, color = fg,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(bg).padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun Tag(text: String, bg: androidx.compose.ui.graphics.Color, fg: androidx.compose.ui.graphics.Color) {
    Text(
        text, fontSize = 10.sp, color = fg,
        modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(bg).padding(horizontal = 6.dp, vertical = 2.5.dp)
    )
}

@Composable
private fun InfoCard(bg: androidx.compose.ui.graphics.Color, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(bg).padding(12.dp)
    ) { content() }
    Spacer(Modifier.height(8.dp))
}

private fun textOf(o: JSONObject): String =
    o.optString("text", o.optString("relation", o.optString("description", o.toString())))

private fun circ(i: Int): String = listOf("①", "②", "③", "④", "⑤", "⑥", "⑦", "⑧").getOrElse(i) { "·" }

private fun JSONArray.toObjList(): List<JSONObject> =
    (0 until length()).mapNotNull { optJSONObject(it) }

private fun JSONArray.toStringList(): List<String> =
    (0 until length()).map { optString(it) }
