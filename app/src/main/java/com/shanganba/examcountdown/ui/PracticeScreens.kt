package com.shanganba.examcountdown.ui

import android.os.SystemClock
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.shanganba.examcountdown.data.ActiveTimer
import com.shanganba.examcountdown.data.PersistedState
import com.shanganba.examcountdown.data.SubjectModule
import com.shanganba.examcountdown.util.Dates
import kotlinx.coroutines.delay
import java.util.UUID

private fun mmss(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    return "%02d:%02d".format(s / 60, s % 60)
}

@Composable
fun PracticeTab(
    vm: AppViewModel,
    state: PersistedState,
    onStart: (ActiveTimer) -> Unit,
    onOpenHistory: () -> Unit
) {
    val c = LocalSgColors.current
    val scroll = rememberScrollState()
    var moduleId by remember { mutableStateOf(state.modules.firstOrNull()?.id ?: "") }
    var questionCount by remember { mutableIntStateOf(20) }
    var countDown by remember { mutableStateOf(true) }
    var minutes by remember { mutableIntStateOf(25) }
    var showNewModule by remember { mutableStateOf(false) }

    val module = state.modules.firstOrNull { it.id == moduleId } ?: state.modules.firstOrNull()

    LaunchedEffect(moduleId) {
        module?.let {
            questionCount = it.defaultQuestionCount
            minutes = (it.defaultSeconds / 60).coerceAtLeast(1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scroll)
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SgCard {
            SgSectionHeader("本次刷什么", "模块可自定义")
            Spacer(Modifier.height(4.dp))
            state.modules.forEachIndexed { index, m ->
                val color = moduleColorOf(m.id, index)
                val picked = m.id == moduleId
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (picked) color.copy(alpha = 0.14f) else Color.Transparent)
                        .clickable { moduleId = m.id }
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (picked) 12.dp else 9.dp)
                            .clip(CircleShape)
                            .background(if (picked) color else c.inkFaint)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        m.name,
                        style = SgType.body,
                        color = if (picked) color else c.ink,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${m.defaultQuestionCount} 题 / ${m.defaultSeconds / 60} 分",
                        style = SgType.meta,
                        color = c.inkMuted
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            SgDivider()
            Spacer(Modifier.height(8.dp))
            Text(
                "＋ 新建模块 / 自定义题型",
                style = SgType.button,
                color = c.accent,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showNewModule = true }
                    .padding(vertical = 4.dp)
            )
        }

        SgCard {
            SgSectionHeader("本次题量", "按纸质卷实际题数")
            Row(verticalAlignment = Alignment.CenterVertically) {
                CounterButton("−") { questionCount = (questionCount - 5).coerceAtLeast(1) }
                Text(
                    "$questionCount",
                    style = SgType.bigStat,
                    color = c.inkTitle,
                    modifier = Modifier
                        .width(80.dp)
                        .padding(horizontal = 8.dp)
                )
                CounterButton("＋") { questionCount = (questionCount + 5).coerceAtMost(200) }
                Spacer(Modifier.weight(1f))
                Text(
                    "约 ${if (questionCount > 0) minutes * 60 / questionCount else 0} 秒 / 题",
                    style = SgType.meta,
                    color = c.inkMuted
                )
            }
        }

        SgCard {
            SgSectionHeader("计时方式", "随时可改")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SegButton("正计时", !countDown, Modifier.weight(1f)) { countDown = false }
                SegButton("倒计时", countDown, Modifier.weight(1f)) { countDown = true }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 25, 30).forEach { m ->
                    SgChip(
                        "$m 分",
                        if (minutes == m) c.accent else c.inkMuted,
                        modifier = Modifier.clickable { minutes = m }
                    )
                }
                SgChip("自定义", c.inkMuted, modifier = Modifier.clickable { minutes = (minutes + 5).coerceAtMost(180) })
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CounterButton("−5") { minutes = (minutes - 5).coerceAtLeast(1) }
                Text("$minutes 分钟", style = SgType.body, color = c.ink, modifier = Modifier.padding(horizontal = 12.dp))
                CounterButton("＋5") { minutes = (minutes + 5).coerceAtMost(180) }
            }
        }

        SgWideButton("开始计时") {
            val m = module ?: return@SgWideButton
            val now = System.currentTimeMillis()
            onStart(
                ActiveTimer(
                    moduleId = m.id,
                    mode = if (countDown) "COUNT_DOWN" else "COUNT_UP",
                    plannedSeconds = minutes * 60,
                    questionTarget = questionCount,
                    startedAtWall = now,
                    lastResumeElapsed = SystemClock.elapsedRealtime(),
                    endAlarmAt = now + minutes * 60_000L
                )
            )
        }

        if (state.sessions.isNotEmpty()) {
            SgCard {
                SgSectionHeader("最近的刷题记录", "${state.sessions.size} 条")
                state.sessions.sortedByDescending { it.endedAt }.take(4).forEach { session ->
                    val name = state.modules.firstOrNull { it.id == session.moduleId }?.name ?: "刷题"
                    val color = moduleColorOf(session.moduleId)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 7.dp)) {
                        SgDot(color)
                        Spacer(Modifier.width(9.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, style = SgType.body, color = c.ink)
                            Text(
                                "${Dates.formatDateTime(session.endedAt)} · 用时 ${mmss(session.usedSeconds)}",
                                style = SgType.meta, color = c.inkMuted
                            )
                        }
                        Text(
                            "${session.correctCount} / ${session.questionCount}",
                            style = SgType.chip,
                            color = color
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                SgSoftButton("看全部记录") { onOpenHistory() }
            }
        }
    }

    if (showNewModule) {
        NewModuleDialog(
            onDismiss = { showNewModule = false },
            onSave = { m ->
                vm.upsertModule(m)
                moduleId = m.id
                showNewModule = false
            }
        )
    }
}

