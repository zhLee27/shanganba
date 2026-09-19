package com.shanganba.examcountdown.ui

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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shanganba.examcountdown.data.ExamNode
import com.shanganba.examcountdown.data.KnowledgeNode
import com.shanganba.examcountdown.data.PersistedState
import com.shanganba.examcountdown.data.TaskTemplate
import com.shanganba.examcountdown.util.Dates
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Composable
private fun BackBar(title: String, onBack: () -> Unit) {
    val c = LocalSgColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 20.dp, top = 8.dp, bottom = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) { Text("‹", style = SgType.pageTitle, color = c.inkMuted) }
        Spacer(Modifier.width(6.dp))
        Text(title, style = SgType.pageTitle, color = c.inkTitle)
    }
}

@Composable
fun NodesScreen(vm: AppViewModel, state: PersistedState, onBack: () -> Unit) {
    val c = LocalSgColors.current
    var editing by remember { mutableStateOf<ExamNode?>(null) }
    var showNew by remember { mutableStateOf(false) }
    var pendingDeleteNode by remember { mutableStateOf<ExamNode?>(null) }
    val scroll = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize()) {
        BackBar("考试节点", onBack)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            state.nodes.sortedBy { it.dateTime }.forEach { node ->
                SgCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(node.title, style = SgType.cardTitle, color = c.inkTitle)
                                Spacer(Modifier.width(8.dp))
                                SgChip(node.type, nodeColor(node.type))
                                if (node.pinned) {
                                    Spacer(Modifier.width(6.dp))
                                    SgChip("首页主倒计时", c.accent)
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                Dates.toLocalDateTime(node.dateTime)
                                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) +
                                    " · 还剩 ${Dates.daysUntil(node.dateTime)} 天",
                                style = SgType.meta,
                                color = c.inkMuted
                            )
                            if (node.note.isNotBlank()) {
                                Text(node.note, style = SgType.meta, color = c.inkFaint)
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    SgDivider()
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "设为首页主倒计时",
                            style = SgType.meta,
                            color = c.inkMuted,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = node.pinned,
                            onCheckedChange = { vm.pinNode(node.id) }
                        )
                        Spacer(Modifier.width(8.dp))
                        SgSoftButton("编辑") { editing = node }
                        Spacer(Modifier.width(8.dp))
                        SgSoftButton("删除") { pendingDeleteNode = node }
                    }
                }
            }
            SgWideButton("＋ 新增节点") { showNew = true }
        }
    }

    if (showNew || editing != null) {
        NodeDialog(
            initial = editing,
            onDismiss = { showNew = false; editing = null },
            onSave = { node ->
                vm.upsertNode(node)
                showNew = false
                editing = null
            }
        )
    }

    pendingDeleteNode?.let { node ->
        SgConfirmDialog(
            title = "删除考试节点",
            message = "确定删除「${node.title}」吗？删掉后首页倒计时会改用其他节点。",
            onConfirm = { vm.deleteNode(node.id) },
            onDismiss = { pendingDeleteNode = null }
        )
    }
}

private fun nodeColor(type: String): Color = when (type) {
    "笔试" -> ModuleColors[3]
    "面试" -> ModuleColors[2]
    "报名开始", "报名截止" -> ModuleColors[1]
    else -> ModuleColors[0]
}

