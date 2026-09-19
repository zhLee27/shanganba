package com.shanganba.examcountdown.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.shanganba.examcountdown.MainActivity
import com.shanganba.examcountdown.R
import com.shanganba.examcountdown.data.AppStore
import com.shanganba.examcountdown.data.PersistedState
import com.shanganba.examcountdown.data.todayKey
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object Reminders {

    const val CHANNEL_ID = "shanganba_reminder"
    const val EXTRA_KIND = "kind"
    const val KIND_DAILY = "daily"
    const val KIND_NODE = "node"
    const val KIND_TIMER = "timer"

    private const val RC_DAILY = 1001
    private const val RC_NODE = 1002
    private const val RC_TIMER = 1003

    fun ensureChannel(ctx: Context) {
        val manager = ctx.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "备考提醒", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "每日计划提醒、考前节点提醒和刷题计时到点提醒"
                }
            )
        }
    }

    fun canNotify(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun pending(ctx: Context, kind: String, requestCode: Int, title: String, text: String): PendingIntent {
        val intent = Intent(ctx, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_KIND, kind)
            putExtra("title", title)
            putExtra("text", text)
        }
        return PendingIntent.getBroadcast(
            ctx, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun scheduleAt(ctx: Context, atMillis: Long, kind: String, requestCode: Int, title: String, text: String) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = pending(ctx, kind, requestCode, title, text)
        try {
            val exactAllowed = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
            if (exactAllowed) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
            }
        } catch (e: SecurityException) {
            am.set(AlarmManager.RTC_WAKEUP, atMillis, pi)
        }
    }

    private fun nextOccurrence(minuteOfDay: Int, plusDays: Long = 0): Long {
        val zone = ZoneId.systemDefault()
        var date = LocalDate.now().plusDays(plusDays)
        var dt = LocalDateTime.of(date, java.time.LocalTime.of(minuteOfDay / 60, minuteOfDay % 60))
        if (plusDays == 0L && dt.atZone(zone).toInstant().toEpochMilli() <= System.currentTimeMillis()) {
            date = date.plusDays(1)
            dt = LocalDateTime.of(date, java.time.LocalTime.of(minuteOfDay / 60, minuteOfDay % 60))
        }
        return dt.atZone(zone).toInstant().toEpochMilli()
    }

    /** 根据当前设置重新排定所有提醒 */
    fun rescheduleAll(ctx: Context, state: PersistedState) {
        ensureChannel(ctx)
        val s = state.settings
        if (s.dailyReminderOn) {
            scheduleAt(
                ctx, nextOccurrence(s.dailyReminderMinute), KIND_DAILY, RC_DAILY,
                "今天的计划还没完成",
                "还有 ${state.templates.count { it.enabled }} 项任务在等你，花几分钟把今天的勾掉吧。"
            )
        }
        if (s.nodeReminderOn) {
            state.nodes.filter { it.type == "笔试" || it.type == "报名截止" }.forEach { node ->
                listOf(7, 3, 1).forEach { offset ->
                    val zone = ZoneId.systemDefault()
                    val target = java.time.Instant.ofEpochMilli(node.dateTime).atZone(zone).toLocalDate().minusDays(offset.toLong())
                    val at = LocalDateTime.of(target, java.time.LocalTime.of(s.nodeReminderMinute / 60, s.nodeReminderMinute % 60))
                        .atZone(zone).toInstant().toEpochMilli()
                    if (at > System.currentTimeMillis()) {
                        scheduleAt(
                            ctx, at, KIND_NODE, RC_NODE + offset,
                            "${node.title}还有 $offset 天",
                            "别忘了核对准考证和材料，把状态调整好。"
                        )
                    }
                }
            }
        }
    }

    fun scheduleTimerEnd(ctx: Context, atMillis: Long, moduleName: String) {
        ensureChannel(ctx)
        scheduleAt(ctx, atMillis, KIND_TIMER, RC_TIMER, "时间到 · $moduleName", "本次计时结束，回来录一下做了多少题吧。")
    }

    fun cancelTimerEnd(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        am.cancel(pending(ctx, KIND_TIMER, RC_TIMER, "", ""))
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra(Reminders.EXTRA_KIND) ?: Reminders.KIND_DAILY
        val title = intent.getStringExtra("title") ?: "上岸吧"
        val text = intent.getStringExtra("text") ?: ""
        Reminders.ensureChannel(context)

        if (Reminders.canNotify(context)) {
            val open = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, Reminders.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_countdown)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .setContentIntent(open)
                .build()
            try {
                context.getSystemService(NotificationManager::class.java)
                    .notify(kind.hashCode(), notification)
            } catch (_: SecurityException) {
            }
        }

        // 每日提醒和节点提醒是“下一次”模式，用完立刻排下一天
        val store = AppStore(context)
        val state = store.readFromDisk()
        if (kind == Reminders.KIND_DAILY) {
            val s = state.settings
            if (s.dailyReminderOn) {
                val done = state.days[todayKey()]?.let { day ->
                    val total = state.templates.count { it.enabled }
                    total > 0 && day.doneTemplateIds.size >= total
                } ?: false
                if (!done) {
                    Reminders.rescheduleAll(context, state)
                } else {
                    Reminders.rescheduleAll(context, state)
                }
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val state = AppStore(context).readFromDisk()
            Reminders.rescheduleAll(context, state)
        }
    }
}
