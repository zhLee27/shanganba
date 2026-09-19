package com.shanganba.examcountdown.data

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

private val zone: ZoneId get() = ZoneId.systemDefault()

private fun millis(y: Int, m: Int, d: Int, hh: Int, mm: Int): Long =
    LocalDateTime.of(y, m, d, hh, mm).atZone(zone).toInstant().toEpochMilli()

@Serializable
data class ExamNode(
    val id: String,
    val title: String,
    val type: String,          // 报名开始 / 报名截止 / 笔试 / 面试 / 自定义
    val dateTime: Long,
    val note: String = "",
    val pinned: Boolean = false
)

@Serializable
data class SubjectModule(
    val id: String,
    val subject: String,       // XINGCE / SHENLUN
    val name: String,
    val order: Int,
    val defaultQuestionCount: Int = 20,
    val defaultSeconds: Int = 1200,
    val custom: Boolean = false
)

@Serializable
data class PracticeSession(
    val id: String,
    val moduleId: String,
    val mode: String,          // COUNT_UP / COUNT_DOWN
    val plannedSeconds: Int,
    val usedSeconds: Int,
    val questionCount: Int,
    val correctCount: Int,
    val markedCount: Int = 0,
    val perQuestionSeconds: List<Int> = emptyList(),
    val note: String = "",
    val startedAt: Long,
    val endedAt: Long
)

@Serializable
data class QuestionRecord(
    val id: String,
    val moduleId: String,
    val kind: String = "WRONG",          // WRONG / KEY
    val source: String = "MANUAL",       // CAMERA / FILE / MANUAL
    val stemText: String = "",
    val stemImagePath: String = "",
    val options: List<String> = emptyList(),
    val myAnswer: String = "",
    val correctAnswer: String = "",
    val errorCause: String = "",
    val knowledgePointIds: List<String> = emptyList(),
    val tipText: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val reviewCount: Int = 0,
    val masteredAt: Long = 0L
)

@Serializable
data class KnowledgeNode(
    val id: String,
    val parentId: String = "",
    val subject: String = "XINGCE",
    val name: String,
    val order: Int = 0,
    val masteryPercent: Int = 0
)

@Serializable
data class TaskTemplate(
    val id: String,
    val title: String,
    val moduleId: String = "",
    val targetAmount: Int = 0,
    val unit: String = "题",
    val repeatRule: String = "DAILY",
    val enabled: Boolean = true,
    val order: Int = 0
)

@Serializable
data class ExtraTask(val id: String, val title: String)

@Serializable
data class ActiveTimer(
    val moduleId: String,
    val mode: String,               // COUNT_UP / COUNT_DOWN
    val plannedSeconds: Int,
    val questionTarget: Int,
    val startedAtWall: Long,
    val lastResumeElapsed: Long,    // SystemClock.elapsedRealtime()
    val accumulatedSeconds: Int = 0,
    val paused: Boolean = false,
    val questionCount: Int = 0,
    val markedCount: Int = 0,
    val lastTapElapsed: Long = 0L,
    val perQuestionSeconds: List<Int> = emptyList(),
    val endAlarmAt: Long = 0L
) {
    fun elapsedSeconds(nowElapsed: Long): Int =
        if (paused) accumulatedSeconds
        else accumulatedSeconds + ((nowElapsed - lastResumeElapsed) / 1000L).toInt().coerceAtLeast(0)
}

@Serializable
data class DayRecord(
    val date: String,
    val doneTemplateIds: List<String> = emptyList(),
    val doneExtraIds: List<String> = emptyList(),
    val extraTasks: List<ExtraTask> = emptyList(),
    val checkIn: Boolean = false,
    val sessionIds: List<String> = emptyList()
)

@Serializable
data class ScoreItem(val moduleId: String, val questionCount: Int, val scorePerQuestion: Double)

@Serializable
data class ScoreConfig(
    val subject: String,
    val items: List<ScoreItem> = emptyList(),
    val totalScore: Double = 100.0
)

@Serializable
data class Settings(
    val theme: String = "fresh",           // fresh / pop / sticker
    val darkMode: String = "system",       // system / light / dark
    val startDate: String = "",            // yyyy-MM-dd
    val targetScore: Double = 135.0,
    val dailyReminderOn: Boolean = true,
    val dailyReminderMinute: Int = 20 * 60,
    val nodeReminderOn: Boolean = true,
    val nodeReminderMinute: Int = 9 * 60,
    val autoCheckUpdate: Boolean = true,
    val updateUrl: String = "",
    val tone: String = "hard",             // hard / soft / fun
    val firstLaunchAt: Long = 0L,
    /** 已经提示过更新的版本号，避免每次启动都弹同一个版本 */
    val lastPromptedUpdateVersionCode: Int = 0,
    /** 计时方式的预设时长（分钟），可增删 */
    val timerPresets: List<Int> = listOf(15, 25, 30)
)

