package com.ideacompost.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ideacompost.app.data.db.entity.LlmCallEntity
import com.ideacompost.app.domain.EcoEngine
import com.ideacompost.app.ui.theme.*

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val recent by vm.recentCalls.collectAsStateWithLifecycle()
    var showProvider by remember { mutableStateOf(false) }
    var showDonation by remember { mutableStateOf(false) }
    var showNickname by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    var pendingMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.ecoReport, state.ecoSuggestions) {
        val __rep = state.ecoReport
        if (__rep != null && __rep.actions.isEmpty() && state.ecoSuggestions.isEmpty()) {
            pendingMsg = "生态安静：菌群无需调整"
        }
    }
    LaunchedEffect(pendingMsg) {
        pendingMsg?.let {
            snackbar.showSnackbar(it)
            pendingMsg = null
        }
    }

    Scaffold(
        containerColor = Paper,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(12.dp))
            Text("← 设置", fontSize = 15.sp, color = InkSoft, modifier = Modifier.clickable { onBack() })
            Spacer(Modifier.height(18.dp))

            ProfileBlock(
                state = state,
                onCycleAvatar = vm::cycleAvatar,
                onEditNickname = { showNickname = true },
            )
            Spacer(Modifier.height(24.dp))

            EcoSection(
                state = state,
                onRunNightly = vm::runNightlyTask,
                onApply = { vm.applySuggestion(it) },
                onDismiss = { vm.dismissSuggestion(it) },
            )
            Spacer(Modifier.height(24.dp))

            DonationSection(onOpen = { showDonation = true })
            Spacer(Modifier.height(24.dp))

            ProviderCard(state, onOpen = { showProvider = true })
            Spacer(Modifier.height(16.dp))
            DemoToggle(state.demoMode, onToggle = vm::setDemoMode)
            Spacer(Modifier.height(16.dp))
            TelemetryPanel(recent)
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showProvider) {
        ProviderDialog(
            state = state,
            onClose = { showProvider = false },
            onSave = { b, k, m ->
                vm.updateProvider(b, k, m, state.demoMode)
                vm.saveProvider { msg -> pendingMsg = msg }
            },
        )
    }
    if (showDonation) DonationDialog(onClose = { showDonation = false })
    if (showNickname) {
        NicknameDialog(
            current = state.nickname,
            onSave = { vm.updateNickname(it); showNickname = false },
            onDismiss = { showNickname = false },
        )
    }
}

/* ---------------- 个人区块 ---------------- */

@Composable
private fun ProfileBlock(state: SettingsViewModel.UiState, onCycleAvatar: () -> Unit, onEditNickname: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(64.dp)
                .background(ClaySoft, CircleShape)
                .clickable { onCycleAvatar() },
            contentAlignment = Alignment.Center,
        ) { Text(state.avatarEmoji, fontSize = 34.sp) }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                state.nickname,
                style = SerifTitle,
                fontSize = 21.sp,
                color = Ink,
                modifier = Modifier.clickable { onEditNickname() },
            )
            Spacer(Modifier.height(3.dp))
            Text("思想园丁 · 认知生态管理员", fontSize = 11.sp, color = InkFaint)
            Text("点头像换形象 · 点昵称改名", fontSize = 10.sp, color = InkFaint)
        }
    }
}

/* ---------------- 菌床生态区 ---------------- */