@Composable
private fun NodeDialog(initial: ExamNode?, onDismiss: () -> Unit, onSave: (ExamNode) -> Unit) {
    val c = LocalSgColors.current
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: "笔试") }
    var date by remember {
        mutableStateOf(
            Dates.toLocalDateTime(initial?.dateTime ?: System.currentTimeMillis())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        )
    }
    var time by remember {
        mutableStateOf(
            Dates.toLocalDateTime(initial?.dateTime ?: System.currentTimeMillis())
                .format(DateTimeFormatter.ofPattern("HH:mm"))
        )
    }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "新增考试节点" else "编辑节点", style = SgType.cardTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SgTextField(
                    value = title, onValueChange = { title = it },
                    label = "名称，如 安徽省考 · 笔试", singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("报名开始", "报名截止", "笔试", "面试", "自定义").forEach { t ->
                        SgChip(t, if (t == type) c.accent else c.inkMuted, modifier = Modifier.clickable { type = t })
                    }
                }
                SgTextField(
                    value = date, onValueChange = { date = it },
                    label = "日期 yyyy-MM-dd", singleLine = true
                )
                SgTextField(
                    value = time, onValueChange = { time = it },
                    label = "时间 HH:mm", singleLine = true
                )
                SgTextField(
                    value = note, onValueChange = { note = it },
                    label = "备注（可空）", singleLine = true
                )
                if (error.isNotEmpty()) {
                    Text(error, style = SgType.meta, color = c.accent2)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val dateValue = Dates.parseDate(date)
                val timeParts = time.split(":")
                if (title.isBlank() || dateValue == null || timeParts.size != 2) {
                    error = "名称、日期、时间都要填对，时间格式如 09:00"
                    return@TextButton
                }
                val hh = timeParts[0].toIntOrNull() ?: 0
                val mm = timeParts[1].toIntOrNull() ?: 0
                onSave(
                    ExamNode(
                        id = initial?.id ?: UUID.randomUUID().toString(),
                        title = title.trim(),
                        type = type,
                        dateTime = Dates.toMillis(LocalDateTime.of(dateValue.year, dateValue.monthValue, dateValue.dayOfMonth, hh, mm)),
                        note = note.trim(),
                        pinned = initial?.pinned ?: false
                    )
                )
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun TasksScreen(vm: AppViewModel, state: PersistedState, onBack: () -> Unit) {
    val c = LocalSgColors.current
    var editing by remember { mutableStateOf<TaskTemplate?>(null) }
    var showNew by remember { mutableStateOf(false) }
    var pendingDeleteTemplate by remember { mutableStateOf<TaskTemplate?>(null) }
    val scroll = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize()) {
        BackBar("每日任务模板", onBack)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SgCard {
                Text(
                    "模板每天自动出现在首页，当天勾选不影响其他日期；刷题结算后可自动勾选对应任务。",
                    style = SgType.bodyLong,
                    color = c.inkMuted
                )
            }
            var lastSubject = ""
            state.templates.sortedBy { it.order }.forEachIndexed { index, t ->
                val subject = state.modules.firstOrNull { it.id == t.moduleId }?.subject ?: "XINGCE"
                if (subject != lastSubject) {
                    lastSubject = subject
                    Text(
                        if (subject == "SHENLUN") "申论" else "行测",
                        style = SgType.chip,
                        color = c.accent,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                SgCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SgDot(moduleColorOf(t.moduleId, index))
                        Spacer(Modifier.width(10.dp))
                        Text(t.title, style = SgType.cardTitle, color = c.inkTitle, modifier = Modifier.weight(1f))
                        Text(
                            if (t.targetAmount > 0) "${t.targetAmount} ${t.unit}" else "不限量",
                            style = SgType.meta, color = c.inkMuted
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("每天重复", style = SgType.meta, color = c.inkMuted, modifier = Modifier.weight(1f))
                        Switch(checked = t.enabled, onCheckedChange = { on -> vm.upsertTemplate(t.copy(enabled = on)) })
                        Spacer(Modifier.width(8.dp))
                        SgSoftButton("编辑") { editing = t }
                        Spacer(Modifier.width(8.dp))
                        SgSoftButton("删除") { pendingDeleteTemplate = t }
                    }
                }
            }
            SgWideButton("＋ 新增任务模板") { showNew = true }
        }
    }

    if (showNew || editing != null) {
        TemplateDialog(
            initial = editing,
            state = state,
            onDismiss = { showNew = false; editing = null },
            onSave = {
                vm.upsertTemplate(it)
                showNew = false
                editing = null
            }
        )
    }

    pendingDeleteTemplate?.let { t ->
        SgConfirmDialog(
            title = "删除任务模板",
            message = "确定删除「${t.title}」吗？之后的每日任务里不会再出现它。",
            onConfirm = { vm.deleteTemplate(t.id) },
            onDismiss = { pendingDeleteTemplate = null }
        )
    }
}