@Serializable
data class PersistedState(
    val schemaVersion: Int = 1,
    val nodes: List<ExamNode> = defaultNodes(),
    val modules: List<SubjectModule> = defaultModules(),
    val templates: List<TaskTemplate> = defaultTemplates(),
    val sessions: List<PracticeSession> = emptyList(),
    val questions: List<QuestionRecord> = emptyList(),
    val knowledge: List<KnowledgeNode> = defaultKnowledge(),
    val days: Map<String, DayRecord> = emptyMap(),
    val streak: Int = 0,
    val totalCheckIn: Int = 0,
    val lastCheckInDate: String = "",
    val scoreConfigs: List<ScoreConfig> = defaultScoreConfigs(),
    val activeTimer: ActiveTimer? = null,
    val settings: Settings = Settings()
)

fun defaultNodes(): List<ExamNode> = listOf(
    ExamNode("n_baoming_start", "报名开始", "报名开始", millis(2027, 1, 8, 9, 0)),
    ExamNode("n_baoming_end", "报名截止", "报名截止", millis(2027, 1, 14, 17, 0)),
    ExamNode("n_bishi", "安徽省考 · 笔试", "笔试", millis(2027, 3, 14, 9, 0), "行测 + 申论", pinned = true),
    ExamNode("n_mianshi", "面试", "面试", millis(2027, 5, 9, 9, 0))
)

fun defaultModules(): List<SubjectModule> = listOf(
    SubjectModule("m_yanyu", "XINGCE", "言语理解与表达", 1, 30, 1800),
    SubjectModule("m_shuliang", "XINGCE", "数量关系", 2, 15, 1200),
    SubjectModule("m_panduan", "XINGCE", "判断推理", 3, 35, 2400),
    SubjectModule("m_ziliao", "XINGCE", "资料分析", 4, 20, 1500),
    SubjectModule("m_changshi", "XINGCE", "常识判断", 5, 20, 900),
    SubjectModule("m_guina", "SHENLUN", "申论 · 归纳概括", 6, 1, 1500),
    SubjectModule("m_zonghe", "SHENLUN", "申论 · 综合分析", 7, 1, 1500),
    SubjectModule("m_duice", "SHENLUN", "申论 · 提出对策", 8, 1, 1500),
    SubjectModule("m_guanche", "SHENLUN", "申论 · 贯彻执行", 9, 1, 1500),
    SubjectModule("m_zuowen", "SHENLUN", "申论 · 文章写作", 10, 1, 3000)
)

fun defaultTemplates(): List<TaskTemplate> = listOf(
    TaskTemplate("t1", "资料分析 · 3 组", "m_ziliao", 60, "题", order = 1),
    TaskTemplate("t2", "言语理解 · 2 组", "m_yanyu", 60, "题", order = 2),
    TaskTemplate("t3", "申论 · 归纳概括 1 篇", "m_guina", 1, "篇", order = 3),
    TaskTemplate("t4", "错题复盘 20 道", "", 20, "道", order = 4)
)

fun defaultKnowledge(): List<KnowledgeNode> = listOf(
    KnowledgeNode("k_xc", "", "XINGCE", "行测", 1),
    KnowledgeNode("k_pd", "k_xc", "XINGCE", "判断推理", 1),
    KnowledgeNode("k_tx", "k_pd", "XINGCE", "图形推理", 1),
    KnowledgeNode("k_tx_wz", "k_tx", "XINGCE", "位置规律", 1),
    KnowledgeNode("k_tx_sl", "k_tx", "XINGCE", "数量规律", 2),
    KnowledgeNode("k_dy", "k_pd", "XINGCE", "定义判断", 2),
    KnowledgeNode("k_zl", "k_xc", "XINGCE", "资料分析", 2),
    KnowledgeNode("k_zl_bz", "k_zl", "XINGCE", "比重与平均数", 1),
    KnowledgeNode("k_zl_zz", "k_zl", "XINGCE", "增长率与增长量", 2),
    KnowledgeNode("k_sl", "k_xc", "XINGCE", "数量关系", 3),
    KnowledgeNode("k_sl_gc", "k_sl", "XINGCE", "工程问题", 1),
    KnowledgeNode("k_yy", "k_xc", "XINGCE", "言语理解", 4),
    KnowledgeNode("k_cs", "k_xc", "XINGCE", "常识判断", 5),
    KnowledgeNode("k_shenlun", "", "SHENLUN", "申论", 1),
    KnowledgeNode("k_gn", "k_shenlun", "SHENLUN", "归纳概括", 1),
    KnowledgeNode("k_xz", "k_shenlun", "SHENLUN", "文章写作", 2)
)

fun defaultScoreConfigs(): List<ScoreConfig> = listOf(
    ScoreConfig(
        "XINGCE",
        listOf(
            ScoreItem("m_changshi", 20, 0.5),
            ScoreItem("m_yanyu", 30, 0.8),
            ScoreItem("m_shuliang", 15, 0.8),
            ScoreItem("m_panduan", 35, 0.8),
            ScoreItem("m_ziliao", 20, 1.3)
        ),
        100.0
    ),
    ScoreConfig(
        "SHENLUN",
        listOf(
            ScoreItem("m_guina", 1, 20.0),
            ScoreItem("m_zonghe", 1, 20.0),
            ScoreItem("m_guanche", 1, 25.0),
            ScoreItem("m_zuowen", 1, 35.0)
        ),
        100.0
    )
)

fun todayKey(date: LocalDate = LocalDate.now()): String = date.toString()
