package com.ideacompost.app.ui.crumbs

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ideacompost.app.data.db.entity.IdeaEntity
import com.ideacompost.app.ui.theme.Clay
import com.ideacompost.app.ui.theme.Ink
import com.ideacompost.app.ui.theme.InkSoft
import com.ideacompost.app.ui.theme.Line
import com.ideacompost.app.ui.theme.Moss
import com.ideacompost.app.ui.theme.Paper
import com.ideacompost.app.ui.theme.PaperWarm
import com.ideacompost.app.ui.theme.Sand
import com.ideacompost.app.ui.theme.SerifTitle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 首页：面包渣。顶部低阻力输入 + 瀑布流卡片（设计稿 home 屏）。 */
@Composable
fun CrumbsScreen(vm: CrumbsViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val crumbs by vm.crumbs.collectAsState()
    var editing by remember { mutableStateOf<IdeaEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .padding(horizontal = 20.dp)
            .imePadding()
    ) {
        Spacer(Modifier.height(52.dp))
        Text("🍞 思想面包渣", style = SerifTitle, color = Ink)
        Spacer(Modifier.height(4.dp))
        Text(
            if (state.justSaved) "已丢进堆肥场 ✅ 继续丢？" else "今天脑子里有什么面包渣？",
            fontSize = 13.sp, color = InkSoft
        )
        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.input,
                onValueChange = vm::onInputChange,
                placeholder = { Text("想到什么写什么，不用完整……", fontSize = 14.sp) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                minLines = 1,
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Moss,
                    unfocusedBorderColor = Line,
                    focusedContainerColor = PaperWarm,
                    unfocusedContainerColor = PaperWarm
                )
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = vm::saveCrumb,
                enabled = state.input.isNotBlank(),
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(if (state.input.isNotBlank()) Clay else Line)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "保存", tint = PaperWarm)
            }
        }
        Spacer(Modifier.height(16.dp))

        if (crumbs.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "这里还空着。\n\n不需要好想法，碎片就行——\n一句话、一个疑问、半截直觉。",
                    textAlign = TextAlign.Center, fontSize = 14.sp, color = InkSoft,
                    lineHeight = 24.sp
                )
            }
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalItemSpacing = 10.dp
            ) {
                items(crumbs, key = { it.id }) { crumb ->
                    CrumbCard(crumb) { editing = crumb }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    editing?.let { crumb ->
        EditCrumbDialog(
            initial = crumb.content,
            onDismiss = { editing = null },
            onSave = { vm.updateCrumb(crumb.id, it); editing = null }
        )
    }
}

@Composable
private fun CrumbCard(crumb: IdeaEntity, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PaperWarm)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Text(
            crumb.content,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            color = Ink,
            maxLines = 10
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (crumb.status == "composted") Moss else Sand)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                formatTime(crumb.createdAt),
                fontSize = 11.sp, color = InkSoft
            )
            if (crumb.status == "composted") {
                Spacer(Modifier.width(6.dp))
                Text("已发酵", fontSize = 11.sp, color = Moss, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun EditCrumbDialog(
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修一修这颗面包渣", fontWeight = FontWeight.SemiBold) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().height(200.dp),
                shape = RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = { onSave(text) },
                enabled = text.isNotBlank()
            ) { Text("保存", color = Clay) }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("算了", color = InkSoft) }
        }
    )
}

private val timeFmt = SimpleDateFormat("M月d日 HH:mm", Locale.CHINA)
private fun formatTime(ts: Long): String = timeFmt.format(Date(ts))
