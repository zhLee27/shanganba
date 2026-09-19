package com.shanganba.examcountdown.ui

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardType
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
    var pendingDeleteModule by remember { mutableStateOf<SubjectModule?>(null) }
    var pendingDeletePreset by remember { mutableStateOf<Int?>(null) }
    var pendingClearPresets by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(setOf<String>()) }
    var reorderMode by remember { mutableStateOf(false) }

    fun pickModule(m: SubjectModule) {
        moduleId = m.id
        if (m.defaultQuestionCount > 0) questionCount = m.defaultQuestionCount
        if (m.defaultSeconds > 0) minutes = (m.defaultSeconds / 60).coerceAtLeast(1)
    }

    fun startWith(m: SubjectModule) {
        val q = if (m.defaultQuestionCount > 0) m.defaultQuestionCount else questionCount
        val mins = if (m.defaultSeconds > 0) m.defaultSeconds / 60 else minutes
        val now = System.currentTimeMillis()
        onStart(
            ActiveTimer(
                moduleId = m.id,
                mode = if (countDown) "COUNT_DOWN" else "COUNT_UP",
                plannedSeconds = mins * 60,
                questionTarget = q,
                startedAtWall = now,
                lastResumeElapsed = SystemClock.elapsedRealtime(),
                endAlarmAt = now + mins * 60_000L
            )
        )
    }
    var editingModule by remember { mutableStateOf<SubjectModule?>(null) }
    var showCustomMinutes by remember { mutableStateOf(false) }
    var presetDeleteMode by remember { mutableStateOf(false) }
    var dragAccum by remember { mutableFloatStateOf(0f) }
    var dragId by remember { mutableStateOf<String?>(null) }
    val dragShift by animateFloatAsState(if (dragId != null) dragAccum else 0f, label = "dragShift")
    val currentOrdered by rememberUpdatedState(state.modules.sortedBy { it.order })
    val rowHeightPx = with(LocalDensity.current) { 54.dp.toPx() }

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
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (reorderMode) "拖动调整顺序" else "本次刷什么",
                    style = SgType.cardTitle,
                    color = c.inkTitle,
                    modifier = Modifier.weight(1f)
                )
                if (reorderMode) {
                    SgPrimaryButton("完成") { reorderMode = false }
                } else {
                    SgSoftButton("排序") { reorderMode = true }
                }
            }
            Spacer(Modifier.height(4.dp))
            // 只列大题型；小题型留给任务模板选（三级结构保留）
            val ordered = currentOrdered.filter { it.parentId.isEmpty() }
            var lastSubject = ""
            ordered.forEachIndexed { index, m ->
                if (m.subject != lastSubject) {
                    lastSubject = m.subject
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (m.subject == "SHENLUN") "申论" else "行测",
                        style = SgType.chip,
                        color = c.accent
                    )
                }
                val color = moduleColorOf(m.id, index)
                val picked = m.id == moduleId
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (picked) color.copy(alpha = 0.14f) else Color.Transparent)
                        .clickable { moduleId = m.id }
                        .pointerInput(m.id) {
                            detectTapGestures(onLongPress = { reorderMode = true })
                        }
                        .pointerInput(m.id, reorderMode) {
                            // 只有进入排序模式才接管拖动，避免影响页面正常滚动
                            if (reorderMode) detectDragGestures(
                                onDragStart = { dragId = m.id; dragAccum = 0f },
                                onDragEnd = { dragId = null; dragAccum = 0f },
                                onDragCancel = { dragId = null; dragAccum = 0f },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragAccum += amount.y
                                    val list = currentOrdered.filter { it.parentId.isEmpty() }
                                    val cur = list.indexOfFirst { it.id == m.id }
                                    if (cur >= 0) {
                                        if (dragAccum > rowHeightPx * 0.6f && cur < list.lastIndex) {
                                            vm.moveModuleTo(m.id, cur + 1)
                                            dragAccum -= rowHeightPx
                                        } else if (dragAccum < -rowHeightPx * 0.6f && cur > 0) {
                                            vm.moveModuleTo(m.id, cur - 1)
                                            dragAccum += rowHeightPx
                                        }
                                    }
                                }
                            )
                        }
                        .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
                        .zIndex(if (m.id == dragId) 1f else 0f)
                        .graphicsLayer {
                            if (m.id == dragId) {
                                translationY = dragShift
                                scaleX = 1.02f
                                scaleY = 1.02f
                                shadowElevation = 12f
                            }
                        }
                ) {
                    // 展开小三角：点它才展开小题型
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .clickable {
                                expanded = if (expanded.contains(m.id)) expanded - m.id else expanded + m.id
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (expanded.contains(m.id)) "▾" else "▸",
                            style = SgType.chip,
                            color = if (currentOrdered.any { it.parentId == m.id }) c.accent else c.inkFaint
                        )
                    }
                    Spacer(Modifier.width(2.dp))
                    Box(
                        modifier = Modifier
                            .size(if (picked) 11.dp else 8.dp)
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
                    Spacer(Modifier.width(6.dp))
                    IconTextButton("✎") { editingModule = m }
                    IconTextButton("✕", danger = true) { pendingDeleteModule = m }
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(50))
                            .background(color.copy(alpha = 0.16f))
                            .clickable { startWith(m) },
                        contentAlignment = Alignment.Center
                    ) { Text("▶", style = SgType.chip, color = color) }
                }
            // 展开后列出小题型，每个都能单独计时
            if (expanded.contains(m.id)) {
                currentOrdered.filter { it.parentId == m.id }.sortedBy { it.order }.forEach { child ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (child.id == moduleId) c.accent.copy(alpha = 0.12f) else Color.Transparent
                            )
                            .clickable { pickModule(child) }
                            .padding(start = 44.dp, end = 4.dp, top = 7.dp, bottom = 7.dp)
                    ) {
                        SgDot(c.inkFaint, 7)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            child.name,
                            style = SgType.body,
                            color = if (child.id == moduleId) c.accent else c.ink,
                            modifier = Modifier.weight(1f)
                        )
                        Text("${child.defaultQuestionCount} 题", style = SgType.meta, color = c.inkMuted)
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(50))
                                .background(c.accent.copy(alpha = 0.14f))
                                .clickable { startWith(child) },
                            contentAlignment = Alignment.Center
                        ) { Text("▶", style = SgType.chip, color = c.accent) }
                    }
                }
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
                Text(
                    "$questionCount",
                    style = SgType.bigStat,
                    color = c.inkTitle,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "约 ${if (questionCount > 0) minutes * 60 / questionCount else 0} 秒 / 题",
                    style = SgType.meta,
                    color = c.inkMuted
                )
            }
        }

        SgCard(onClick = { if (presetDeleteMode) presetDeleteMode = false }) {
            SgSectionHeader("计时方式", "随时可改")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SegButton("正计时", !countDown, Modifier.weight(1f)) { countDown = false }
                SegButton("倒计时", countDown, Modifier.weight(1f)) { countDown = true }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (presetDeleteMode) "点 ✕ 删除预设，点空白处退出" else "预设时长（点选，长按可删除）",
                    style = SgType.meta,
                    color = if (presetDeleteMode) c.accent2 else c.inkMuted,
                    modifier = Modifier.weight(1f)
                )
                if (presetDeleteMode) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { presetDeleteMode = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "🗑",
                            style = SgType.chip,
                            color = c.accent2,
                            modifier = Modifier.clickable { pendingClearPresets = true }
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                state.settings.timerPresets.forEach { m ->
                    if (presetDeleteMode) {
                        SgChip(
                            "$m 分 ✕",
                            c.accent2,
                            modifier = Modifier.clickable { pendingDeletePreset = m }
                        )
                    } else {
                        SgChip(
                            "$m 分",
                            if (minutes == m) c.accent else c.inkMuted,
                            modifier = Modifier.pointerInput(m) {
                                detectTapGestures(
                                    onTap = { minutes = m },
                                    onLongPress = { presetDeleteMode = true }
                                )
                            }
                        )
                    }
                }
                if (!presetDeleteMode) {
                    SgChip("自定义", c.inkMuted, modifier = Modifier.clickable { showCustomMinutes = true })
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CounterButton("−1") { minutes = (minutes - 1).coerceAtLeast(1) }
                Text("$minutes 分钟", style = SgType.body, color = c.ink, modifier = Modifier.padding(horizontal = 12.dp))
                CounterButton("＋1") { minutes = (minutes + 1).coerceAtMost(180) }
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
        ModuleEditDialog(
            initial = null,
            onDismiss = { showNewModule = false },
            onSave = { m -> vm.upsertModule(m); moduleId = m.id; showNewModule = false },
            onDelete = null
        )
    }

    if (editingModule != null) {
        ModuleEditDialog(
            initial = editingModule,
            onDismiss = { editingModule = null },
            onSave = { m -> vm.upsertModule(m); editingModule = null },
            onDelete = { m -> pendingDeleteModule = m; editingModule = null }
        )
    }

    pendingDeleteModule?.let { m ->
        SgConfirmDialog(
            title = "删除模块",
            message = "确定删除「${m.name}」吗？已有的刷题记录会保留，只是不再出现在列表里。",
            onConfirm = {
                vm.deleteModule(m.id)
                if (moduleId == m.id) moduleId = ""
            },
            onDismiss = { pendingDeleteModule = null }
        )
    }

    pendingDeletePreset?.let { m ->
        SgConfirmDialog(
            title = "删除预设时长",
            message = "确定删掉「$m 分」这个预设吗？",
            onConfirm = { vm.updateTimerPresets(state.settings.timerPresets - m) },
            onDismiss = { pendingDeletePreset = null }
        )
    }

    if (pendingClearPresets) {
        SgConfirmDialog(
            title = "清空全部预设",
            message = "确定把预设时长全部清空吗？清空后还能自己再添加。",
            confirmText = "全部清空",
            onConfirm = { vm.updateTimerPresets(emptyList()) },
            onDismiss = { pendingClearPresets = false }
        )
    }

    if (showCustomMinutes) {
        var text by remember { mutableStateOf(minutes.toString()) }
        AlertDialog(
            onDismissRequest = { showCustomMinutes = false },
            title = { Text("自定义时长", style = SgType.cardTitle) },
            text = {
                SgTextField(
                    value = text,
                    onValueChange = { v -> text = v.filter { it.isDigit() }.take(3) },
                    label = "分钟数（1–180）",
                    keyboardType = KeyboardType.Number
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    text.toIntOrNull()?.let { v ->
                        val m = v.coerceIn(1, 180)
                        minutes = m
                        // 自定义时间直接变成预设，省掉再点一次“存为预设”
                        vm.updateTimerPresets(state.settings.timerPresets + m)
                    }
                    showCustomMinutes = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showCustomMinutes = false }) { Text("取消") } }
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
private fun IconTextButton(text: String, danger: Boolean = false, onClick: () -> Unit) {
    val c = LocalSgColors.current
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = SgType.chip, color = if (danger) c.accent2 else c.inkMuted)
    }
}

