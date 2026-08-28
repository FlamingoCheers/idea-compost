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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ideacompost.app.data.db.entity.ProbioticEntity
import com.ideacompost.app.ui.theme.Amber
import com.ideacompost.app.ui.theme.AmberSoft
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
    var showManage by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ProbioticEntity?>(null) }
    var creating by remember { mutableStateOf(false) }

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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🦠 思想益生菌", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Spacer(Modifier.width(10.dp))
            Text(
                "管理 / 自定义",
                fontSize = 11.5.sp, color = MossDeep,
                modifier = Modifier.clickable { showManage = true }
            )
            Spacer(Modifier.weight(1f))
            Text("最多 2 个 · 可不选", fontSize = 12.sp, color = InkFaint, letterSpacing = 0.8.sp)
        }
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

    if (showManage) {
        ManageProbioticsDialog(
            probiotics = state.probiotics,
            onClose = { showManage = false },
            onCreate = { showManage = false; creating = true },
            onEdit = { showManage = false; editing = it },
            onDelete = { vm.deleteProbiotic(it.id) }
        )
    }
    if (creating || editing != null) {
        ProbioticEditorDialog(
            initial = editing,
            onDismiss = { creating = false; editing = null },
            onSave = { name, desc ->
                vm.upsertProbiotic(editing?.id, name, desc)
                creating = false; editing = null
            }
        )
    }
}

/** 益生菌管理：内置可删除（软删），自定义可编辑可删除，底部入口新建。 */
@Composable
private fun ManageProbioticsDialog(
    probiotics: List<ProbioticEntity>,
    onClose: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (ProbioticEntity) -> Unit,
    onDelete: (ProbioticEntity) -> Unit
) {
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = PaperWarm,
        shape = RoundedCornerShape(18.dp),
        title = { Text("🦠 益生菌管理", style = SerifSection, color = Ink) },
        text = {
            Column {
                Text(
                    "内置菌种可隐藏；自定义菌种可编辑、可删除。隐藏或删除后不再出现在选项中。",
                    fontSize = 11.sp, color = InkSoft, lineHeight = 16.sp
                )
                Spacer(Modifier.height(10.dp))
                Column(
                    Modifier
                        .height(320.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    probiotics.forEach { pb ->
                        val custom = pb.scope == "user_defined"
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        pb.name, fontSize = 13.sp, color = Ink,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        if (custom) "自定义" else "内置",
                                        fontSize = 9.sp, color = if (custom) Amber else InkFaint,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(if (custom) AmberSoft else Sand)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    pb.description, fontSize = 10.5.sp, color = InkSoft,
                                    lineHeight = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (custom) {
                                Text(
                                    "编辑",
                                    fontSize = 12.sp, color = MossDeep,
                                    modifier = Modifier.clickable { onEdit(pb) }.padding(6.dp)
                                )
                            }
                            Text(
                                if (custom) "删除" else "隐藏",
                                fontSize = 12.sp, color = Clay,
                                modifier = Modifier.clickable { onDelete(pb) }.padding(6.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(11.dp))
                        .background(MossSoft)
                        .clickable(onClick = onCreate)
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("＋ 自定义益生菌", fontSize = 12.5.sp, color = MossDeep)
                }
            }
        },
        confirmButton = {
            Text("完成", fontSize = 13.sp, color = Ink,
                modifier = Modifier.clickable(onClick = onClose).padding(horizontal = 8.dp, vertical = 4.dp))
        }
    )
}

/** 新建/编辑益生菌：名称 + 思考方向描述（描述即 prompt_logic 的核心）。 */
@Composable
private fun ProbioticEditorDialog(
    initial: ProbioticEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial?.name?.removeSuffix("益生菌") ?: "") }
    var desc by remember(initial) { mutableStateOf(initial?.description ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PaperWarm,
        shape = RoundedCornerShape(18.dp),
        title = { Text(if (initial == null) "自定义益生菌" else "编辑益生菌", style = SerifSection, color = Ink) },
        text = {
            Column {
                Text(
                    "它将成为本次发酵的一种思考视角，下次堆肥也可以继续使用。",
                    fontSize = 11.sp, color = InkSoft, lineHeight = 16.sp
                )
                Spacer(Modifier.height(12.dp))
                Text("名称", fontSize = 11.sp, color = InkSoft, modifier = Modifier.padding(bottom = 4.dp))
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    placeholder = { Text("如：文学视角（自动加「益生菌」后缀）", fontSize = 11.sp, color = InkFaint) },
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = Ink),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Paper, unfocusedContainerColor = Paper,
                        focusedIndicatorColor = Moss, unfocusedIndicatorColor = Line, cursorColor = Moss
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Text("思考方向", fontSize = 11.sp, color = InkSoft, modifier = Modifier.padding(bottom = 4.dp))
                TextField(
                    value = desc,
                    onValueChange = { desc = it },
                    minLines = 3,
                    placeholder = { Text("如：从叙事结构、意象与隐喻的角度切入，关注故事如何塑造判断", fontSize = 11.sp, color = InkFaint, lineHeight = 15.sp) },
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.5.sp, color = Ink, lineHeight = 18.sp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Paper, unfocusedContainerColor = Paper,
                        focusedIndicatorColor = Moss, unfocusedIndicatorColor = Line, cursorColor = Moss
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Text(
                if (name.isNotBlank() && desc.isNotBlank()) "保存" else "填写完整后保存",
                fontSize = 13.sp,
                color = if (name.isNotBlank() && desc.isNotBlank()) Clay else InkFaint,
                modifier = Modifier.clickable {
                    if (name.isNotBlank() && desc.isNotBlank()) onSave(name, desc)
                }.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        },
        dismissButton = {
            Text("取消", fontSize = 13.sp, color = InkSoft,
                modifier = Modifier.clickable(onClick = onDismiss).padding(horizontal = 8.dp, vertical = 4.dp))
        }
    )
}

@Composable
private fun BlockHeader(title: String, right: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        Spacer(Modifier.weight(1f))
        Text(right, fontSize = 12.sp, color = InkFaint, letterSpacing = 0.8.sp)
    }
}
