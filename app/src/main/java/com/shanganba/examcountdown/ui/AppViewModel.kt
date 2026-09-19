package com.shanganba.examcountdown.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shanganba.examcountdown.data.AppStore
import com.shanganba.examcountdown.data.ActiveTimer
import com.shanganba.examcountdown.data.DayRecord
import com.shanganba.examcountdown.data.ExamNode
import com.shanganba.examcountdown.data.ExtraTask
import com.shanganba.examcountdown.data.KnowledgeNode
import com.shanganba.examcountdown.data.PersistedState
import com.shanganba.examcountdown.data.PracticeSession
import com.shanganba.examcountdown.data.QuestionRecord
import com.shanganba.examcountdown.data.SubjectModule
import com.shanganba.examcountdown.data.Settings
import com.shanganba.examcountdown.data.TaskTemplate
import com.shanganba.examcountdown.data.todayKey
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

class AppViewModel(app: Application) : AndroidViewModel(app) {

    val store = AppStore(app)
    val state: StateFlow<PersistedState> = store.state

    fun mutate(block: (PersistedState) -> PersistedState) {
        viewModelScope.launch { store.update(block) }
    }

    fun today(state: PersistedState): DayRecord =
        state.days[todayKey()] ?: DayRecord(date = todayKey())

    // ---------- 任务 ----------

    fun toggleTemplate(templateId: String) = mutate { s ->
        val key = todayKey()
        val day = s.days[key] ?: DayRecord(date = key)
        val done = day.doneTemplateIds.toMutableList()
        if (!done.remove(templateId)) done.add(templateId)
        s.copy(days = s.days + (key to day.copy(doneTemplateIds = done)))
    }

    fun addExtraTask(title: String) = mutate { s ->
        val key = todayKey()
        val day = s.days[key] ?: DayRecord(date = key)
        val task = ExtraTask(UUID.randomUUID().toString(), title)
        s.copy(days = s.days + (key to day.copy(extraTasks = day.extraTasks + task)))
    }

    fun toggleExtraTask(id: String) = mutate { s ->
        val key = todayKey()
        val day = s.days[key] ?: return@mutate s
        val done = day.doneExtraIds.toMutableList()
        if (!done.remove(id)) done.add(id)
        s.copy(days = s.days + (key to day.copy(doneExtraIds = done)))
    }

    fun removeExtraTask(id: String) = mutate { s ->
        val key = todayKey()
        val day = s.days[key] ?: return@mutate s
        s.copy(
            days = s.days + (key to day.copy(
                extraTasks = day.extraTasks.filterNot { it.id == id },
                doneExtraIds = day.doneExtraIds.filterNot { it == id }
            ))
        )
    }

    // ---------- 打卡 ----------

    fun checkInToday() = mutate { s ->
        val key = todayKey()
        if (s.lastCheckInDate == key) return@mutate s
        val yesterday = LocalDate.now().minusDays(1).toString()
        val streak = if (s.lastCheckInDate == yesterday) s.streak + 1 else 1
        val day = s.days[key] ?: DayRecord(date = key)
        s.copy(
            streak = streak,
            totalCheckIn = s.totalCheckIn + 1,
            lastCheckInDate = key,
            days = s.days + (key to day.copy(checkIn = true))
        )
    }

    // ---------- 考试节点 ----------

    fun upsertNode(node: ExamNode) = mutate { s ->
        val exists = s.nodes.any { it.id == node.id }
        val nodes = if (exists) s.nodes.map { if (it.id == node.id) node else it } else s.nodes + node
        val fixed = if (node.pinned) nodes.map { if (it.id == node.id) it else it.copy(pinned = false) } else nodes
        s.copy(nodes = fixed.sortedBy { it.dateTime })
    }

    fun pinNode(id: String) = mutate { s ->
        s.copy(nodes = s.nodes.map { it.copy(pinned = it.id == id) })
    }

    fun deleteNode(id: String) = mutate { s ->
        s.copy(nodes = s.nodes.filterNot { it.id == id })
    }

    // ---------- 任务模板 ----------

