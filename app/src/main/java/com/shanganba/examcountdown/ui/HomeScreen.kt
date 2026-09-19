package com.shanganba.examcountdown.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shanganba.examcountdown.data.DayRecord
import com.shanganba.examcountdown.data.PersistedState
import com.shanganba.examcountdown.util.Dates

@Composable
fun HomeScreen(
    vm: AppViewModel,
    state: PersistedState,
    now: Long,
    onQuick: (QuickTarget) -> Unit,
    onOpenNodes: () -> Unit
) {
    val c = LocalSgColors.current
    val scroll = rememberScrollState()
    val day = vm.today(state)
    val totalTasks = state.templates.count { it.enabled } + day.extraTasks.size
    val doneTasks = day.doneTemplateIds.count { id -> state.templates.any { it.id == id && it.enabled } } +
        day.doneExtraIds.count { id -> day.extraTasks.any { it.id == id } }
    val donePct = if (totalTasks == 0) 0 else (doneTasks * 100 / totalTasks)

    var showAddTask by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scroll)
            .padding(start = 16.dp, end = 16.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HeroCard(state, now, day)

        SgCard {
            SgSectionHeader("今日任务", "$doneTasks / $totalTasks · 完成 $donePct%")
            Spacer(Modifier.height(2.dp))
            state.templates.filter { it.enabled }.sortedBy { it.order }.forEachIndexed { index, t ->
                TaskRow(
                    title = t.title,
                    note = if (t.targetAmount > 0) "${t.targetAmount} ${t.unit}" else "",
                    color = moduleColorOf(t.moduleId, index),
                    done = day.doneTemplateIds.contains(t.id)
                ) { vm.toggleTemplate(t.id) }
            }
            day.extraTasks.forEach { extra ->
                TaskRow(
                    title = extra.title,
                    note = "临时",
                    color = c.accent2,
                    done = day.doneExtraIds.contains(extra.id),
                    onLongClick = { vm.removeExtraTask(extra.id) }
                ) { vm.toggleExtraTask(extra.id) }
            }
            Spacer(Modifier.height(4.dp))
            SgDivider()
            Spacer(Modifier.height(8.dp))
            Text(
                "＋ 自定义今天的任务",
                style = SgType.button,
                color = c.accent,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAddTask = true }
                    .padding(vertical = 4.dp)
            )
        }

        QuickRow(onQuick)

        SgCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔥", style = SgType.statValue)
                Spacer(Modifier.width(10.dp))
                SgLabelValue("连续打卡", "${state.streak}", suffix = "天")
                Spacer(Modifier.width(18.dp))
                SgLabelValue("累计打卡", "${state.totalCheckIn}", suffix = "天")
                Spacer(Modifier.weight(1f))
                val checked = day.checkIn
                SgPrimaryButton(
                    text = if (checked) "今日已打卡" else "今日打卡",
                    onClick = { vm.checkInToday() },
                    enabled = !checked
                )
            }
            Spacer(Modifier.height(6.dp))
            SgDivider()
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenNodes() }
            ) {
                Text("全部考试节点 · ${state.nodes.size} 个", style = SgType.caption, color = c.inkMuted)
                Spacer(Modifier.weight(1f))
                Text("›", style = SgType.cardTitle, color = c.inkMuted)
            }
        }
    }

    if (showAddTask) {
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddTask = false },
            title = { Text("自定义今天的任务", style = SgType.cardTitle) },
            text = {
                SgTextField(
                    value = text,
                    onValueChange = { text = it.take(30) },
                    singleLine = true,
                    label = "任务内容"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (text.isNotBlank()) vm.addExtraTask(text.trim())
                    showAddTask = false
                }) { Text("添加") }
            },
            dismissButton = {
                TextButton(onClick = { showAddTask = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun HeroCard(state: PersistedState, now: Long, day: DayRecord) {
    val c = LocalSgColors.current
    val node = Dates.primaryNode(state.nodes, now)
    val target = node?.dateTime ?: now
    val remain = (target - now).coerceAtLeast(0)
    val days = remain / 86_400_000L
    val hours = remain % 86_400_000L / 3_600_000L
    val minutes = remain % 3_600_000L / 60_000L
    val seconds = remain % 60_000L / 1000L

    val startMillis = state.settings.startDate
        .takeIf { it.isNotBlank() }
        ?.let { Dates.parseDate(it) }
        ?.let { Dates.startOfDay(it) }
        ?: state.settings.firstLaunchAt.takeIf { it > 0L }
        ?: now
    val examNode = state.nodes.firstOrNull { it.type == "笔试" } ?: node
    val pct = if (examNode != null) Dates.progressPercent(startMillis, examNode.dateTime, now) else 0.0
    val passedDays = (((now - startMillis) / 86_400_000L).toInt() + 1).coerceAtLeast(1)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(c.heroStart, c.heroEnd)))
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .offset(x = 250.dp, y = (-46).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f))
        )
        Box(
            modifier = Modifier
                .size(86.dp)
                .offset(x = (-22).dp, y = 128.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.16f))
        )
        Column(modifier = Modifier.padding(18.dp, 16.dp, 18.dp, 16.dp)) {
            SgHeroHeader(
                label = dateHeadline(node?.title ?: "考试"),
                sticker = if (day.checkIn) "🔥 今日已打卡" else "🔥 连续 ${state.streak} 天"
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$days", style = SgType.hero, color = c.heroInk)
                Spacer(Modifier.width(8.dp))
                Text("天", style = SgType.body, color = c.heroInk.copy(alpha = 0.92f))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                ClockPill("%02d 时".format(hours))
                ClockPill("%02d 分".format(minutes))
                ClockPill("%02d 秒".format(seconds))
            }
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.28f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((pct / 100.0).toFloat().coerceIn(0f, 1f))
                        .height(7.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.95f))
                )
            }
            Spacer(Modifier.height(7.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("备考第 $passedDays 天", style = SgType.meta, color = c.heroInk.copy(alpha = 0.94f))
                Spacer(Modifier.weight(1f))
                Text("进度 %.1f%%".format(pct), style = SgType.meta, color = c.heroInk.copy(alpha = 0.94f))
            }
        }
    }
}

