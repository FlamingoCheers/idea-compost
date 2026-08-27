package com.ideacompost.app.ui.setup

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ideacompost.app.ui.theme.Amber
import com.ideacompost.app.ui.theme.Clay
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

/** 堆肥设置（设计稿 setup 屏）：原料→菌种→火候。 */
@Composable
fun SetupScreen(
    onFired: (String) -> Unit,
    onBack: () -> Unit,
    vm: SetupViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val roundCounts = mapOf("shallow" to 2, "standard" to 3, "deep" to 3)
    val estCalls = 3 + (roundCounts[state.depth] ?: 3) * 5

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(36.dp))
        Text(
            "← 返回",
            fontSize = 14.sp, color = InkSoft,
            modifier = Modifier.clickable(onClick = onBack)
        )
        Spacer(Modifier.height(12.dp))
        Text("开始一次堆肥", style = SerifTitle, color = Ink)
        Spacer(Modifier.height(6.dp))
        Text(
            "选好的面包渣将被菌群们一起发酵",
            fontSize = 12.sp, color = InkFaint, letterSpacing = 0.9.sp
        )
        Spacer(Modifier.height(22.dp))

        BlockHeader("🍞 面包渣", "${state.ideas.size} / 无上限")
        Spacer(Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            state.ideas.take(3).forEach { idea ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(PaperWarm)
                        .border(1.dp, Line, RoundedCornerShape(50))
                        .clickable { vm.removeIdea(idea.id) }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        "${idea.content.take(12)}… ✕",
                        fontSize = 12.sp, color = InkSoft, maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        BlockHeader("🦠 思想益生菌", "最多 2 个 · 可不选")
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.probiotics.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { pb ->
                        val on = pb.id in state.picked
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(15.dp))
                                .background(if (on) MossSoft else PaperWarm)
                                .border(1.5.dp, if (on) Moss else Line, RoundedCornerShape(15.dp))
                                .clickable { vm.toggleProbiotic(pb.id) }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    pb.name.removeSuffix("益生菌"),
                                    style = SerifSection, color = Ink, fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(if (on) Moss else Paper)
                                        .border(1.5.dp, if (on) Moss else Line, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (on) Text("✓", color = PaperWarm, fontSize = 10.sp)
                                }
                            }
                            Text(
                                pb.description, fontSize = 11.sp, color = InkSoft,
                                lineHeight = 16.sp,
                                maxLines = 2, overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        BlockHeader("发酵深度", "影响轮数与时长")
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(13.dp))
                .background(Sand)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                "shallow" to ("浅" to "约 3 分钟"),
                "standard" to ("标准" to "约 5 分钟"),
                "deep" to ("深" to "约 10 分钟")
            ).forEach { (key, label) ->
                val on = state.depth == key
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (on) PaperWarm else Sand)
                        .clickable { vm.setDepth(key) }
                        .padding(vertical = 9.dp)
                ) {
                    Text(
                        label.first, fontSize = 12.5.sp,
                        color = if (on) Ink else InkSoft,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal
                    )
                    Text(label.second, fontSize = 9.5.sp, color = InkFaint)
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        if (!state.providerReady) {
            Text(
                "还未配置模型服务（设置 → 模型服务），当前无法点火。",
                fontSize = 12.sp, color = Clay, lineHeight = 18.sp
            )
            Spacer(Modifier.height(10.dp))
        }
        Button(
            onClick = { vm.fire(onFired) },
            enabled = state.ideas.isNotEmpty() && state.providerReady && !state.firing,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Clay,
                disabledContainerColor = Sand,
                contentColor = PaperWarm,
                disabledContentColor = InkFaint
            )
        ) {
            Text(
                if (state.firing) "点火中……" else "🔥 开始堆肥",
                fontSize = 15.5.sp
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            buildString {
                append("预计消耗约 $estCalls 次模型调用 · 可随时中断，产物自动保存")
                if (state.mockMode) append("\n演示模式：无需 API Key，产物由本地模拟生成")
            },
            fontSize = 11.sp, color = InkFaint, lineHeight = 17.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(34.dp))
    }
}

@Composable
private fun BlockHeader(title: String, right: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        Spacer(Modifier.weight(1f))
        Text(right, fontSize = 12.sp, color = InkFaint, letterSpacing = 0.8.sp)
    }
}