@Composable
private fun TemplateDialog(
    initial: TaskTemplate?,
    state: PersistedState,
    onDismiss: () -> Unit,
    onSave: (TaskTemplate) -> Unit
) {
    val c = LocalSgColors.current
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var amount by remember { mutableStateOf((initial?.targetAmount ?: 30).toString()) }
    var unit by remember { mutableStateOf(initial?.unit ?: "题") }
    var moduleId by remember { mutableStateOf(initial?.moduleId ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "新增任务模板" else "编辑任务模板", style = SgType.cardTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SgTextField(
                    value = title, onValueChange = { title = it },
                    label = "任务标题，如 资料分析 · 3 组", singleLine = true
                )
                SgTextField(
                    value = amount, onValueChange = { amount = it.filter { ch -> ch.isDigit() }.take(4) },
                    label = "数量", singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("题", "篇", "道", "组", "分钟").forEach { u ->
                        SgChip(u, if (u == unit) c.accent else c.inkMuted, modifier = Modifier.clickable { unit = u })
                    }
                }
                Text("关联模块（用于分析和配色）", style = SgType.meta, color = c.inkMuted)
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SgChip("不关联", if (moduleId.isEmpty()) c.accent else c.inkMuted,
                            modifier = Modifier.clickable { moduleId = "" })
                    }
                    // 三级结构：行测 / 申论 → 大题型 → 小题型
                    listOf("XINGCE" to "行测", "SHENLUN" to "申论").forEach { (subject, label) ->
                        val parents = state.modules
                            .filter { it.subject == subject && it.parentId.isEmpty() }
                            .sortedBy { it.order }
                        if (parents.isEmpty()) return@forEach
                        Spacer(Modifier.height(8.dp))
                        Text(label, style = SgType.chip, color = c.accent)
                        parents.forEach { parent ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                SgChip(
                                    parent.name,
                                    if (moduleId == parent.id) moduleColorOf(parent.id) else c.inkMuted,
                                    modifier = Modifier.clickable { moduleId = parent.id }
                                )
                            }
                            state.modules.filter { it.parentId == parent.id }.sortedBy { it.order }.forEach { child ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(start = 14.dp, top = 3.dp)
                                ) {
                                    SgChip(
                                        "· " + child.name,
                                        if (moduleId == child.id) moduleColorOf(child.id) else c.inkMuted,
                                        modifier = Modifier.clickable { moduleId = child.id }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank()) {
                    onSave(
                        TaskTemplate(
                            id = initial?.id ?: UUID.randomUUID().toString(),
                            title = title.trim(),
                            moduleId = moduleId,
                            targetAmount = amount.toIntOrNull() ?: 0,
                            unit = unit,
                            enabled = initial?.enabled ?: true,
                            order = initial?.order ?: (state.templates.size + 1)
                        )
                    )
                }
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun KnowledgeScreenLegacy(state: PersistedState, onBack: () -> Unit) {
    val c = LocalSgColors.current
    val scroll = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize()) {
        BackBar("知识框架", onBack)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SgCard {
                Text(
                    "这是预置的安徽行测 + 申论框架骨架。第二批会支持自己增删节点、" +
                        "拖拽排序，并根据错题和刷题记录自动算掌握度。",
                    style = SgType.bodyLong,
                    color = c.inkMuted
                )
            }
            listOf("XINGCE" to "行测", "SHENLUN" to "申论").forEach { (subject, label) ->
                SgCard {
                    SgSectionHeader(label, "${state.knowledge.count { it.subject == subject }} 个节点")
                    Spacer(Modifier.height(4.dp))
                    val roots = state.knowledge.filter { it.subject == subject && it.parentId.isEmpty() }
                    roots.forEach { root ->
                        TreeNode(root, state.knowledge, 0) { depth ->
                            if (depth == 0) moduleColorOf("", 0) else c.accent
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TreeNode(
    node: KnowledgeNode,
    all: List<KnowledgeNode>,
    depth: Int,
    colorOf: (Int) -> Color
) {
    val c = LocalSgColors.current
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (depth * 16).dp, top = 7.dp, bottom = 7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(if (depth == 0) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(colorOf(depth))
            )
            Spacer(Modifier.width(9.dp))
            Text(
                node.name,
                style = if (depth == 0) SgType.cardTitle else SgType.body,
                color = if (depth == 0) c.inkTitle else c.ink
            )
            Spacer(Modifier.weight(1f))
            if (node.masteryPercent > 0) {
                Text("${node.masteryPercent}%", style = SgType.meta, color = c.inkMuted)
            }
        }
        all.filter { it.parentId == node.id }.sortedBy { it.order }.forEach { child ->
            TreeNode(child, all, depth + 1, colorOf)
        }
    }
}