    fun upsertTemplate(template: TaskTemplate) = mutate { s ->
        val exists = s.templates.any { it.id == template.id }
        val list = if (exists) s.templates.map { if (it.id == template.id) template else it }
        else s.templates + template
        s.copy(templates = list.sortedBy { it.order })
    }

    fun deleteTemplate(id: String) = mutate { s ->
        s.copy(templates = s.templates.filterNot { it.id == id })
    }

    // ---------- 设置 ----------

    fun updateSettings(block: (Settings) -> Settings) = mutate { s ->
        s.copy(settings = block(s.settings))
    }

    // ---------- 模块 ----------

    fun upsertModule(module: SubjectModule) = mutate { s ->
        val exists = s.modules.any { it.id == module.id }
        val list = if (exists) s.modules.map { if (it.id == module.id) module else it } else s.modules + module
        s.copy(modules = list.sortedBy { it.order })
    }

    fun deleteModule(id: String) = mutate { s ->
        s.copy(modules = s.modules.filterNot { it.id == id })
    }

    // ---------- 刷题计时 ----------

    fun startTimer(timer: ActiveTimer) = mutate { s -> s.copy(activeTimer = timer) }

    fun updateTimer(block: (ActiveTimer) -> ActiveTimer) = mutate { s ->
        val t = s.activeTimer ?: return@mutate s
        s.copy(activeTimer = block(t))
    }

    fun cancelTimer() = mutate { s -> s.copy(activeTimer = null) }

    fun finishTimer(questionCount: Int, correctCount: Int, markedCount: Int, note: String) = mutate { s ->
        val t = s.activeTimer ?: return@mutate s
        val now = System.currentTimeMillis()
        val session = PracticeSession(
            id = UUID.randomUUID().toString(),
            moduleId = t.moduleId,
            mode = t.mode,
            plannedSeconds = t.plannedSeconds,
            usedSeconds = t.elapsedSeconds(android.os.SystemClock.elapsedRealtime()),
            questionCount = questionCount,
            correctCount = correctCount,
            markedCount = markedCount,
            perQuestionSeconds = t.perQuestionSeconds,
            note = note,
            startedAt = t.startedAtWall,
            endedAt = now
        )
        val key = todayKey()
        val day = s.days[key] ?: DayRecord(date = key)
        s.copy(
            sessions = s.sessions + session,
            activeTimer = null,
            days = s.days + (key to day.copy(sessionIds = day.sessionIds + session.id))
        )
    }

    fun deleteSession(id: String) = mutate { s ->
        s.copy(sessions = s.sessions.filterNot { it.id == id })
    }

    // ---------- 错题 ----------

    fun upsertQuestion(record: QuestionRecord) = mutate { s ->
        val exists = s.questions.any { it.id == record.id }
        val list = if (exists) s.questions.map { if (it.id == record.id) record else it } else s.questions + record
        s.copy(questions = list.sortedByDescending { it.createdAt })
    }

    fun markQuestionMastered(id: String, mastered: Boolean) = mutate { s ->
        s.copy(
            questions = s.questions.map {
                if (it.id == id) it.copy(masteredAt = if (mastered) System.currentTimeMillis() else 0L)
                else it
            }
        )
    }

    fun bumpReview(id: String) = mutate { s ->
        s.copy(questions = s.questions.map { if (it.id == id) it.copy(reviewCount = it.reviewCount + 1) else it })
    }

    fun deleteQuestion(id: String) = mutate { s ->
        s.copy(questions = s.questions.filterNot { it.id == id })
    }

    // ---------- 知识框架 ----------

    fun upsertKnowledge(node: KnowledgeNode) = mutate { s ->
        val exists = s.knowledge.any { it.id == node.id }
        val list = if (exists) s.knowledge.map { if (it.id == node.id) node else it } else s.knowledge + node
        s.copy(knowledge = list)
    }

    fun deleteKnowledge(id: String) = mutate { s ->
        val doomed = mutableSetOf(id)
        var grew = true
        while (grew) {
            val next = s.knowledge.filter { it.parentId in doomed }.map { it.id }
            grew = doomed.addAll(next)
        }
        s.copy(knowledge = s.knowledge.filterNot { it.id in doomed })
    }

    fun exportText(): String = store.exportText()

    fun importText(text: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch { onResult(store.importText(text)) }
    }
}