private fun dateHeadline(nodeTitle: String): String = "距离${nodeTitle}还有"

@Composable
private fun ClockPill(text: String) {
    val c = LocalSgColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(11.dp))
            .background(Color.White.copy(alpha = 0.24f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, style = SgType.body, color = c.heroInk)
    }
}

@Composable
private fun TaskRow(
    title: String,
    note: String,
    color: Color,
    done: Boolean,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val c = LocalSgColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 9.dp)
    ) {
        SgDot(color, if (done) 9 else 9)
        Spacer(Modifier.width(10.dp))
        Text(
            title,
            style = SgType.body,
            color = if (done) c.inkMuted else c.ink,
            modifier = Modifier.weight(1f)
        )
        if (note.isNotEmpty()) {
            Text(note, style = SgType.meta, color = c.inkMuted)
        }
        if (done) {
            Spacer(Modifier.width(8.dp))
            Text("✓", style = SgType.chip, color = c.accent)
        }
    }
    if (onLongClick != null) {
        // 长按删除交给后续版本，这里保持轻量
    }
}

enum class QuickTarget { PRACTICE, WRONG, KNOWLEDGE, ANALYSIS }

@Composable
private fun QuickRow(onQuick: (QuickTarget) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        QuickItem("⏱", "刷题计时", ModuleColors[3], Modifier.weight(1f)) { onQuick(QuickTarget.PRACTICE) }
        QuickItem("📝", "错题本", ModuleColors[1], Modifier.weight(1f)) { onQuick(QuickTarget.WRONG) }
        QuickItem("🗂", "知识框架", ModuleColors[2], Modifier.weight(1f)) { onQuick(QuickTarget.KNOWLEDGE) }
        QuickItem("🎯", "水平分析", ModuleColors[0], Modifier.weight(1f)) { onQuick(QuickTarget.ANALYSIS) }
    }
}

@Composable
private fun QuickItem(
    emoji: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.13f))
            .clickable { onClick() }
            .padding(vertical = 12.dp)
    ) {
        Text(emoji, style = SgType.statValue)
        Spacer(Modifier.height(6.dp))
        Text(label, style = SgType.chip, color = color.copy(alpha = 0.95f), fontWeight = FontWeight.Medium)
    }
}
