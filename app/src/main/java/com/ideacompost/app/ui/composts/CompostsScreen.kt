package com.ideacompost.app.ui.composts

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ideacompost.app.data.db.entity.CompostEntity
import com.ideacompost.app.ui.crumbs.BottomBar
import com.ideacompost.app.ui.theme.AmberSoft
import com.ideacompost.app.ui.theme.Amber
import com.ideacompost.app.ui.theme.Clay
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
import com.ideacompost.app.ui.theme.SerifSection
import com.ideacompost.app.ui.theme.SerifTitle
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 堆肥列表页（设计稿：🌱 tab）。 */
@Composable
fun CompostsScreen(
    onOpen: (String) -> Unit,
    onNewCompost: () -> Unit,
    vm: CompostsViewModel = hiltViewModel()
) {
    val composts by vm.composts.collectAsState()

    Column(Modifier.fillMaxSize().background(Paper)) {
        Spacer(Modifier.height(46.dp))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 22.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Text("🌱 堆肥", style = SerifTitle, color = Ink)
            Spacer(Modifier.width(8.dp))
            Text("COMPOST", fontSize = 9.sp, color = InkFaint, modifier = Modifier.padding(bottom = 4.dp))
            Spacer(Modifier.weight(1f))
            Text("${composts.size} 次", fontSize = 11.sp, color = InkFaint, modifier = Modifier.padding(bottom = 4.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "每一次发酵，都会留在土壤里。",
            fontSize = 11.5.sp, color = InkSoft, modifier = Modifier.padding(horizontal = 22.dp)
        )
        Spacer(Modifier.height(14.dp))

        if (composts.isEmpty()) {
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("🌱", fontSize = 40.sp)
                Spacer(Modifier.height(14.dp))
                Text("还没有堆肥", style = SerifSection, color = Ink, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(
                    "去面包渣里挑几颗想法，\n加点益生菌，让它们发酵。",
                    fontSize = 12.sp, color = InkSoft, textAlign = TextAlign.Center, lineHeight = 19.sp
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    "→ 去挑面包渣",
                    fontSize = 12.5.sp, color = Clay,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(ClaySoft)
                        .clickable(onClick = onNewCompost)
                        .padding(horizontal = 16.dp, vertical = 9.dp)
                )
            }
        } else {
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(composts, key = { it.id }) { c ->
                    CompostCard(c, onClick = { onOpen(c.id) })
                }
                item { Spacer(Modifier.height(6.dp)) }
            }
        }

        BottomBar(tab = 1, onCrumbs = onNewCompost, onCompost = {})
    }
}

@Composable
private fun CompostCard(c: CompostEntity, onClick: () -> Unit) {
    val (label, bg, fg) = when {
        c.status == "failed" -> Triple("中断", ClaySoft, Clay)
        c.status == "suspended" -> Triple("暂停中", Sand, InkSoft)
        c.status == "running" || c.status == "pending" -> Triple("发酵中", MossSoft, MossDeep)
        c.status == "awaiting_feedback" -> Triple("待反馈", AmberSoft, Amber)
        else -> Triple("已成熟", MossSoft, MossDeep)
    }
    val agents = runCatching {
        JSONArray(c.rosterJson).let { l -> (0 until l.length()).map { l.getJSONObject(it).optString("name") } }
    }.getOrDefault(emptyList())

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PaperWarm)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label, fontSize = 10.sp, color = fg,
                modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(bg).padding(horizontal = 6.dp, vertical = 2.5.dp)
            )
            Spacer(Modifier.weight(1f))
            Text(SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(Date(c.createdAt)), fontSize = 10.5.sp, color = InkFaint)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            c.title ?: "（发酵未完成的堆肥）",
            fontSize = 14.sp, color = Ink, fontWeight = FontWeight.Medium, lineHeight = 21.sp
        )
        if (agents.isNotEmpty()) {
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🧠 ${agents.joinToString(" · ").take(24)}${if (agents.joinToString("").length > 24) "…" else ""}", fontSize = 10.5.sp, color = InkSoft)
            }
        }
        if (c.status == "failed" || c.status == "suspended") {
            Spacer(Modifier.height(6.dp))
            Text("点开可继续 / 重来", fontSize = 10.5.sp, color = Clay)
        }
    }
}
