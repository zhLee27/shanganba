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
import androidx.compose.ui.unit.dp
import com.shanganba.examcountdown.data.KnowledgeNode
import com.shanganba.examcountdown.data.PersistedState
import java.util.UUID

/** 可编辑的知识框架：增删节点、加子节点、按错题数显示掌握度 */
@Composable
fun KnowledgeScreen(vm: AppViewModel, state: PersistedState, onBack: () -> Unit) {
    val c = LocalSgColors.current
    var editing by remember { mutableStateOf<KnowledgeNode?>(null) }
    var parentForNew by remember { mutableStateOf("") }
    var subjectForNew by remember { mutableStateOf("XINGCE") }
    var showDialog by remember { mutableStateOf(false) }
    var collapsed by remember { mutableStateOf(setOf<String>()) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 20.dp, top = 8.dp, bottom = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) { Text("‹", style = SgType.pageTitle, color = c.inkMuted) }
            Spacer(Modifier.width(6.dp))
            Text("知识框架", style = SgType.pageTitle, color = c.inkTitle)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SgCard {
                Text(
                    "按自己的思路搭这棵树：点标题左边的圆点可以收起/展开，" +
                        "点名字改内容，点右侧 ＋ 加子节点。掌握度按关联错题的复盘情况自动算。",
                    style = SgType.bodyLong,
                    color = c.inkMuted
                )
                Spacer(Modifier.height(10.dp))
                SgWideButton("＋ 新建顶级节点") {
                    parentForNew = ""
                    subjectForNew = "XINGCE"
                    editing = null
                    showDialog = true
                }
            }

            listOf("XINGCE" to "行测", "SHENLUN" to "申论").forEach { (subject, label) ->
                val nodes = state.knowledge.filter { it.subject == subject }
                SgCard {
                    SgSectionHeader(label, "${nodes.size} 个节点")
                    Spacer(Modifier.height(4.dp))
                    nodes.filter { it.parentId.isEmpty() }.sortedBy { it.order }.forEach { root ->
                        KnowledgeBranch(
                            vm = vm,
                            state = state,
                            node = root,
                            nodes = nodes,
                            depth = 0,
                            collapsed = collapsed,
                            onToggle = { id ->
                                collapsed = if (collapsed.contains(id)) collapsed - id else collapsed + id
                            },
                            onEdit = { editing = it; showDialog = true },
                            onAddChild = { parent ->
                                parentForNew = parent.id
                                subjectForNew = parent.subject
                                editing = null
                                showDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        KnowledgeDialog(
            initial = editing,
            parentId = if (editing == null) parentForNew else editing!!.parentId,
            subject = if (editing == null) subjectForNew else editing!!.subject,
            subjects = listOf("XINGCE" to "行测", "SHENLUN" to "申论"),
            onDismiss = { showDialog = false },
            onDelete = { node ->
                vm.deleteKnowledge(node.id)
                showDialog = false
            },
            onSave = { node ->
                vm.upsertKnowledge(node)
                showDialog = false
            }
        )
    }
}

@Composable
private fun KnowledgeBranch(
    vm: AppViewModel,
    state: PersistedState,
    node: KnowledgeNode,
    nodes: List<KnowledgeNode>,
    depth: Int,
    collapsed: Set<String>,
    onToggle: (String) -> Unit,
    onEdit: (KnowledgeNode) -> Unit,
    onAddChild: (KnowledgeNode) -> Unit
) {
    val c = LocalSgColors.current
    val children = nodes.filter { it.parentId == node.id }.sortedBy { it.order }
    val isCollapsed = collapsed.contains(node.id)
    val color = if (depth == 0) moduleColorOf(node.parentId.ifBlank { "m_ziliao" }) else c.accent

    val wrongCount = state.questions.count { it.knowledgePointIds.contains(node.id) }
    val masteredCount = state.questions.count { it.knowledgePointIds.contains(node.id) && it.masteredAt > 0L }
    val mastery = if (wrongCount == 0) node.masteryPercent
    else (masteredCount * 100 / wrongCount)

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (depth * 16).dp, top = 7.dp, bottom = 7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(if (depth == 0) 9.dp else 7.dp)
                    .clip(CircleShape)
                    .background(color)
                    .clickable { if (children.isNotEmpty()) onToggle(node.id) }
            )
            Spacer(Modifier.width(9.dp))
            Text(
                node.name,
                style = if (depth == 0) SgType.cardTitle else SgType.body,
                color = if (depth == 0) c.inkTitle else c.ink,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onEdit(node) }
            )
            if (mastery > 0) {
                Text("$mastery%", style = SgType.meta, color = c.inkMuted)
                Spacer(Modifier.width(8.dp))
            }
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(c.surface2)
                    .clickable { onAddChild(node) },
                contentAlignment = Alignment.Center
            ) { Text("＋", style = SgType.meta, color = c.accent) }
        }
        if (!isCollapsed) {
            children.forEach { child ->
                KnowledgeBranch(vm, state, child, nodes, depth + 1, collapsed, onToggle, onEdit, onAddChild)
            }
        }
    }
}

@Composable
private fun KnowledgeDialog(
    initial: KnowledgeNode?,
    parentId: String,
    subject: String,
    subjects: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onSave: (KnowledgeNode) -> Unit,
    onDelete: (KnowledgeNode) -> Unit
) {
    val c = LocalSgColors.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var currentSubject by remember { mutableStateOf(if (initial == null) subject else initial.subject) }
    var currentParent by remember { mutableStateOf(if (initial == null) parentId else initial.parentId) }
    var mastery by remember { mutableStateOf((initial?.masteryPercent ?: 0).toString()) }
    var parentInput by remember { mutableStateOf(if (initial == null) parentId else initial.parentId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "新建知识点" else "编辑知识点", style = SgType.cardTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("名称，如 数量规律") }, singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    subjects.forEach { (key, label) ->
                        SgChip(
                            label,
                            if (currentSubject == key) c.accent else c.inkMuted,
                            modifier = Modifier.clickable { currentSubject = key }
                        )
                    }
                }
                OutlinedTextField(
                    value = parentInput,
                    onValueChange = { parentInput = it },
                    label = { Text("上级节点 id（留空为顶级）") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = mastery,
                    onValueChange = { mastery = it.filter { ch -> ch.isDigit() }.take(3) },
                    label = { Text("手动掌握度 %（没有错题关联时用）") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    onSave(
                        KnowledgeNode(
                            id = initial?.id ?: "k_" + UUID.randomUUID().toString().take(6),
                            parentId = parentInput.trim(),
                            subject = currentSubject,
                            name = name.trim(),
                            order = initial?.order ?: 0,
                            masteryPercent = mastery.toIntOrNull() ?: 0
                        )
                    )
                }
            }) { Text("保存") }
        },
        dismissButton = {
            Row {
                if (initial != null) {
                    TextButton(onClick = { onDelete(initial) }) { Text("删除") }
                }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        }
    )
}