@Composable
private fun EcoSection(
    state: SettingsViewModel.UiState,
    onRunNightly: () -> Unit,
    onApply: (EcoEngine.EcoSuggestion) -> Unit,
    onDismiss: (EcoEngine.EcoSuggestion) -> Unit,
) {
    Column {
        Text("🪴 菌床生态", style = SerifTitle, fontSize = 15.sp, color = Ink)
        Spacer(Modifier.height(10.dp))

        if (state.agents.isEmpty()) {
            Text("菌床尚未播种。", fontSize = 12.sp, color = InkFaint)
        } else {
            state.agents.take(8).forEach { a ->
                val v = a.vitality.toInt().coerceIn(0, 100)
                val statusLabel = when (a.status) {
                    "dormant" -> "休眠"
                    "compressed" -> "压缩"
                    "embryo" -> "新芽"
                    "fused" -> "已融合"
                    else -> ""
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(a.name, fontSize = 12.sp, color = Ink, modifier = Modifier.width(104.dp), maxLines = 1)
                    Box(
                        Modifier
                            .weight(1f)
                            .height(8.dp)
                            .background(Sand, RoundedCornerShape(4.dp)),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(v / 100f)
                                .fillMaxHeight()
                                .background(if (v >= 55) Moss else if (v >= 25) Amber else Clay, RoundedCornerShape(4.dp)),
                        )
                    }
                    Text(
                        (if (statusLabel.isNotEmpty()) "$statusLabel " else "") + "$v",
                        fontSize = 10.sp,
                        color = InkFaint,
                        modifier = Modifier.width(64.dp),
                        textAlign = TextAlign.End,
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Button(
            onClick = onRunNightly,
            enabled = !state.ecoRunning,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MossDeep, contentColor = Color.White),
            modifier = Modifier.fillMaxWidth().height(46.dp),
        ) {
            if (state.ecoRunning) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                Spacer(Modifier.width(10.dp))
                Text("夜间任务进行中 · ${state.ecoProgress}", fontSize = 12.sp)
            } else {
                Text("🌙 运行夜间生态任务", fontSize = 13.sp)
            }
        }
        Text(
            "重算活力 · 压缩与休眠 · 唤醒 · 增殖与融合建议（建议需你确认）",
            fontSize = 10.sp,
            color = InkFaint,
            modifier = Modifier.padding(top = 4.dp),
        )

        state.ecoReport?.let { rep ->
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(PaperWarm, RoundedCornerShape(12.dp))
                    .border(1.dp, Line, RoundedCornerShape(12.dp))
                    .padding(14.dp),
            ) {
                Text("🌙 昨夜生态报告", style = SerifTitle, fontSize = 13.sp, color = Ink)
                Spacer(Modifier.height(6.dp))
                rep.actions.forEach { act ->
                    Text("· ${typeLabel(act.type)}：${act.agentName} — ${act.detail}", fontSize = 11.sp, color = InkSoft, modifier = Modifier.padding(vertical = 2.dp))
                }
                if (rep.actions.isEmpty()) Text("· 菌群状态良好，无需自动调整", fontSize = 11.sp, color = InkFaint)
            }
        }

        state.ecoSuggestions.forEach { s ->
            Spacer(Modifier.height(10.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(AmberSoft, RoundedCornerShape(12.dp))
                    .border(1.dp, Amber.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(14.dp),
            ) {
                Text("${typeLabel(s.type)}：${s.title}", style = SerifTitle, fontSize = 13.sp, color = Ink)
                Spacer(Modifier.height(4.dp))
                Text(s.detail, fontSize = 11.sp, color = InkSoft, lineHeight = 16.sp)
                Spacer(Modifier.height(10.dp))
                Row {
                    Button(
                        onClick = { onApply(s) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Moss, contentColor = Color.White),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                    ) { Text("✓ 采纳", fontSize = 12.sp) }
                    Spacer(Modifier.width(10.dp))
                    OutlinedButton(
                        onClick = { onDismiss(s) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = InkSoft),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                    ) { Text("✗ 忽略", fontSize = 12.sp) }
                }
            }
        }
    }
}

/* ---------------- 捐赠区 ---------------- */

@Composable
private fun DonationSection(onOpen: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(PaperWarm, RoundedCornerShape(14.dp))
            .border(1.dp, Line, RoundedCornerShape(14.dp))
            .clickable { onOpen() }
            .padding(16.dp),
    ) {
        Text("❤️ 支持思想堆肥", style = SerifTitle, fontSize = 14.sp, color = Ink)
        Spacer(Modifier.height(4.dp))
        Text("独立开发，无广告无订阅。如果它陪你养出了新想法，可以请我喝一杯咖啡。", fontSize = 11.sp, color = InkSoft, lineHeight = 16.sp)
    }
}

@Composable
private fun DonationDialog(onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = PaperWarm,
        titleContentColor = Ink,
        textContentColor = InkSoft,
        title = { Text("❤️ 请作者喝杯咖啡", style = SerifTitle, fontSize = 17.sp) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    Modifier.size(160.dp).background(Sand, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) { Text("收款码\n占位", fontSize = 13.sp, color = InkFaint, textAlign = TextAlign.Center) }
                Spacer(Modifier.height(10.dp))
                Text("当前为演示占位。正式版将接入收款码。", fontSize = 11.sp, color = InkFaint)
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("好的", color = Clay) } },
    )
}

/* ---------------- 昵称对话框 ---------------- */

@Composable
private fun NicknameDialog(current: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PaperWarm,
        titleContentColor = Ink,
        textContentColor = InkSoft,
        title = { Text("怎么称呼你", style = SerifTitle, fontSize = 17.sp) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(16) },
                singleLine = true,
                placeholder = { Text("最多 16 字", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onSave(text.trim()) }) { Text("保存", color = Clay) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = InkFaint) } },
    )
}

/* ---------------- AI 服务商 / 演示模式 / 遥测 ---------------- */

@Composable
private fun typeLabel(type: String): String = when (type) {
    "vitality" -> "活力重算"
    "compressed" -> "压缩"
    "dormant" -> "休眠"
    "awakened" -> "唤醒"
    "proliferation" -> "🧬 增殖建议"
    "fusion" -> "共生融合建议"
    else -> type
}

@Composable
private fun ProviderCard(
    state: SettingsViewModel.UiState,
    onOpen: () -> Unit,
) {
    val connected = state.baseUrl.isNotBlank() && state.apiKey.isNotBlank() && state.model.isNotBlank()
    Column(
        Modifier
            .fillMaxWidth()
            .background(PaperWarm, RoundedCornerShape(14.dp))
            .border(1.dp, Line, RoundedCornerShape(14.dp))
            .clickable { onOpen() }
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🧠 AI 服务商", style = SerifTitle, fontSize = 14.sp, color = Ink)
            Spacer(Modifier.weight(1f))
            Text(
                if (state.demoMode) "演示模式" else if (connected) "已连接" else "未配置",
                fontSize = 11.sp,
                color = if (state.demoMode || connected) MossDeep else Clay,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            if (state.demoMode) "不填 Key 也能走通完整堆肥流程（产物由本地模拟生成）"
            else if (connected) "${state.model} · 直连你的服务商，Key 只存在本机"
            else "填入 OpenAI 兼容接口与 Key，堆肥时直连，不经任何中间服务器",
            fontSize = 11.sp, color = InkSoft, lineHeight = 16.sp,
        )
    }
}