@Composable
private fun CounterButton(text: String, onClick: () -> Unit) {
    val c = LocalSgColors.current
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(c.surface2)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = SgType.body, color = c.ink)
    }
}

@Composable
private fun SegButton(text: String, on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = LocalSgColors.current
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(50))
            .background(if (on) c.accent else c.surface2)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = SgType.button, color = if (on) c.onAccent else c.inkMuted)
    }
}

@Composable
private fun NewModuleDialog(onDismiss: () -> Unit, onSave: (SubjectModule) -> Unit) {
    var name by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("XINGCE") }
    var count by remember { mutableStateOf("20") }
    var minutes by remember { mutableStateOf("25") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建刷题模块", style = SgType.cardTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("模块名，如 资料分析 · 增长率") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SgChip("行测", if (subject == "XINGCE") LocalSgColors.current.accent else LocalSgColors.current.inkMuted,
                        modifier = Modifier.clickable { subject = "XINGCE" })
                    SgChip("申论", if (subject == "SHENLUN") LocalSgColors.current.accent else LocalSgColors.current.inkMuted,
                        modifier = Modifier.clickable { subject = "SHENLUN" })
                }
                OutlinedTextField(value = count, onValueChange = { count = it.filter { ch -> ch.isDigit() }.take(3) }, label = { Text("默认题量") }, singleLine = true)
                OutlinedTextField(value = minutes, onValueChange = { minutes = it.filter { ch -> ch.isDigit() }.take(3) }, label = { Text("默认时长（分钟）") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    onSave(
                        SubjectModule(
                            id = "m_" + UUID.randomUUID().toString().take(6),
                            subject = subject,
                            name = name.trim(),
                            order = 90,
                            defaultQuestionCount = count.toIntOrNull() ?: 20,
                            defaultSeconds = (minutes.toIntOrNull() ?: 25) * 60,
                            custom = true
                        )
                    )
                }
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun FocusTimerScreen(
    vm: AppViewModel,
    state: PersistedState,
    onFinishRequest: () -> Unit,
    onQuit: () -> Unit
) {
    val c = LocalSgColors.current
    val timer = state.activeTimer ?: return
    val module = state.modules.firstOrNull { it.id == timer.moduleId }
    val color = moduleColorOf(timer.moduleId)
    val view = LocalView.current

    var nowElapsed by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowElapsed = SystemClock.elapsedRealtime()
            delay(250)
        }
    }
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val elapsed = timer.elapsedSeconds(nowElapsed)
    val remaining = (timer.plannedSeconds - elapsed).coerceAtLeast(0)
    val fraction = if (timer.mode == "COUNT_DOWN") {
        if (timer.plannedSeconds > 0) remaining.toFloat() / timer.plannedSeconds else 0f
    } else {
        if (timer.plannedSeconds > 0) (elapsed.toFloat() / timer.plannedSeconds).coerceIn(0f, 1f) else 0f
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SgScreenTitle("专注计时") {
            Text(
                if (timer.mode == "COUNT_DOWN") "倒计时中" else "正计时中",
                style = SgType.chip, color = color
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(module?.name ?: "刷题", style = SgType.cardTitle, color = c.inkMuted)
            Spacer(Modifier.height(18.dp))
            Box(contentAlignment = Alignment.Center) {
                val track = c.surface2
                Canvas(modifier = Modifier.size(248.dp)) {
                    val stroke = 18f
                    drawArc(
                        color = track,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = color,
                        startAngle = -90f,
                        sweepAngle = 360f * fraction,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        mmss(if (timer.mode == "COUNT_DOWN") remaining else elapsed),
                        style = SgType.heroNumber,
                        color = c.inkTitle
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (timer.paused) "已暂停" else if (timer.mode == "COUNT_DOWN") "剩余时间" else "已用时间",
                        style = SgType.meta,
                        color = if (timer.paused) color else c.inkMuted
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "目标 ${timer.questionTarget} 题 · 已做 ${timer.questionCount} 题 · 标记 ${timer.markedCount} 道",
                style = SgType.meta,
                color = c.inkMuted
            )
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(color.copy(alpha = 0.13f))
                    .clickable {
                        val tap = SystemClock.elapsedRealtime()
                        val delta = if (timer.lastTapElapsed > 0) ((tap - timer.lastTapElapsed) / 1000L).toInt() else 0
                        vm.updateTimer {
                            it.copy(
                                questionCount = it.questionCount + 1,
                                lastTapElapsed = tap,
                                perQuestionSeconds = it.perQuestionSeconds + delta
                            )
                        }
                    }
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("做完一题", style = SgType.body, color = c.ink)
                    Spacer(Modifier.width(8.dp))
                    Text("＋1", style = SgType.bigStat, color = color)
                    Spacer(Modifier.width(8.dp))
                    Text("点一下就好", style = SgType.meta, color = c.inkMuted)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SgSoftButton("标记疑难题") {
                    vm.updateTimer { it.copy(markedCount = it.markedCount + 1) }
                }
                SgSoftButton(if (timer.paused) "继续" else "暂停") {
                    vm.updateTimer {
                        if (it.paused) {
                            it.copy(paused = false, lastResumeElapsed = SystemClock.elapsedRealtime())
                        } else {
                            val secs = it.elapsedSeconds(SystemClock.elapsedRealtime())
                            it.copy(paused = true, accumulatedSeconds = secs)
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SgSoftButton("放弃本次", modifier = Modifier.weight(1f)) {
                vm.cancelTimer()
                onQuit()
            }
            SgPrimaryButton("结束并录入", modifier = Modifier.weight(1f)) { onFinishRequest() }
        }
    }
}

@Composable
fun PracticeResultScreen(
    vm: AppViewModel,
    state: PersistedState,
    onDone: () -> Unit
) {
    val c = LocalSgColors.current
    val timer = state.activeTimer ?: return
    val module = state.modules.firstOrNull { it.id == timer.moduleId }
    val color = moduleColorOf(timer.moduleId)
    var questionCount by remember { mutableIntStateOf(timer.questionCount.coerceAtLeast(1)) }
    var correct by remember { mutableIntStateOf(timer.questionCount) }
    var note by remember { mutableStateOf("") }

    val elapsed = timer.elapsedSeconds(SystemClock.elapsedRealtime())
    val perQuestion = if (questionCount > 0) elapsed / questionCount else 0
    val accuracy = if (questionCount > 0) correct * 100 / questionCount else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SgScreenTitle("本次结算")
        SgCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SgDot(color, 10)
                Spacer(Modifier.width(9.dp))
                Text(module?.name ?: "刷题", style = SgType.cardTitle, color = c.inkTitle)
                Spacer(Modifier.weight(1f))
                Text(if (timer.mode == "COUNT_DOWN") "倒计时 ${mmss(timer.plannedSeconds)}" else "正计时", style = SgType.meta, color = c.inkMuted)
            }
            Spacer(Modifier.height(10.dp))
            Row {
                SgLabelValue("用时", mmss(elapsed))
                Spacer(Modifier.width(22.dp))
                SgLabelValue("平均", "${perQuestion} 秒", suffix = "/ 题")
            }
        }
        SgCard {
            SgSectionHeader("做了几题", "可以改")
            Row(verticalAlignment = Alignment.CenterVertically) {
                CounterButton("−") { questionCount = (questionCount - 1).coerceAtLeast(1); if (correct > questionCount) correct = questionCount }
                Text("$questionCount", style = SgType.bigStat, color = c.inkTitle, modifier = Modifier.padding(horizontal = 12.dp))
                CounterButton("＋") { questionCount += 1 }
            }
            Spacer(Modifier.height(10.dp))
            SgSectionHeader("做对几题", "做错自动算")
            Row(verticalAlignment = Alignment.CenterVertically) {
                CounterButton("−") { correct = (correct - 1).coerceAtLeast(0) }
                Text("$correct", style = SgType.bigStat, color = c.inkTitle, modifier = Modifier.padding(horizontal = 12.dp))
                CounterButton("＋") { correct = (correct + 1).coerceAtMost(questionCount) }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text("正确率", style = SgType.meta, color = c.inkMuted)
                    Text("$accuracy%", style = SgType.statValue, color = color)
                }
            }
            Spacer(Modifier.height(10.dp))
            SgProgress(fraction = accuracy / 100f, color = color)
        }
        SgCard {
            SgSectionHeader("本次备注", "随手记")
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("比如：增长率比较又忘了看基期") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (timer.markedCount > 0) {
            SgCard {
                SgSectionHeader("待复盘", "标记了 ${timer.markedCount} 道")
                Text(
                    "保存后到「错题」页用拍照或手打把这 ${timer.markedCount} 道录进去，复盘完再标已掌握。",
                    style = SgType.bodyLong,
                    color = c.inkMuted
                )
            }
        }
        SgWideButton("保存记录") {
            vm.finishTimer(questionCount, correct, timer.markedCount, note.trim())
            onDone()
        }
        SgSoftButton("返回", modifier = Modifier.fillMaxWidth()) { onDone() }
    }
}

@Composable
fun PracticeHistoryScreen(state: PersistedState, onBack: () -> Unit) {
    val c = LocalSgColors.current
    Column(modifier = Modifier.fillMaxSize()) {
        SgScreenTitle("刷题记录", right = {
            SgSoftButton("返回") { onBack() }
        })
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (state.sessions.isEmpty()) {
                SgCard { Text("还没有刷题记录，去「刷题」页开一次计时吧。", style = SgType.bodyLong, color = c.inkMuted) }
            }
            state.sessions.sortedByDescending { it.endedAt }.forEach { s ->
                val name = state.modules.firstOrNull { it.id == s.moduleId }?.name ?: "刷题"
                val color = moduleColorOf(s.moduleId)
                val accuracy = if (s.questionCount > 0) s.correctCount * 100 / s.questionCount else 0
                SgCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SgDot(color, 10)
                        Spacer(Modifier.width(9.dp))
                        Text(name, style = SgType.cardTitle, color = c.inkTitle, modifier = Modifier.weight(1f))
                        Text("$accuracy%", style = SgType.statValue, color = color)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${Dates.formatDateTime(s.endedAt)} · 用时 ${mmss(s.usedSeconds)} · " +
                            "${s.correctCount}/${s.questionCount} 题" +
                            if (s.markedCount > 0) " · 标记 ${s.markedCount} 道" else "",
                        style = SgType.meta, color = c.inkMuted
                    )
                    if (s.note.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(s.note, style = SgType.bodyLong, color = c.ink)
                    }
                }
            }
        }
    }
}
