package com.shanganba.examcountdown.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.shanganba.examcountdown.data.PersistedState
import com.shanganba.examcountdown.util.Dates
import kotlin.math.cos
import kotlin.math.sin

data class ModuleStat(val id: String, val name: String, val accuracy: Double, val questions: Int, val gain: Double)

/** 第三级小题型的数据汇总到所属大题型（第二级），保证分析口径不变 */
private fun parentIdOf(state: PersistedState, moduleId: String): String {
    val m = state.modules.firstOrNull { it.id == moduleId } ?: return moduleId
    return m.parentId.ifBlank { m.id }
}

fun moduleStats(state: PersistedState, subject: String): List<ModuleStat> {
    val config = state.scoreConfigs.firstOrNull { it.subject == subject } ?: return emptyList()
    return config.items.map { item ->
        val sessions = state.sessions.filter { parentIdOf(state, it.moduleId) == item.moduleId }
        val questions = sessions.sumOf { it.questionCount }
        val correct = sessions.sumOf { it.correctCount }
        val accuracy = if (questions > 0) correct.toDouble() / questions else 0.0
        val full = item.questionCount * item.scorePerQuestion
        ModuleStat(
            id = item.moduleId,
            name = state.modules.firstOrNull { it.id == item.moduleId }?.name ?: item.moduleId,
            accuracy = accuracy,
            questions = questions,
            gain = (1.0 - accuracy) * full
        )
    }
}

fun estimateScore(state: PersistedState, subject: String): Double {
    val config = state.scoreConfigs.firstOrNull { it.subject == subject } ?: return 0.0
    return config.items.sumOf { item ->
        val sessions = state.sessions.filter { parentIdOf(state, it.moduleId) == item.moduleId }
        val questions = sessions.sumOf { it.questionCount }
        val correct = sessions.sumOf { it.correctCount }
        val accuracy = if (questions > 0) correct.toDouble() / questions else 0.0
        accuracy * item.questionCount * item.scorePerQuestion
    }
}

@Composable
fun AnalysisTab(state: PersistedState) {
    val c = LocalSgColors.current
    var subject by remember { mutableStateOf("XINGCE") }
    val xingceStats = moduleStats(state, "XINGCE")
    val shenlunStats = moduleStats(state, "SHENLUN")
    val xingceScore = estimateScore(state, "XINGCE")
    val shenlunScore = estimateScore(state, "SHENLUN")
    val hasData = state.sessions.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 行测 / 申论 两个独立标签页
        SgTabRow(
            tabs = listOf("行测", "申论"),
            selectedIndex = if (subject == "XINGCE") 0 else 1,
            onSelect = { subject = if (it == 0) "XINGCE" else "SHENLUN" }
        )
        if (subject == "XINGCE") {
        SgCard {
            SgSectionHeader("行测模块正确率", if (hasData) "按安徽省考分值加权" else "还没有刷题数据")
            Spacer(Modifier.height(6.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                RadarChart(xingceStats, c.accent, c.line, c.inkMuted)
            }
        }

        SgCard {
            SgSectionHeader("行测估分", "目标 75 分（可在设置改）")
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "%.1f".format(xingceScore),
                    style = SgType.heroNumber,
                    color = c.accent
                )
                Spacer(Modifier.width(8.dp))
                Text("分 / 100", style = SgType.meta, color = c.inkMuted, modifier = Modifier.padding(bottom = 8.dp))
            }
            Spacer(Modifier.height(8.dp))
            SgProgress(fraction = (xingceScore / 100.0).toFloat())
            if (!hasData) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "估分用的是已记录的刷题数据，现在还没有记录，先去刷一次题再回来看。",
                    style = SgType.meta, color = c.inkMuted
                )
            }
        }

        if (hasData) {
            SgCard {
                SgSectionHeader("正确率趋势", "最近 8 次刷题")
                Spacer(Modifier.height(10.dp))
                TrendChart(state.sessions.sortedBy { it.endedAt }.takeLast(8), c.accent, c.line)
            }
        }

        SgCard {
            SgSectionHeader("最该补的三块", "按提分空间排序")
            Spacer(Modifier.height(6.dp))
            val worst = xingceStats.sortedByDescending { it.gain }.take(3)
            if (worst.all { it.questions == 0 }) {
                Text("还没有足够的模块数据，先各刷一组再来对比。", style = SgType.bodyLong, color = c.inkMuted)
            } else {
                worst.forEach { st ->
                    val color = moduleColorOf(st.id)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 7.dp)) {
                        SgDot(color, 10)
                        Spacer(Modifier.width(9.dp))
                        Text(st.name, style = SgType.body, color = c.ink, modifier = Modifier.weight(1f))
                        Text(
                            if (st.questions == 0) "没刷过" else "%.0f%% · 可提 %.1f 分".format(st.accuracy * 100, st.gain),
                            style = SgType.meta, color = if (st.questions == 0) c.inkFaint else color
                        )
                    }
                }
            }
        }

        SgCard {
            SgSectionHeader("申论估分", "按各题型得分率")
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "%.1f".format(shenlunScore),
                    style = SgType.heroNumber,
                    color = c.accent
                )
                Spacer(Modifier.width(8.dp))
                Text("分 / 100", style = SgType.meta, color = c.inkMuted, modifier = Modifier.padding(bottom = 8.dp))
            }
            Spacer(Modifier.height(8.dp))
            SgProgress(fraction = (shenlunScore / 100.0).toFloat())
            if (shenlunStats.all { it.questions == 0 }) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "还没有申论的刷题记录，去「刷题」页选申论题型记一次就有了。",
                    style = SgType.meta, color = c.inkMuted
                )
            }
        }

        SgCard {
            SgSectionHeader("申论各题型得分率", "按得分估")
            Spacer(Modifier.height(6.dp))
            shenlunStats.forEach { st ->
                val color = moduleColorOf(st.id)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                    Text(st.name, style = SgType.body, color = c.ink, modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(c.surface2)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(st.accuracy.toFloat())
                                .height(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(color)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(if (st.questions == 0) "—" else "%.0f%%".format(st.accuracy * 100), style = SgType.meta, color = c.inkMuted)
                }
            }
        }
        }
    }
}

