package com.shanganba.examcountdown.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.shanganba.examcountdown.MainActivity
import com.shanganba.examcountdown.data.AppStore
import com.shanganba.examcountdown.data.PersistedState
import com.shanganba.examcountdown.ui.estimateScore
import com.shanganba.examcountdown.util.Dates

class CountdownWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = AppStore(context).readFromDisk()
        provideContent { WidgetContent(state) }
    }
}

class CountdownWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CountdownWidget()
}

@Composable
private fun WidgetContent(state: PersistedState) {
    val node = Dates.primaryNode(state.nodes)
    val days = if (node != null) Dates.daysUntil(node.dateTime) else 0L
    val startMillis = state.settings.startDate
        .takeIf { it.isNotBlank() }
        ?.let { Dates.parseDate(it) }
        ?.let { Dates.startOfDay(it) }
        ?: System.currentTimeMillis()
    val exam = state.nodes.firstOrNull { it.type == "笔试" } ?: node
    val pct = if (exam != null) Dates.progressPercent(startMillis, exam.dateTime) else 0.0
    val score = estimateScore(state, "XINGCE") + estimateScore(state, "SHENLUN")

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xFFF3FBF7)))
            .cornerRadius(26.dp)
            .padding(16.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        Text(
            "上岸吧 · 距离${node?.title ?: "考试"}",
            style = TextStyle(color = ColorProvider(Color(0xFF5E7A6C)), fontSize = 12.sp)
        )
        Spacer(GlanceModifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "$days",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF1F2A24)),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            Spacer(GlanceModifier.width(6.dp))
            Text("天", style = TextStyle(color = ColorProvider(Color(0xFF7C8A82)), fontSize = 14.sp))
        }
        Spacer(GlanceModifier.height(8.dp))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                "备考进度 %.1f%%".format(pct),
                style = TextStyle(color = ColorProvider(Color(0xFF7C8A82)), fontSize = 12.sp)
            )
            Spacer(GlanceModifier.width(12.dp))
            Text(
                "估分 %.1f / 200".format(score),
                style = TextStyle(color = ColorProvider(Color(0xFF2E9E77)), fontSize = 12.sp)
            )
        }
    }
}