/** 新建 / 编辑刷题模块：名称、行测申论、默认题量、默认时长，编辑时还能删除 */
@Composable
private fun ModuleEditDialog(
    initial: SubjectModule?,
    onDismiss: () -> Unit,
    onSave: (SubjectModule) -> Unit,
    onDelete: ((SubjectModule) -> Unit)?
) {
    val c = LocalSgColors.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var subject by remember { mutableStateOf(initial?.subject ?: "XINGCE") }
    var count by remember { mutableStateOf((initial?.defaultQuestionCount ?: 20).toString()) }
    var minutes by remember { mutableStateOf(((initial?.defaultSeconds ?: 1500) / 60).toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "新建刷题模块" else "编辑刷题模块", style = SgType.cardTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SgTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "模块名，如 资料分析 · 增长率"
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SgChip("行测", if (subject == "XINGCE") c.accent else c.inkMuted,
                        modifier = Modifier.clickable { subject = "XINGCE" })
                    SgChip("申论", if (subject == "SHENLUN") c.accent else c.inkMuted,
                        modifier = Modifier.clickable { subject = "SHENLUN" })
                }
                SgTextField(
                    value = count,
                    onValueChange = { count = it.filter { ch -> ch.isDigit() }.take(3) },
                    label = "默认题量",
                    keyboardType = KeyboardType.Number
                )
                SgTextField(
                    value = minutes,
                    onValueChange = { minutes = it.filter { ch -> ch.isDigit() }.take(3) },
                    label = "默认时长（分钟）",
                    keyboardType = KeyboardType.Number
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    onSave(
                        SubjectModule(
                            id = initial?.id ?: ("m_" + UUID.randomUUID().toString().take(6)),
                            subject = subject,
                            name = name.trim(),
                            order = initial?.order ?: 90,
                            defaultQuestionCount = count.toIntOrNull() ?: 20,
                            defaultSeconds = (minutes.toIntOrNull() ?: 25) * 60,
                            custom = initial?.custom ?: true
                        )
                    )
                }
            }) { Text("保存") }
        },
        dismissButton = {
            Row {
                if (initial != null && onDelete != null) {
                    TextButton(onClick = { onDelete(initial) }) { Text("删除") }
                }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        }
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
            SgTextField(
                value = note,
                onValueChange = { note = it },
                label = "本次备注，比如：增长率比较又忘了看基期",
                singleLine = false,
                minLines = 2
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
