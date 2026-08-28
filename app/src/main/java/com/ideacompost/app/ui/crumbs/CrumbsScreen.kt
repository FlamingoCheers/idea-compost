package com.ideacompost.app.ui.crumbs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ideacompost.app.data.db.entity.IdeaEntity
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
import com.ideacompost.app.ui.theme.SansTiny
import com.ideacompost.app.ui.theme.SerifBody
import com.ideacompost.app.ui.theme.SerifDisplay
import com.ideacompost.app.ui.theme.SerifSection
import com.ideacompost.app.ui.theme.Wordmark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CrumbsScreen(
    onTabCompost: () -> Unit,
    onSetup: (List<String>) -> Unit,
    onSettings: () -> Unit,
    vm: CrumbsViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val crumbs by vm.crumbs.collectAsState()
    var editing by remember { mutableStateOf<IdeaEntity?>(null) }

    Box(Modifier.fillMaxSize().background(Paper)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp)
                .imePadding()
        ) {
            Spacer(Modifier.height(44.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.weight(1f)) {
                    Text("思想堆肥", style = Wordmark, color = Ink)
                    Text(
                        "IDEA COMPOST", fontSize = 9.5.sp, color = InkFaint,
                        letterSpacing = 1.4.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Moss)
                        .clickable(onClick = onSettings),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        remember { vm.profile.avatarEmoji },
                        color = PaperWarm, fontSize = 17.sp
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            val prompt: AnnotatedString = buildAnnotatedString {
                append("今天，脑子里\n有什么")
                pushStyle(SpanStyle(color = Clay))
                append("面包渣")
                pop()
                append("？")
            }
            Text(prompt, style = SerifDisplay, color = Ink)

            Spacer(Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(PaperWarm)
                    .border(1.dp, Line, RoundedCornerShape(18.dp))
                    .padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 10.dp)
            ) {
                OutlinedTextField(
                    value = state.input,
                    onValueChange = vm::onInputChange,
                    placeholder = {
                        Text("想到什么就丢什么，不用完整，不用正确……", fontSize = 14.5.sp, color = InkFaint)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2,
                    maxLines = 6,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GhostChip("粘贴")
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = vm::saveCrumb,
                        enabled = state.input.isNotBlank(),
                        modifier = Modifier.height(38.dp),
                        shape = RoundedCornerShape(11.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Clay,
                            disabledContainerColor = Sand,
                            contentColor = PaperWarm,
                            disabledContentColor = InkFaint
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp)
                    ) {
                        Text("丢进去", fontSize = 14.sp)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (state.selecting) "已选 ${state.selected.size} 颗" else "最近的面包渣",
                    style = SerifSection, color = Ink, fontSize = 15.sp
                )
                Spacer(Modifier.weight(1f))
                Text(
                    if (state.selecting) "点选 · 再点取消" else "长按多选 → 堆肥",
                    fontSize = 12.sp, color = InkFaint, letterSpacing = 0.9.sp,
                    modifier = Modifier.clickable(enabled = crumbs.isNotEmpty()) {
                        crumbs.firstOrNull()?.let { vm.toggleSelect(it.id) }
                    }
                )
            }
            Spacer(Modifier.height(12.dp))

            if (crumbs.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        "这里还空着。\n\n不需要好想法，碎片就行——\n一句话、一个疑问、半截直觉。",
                        textAlign = TextAlign.Center, style = SansNote, color = InkSoft
                    )
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalItemSpacing = 12.dp
                ) {
                    items(crumbs, key = { it.id }) { crumb ->
                        CrumbCard(
                            crumb = crumb,
                            selecting = state.selecting,
                            picked = crumb.id in state.selected,
                            onClick = {
                                if (state.selecting) vm.toggleSelect(crumb.id) else editing = crumb
                            },
                            onLongClick = { vm.toggleSelect(crumb.id) }
                        )
                    }
                }
            }
            Spacer(Modifier.height(86.dp))
        }

        BottomBar(tab = 0, onCrumbs = {}, onCompost = onTabCompost, modifier = Modifier.align(Alignment.BottomCenter))

        AnimatedVisibility(
            visible = state.selecting,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PaperWarm.copy(alpha = 0.96f))
                    .padding(horizontal = 22.dp, vertical = 14.dp)
            ) {
                TextButton(onClick = vm::exitSelection) {
                    Text("取消", color = InkSoft, fontSize = 14.sp)
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        vm.exitSelection()
                        onSetup(state.selected.toList())
                    },
                    enabled = state.selected.isNotEmpty(),
                    modifier = Modifier.height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Clay,
                        disabledContainerColor = Sand,
                        contentColor = PaperWarm,
                        disabledContentColor = InkFaint
                    )
                ) {
                    Text("去堆肥（${state.selected.size}）", fontSize = 14.5.sp)
                }
            }
        }
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
private fun GhostChip(label: String) {
    var soon by remember { mutableStateOf(false) }
    Column {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(11.dp))
                .background(Paper)
                .border(1.dp, Line, RoundedCornerShape(11.dp))
                .clickable { soon = true }
                .padding(horizontal = 14.dp, vertical = 7.dp)
        ) {
            Text(label, fontSize = 12.5.sp, color = InkSoft)
        }
        if (soon) {
            Text("即将支持", fontSize = 9.sp, color = InkFaint, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CrumbCard(
    crumb: IdeaEntity,
    selecting: Boolean,
    picked: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(168.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(PaperWarm)
            .border(
                1.dp,
                when {
                    picked -> Clay
                    else -> Line
                },
                RoundedCornerShape(16.dp)
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(14.dp)
    ) {
        Text(
            crumb.content,
            style = SerifBody,
            color = Ink,
            maxLines = 6,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            lineHeight = 21.sp
        )
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(formatTime(crumb.createdAt), style = SansTiny, color = InkFaint, maxLines = 1)
            Spacer(Modifier.weight(1f))
            val composted = crumb.status == "composted"
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (composted) MossSoft else Sand)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (composted) "🌱 已发酵" else "未发酵",
                    fontSize = 10.sp,
                    maxLines = 1,
                    softWrap = false,
                    color = if (composted) MossDeep else InkFaint
                )
            }
        }
    }
}

