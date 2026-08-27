package com.ideacompost.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.ideacompost.app.ui.theme.SansNote
import com.ideacompost.app.ui.theme.SansTiny
import com.ideacompost.app.ui.theme.SerifSection
import com.ideacompost.app.ui.theme.SerifTitle

/** 设置页：BYO Key（08 §5）——仅存本地、仅直连你的服务商；或使用演示模式。 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current

    Column(
        Modifier.fillMaxSize().background(Paper).verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth().padding(start = 10.dp, end = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("←", fontSize = 20.sp, color = InkSoft, modifier = Modifier.clickable(onClick = onBack).padding(10.dp))
            Text("设置", style = SerifTitle, color = Ink)
        }

        Column(Modifier.padding(horizontal = 22.dp)) {
            Spacer(Modifier.height(10.dp))
            Text("🧠 AI 服务商", style = SerifSection, color = Ink)
            Spacer(Modifier.height(6.dp))
            Text(
                "你的思想首先属于你：Key 只保存在这台设备上，堆肥时直接连你指定的服务商，不经过任何中间服务器。",
                fontSize = 11.5.sp, color = InkSoft, lineHeight = 18.sp
            )
            Spacer(Modifier.height(14.dp))

            FieldLabel("接口地址（OpenAI 兼容）")
            Field(
                value = state.baseUrl,
                placeholder = "https://api.openai.com/v1",
                onChange = { vm.update(it, state.apiKey, state.model, state.mockMode) }
            )
            Spacer(Modifier.height(12.dp))

            FieldLabel("API Key")
            Field(
                value = state.apiKey,
                placeholder = "sk-…",
                secret = true,
                onChange = { vm.update(state.baseUrl, it, state.model, state.mockMode) }
            )
            Spacer(Modifier.height(12.dp))

            FieldLabel("模型名")
            Field(
                value = state.model,
                placeholder = "gpt-4o-mini / glm-4.7 / deepseek-chat …",
                onChange = { vm.update(state.baseUrl, state.apiKey, it, state.mockMode) }
            )
            Spacer(Modifier.height(18.dp))

            // 演示模式开关
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (state.mockMode) MossSoft else PaperWarm)
                    .clickable { vm.update(state.baseUrl, state.apiKey, state.model, !state.mockMode) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🎭", fontSize = 18.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("演示模式", fontSize = 13.5.sp, color = Ink, fontWeight = FontWeight.Medium)
                    Text(
                        "不填 Key 也能走通完整堆肥流程（产物由本地模拟生成）",
                        fontSize = 11.sp, color = InkSoft, lineHeight = 16.sp
                    )
                }
                Box(
                    Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (state.mockMode) Moss else Line),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.mockMode) Text("✓", fontSize = 12.sp, color = PaperWarm)
                }
            }

            Spacer(Modifier.height(22.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (state.saved) Moss else Clay)
                    .clickable {
                        vm.save { msg -> android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_SHORT).show() }
                    }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(if (state.saved) "已保存 ✓" else "保存", fontSize = 13.5.sp, color = PaperWarm, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                if (state.mockMode) "当前：演示模式" else if (state.baseUrl.isNotBlank() && state.apiKey.isNotBlank() && state.model.isNotBlank()) "当前：已连接你的服务商" else "当前：未配置完整（堆肥前需补全或切回演示模式）",
                fontSize = 11.sp,
                color = if (state.mockMode || (state.baseUrl.isNotBlank() && state.apiKey.isNotBlank() && state.model.isNotBlank())) MossDeep else Clay,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, fontSize = 11.5.sp, color = InkSoft, modifier = Modifier.padding(bottom = 6.dp))
}

@Composable
private fun Field(value: String, placeholder: String, secret: Boolean = false, onChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        placeholder = { Text(placeholder, fontSize = 12.sp, color = InkFaint) },
        visualTransformation = if (secret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = Ink),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = PaperWarm,
            unfocusedContainerColor = PaperWarm,
            focusedIndicatorColor = Clay,
            unfocusedIndicatorColor = Line,
            cursorColor = Clay
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    )
}
