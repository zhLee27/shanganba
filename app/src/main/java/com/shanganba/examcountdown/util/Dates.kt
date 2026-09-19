package com.shanganba.examcountdown.util

import com.shanganba.examcountdown.data.ExamNode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object Dates {

    private val zone: ZoneId get() = ZoneId.systemDefault()
    private val dayFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFmt = DateTimeFormatter.ofPattern("MM-dd HH:mm")

    fun todayKey(now: Long = System.currentTimeMillis()): String =
        LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(now), zone).toLocalDate().toString()

    fun daysUntil(target: Long, now: Long = System.currentTimeMillis()): Long {
        val diff = target - now
        return if (diff >= 0) diff / 86_400_000L else -((-diff + 86_399_999L) / 86_400_000L)
    }

    fun spanDays(from: Long, to: Long): Long =
        Math.round((to - from).toDouble() / 86_400_000.0)

    fun progressPercent(from: Long, to: Long, now: Long = System.currentTimeMillis()): Double {
        val span = (to - from).toDouble()
        if (span <= 0) return 100.0
        val used = (now - from).toDouble()
        return (used / span * 100.0).coerceIn(0.0, 100.0)
    }

    fun formatDateTime(millis: Long): String =
        LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis), zone).format(timeFmt)

    fun toLocalDateTime(millis: Long): LocalDateTime =
        LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis), zone)

    fun toMillis(dateTime: LocalDateTime): Long = dateTime.atZone(zone).toInstant().toEpochMilli()

    fun parseDate(text: String): LocalDate? = try {
        LocalDate.parse(text, dayFmt)
    } catch (e: Exception) {
        null
    }

    fun startOfDay(date: LocalDate): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()

    /** 主节点：钉选的优先，否则取最近一个未过期的，全部过期则取最后一个。 */
    fun primaryNode(nodes: List<ExamNode>, now: Long = System.currentTimeMillis()): ExamNode? {
        if (nodes.isEmpty()) return null
        nodes.firstOrNull { it.pinned }?.let { return it }
        return nodes.filter { it.dateTime >= now }.minByOrNull { it.dateTime }
            ?: nodes.maxByOrNull { it.dateTime }
    }

    fun upcomingNodes(nodes: List<ExamNode>, now: Long = System.currentTimeMillis()): List<ExamNode> =
        nodes.filter { it.dateTime >= now }.sortedBy { it.dateTime }
}