@Composable
fun BottomBar(tab: Int, onCrumbs: () -> Unit, onCompost: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PaperWarm.copy(alpha = 0.96f))
                .padding(top = 10.dp, bottom = 18.dp)
        ) {
            BottomItem(
                selected = tab == 0,
                label = "面包渣",
                icon = { CrumbsIcon(it) },
                onClick = onCrumbs,
                modifier = Modifier.weight(1f)
            )
            BottomItem(
                selected = tab == 1,
                label = "堆肥",
                icon = { CompostIcon(it) },
                onClick = onCompost,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BottomItem(
    selected: Boolean,
    label: String,
    icon: @Composable (Color) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (selected) Clay else InkFaint
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        icon(color)
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 10.5.sp, color = color)
    }
}

@Composable
private fun CrumbsIcon(color: Color) {
    Canvas(Modifier.size(22.dp)) {
        val dots = listOf(
            Offset(0.28f, 0.36f) to 0.10f,
            Offset(0.60f, 0.52f) to 0.078f,
            Offset(0.38f, 0.76f) to 0.064f,
            Offset(0.68f, 0.84f) to 0.05f
        )
        dots.forEach { (c, r) ->
            drawCircle(
                color,
                radius = size.minDimension * r,
                center = Offset(c.x * size.width, c.y * size.height)
            )
        }
    }
}

@Composable
private fun CompostIcon(color: Color) {
    Canvas(Modifier.size(22.dp)) {
        val s = size
        val w = s.width
        val h = s.height
        drawLine(color, Offset(w * 0.5f, h * 0.95f), Offset(w * 0.5f, h * 0.5f), strokeWidth = w * 0.075f, cap = StrokeCap.Round)
        val left = Path().apply {
            moveTo(w * 0.5f, h * 0.62f)
            cubicTo(w * 0.44f, h * 0.44f, w * 0.30f, h * 0.34f, w * 0.16f, h * 0.30f)
            cubicTo(w * 0.16f, h * 0.52f, w * 0.30f, h * 0.64f, w * 0.5f, h * 0.66f)
            close()
        }
        val right = Path().apply {
            moveTo(w * 0.5f, h * 0.52f)
            cubicTo(w * 0.54f, h * 0.36f, w * 0.66f, h * 0.26f, w * 0.82f, h * 0.22f)
            cubicTo(w * 0.84f, h * 0.42f, w * 0.72f, h * 0.55f, w * 0.5f, h * 0.56f)
            close()
        }
        drawPath(left, color, style = Stroke(width = w * 0.06f, cap = StrokeCap.Round))
        drawPath(right, color, style = Stroke(width = w * 0.06f, cap = StrokeCap.Round))
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
        containerColor = PaperWarm,
        title = { Text("修一修这颗面包渣", style = SerifSection, color = Ink) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().height(200.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Moss,
                    unfocusedBorderColor = Line
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }, enabled = text.isNotBlank()) {
                Text("保存", color = Clay)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("算了", color = InkSoft) }
        }
    )
}

private val timeFmt = SimpleDateFormat("M月d日 HH:mm", Locale.CHINA)
private fun formatTime(ts: Long): String = timeFmt.format(Date(ts))