@Composable
private fun DemoToggle(on: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (on) MossSoft else PaperWarm, RoundedCornerShape(14.dp))
            .border(1.dp, if (on) Moss.copy(alpha = 0.4f) else Line, RoundedCornerShape(14.dp))
            .clickable { onToggle(!on) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🎭", fontSize = 18.sp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("演示模式", fontSize = 13.5.sp, color = Ink, fontWeight = FontWeight.Medium)
            Text("不填 Key 也能走通完整堆肥流程（产物由本地模拟生成）", fontSize = 11.sp, color = InkSoft, lineHeight = 16.sp)
        }
        Box(
            Modifier.size(22.dp).background(if (on) Moss else Line, CircleShape),
            contentAlignment = Alignment.Center,
        ) { if (on) Text("✓", fontSize = 12.sp, color = PaperWarm) }
    }
}

@Composable
private fun TelemetryPanel(recent: List<LlmCallEntity>) {
    Column {
        Text("📡 调用遥测", style = SerifTitle, fontSize = 15.sp, color = Ink)
        Spacer(Modifier.height(6.dp))
        Text(
            "每次 AI 调用都会留痕（含演示模式），只记录阶段、耗时与成败，不记录内容。",
            fontSize = 11.sp, color = InkSoft, lineHeight = 16.sp,
        )
        Spacer(Modifier.height(10.dp))
        if (recent.isEmpty()) {
            Text("还没有调用记录——去丢两颗面包渣，堆一次肥试试。", fontSize = 11.5.sp, color = InkFaint)
        } else {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(PaperWarm, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                recent.forEach { c ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(7.dp).background(if (c.status == "ok") Moss else Clay, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text(c.stageKey, fontSize = 11.5.sp, color = Ink, modifier = Modifier.width(84.dp), maxLines = 1)
                        Text(
                            if (c.status == "ok") "✓" else "✗",
                            fontSize = 11.sp,
                            color = if (c.status == "ok") MossDeep else Clay,
                            modifier = Modifier.width(20.dp),
                        )
                        Text(
                            if (c.latencyMs >= 60000)
                                "${c.latencyMs / 60000}′${String.format("%02d", (c.latencyMs % 60000) / 1000)}″"
                            else "%.1fs".format(c.latencyMs / 1000.0),
                            fontSize = 11.sp, color = InkSoft,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(c.provider, fontSize = 10.sp, color = InkFaint, maxLines = 1)
                    }
                    if (c.error != null) {
                        Text(
                            "└ ${c.error}",
                            fontSize = 10.sp, color = Clay, lineHeight = 14.sp,
                            modifier = Modifier.padding(start = 15.dp, bottom = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderDialog(
    state: SettingsViewModel.UiState,
    onClose: () -> Unit,
    onSave: (baseUrl: String, apiKey: String, model: String) -> Unit,
) {
    var baseUrl by remember { mutableStateOf(state.baseUrl) }
    var apiKey by remember { mutableStateOf(state.apiKey) }
    var model by remember { mutableStateOf(state.model) }
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = PaperWarm,
        titleContentColor = Ink,
        textContentColor = InkSoft,
        title = { Text("🧠 AI 服务商", style = SerifTitle, fontSize = 17.sp) },
        text = {
            Column {
                Text(
                    "你的思想首先属于你：Key 只保存在这台设备上，堆肥时直接连你指定的服务商。",
                    fontSize = 11.sp, color = InkSoft, lineHeight = 16.sp,
                )
                Spacer(Modifier.height(12.dp))
                FieldLabel("接口地址（OpenAI 兼容）")
                Field(baseUrl, "https://api.openai.com/v1") { baseUrl = it }
                Spacer(Modifier.height(10.dp))
                FieldLabel("API Key")
                Field(apiKey, "sk-…", secret = true) { apiKey = it }
                Spacer(Modifier.height(10.dp))
                FieldLabel("模型名")
                Field(model, "gpt-4o-mini / glm-4.7 / deepseek-chat …") { model = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(baseUrl.trim(), apiKey.trim(), model.trim()); onClose() }) {
                Text("保存", color = Clay)
            }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("取消", color = InkFaint) } },
    )
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, fontSize = 11.5.sp, color = InkSoft, modifier = Modifier.padding(bottom = 6.dp))
}

@Composable
private fun Field(value: String, placeholder: String, secret: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        placeholder = { Text(placeholder, fontSize = 12.sp, color = InkFaint) },
        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = Ink),
        modifier = Modifier.fillMaxWidth(),
    )
}