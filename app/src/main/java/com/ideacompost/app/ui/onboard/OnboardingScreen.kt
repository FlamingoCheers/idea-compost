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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ideacompost.app.ui.theme.Clay
import com.ideacompost.app.ui.theme.ClaySoft
import com.ideacompost.app.ui.theme.Ink
import com.ideacompost.app.ui.theme.InkFaint
import com.ideacompost.app.ui.theme.InkSoft
import com.ideacompost.app.ui.theme.Line
import com.ideacompost.app.ui.theme.MossDeep
import com.ideacompost.app.ui.theme.MossSoft
import com.ideacompost.app.ui.theme.Paper
import com.ideacompost.app.ui.theme.PaperWarm
import com.ideacompost.app.ui.theme.SansNote
import com.ideacompost.app.ui.theme.SansTiny
import com.ideacompost.app.ui.theme.SerifDisplay
import com.ideacompost.app.ui.theme.SerifSection

@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    vm: OnboardingViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().background(Paper)
    ) {
        Spacer(Modifier.height(48.dp))
        Text(
            "挑选你的第一批菌域",
            style = SerifDisplay, color = Ink,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "它们决定了你的菌床最初的生态。\n没有选中的会沉睡——将来某次堆肥中，它们仍可能被唤醒。",
            style = SansNote, color = InkSoft,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(state.domains, key = { it.id }) { card ->
                val picked = card.id in state.picked
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (picked) ClaySoft else PaperWarm)
                        .border(1.5.dp, if (picked) Clay else Line, RoundedCornerShape(16.dp))
                        .clickable { vm.toggle(card.id) }
                        .padding(horizontal = 14.dp, vertical = 13.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(if (picked) Clay else MossSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            card.name.first().toString(),
                            color = if (picked) PaperWarm else MossDeep,
                            style = SerifSection,
                            fontSize = 19.sp
                        )
                    }
                    Spacer(Modifier.size(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(card.name, style = SerifSection, color = Ink, fontSize = 15.sp)
                        Text(
                            card.description, style = SansTiny, color = InkSoft,
                            fontSize = 11.5.sp, lineHeight = 16.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(if (picked) Clay else PaperWarm)
                            .border(1.5.dp, if (picked) Clay else Line, androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (picked) Text("✓", color = PaperWarm, fontSize = 13.sp)
                    }
                }
            }
        }

        Column(Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(10.dp))
            Text(
                "不确定也没关系——菌床会随着你的使用，自己长出新的菌群。",
                style = SansTiny, color = InkFaint,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                fontSize = 11.sp, lineHeight = 17.sp
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "已选 ${state.picked.size} / 7",
                    style = SansNote, color = InkSoft, fontSize = 12.5.sp
                )
                Spacer(Modifier.size(14.dp))
                Button(
                    onClick = { vm.confirm(onDone) },
                    enabled = state.canConfirm,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Clay,
                        disabledContainerColor = Line,
                        contentColor = PaperWarm,
                        disabledContentColor = InkFaint
                    )
                ) {
                    Text(
                        if (state.seeding) "正在播下菌种……" else "播下菌种",
                        fontSize = 15.sp
                    )
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}
