package com.ideacompost.app.ui.onboard

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ideacompost.app.ui.theme.Amber
import com.ideacompost.app.ui.theme.Clay
import com.ideacompost.app.ui.theme.Ink
import com.ideacompost.app.ui.theme.InkSoft
import com.ideacompost.app.ui.theme.Line
import com.ideacompost.app.ui.theme.Moss
import com.ideacompost.app.ui.theme.Paper
import com.ideacompost.app.ui.theme.PaperWarm
import com.ideacompost.app.ui.theme.Sand
import com.ideacompost.app.ui.theme.SerifTitle

/** 首启：菌域 7 选 N（02 §1 / 设计稿 onboard 屏）。未选菌域休眠，日后可由增殖生成。 */
@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    vm: OnboardingViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(64.dp))
        Text("欢迎来到你的", style = SerifTitle, color = Ink)
        Text("思想堆肥场", style = SerifTitle, color = Clay)
        Spacer(Modifier.height(12.dp))
        Text(
            "你的每一条念头都是面包渣。\n\n先选择几支领域菌，作为菌床的起点——\n其余的菌，会在你思考的过程中自己长出来。",
            fontSize = 15.sp,
            lineHeight = 24.sp,
            color = InkSoft
        )
        Spacer(Modifier.height(28.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(state.domains, key = { it.id }) { card ->
                val picked = card.id in state.picked
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (picked) Sand else PaperWarm)
                        .border(1.dp, if (picked) Moss else Line, RoundedCornerShape(14.dp))
                        .clickable { vm.toggle(card.id) }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (picked) Moss else Line),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            card.name.first().toString(),
                            color = PaperWarm,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.size(14.dp))
                    Column {
                        Text(card.name, fontWeight = FontWeight.SemiBold, color = Ink)
                        Text(card.description, fontSize = 12.sp, color = InkSoft)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "已选 ${state.picked.size} 支 · 可多选，至少 1 支",
            fontSize = 12.sp,
            color = if (state.picked.isEmpty()) Clay else Amber
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { vm.confirm(onDone) },
            enabled = state.canConfirm,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Clay)
        ) {
            Text(if (state.seeding) "正在为你准备菌床……" else "开始堆肥生活", fontSize = 16.sp)
        }
        Spacer(Modifier.height(32.dp))
    }
}
