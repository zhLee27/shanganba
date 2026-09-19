package com.shanganba.examcountdown.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.shanganba.examcountdown.data.PersistedState
import com.shanganba.examcountdown.data.QuestionRecord
import com.shanganba.examcountdown.util.Dates
import java.io.File
import java.util.UUID

private val ERROR_CAUSES = listOf("计算失误", "审题不清", "规律没看出来", "公式记错", "要点遗漏", "时间不够", "蒙的")

private fun newPhotoFile(ctx: android.content.Context): File {
    val dir = File(ctx.filesDir, "photos").apply { mkdirs() }
    return File(dir, "q_${System.currentTimeMillis()}.jpg")
}

@Composable
fun WrongBookTab(
    vm: AppViewModel,
    state: PersistedState,
    onOpenDetail: (String) -> Unit
) {
    val c = LocalSgColors.current
    val ctx = LocalContext.current
    var pendingPhoto by remember { mutableStateOf<File?>(null) }
    var cropTarget by remember { mutableStateOf<String?>(null) }
    var showManual by remember { mutableStateOf(false) }
    var filterPending by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val file = pendingPhoto
        if (ok && file != null) {
            cropTarget = file.absolutePath
        }
    }
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                val dest = newPhotoFile(ctx)
                ctx.contentResolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                }
                cropTarget = dest.absolutePath
            } catch (_: Exception) {
            }
        }
    }

    // 裁剪界面：整屏接管，裁完再入库
    val cropping = cropTarget
    if (cropping != null) {
        PhotoCropScreen(
            sourcePath = cropping,
            onCancel = { cropTarget = null },
            onDone = { cropped ->
                vm.upsertQuestion(
                    QuestionRecord(
                        id = UUID.randomUUID().toString(),
                        moduleId = state.modules.firstOrNull()?.id ?: "",
                        source = "CAMERA",
                        stemImagePath = cropped
                    )
                )
                cropTarget = null
            }
        )
        return
    }

    val all = state.questions.sortedByDescending { it.createdAt }
        .filter { !filterPending || (it.masteredAt == 0L) }
    val pending = state.questions.count { it.masteredAt == 0L }
    val mastered = state.questions.count { it.masteredAt > 0L }
    val rate = if (state.questions.isEmpty()) 0 else mastered * 100 / state.questions.size

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
                SgLabelValue("待复盘", "$pending", suffix = "道")
                Spacer(Modifier.width(20.dp))
                SgLabelValue("已掌握", "$mastered", suffix = "道")
                Spacer(Modifier.width(20.dp))
                SgLabelValue("掌握率", "$rate", suffix = "%")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SgSoftButton("📷 拍照录错题", modifier = Modifier.weight(1f)) {
                val file = newPhotoFile(ctx)
                pendingPhoto = file
                val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
                cameraLauncher.launch(uri)
            }
            SgSoftButton("📁 选文件", modifier = Modifier.weight(1f)) {
                fileLauncher.launch(arrayOf("image/*", "application/pdf", "*/*"))
            }
        }
        SgWideButton("✍️ 手打题目") { showManual = true }

        SgCard {
            SgSectionHeader("错题列表", if (filterPending) "只看待复盘" else "全部 ${all.size} 道")
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SgChip("全部", if (!filterPending) c.accent else c.inkMuted, modifier = Modifier.clickable { filterPending = false })
                SgChip("只看待复盘", if (filterPending) c.accent else c.inkMuted, modifier = Modifier.clickable { filterPending = true })
            }
            Spacer(Modifier.height(6.dp))
            if (all.isEmpty()) {
                Text("还没有错题，刷完卷子把错的拍进来吧。", style = SgType.bodyLong, color = c.inkMuted)
            }
            listOf(
                "行测错题" to all.filter { q ->
                    (state.modules.firstOrNull { it.id == q.moduleId }?.subject ?: "XINGCE") != "SHENLUN"
                },
                "申论错题" to all.filter { q ->
                    state.modules.firstOrNull { it.id == q.moduleId }?.subject == "SHENLUN"
                }
            ).forEach { (groupLabel, group) ->
                if (group.isEmpty()) return@forEach
                Spacer(Modifier.height(10.dp))
                Text("$groupLabel · ${group.size} 道", style = SgType.cardTitle, color = c.inkTitle)
                group.take(60).forEach { q ->
                val name = state.modules.firstOrNull { it.id == q.moduleId }?.name ?: "未分类"
                val color = moduleColorOf(q.moduleId)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenDetail(q.id) }
                        .padding(vertical = 9.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(c.surface2),
                        contentAlignment = Alignment.Center
                    ) {
                        if (q.stemImagePath.isNotBlank() && File(q.stemImagePath).exists()) {
                            AsyncImage(
                                model = File(q.stemImagePath),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(if (q.source == "MANUAL") "✍️" else "📷", style = SgType.cardTitle)
                        }
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            q.stemText.ifBlank { name }.take(24),
                            style = SgType.body,
                            color = c.ink,
                            maxLines = 1
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SgChip(name, color)
                            if (q.errorCause.isNotBlank()) SgChip(q.errorCause, ModuleColors[1])
                            SgChip(if (q.masteredAt > 0L) "已掌握" else "待复盘",
                                if (q.masteredAt > 0L) ModuleColors[3] else ModuleColors[1])
                        }
                    }
                }
                }
            }
        }
    }

    if (showManual) {
        ManualEntryDialog(
            state = state,
            onDismiss = { showManual = false },
            onSave = { vm.upsertQuestion(it); showManual = false }
        )
    }
}

