package com.ideacompost.app.ui.wait

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.ideacompost.app.ui.theme.SerifTitle
import kotlinx.coroutines.delay
import org.json.JSONArray

/** 等待页（设计稿 wait 屏）：氧气隐喻 + 铲子挖土 + 五阶段步进条。 */
@Composable
fun WaitScreen(
    onDone: (String) -> Unit,
    onBack: () -> Unit,
    vm: WaitViewModel = hiltViewModel()
) {
    val compost by vm.compost.collectAsState()
    val progress by vm.progress.collectAsState()
    val c = compost
    val ctx = androidx.compose.ui.platform.LocalContext.current

    // 发酵期间保持屏幕常亮（离开本页自动恢复系统默认）
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    // Android 13+ 请求通知权限（前台服务进度通知可见；拒绝也不影响发酵）
    val notifPerm = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                ctx, android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notifPerm.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val finished = c?.status == "done" || c?.status == "completed" ||
        c?.status == "awaiting_feedback"
    LaunchedEffect(c?.id) { }
    LaunchedEffect(c?.status) {
        if (finished) {
            vm.pause()
            onDone(requireNotNull(c?.id))
        }
    }

    if (c == null) {
        Box(Modifier.fillMaxSize().background(Paper))
        return
    }

    val stageIndex = when (c.currentStage) {
        "preflight" -> 0
        "identify" -> 0
        "convoke" -> 1
        "integrate" -> 3
        "assess" -> 4
        "done" -> 5
        else -> 2 // ferment_*
    }
    val failed = c.status == "failed"

    Column(
        modifier = Modifier.fillMaxSize().background(Paper),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))

        StepBar(current = stageIndex)
        Spacer(Modifier.height(6.dp))

        DigScene(Modifier.size(230.dp, 200.dp))
        Spacer(Modifier.height(2.dp))

        Text(
            if (failed) "发酵中断了" else "堆肥需要氧气",
            style = SerifTitle, color = Ink, letterSpacing = 0.5.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (failed) (c.error ?: "发生了一点意外。") +
                "\n这批面包渣还完好，随时可以重新点火。"
            else "离开这一页或熄屏后，发酵也会继续；\n状态栏会实时播报进度。屏幕已为你保持常亮。",
            style = SansNote, color = InkSoft, textAlign = TextAlign.Center, lineHeight = 19.sp
        )
        if (!failed) {
            Spacer(Modifier.height(10.dp))
            Text(
                "等待的时候，不妨冥想一下，享受安宁。",
                style = SansTiny, color = InkFaint, textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(16.dp))

        if (!failed) {
            Narration(rosterJson = c.rosterJson, stage = c.currentStage)
            Spacer(Modifier.height(14.dp))
            var elapsed by remember(c.id) { mutableLongStateOf(0L) }
            LaunchedEffect(c.id) {
                while (true) {
                    delay(1000)
                    elapsed++
                }
            }
            val liveProgress = progress?.takeIf {
                it.compostId == c.id && c.currentStage.startsWith("ferment_") && it.round == c.currentStage
            }
            // r 编号是 prompt 标识不是序号：浅 = r1,r3（第1/2轮）；深 = r1-r4；非发酵阶段为 0
            val seq: Int = if (!c.currentStage.startsWith("ferment_")) 0 else when (c.depth) {
                "shallow" -> if (c.currentStage == "ferment_r3") 2 else 1
                "deep" -> when (c.currentStage) {
                    "ferment_r2" -> 2; "ferment_r3" -> 3; "ferment_r4" -> 4; else -> 1
                }
                else -> when (c.currentStage) {
                    "ferment_r2" -> 2; "ferment_r3" -> 3; else -> 1
                }
            }
            val totalRounds = when (c.depth) { "shallow" -> 2; "deep" -> 4; else -> 3 }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("%02d:%02d".format(elapsed / 60, elapsed % 60), fontSize = 11.5.sp, color = InkFaint)
                Dot()
                Text(
                    when {
                        seq > 0 && liveProgress != null ->
                            "第 $seq 轮 · ${liveProgress.done}/${liveProgress.total} 菌已归位"
                        seq > 0 -> "第 $seq 轮 / 共 $totalRounds 轮"
                        c.currentStage == "integrate" -> "园丁整合中"
                        c.currentStage == "assess" -> "结算营养中"
                        else -> "准备发酵"
                    },
                    fontSize = 11.5.sp, color = InkFaint
                )
                Dot()
                Text("${rosterNames(c.rosterJson).size} 位菌群在场", fontSize = 11.5.sp, color = InkFaint)
            }
        }

        Spacer(Modifier.weight(1f))
        if (failed) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .background(Clay)
                    .clickable { vm.retry() }
                    .padding(horizontal = 34.dp, vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("🔥 重新点火", fontSize = 14.sp, color = PaperWarm)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "← 回到面包渣",
                fontSize = 12.5.sp, color = InkSoft,
                modifier = Modifier.clickable(onClick = onBack).padding(12.dp)
            )
        } else {
            Text(
                "暂停发酵（已完成的轮次会保留，回来接着发酵）",
                fontSize = 11.5.sp, color = InkFaint,
                modifier = Modifier
                    .clickable { vm.pause(); onBack() }
                    .padding(12.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun Dot() {
    Box(
        Modifier.padding(horizontal = 8.dp).size(3.dp).clip(CircleShape).background(InkFaint)
    )
}

@Composable
private fun StepBar(current: Int) {
    val steps = listOf("识别", "召集", "发酵", "整合", "反哺")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 18.dp)
    ) {
        steps.forEachIndexed { i, label ->
            val state = when {
                i < current -> "done"
                i == current -> "act"
                else -> "todo"
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(
                            when (state) {
                                "done" -> Moss
                                "act" -> Clay
                                else -> Sand
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (state == "done") "✓" else "${i + 1}",
                        color = if (state == "todo") InkFaint else PaperWarm,
                        fontSize = 11.sp
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    label, fontSize = 10.5.sp,
                    color = if (state == "act") Clay else InkFaint
                )
            }
            if (i < steps.lastIndex) {
                Box(
                    Modifier
                        .padding(horizontal = 6.dp)
                        .width(16.dp)
                        .height(2.dp)
                        .background(if (i < current) Moss else Line)
                )
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun DigScene(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "dig")
    val shovelAngle by transition.animateFloat(
        initialValue = 16f, targetValue = -20f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shovel"
    )
    val bubbles = List(6) { i ->
        val delayFrac = i * 0.16f
        val xFrac = listOf(0.24f, 0.37f, 0.53f, 0.68f, 0.18f, 0.78f)[i]
        val r = listOf(5.5f, 4f, 7f, 3.5f, 3f, 4.5f)[i]
        transition.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(3200, delayMillis = (delayFrac * 3200).toInt(), easing = LinearEasing)
            ),
            label = "bub$i"
        ) to Triple(xFrac, r, i)
    }
    val sway by transition.animateFloat(
        initialValue = -5f, targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "sway"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // —— 先在 DrawScope 里换算所有尺寸 ——
        val handleH = 22.dp.toPx()
        val handleW = 6.dp.toPx()
        val gripW = 14.dp.toPx()
        val gripH = 5.dp.toPx()
        val headW = 12.dp.toPx()
        val headTop = 25.dp.toPx()
        val headBot = 38.dp.toPx()
        val pivotY = 8.dp.toPx()
        val sproutLift = 8.dp.toPx()
        val stemBase = 34.dp.toPx()
        val strokeW = 2.dp.toPx()

        // 土堆
        val moundCx = w * 0.5f
        val moundCy = h * 0.80f
        val moundW = w * 0.76f
        val moundH = h * 0.30f
        val mound = Path().apply {
            moveTo(moundCx - moundW / 2, moundCy)
            cubicTo(
                moundCx - moundW / 2, moundCy - moundH * 0.9f,
                moundCx - moundW * 0.15f, moundCy - moundH,
                moundCx, moundCy - moundH
            )
            cubicTo(
                moundCx + moundW * 0.15f, moundCy - moundH,
                moundCx + moundW / 2, moundCy - moundH * 0.9f,
                moundCx + moundW / 2, moundCy
            )
            close()
        }
        drawPath(mound, Color(0xFFCBBDA0))
        // 土粒
        listOf(
            Offset(0.22f, 0.46f), Offset(0.55f, 0.30f),
            Offset(0.78f, 0.55f), Offset(0.38f, 0.70f)
        ).forEach { f ->
            drawCircle(
                Color(0xFF8F7F5F), radius = 2.2f,
                center = Offset(moundCx + (f.x - 0.5f) * moundW, moundCy - f.y * moundH)
            )
        }
        // 铲子（绕柄顶摆动；柄顶下移至铲头接触肥堆）
        withTransform({
            translate(left = moundCx - gripW / 2, top = h * 0.33f)
            rotate(shovelAngle, pivot = Offset(gripW / 2, pivotY))
        }) {
            drawRoundRect(
                color = Color(0xFFB08D64),
                topLeft = Offset(gripW / 2 - handleW / 2, pivotY - gripH),
                size = androidx.compose.ui.geometry.Size(handleW, handleH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
            )
            drawRoundRect(
                color = Color(0xFF8F6F4B),
                topLeft = Offset(0f, 0f),
                size = androidx.compose.ui.geometry.Size(gripW, gripH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.5f)
            )
            val head = Path().apply {
                moveTo(gripW / 2 - headW, headTop)
                lineTo(gripW / 2 + headW, headTop)
                lineTo(gripW / 2 + headW * 0.83f, headBot)
                quadraticBezierTo(gripW / 2, headBot + 6.dp.toPx(), gripW / 2 - headW * 0.83f, headBot)
                close()
            }
            drawPath(head, Color(0xFF8A9490))
        }
        // 气泡（氧气）
        bubbles.forEach { (progress, cfg) ->
            val (xFrac, rDp, _) = cfg
            val p = progress.value
            val alpha = (0.9f * (1f - p)).coerceIn(0f, 0.9f)
            val y = moundCy - 6.dp.toPx() - p * h * 0.5f
            drawCircle(
                color = Moss.copy(alpha = alpha * 0.25f),
                radius = rDp.dp.toPx() * (0.5f + p * 0.65f),
                center = Offset(w * xFrac, y)
            )
            drawCircle(
                color = Moss.copy(alpha = alpha * 0.5f),
                radius = rDp.dp.toPx() * (0.5f + p * 0.65f),
                center = Offset(w * xFrac, y),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f)
            )
        }
        // 幼芽（右侧摇摆）
        withTransform({
            translate(left = moundCx + moundW * 0.32f, top = moundCy - sproutLift)
            rotate(sway, pivot = Offset(0f, stemBase))
        }) {
            val stem = Path().apply {
                moveTo(0f, stemBase)
                quadraticBezierTo(1.dp.toPx(), stemBase / 2f, 0f, 4.dp.toPx())
            }
            drawPath(
                stem, Moss,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeW, cap = StrokeCap.Round)
            )
            val leafL = Path().apply {
                moveTo(0f, 18.dp.toPx())
                quadraticBezierTo(-10.dp.toPx(), 12.dp.toPx(), -12.dp.toPx(), 2.dp.toPx())
                quadraticBezierTo(-4.dp.toPx(), 6.dp.toPx(), 0f, 18.dp.toPx())
                close()
            }
            val leafR = Path().apply {
                moveTo(0f, 12.dp.toPx())
                quadraticBezierTo(9.dp.toPx(), 8.dp.toPx(), 11.dp.toPx(), 0f)
                quadraticBezierTo(3.dp.toPx(), 3.dp.toPx(), 0f, 12.dp.toPx())
                close()
            }
            drawPath(leafL, MossSoft)
            drawPath(leafR, MossSoft)
        }
    }
}

@Composable
private fun Narration(rosterJson: String, stage: String) {
    val names = remember(rosterJson) { rosterNames(rosterJson) }
    var idx by remember(stage) { mutableIntStateOf(0) }
    LaunchedEffect(stage, names.size) {
        while (true) {
            delay(3000)
            idx++
        }
    }
    val templates = listOf(
        "正在阅读这批面包渣的纹理……",
        "园丁正在挑选适合的菌群……"
    )
    val lines = if (names.isEmpty()) templates else names.flatMap { n ->
        listOf(
            "$n 正在这批碎片里找它的食物……",
            "$n 亮出了自己的视角……"
        )
    }
    val text = lines[idx.coerceIn(0, lines.size - 1)]
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MossSoft)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(Moss))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 12.5.sp, color = MossDeep, lineHeight = 18.sp, modifier = Modifier.width(232.dp))
    }
}

private fun rosterNames(rosterJson: String): List<String> = runCatching {
    val arr = JSONArray(rosterJson)
    (0 until arr.length()).map { arr.getJSONObject(it).optString("name") }.filter { it.isNotBlank() }
}.getOrDefault(emptyList())