@Composable
private fun RadarChart(stats: List<ModuleStat>, accent: Color, line: Color, muted: Color) {
    val points = if (stats.isEmpty()) emptyList() else stats
    val gridColor = line
    Canvas(modifier = Modifier.size(240.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f - 26f
        val count = maxOf(points.size, 1)

        fun vertex(index: Int, value: Float): Offset {
            val angle = (-90f + 360f * index / count) * (Math.PI / 180f)
            return Offset(
                center.x + (radius * value * cos(angle)).toFloat(),
                center.y + (radius * value * sin(angle)).toFloat()
            )
        }

        // 网格
        listOf(0.33f, 0.66f, 1f).forEach { scale ->
            val path = Path()
            for (i in 0 until count) {
                val p = vertex(i, scale)
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            path.close()
            drawPath(path, color = gridColor, style = Stroke(width = 2f))
        }
        for (i in 0 until count) {
            drawLine(gridColor, center, vertex(i, 1f), strokeWidth = 2f)
        }

        // 数据
        if (points.isNotEmpty()) {
            val path = Path()
            points.forEachIndexed { i, st ->
                val p = vertex(i, st.accuracy.toFloat().coerceIn(0.04f, 1f))
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            path.close()
            drawPath(path, color = accent.copy(alpha = 0.26f))
            val strokePath = Path()
            points.forEachIndexed { i, st ->
                val p = vertex(i, st.accuracy.toFloat().coerceIn(0.04f, 1f))
                if (i == 0) strokePath.moveTo(p.x, p.y) else strokePath.lineTo(p.x, p.y)
            }
            strokePath.close()
            drawPath(strokePath, color = accent, style = Stroke(width = 5f))
            points.forEachIndexed { i, st ->
                val p = vertex(i, st.accuracy.toFloat().coerceIn(0.04f, 1f))
                drawCircle(moduleColorOf(st.id), radius = 7f, center = p)
            }
        }
    }
    Column(modifier = Modifier.padding(start = 12.dp)) {
        stats.forEach { st ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                SgDot(moduleColorOf(st.id), 8)
                Spacer(Modifier.width(6.dp))
                Text(
                    "${st.name.take(4)} ${if (st.questions == 0) "—" else "%.0f%%".format(st.accuracy * 100)}",
                    style = SgType.meta, color = muted
                )
            }
        }
    }
}

@Composable
private fun TrendChart(sessions: List<com.shanganba.examcountdown.data.PracticeSession>, accent: Color, line: Color) {
    if (sessions.isEmpty()) return
    val values = sessions.map {
        if (it.questionCount > 0) it.correctCount.toDouble() / it.questionCount else 0.0
    }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        val w = size.width
        val h = size.height
        val stepX = if (values.size > 1) w / (values.size - 1) else w
        drawLine(line, Offset(0f, h * 0.25f), Offset(w, h * 0.25f), strokeWidth = 2f)

        val path = Path()
        values.forEachIndexed { i, v ->
            val x = stepX * i
            val y = h - (h * v.toFloat()).coerceIn(6f, h - 6f)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = accent, style = Stroke(width = 5f))
        values.forEachIndexed { i, v ->
            val x = stepX * i
            val y = h - (h * v.toFloat()).coerceIn(6f, h - 6f)
            drawCircle(accent, radius = 6f, center = Offset(x, y))
        }
    }
    Spacer(Modifier.height(6.dp))
    Text(
        "第一次 %.0f%% → 最近一次 %.0f%%".format(values.first() * 100, values.last() * 100),
        style = SgType.meta,
        color = LocalSgColors.current.inkMuted
    )
}

@Composable
fun CheckinScreen(state: PersistedState, onBack: () -> Unit) {
    val c = LocalSgColors.current
    val today = java.time.LocalDate.now()
    val badges = listOf(
        Triple("🔥", "连续打卡 5 天", state.streak >= 5),
        Triple("📚", "累计刷题 1000 题", state.sessions.sumOf { it.questionCount } >= 1000),
        Triple("🧠", "复盘错题 20 道", state.questions.count { it.reviewCount > 0 } >= 20),
        Triple("⏱", "连续打卡 30 天", state.streak >= 30),
        Triple("🏆", "单次正确率 85%", state.sessions.any { it.questionCount >= 10 && it.correctCount * 100 / it.questionCount >= 85 }),
        Triple("🎯", "估分达到目标", estimateScore(state, "XINGCE") + estimateScore(state, "SHENLUN") >= state.settings.targetScore)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        SgScreenTitle("打卡与激励", right = { SgSoftButton("返回") { onBack() } })
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SgCard {
                Row {
                    SgLabelValue("连续打卡", "${state.streak}", suffix = "天")
                    Spacer(Modifier.width(20.dp))
                    SgLabelValue("累计打卡", "${state.totalCheckIn}", suffix = "天")
                    Spacer(Modifier.width(20.dp))
                    SgLabelValue("累计刷题", "${state.sessions.sumOf { it.questionCount }}", suffix = "题")
                }
            }
            SgCard {
                SgSectionHeader("最近 4 周打卡", "颜色越深完成度越高")
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("一", "二", "三", "四", "五", "六", "日").forEach { w ->
                        Text(
                            w,
                            style = SgType.meta,
                            color = c.inkFaint,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (week in 3 downTo 0) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (day in 0..6) {
                                val date = today.minusDays((week * 7 + (6 - day)).toLong())
                                val record = state.days[date.toString()]
                                val ratio = when {
                                    record == null -> 0f
                                    !record.checkIn -> 0.2f
                                    else -> 1f
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (ratio == 0f) c.surface2
                                            else c.accent.copy(alpha = 0.25f + 0.65f * ratio)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "${date.dayOfMonth}",
                                        style = SgType.meta,
                                        color = if (ratio > 0.6f) Color.White else c.inkMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
            SgCard {
                SgSectionHeader("勋章墙", "已点亮 ${badges.count { it.third }} / ${badges.size}")
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    badges.chunked(3).forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            row.forEach { (emoji, label, unlocked) ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(104.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (unlocked) c.accent.copy(alpha = 0.14f) else c.surface2)
                                        .padding(horizontal = 6.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(emoji, style = SgType.statValue)
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        label,
                                        style = SgType.meta.copy(fontSize = 11.sp, lineHeight = 15.sp),
                                        color = if (unlocked) c.ink else c.inkFaint,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            repeat(3 - row.size) {
                                Spacer(
                                    Modifier
                                        .weight(1f)
                                        .height(104.dp)
                                )
                            }
                        }
                    }
                }
            }
            SgCard {
                val quote = when (state.settings.tone) {
                    "soft" -> "今天坐下来写完这几道题，就已经比昨天的自己靠前了。"
                    "fun" -> "行测不会因为你叹气就变简单，但它会因为你刷题而变熟。"
                    else -> "今天这四道题不做，明天就变成八道。"
                }
                Text(quote, style = SgType.body, color = c.ink)
                Spacer(Modifier.height(6.dp))
                Text(
                    when (state.settings.tone) {
                        "soft" -> "温柔模式"
                        "fun" -> "轻松模式"
                        else -> "硬核模式"
                    },
                    style = SgType.meta, color = c.inkMuted
                )
            }
            SgCard {
                Text(
                    "数据截止 ${Dates.todayKey()} · 打卡和刷题记录都只存在本机。",
                    style = SgType.meta, color = c.inkFaint
                )
            }
        }
    }
}