@Composable
private fun ManualEntryDialog(
    state: PersistedState,
    onDismiss: () -> Unit,
    onSave: (QuestionRecord) -> Unit
) {
    val c = LocalSgColors.current
    var moduleId by remember { mutableStateOf(state.modules.firstOrNull()?.id ?: "") }
    var stem by remember { mutableStateOf("") }
    var options by remember { mutableStateOf(mutableStateListOf("", "", "", "")) }
    var myAnswer by remember { mutableStateOf("") }
    var correct by remember { mutableStateOf("") }
    var cause by remember { mutableStateOf("") }
    var tip by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("手打错题", style = SgType.cardTitle) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("所属模块", style = SgType.meta, color = c.inkMuted)
                Column {
                    state.modules.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 3.dp)) {
                            pair.forEach { m ->
                                SgChip(
                                    m.name.take(8),
                                    if (m.id == moduleId) moduleColorOf(m.id) else c.inkMuted,
                                    modifier = Modifier.clickable { moduleId = m.id }
                                )
                            }
                        }
                    }
                }
                SgTextField(
                    value = stem, onValueChange = { stem = it },
                    label = "题干", minLines = 3, modifier = Modifier.fillMaxWidth()
                )
                Text("选项（可留空）", style = SgType.meta, color = c.inkMuted)
                options.forEachIndexed { index, value ->
                    SgTextField(
                        value = value,
                        onValueChange = { options[index] = it },
                        label = listOf("A", "B", "C", "D")[index]
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SgTextField(value = myAnswer, onValueChange = { myAnswer = it }, label = "我的答案", singleLine = true, modifier = Modifier.weight(1f))
                    SgTextField(value = correct, onValueChange = { correct = it }, label = "正确答案", singleLine = true, modifier = Modifier.weight(1f))
                }
                Text("错因", style = SgType.meta, color = c.inkMuted)
                Column {
                    ERROR_CAUSES.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 3.dp)) {
                            row.forEach { e ->
                                SgChip(e, if (e == cause) ModuleColors[1] else c.inkMuted, modifier = Modifier.clickable { cause = e })
                            }
                        }
                    }
                }
                SgTextField(
                    value = tip, onValueChange = { tip = it },
                    label = "解题技巧 / 知识点总结", minLines = 2, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (stem.isNotBlank() || options.any { it.isNotBlank() }) {
                    onSave(
                        QuestionRecord(
                            id = UUID.randomUUID().toString(),
                            moduleId = moduleId,
                            source = "MANUAL",
                            stemText = stem.trim(),
                            options = options.filter { it.isNotBlank() },
                            myAnswer = myAnswer.trim(),
                            correctAnswer = correct.trim(),
                            errorCause = cause,
                            tipText = tip.trim()
                        )
                    )
                }
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun QuestionDetailScreen(
    vm: AppViewModel,
    state: PersistedState,
    questionId: String,
    onBack: () -> Unit
) {
    val c = LocalSgColors.current
    val q = state.questions.firstOrNull { it.id == questionId } ?: return
    val module = state.modules.firstOrNull { it.id == q.moduleId }
    val color = moduleColorOf(q.moduleId)
    var cause by remember(q.id) { mutableStateOf(q.errorCause) }
    var tip by remember(q.id) { mutableStateOf(q.tipText) }
    var stem by remember(q.id) { mutableStateOf(q.stemText) }
    var correct by remember(q.id) { mutableStateOf(q.correctAnswer) }
    var myAnswer by remember(q.id) { mutableStateOf(q.myAnswer) }

    Column(modifier = Modifier.fillMaxSize()) {
        SgScreenTitle("错题复盘", right = { SgSoftButton("返回") { onBack() } })
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (q.stemImagePath.isNotBlank() && File(q.stemImagePath).exists()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(c.surface2)
                ) {
                    AsyncImage(
                        model = File(q.stemImagePath),
                        contentDescription = "题目照片",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            SgCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SgChip(module?.name ?: "未分类", color)
                    Spacer(Modifier.width(8.dp))
                    Text(Dates.formatDateTime(q.createdAt), style = SgType.meta, color = c.inkMuted)
                    Spacer(Modifier.weight(1f))
                    SgChip(if (q.masteredAt > 0L) "已掌握" else "待复盘",
                        if (q.masteredAt > 0L) ModuleColors[3] else ModuleColors[1])
                }
                Spacer(Modifier.height(8.dp))
                Text("题干", style = SgType.cardTitle, color = c.inkTitle)
                SgTextField(
                    value = stem,
                    onValueChange = { stem = it },
                    label = "题干（可编辑）",
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            SgCard {
                SgSectionHeader("答案对比", "复盘 ${q.reviewCount} 次")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SgTextField(value = myAnswer, onValueChange = { myAnswer = it }, label = "我的答案", singleLine = true, modifier = Modifier.weight(1f))
                    SgTextField(value = correct, onValueChange = { correct = it }, label = "正确答案", singleLine = true, modifier = Modifier.weight(1f))
                }
            }
            SgCard {
                SgSectionHeader("错因", "点一下选")
                Spacer(Modifier.height(8.dp))
                Column {
                    ERROR_CAUSES.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 3.dp)) {
                            row.forEach { e ->
                                SgChip(e, if (e == cause) ModuleColors[1] else c.inkMuted, modifier = Modifier.clickable { cause = e })
                            }
                        }
                    }
                }
            }
            SgCard {
                SgSectionHeader("解题技巧总结", "我写的")
                Spacer(Modifier.height(8.dp))
                SgTextField(
                    value = tip,
                    onValueChange = { tip = it },
                    label = "下次遇到同类题怎么做",
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            SgWideButton("保存复盘") {
                vm.upsertQuestion(
                    q.copy(
                        stemText = stem.trim(),
                        myAnswer = myAnswer.trim(),
                        correctAnswer = correct.trim(),
                        errorCause = cause,
                        tipText = tip.trim(),
                        reviewCount = q.reviewCount + 1
                    )
                )
                onBack()
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SgSoftButton(
                    if (q.masteredAt > 0L) "取消掌握标记" else "标记已掌握",
                    modifier = Modifier.weight(1f)
                ) {
                    vm.markQuestionMastered(q.id, q.masteredAt == 0L)
                    onBack()
                }
                SgSoftButton("删除", modifier = Modifier.weight(1f)) {
                    vm.deleteQuestion(q.id)
                    onBack()
                }
            }
        }
    }
}
