package com.shanganba.examcountdown

import com.shanganba.examcountdown.data.ExamNode
import com.shanganba.examcountdown.data.defaultScoreConfigs
import com.shanganba.examcountdown.util.Dates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class DatesTest {

    private fun at(y: Int, m: Int, d: Int, hh: Int = 9, mm: Int = 0): Long =
        Dates.toMillis(LocalDateTime.of(y, m, d, hh, mm))

    @Test
    fun daysUntil_counts_whole_days() {
        val now = at(2026, 9, 19, 0, 0)
        assertEquals(176L, Dates.daysUntil(at(2027, 3, 14), now))
        assertEquals(0L, Dates.daysUntil(now, now))
        assertTrue(Dates.daysUntil(at(2026, 9, 18), now) < 0)
    }

    @Test
    fun progress_is_clamped_between_zero_and_hundred() {
        val start = at(2026, 9, 1, 0, 0)
        val exam = at(2027, 3, 14, 9, 0)
        val before = Dates.progressPercent(start, exam, at(2026, 8, 1, 0, 0))
        val after = Dates.progressPercent(start, exam, at(2027, 5, 1, 0, 0))
        val middle = Dates.progressPercent(start, exam, at(2026, 9, 19, 0, 0))
        assertEquals(0.0, before, 0.001)
        assertEquals(100.0, after, 0.001)
        assertTrue(middle in 9.0..10.0)
    }

    @Test
    fun pinned_node_wins_over_nearest() {
        val nodes = listOf(
            ExamNode("a", "报名开始", "报名开始", at(2027, 1, 8)),
            ExamNode("b", "笔试", "笔试", at(2027, 3, 14), pinned = true),
            ExamNode("c", "面试", "面试", at(2027, 5, 9))
        )
        val primary = Dates.primaryNode(nodes, at(2026, 9, 19, 0, 0))
        assertNotNull(primary)
        assertEquals("b", primary!!.id)
    }

    @Test
    fun nearest_future_node_is_used_when_nothing_pinned() {
        val nodes = listOf(
            ExamNode("a", "报名开始", "报名开始", at(2027, 1, 8)),
            ExamNode("b", "笔试", "笔试", at(2027, 3, 14)),
            ExamNode("c", "面试", "面试", at(2027, 5, 9))
        )
        val primary = Dates.primaryNode(nodes, at(2026, 9, 19, 0, 0))
        assertEquals("a", primary!!.id)
    }

    @Test
    fun score_config_matches_anhui_defaults() {
        val configs = defaultScoreConfigs()
        val xingce = configs.first { it.subject == "XINGCE" }
        val total = xingce.items.sumOf { it.questionCount * it.scorePerQuestion }
        // 2027 新结构：125 题；按最新分值表相加为 108（10.5+12+22.5+13.5+4.5+9+8+10+18）
        assertEquals(125, xingce.items.sumOf { it.questionCount })
        assertEquals(108.0, total, 0.01)

        val shenlun = configs.first { it.subject == "SHENLUN" }
        assertEquals(100.0, shenlun.items.sumOf { it.scorePerQuestion }, 0.001)
    }
}
