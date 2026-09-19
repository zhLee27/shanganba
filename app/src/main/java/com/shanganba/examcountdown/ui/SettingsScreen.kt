package com.shanganba.examcountdown.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.shanganba.examcountdown.data.PersistedState
import com.shanganba.examcountdown.update.UpdateManager
import com.shanganba.examcountdown.util.CrashLogger
import androidx.compose.foundation.layout.heightIn
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun SettingsScreen(
    vm: AppViewModel,
    state: PersistedState,
    onOpenTasks: () -> Unit,
    onOpenKnowledge: () -> Unit,
    onOpenLogin: () -> Unit,
    onOpenEditProfile: () -> Unit,
    onLogout: () -> Unit
) {
    val c = LocalSgColors.current
    val ctx = LocalContext.current
    val scroll = rememberScrollState()
    val s = state.settings
    val scope = rememberCoroutineScope()
    var updateStatus by remember { mutableStateOf("") }

    var editStart by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf(false) }
    var editDailyTime by remember { mutableStateOf(false) }
    var editNodeTime by remember { mutableStateOf(false) }
    var editUrl by remember { mutableStateOf(false) }
    var showCrash by remember { mutableStateOf(false) }
    var editingDay by remember { mutableStateOf<Int?>(null) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var reminderPick by remember { mutableStateOf<Int?>(null) }
    var crashText by remember { mutableStateOf(CrashLogger.read(ctx)) }
    var versionStatus by remember { mutableStateOf("检查中…") }

    LaunchedEffect(s.updateUrl, s.autoCheckUpdate) {
        val url = s.effectiveUpdateUrl()
        versionStatus = "检查中…"
        UpdateManager.fetch(url)
            .onSuccess { m ->
                val cur = UpdateManager.currentVersionCode(ctx)
                versionStatus = if (m.versionCode > cur) "有新版本 ${m.versionName}" else "已是最新版"
            }
            .onFailure { versionStatus = "检查失败（网络不通）" }
    }

    val crashExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            ctx.contentResolver.openOutputStream(uri)?.use { os ->
                os.write((crashText ?: "没有崩溃日志").toByteArray(Charsets.UTF_8))
            }
            toast(ctx, "崩溃日志已导出")
        } catch (e: Exception) {
            toast(ctx, "导出失败：${e.message}")
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            ctx.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(vm.exportText().toByteArray(Charsets.UTF_8))
            }
            toast(ctx, "已导出数据")
        } catch (e: Exception) {
            toast(ctx, "导出失败：${e.message}")
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val text = ctx.contentResolver.openInputStream(uri)?.use { ins ->
                ins.readBytes().toString(Charsets.UTF_8)
            } ?: ""
            vm.importText(text) { result ->
                toast(ctx, if (result.isSuccess) "导入成功" else "导入失败：文件格式不对")
            }
        } catch (e: Exception) {
            toast(ctx, "导入失败：${e.message}")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scroll)
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(2.dp))

        ProfileCard(
            state = state,
            onLogin = onOpenLogin,
            onEdit = onOpenEditProfile,
            onLogout = onLogout
        )

        SgCard {
            SgSectionHeader("外观")
            Text("主题配色", style = SgType.meta, color = c.inkMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("fresh" to "清新渐变", "pop" to "活力撞色", "sticker" to "贴纸手帐").forEach { (key, label) ->
                    SgChip(
                        label,
                        if (s.theme == key) c.accent else c.inkMuted,
                        modifier = Modifier.clickable { vm.updateSettings { it.copy(theme = key) } }
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("深浅模式", style = SgType.meta, color = c.inkMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("system" to "跟随系统", "light" to "浅色", "dark" to "深色").forEach { (key, label) ->
                    SgChip(
                        label,
                        if (s.darkMode == key) c.accent else c.inkMuted,
                        modifier = Modifier.clickable { vm.updateSettings { it.copy(darkMode = key) } }
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("鼓励语气", style = SgType.meta, color = c.inkMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("hard" to "硬核", "soft" to "温柔", "fun" to "轻松").forEach { (key, label) ->
                    SgChip(
                        label,
                        if (s.tone == key) c.accent else c.inkMuted,
                        modifier = Modifier.clickable { vm.updateSettings { it.copy(tone = key) } }
                    )
                }
            }
        }

        SgCard {
            SgSectionHeader("备考设置")
            SettingRow("备考起跑日", s.startDate.ifBlank { "首次启动日" }) { editStart = true }
            SgDivider()
            SettingRow(
                "目标分（笔试总分）",
                "${trimDouble(s.targetXingce + s.targetShenlun)} 分（行测 ${trimDouble(s.targetXingce)} + 申论 ${trimDouble(s.targetShenlun)}）"
            ) { editTarget = true }
            SgDivider()
            SettingRow("每日任务模板", "${state.templates.count { it.enabled }} 条启用") { onOpenTasks() }
            SgDivider()
            SettingRow("知识框架", "${state.knowledge.size} 个节点") { onOpenKnowledge() }
            SgDivider()
            SettingRow("考试节点", "${state.nodes.size} 个") { }
        }

        SgCard {
            SgSectionHeader("提醒")
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("每日计划提醒", style = SgType.body, color = c.ink)
                    Text(
                        "当天任务没全部完成才提醒 · 已选 ${s.dailyReminderDays.size} 天",
                        style = SgType.meta, color = c.inkMuted
                    )
                    Spacer(Modifier.height(6.dp))
                    SgSoftButton("设置提醒时间") { showReminderDialog = true }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        listOf("一", "二", "三", "四", "五", "六", "日").forEachIndexed { i, label ->
                            val day = i + 1
                            val on = s.dailyReminderDays.contains(day)
                            SgChip(
                                label,
                                if (on) c.accent else c.inkFaint,
                                modifier = Modifier.clickable {
                                    val next = if (on) s.dailyReminderDays - day else s.dailyReminderDays + day
                                    vm.updateSettings { it.copy(dailyReminderDays = next.sorted()) }
                                }
                            )
                        }
                    }
                    // 每个选中的星期可以单独设时间
                    if (s.dailyReminderOn && s.dailyReminderDays.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        s.dailyReminderDays.sorted().forEach { day ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "周" + listOf("一", "二", "三", "四", "五", "六", "日")[day - 1],
                                    style = SgType.meta,
                                    color = c.inkMuted,
                                    modifier = Modifier.weight(1f)
                                )
                                SgChip(
                                    formatMinute(s.dailyReminderTimes[day] ?: s.dailyReminderMinute),
                                    if (s.dailyReminderTimes.containsKey(day)) c.accent else c.inkMuted,
                                    modifier = Modifier.clickable { editingDay = day }
                                )
                            }
                            Spacer(Modifier.height(3.dp))
                        }
                        Text(
                            "点某天的具体时间可以单独调整（灰色表示沿用统一时间）",
                            style = SgType.meta,
                            color = c.inkFaint
                        )
                    }
                }
                SgSoftButton("改时间") { editDailyTime = true }
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = s.dailyReminderOn,
                    onCheckedChange = { on -> vm.updateSettings { it.copy(dailyReminderOn = on) } }
                )
            }
            SgDivider()
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("考前节点提醒", style = SgType.body, color = c.ink)
                    Text(
                        "笔试前 7 / 3 / 1 天各提醒一次 · ${formatMinute(s.nodeReminderMinute)}",
                        style = SgType.meta, color = c.inkMuted
                    )
                }
                SgSoftButton("改时间") { editNodeTime = true }
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = s.nodeReminderOn,
                    onCheckedChange = { on -> vm.updateSettings { it.copy(nodeReminderOn = on) } }
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "已经接入系统闹钟：每日提醒只在你当天任务没全部完成时响；" +
                    "考前 7 / 3 / 1 天各提醒一次。手机重启后会自动重排。",
                style = SgType.meta,
                color = c.inkFaint
            )
        }

        SgCard {
            SgSectionHeader("数据")
            Row(verticalAlignment = Alignment.CenterVertically) {
                SgSoftButton("导出 JSON") {
                    exportLauncher.launch("shanganba-backup-${LocalDate.now()}.json")
                }
                Spacer(Modifier.width(10.dp))
                SgSoftButton("导入 JSON") { importLauncher.launch(arrayOf("application/json", "*/*")) }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "导出文件里包含节点、任务、打卡和设置；导入会覆盖当前数据。",
                style = SgType.meta,
                color = c.inkMuted
            )
        }

        SgCard {
            SgSectionHeader("应用内更新")
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("启动时自动检查", style = SgType.body, color = c.ink)
                    Text(
                        if (s.updateUrl.isBlank()) "更新地址还没填" else s.updateUrl,
                        style = SgType.meta, color = c.inkMuted,
                        maxLines = 2
                    )
                }
                Switch(
                    checked = s.autoCheckUpdate,
                    onCheckedChange = { on -> vm.updateSettings { it.copy(autoCheckUpdate = on) } }
                )
            }
            SgDivider()
            SettingRow("更新地址（version.json 直链）", if (s.updateUrl.isBlank()) "未设置" else "已设置") { editUrl = true }
            Spacer(Modifier.height(6.dp))
            Text(
                if (s.updateUrl.isBlank()) {
                    "当前用内置地址（始终指向最新 Release）：${s.effectiveUpdateUrl()}"
                } else {
                    "当前用你自定义的地址：${s.updateUrl}"
                },
                style = SgType.meta,
                color = c.inkFaint
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SgSoftButton("检查更新") {
                    val checkUrl = s.effectiveUpdateUrl()
                    if (checkUrl.isBlank()) {
                        updateStatus = "没有可用的更新地址"
                    } else {
                        updateStatus = "正在检查…"
                        scope.launch {
                            UpdateManager.fetch(checkUrl)
                                .onSuccess { manifest ->
                                    val current = UpdateManager.currentVersionCode(ctx)
                                    if (manifest.versionCode > current) {
                                        updateStatus = "发现 ${manifest.versionName}，开始下载…"
                                        UpdateManager.download(ctx, manifest) { p ->
                                            updateStatus = "下载中 $p%"
                                        }.onSuccess { file ->
                                            val ok = file != null
                                            if (ok) {
                                                updateStatus = "下载完成，拉起安装器"
                                                UpdateManager.install(ctx, file!!)
                                            }
                                        }.onFailure {
                                            updateStatus = "下载失败：${it.message}"
                                        }
                                    } else {
                                        updateStatus = "已经是最新版 ${UpdateManager.currentVersionName(ctx)}"
                                    }
                                }
                                .onFailure {
                                    updateStatus = "检查失败：${it.message}"
                                }
                        }
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    updateStatus.ifBlank { "当前 ${UpdateManager.currentVersionName(ctx)}" },
                    style = SgType.meta,
                    color = c.inkMuted
                )
            }
        }

        SgCard {
            SgSectionHeader("关于")
            SettingRow("应用", "上岸吧 ${UpdateManager.currentVersionName(ctx)}") { }
            SgDivider()
            SettingRow("版本状态", versionStatus) { }
            SgDivider()
            SettingRow(
                "崩溃日志",
                if (crashText == null) "暂无崩溃记录" else "有 1 条，点开查看"
            ) {
                crashText = CrashLogger.read(ctx)
                showCrash = true
            }
            SgDivider()
            SettingRow("目标考试", "2027 年安徽省考 · 行测 + 申论") { }
            SgDivider()
            SettingRow("数据存储", "仅本机，可导出备份") { }
        }
    }

    if (editStart) {
        TextInputDialog(
            title = "备考起跑日",
            label = "yyyy-MM-dd",
            initial = s.startDate.ifBlank { LocalDate.now().toString() },
            onDismiss = { editStart = false },
            onSave = { value ->
                vm.updateSettings { it.copy(startDate = value) }
                editStart = false
            }
        )
    }
    if (editTarget) {
        var xc by remember { mutableStateOf(trimDouble(s.targetXingce)) }
        var sl by remember { mutableStateOf(trimDouble(s.targetShenlun)) }
        AlertDialog(
            onDismissRequest = { editTarget = false },
            title = { Text("设置目标分", style = SgType.cardTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SgTextField(
                        value = xc,
                        onValueChange = { xc = it.filter { ch -> ch.isDigit() || ch == '.' }.take(5) },
                        label = "行测目标分（满分 100）",
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    )
                    SgTextField(
                        value = sl,
                        onValueChange = { sl = it.filter { ch -> ch.isDigit() || ch == '.' }.take(5) },
                        label = "申论目标分（满分 100）",
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    )
                    val total = (xc.toDoubleOrNull() ?: 0.0) + (sl.toDoubleOrNull() ?: 0.0)
                    Text(
                        "合计目标：${trimDouble(total)} 分（笔试总分 200）",
                        style = SgType.meta,
                        color = c.accent
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val x = xc.toDoubleOrNull() ?: s.targetXingce
                    val y = sl.toDoubleOrNull() ?: s.targetShenlun
                    vm.updateSettings { it.copy(targetXingce = x, targetShenlun = y, targetScore = x + y) }
                    editTarget = false
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { editTarget = false }) { Text("取消") } }
        )
    }
    if (editDailyTime) {
        TimePickDialog(
            title = "每日提醒时间",
            initial = formatMinute(s.dailyReminderMinute),
            onDismiss = { editDailyTime = false },
            onSave = { value ->
                vm.updateSettings { it.copy(dailyReminderMinute = value, dailyReminderTimes = emptyMap()) }
                editDailyTime = false
            }
        )
    }
    editingDay?.let { day ->
        TimePickDialog(
            title = "周" + listOf("一", "二", "三", "四", "五", "六", "日")[day - 1] + " 的提醒时间",
            initial = formatMinute(s.dailyReminderTimes[day] ?: s.dailyReminderMinute),
            onDismiss = { editingDay = null },
            onSave = { value ->
                vm.updateSettings { it.copy(dailyReminderTimes = it.dailyReminderTimes + (day to value)) }
                editingDay = null
            }
        )
    }

    if (showReminderDialog) {
        var days by remember { mutableStateOf(s.dailyReminderDays.toSet()) }
        var times by remember { mutableStateOf(s.dailyReminderTimes) }
        var unified by remember { mutableIntStateOf(s.dailyReminderMinute) }
        val week = listOf("一", "二", "三", "四", "五", "六", "日")
        AlertDialog(
            onDismissRequest = { showReminderDialog = false },
            title = { Text("每日计划提醒", style = SgType.cardTitle) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("一键统一时间", style = SgType.body, color = c.ink, modifier = Modifier.weight(1f))
                        SgChip(
                            "设为 ${formatMinute(unified)}",
                            c.accent,
                            modifier = Modifier.clickable { reminderPick = 0 }
                        )
                    }
                    SgDivider()
                    (1..7).forEach { day ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "周" + week[day - 1],
                                style = SgType.body,
                                color = if (days.contains(day)) c.ink else c.inkFaint,
                                modifier = Modifier.weight(1f)
                            )
                            SgChip(
                                formatMinute(times[day] ?: unified),
                                if (times.containsKey(day)) c.accent else c.inkMuted,
                                modifier = Modifier.clickable { if (days.contains(day)) reminderPick = day }
                            )
                            Spacer(Modifier.width(8.dp))
                            Switch(
                                checked = days.contains(day),
                                onCheckedChange = { on ->
                                    days = if (on) days + day else days - day
                                    if (!on) times = times - day
                                }
                            )
                        }
                    }
                    Text(
                        "当天任务全部完成就不提醒；改统一时间会清掉单独设置。",
                        style = SgType.meta,
                        color = c.inkFaint
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.updateSettings {
                        it.copy(
                            dailyReminderDays = days.sorted(),
                            dailyReminderTimes = times,
                            dailyReminderMinute = unified
                        )
                    }
                    showReminderDialog = false
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showReminderDialog = false }) { Text("取消") } }
        )
        // 点某天 / 统一时间 → 弹出滚轮选择
        // 用独立的编辑器同步回本弹窗的临时状态
        reminderPick?.let { target ->
            TimePickDialog(
                title = if (target == 0) "统一提醒时间" else "周" + week[target - 1] + " 的提醒时间",
                initial = formatMinute(if (target == 0) unified else (times[target] ?: unified)),
                onDismiss = { reminderPick = null },
                onSave = { v ->
                    if (target == 0) {
                        unified = v
                        times = emptyMap()
                    } else {
                        times = times + (target to v)
                    }
                    reminderPick = null
                }
            )
        }
    }
    if (editNodeTime) {
        TimePickDialog(
            title = "考前提醒时间",
            initial = formatMinute(s.nodeReminderMinute),
            onDismiss = { editNodeTime = false },
            onSave = { value ->
                vm.updateSettings { it.copy(nodeReminderMinute = value) }
                editNodeTime = false
            }
        )
    }

    // 时间选择器放在文件末尾单独定义（见下方 TimePickDialog）
    if (editUrl) {
        TextInputDialog(
            title = "更新地址",
            label = "https://…/version.json",
            initial = s.updateUrl,
            onDismiss = { editUrl = false },
            onSave = { value ->
                vm.updateSettings { it.copy(updateUrl = value.trim()) }
                editUrl = false
            }
        )
    }

    if (showCrash) {
        AlertDialog(
            onDismissRequest = { showCrash = false },
            title = { Text("崩溃日志", style = SgType.cardTitle) },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        crashText ?: "还没有崩溃记录。如果 App 闪退过，重新打开后这里会显示堆栈信息。",
                        style = SgType.bodyLong,
                        color = c.ink
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { crashExportLauncher.launch("shanganba-crash.txt") }) {
                    Text("导出文件")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        CrashLogger.clear(ctx)
                        crashText = null
                        showCrash = false
                    }) { Text("清空") }
                    TextButton(onClick = { showCrash = false }) { Text("关闭") }
                }
            }
        )
    }
}

@Composable
private fun SettingRow(label: String, value: String, onClick: () -> Unit) {
    val c = LocalSgColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp)
    ) {
        Text(label, style = SgType.body, color = c.ink, modifier = Modifier.weight(1f))
        Text(value, style = SgType.meta, color = c.inkMuted, maxLines = 1)
        Spacer(Modifier.width(6.dp))
        Text("›", style = SgType.cardTitle, color = c.inkFaint)
    }
}

@Composable
private fun TextInputDialog(
    title: String,
    label: String,
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = SgType.cardTitle) },
        text = {
            SgTextField(
                value = value,
                onValueChange = { value = it },
                label = label
            )
        },
        confirmButton = { TextButton(onClick = { onSave(value.trim()) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

private fun formatMinute(minute: Int): String = "%02d:%02d".format(minute / 60, minute % 60)

private fun parseMinute(text: String): Int? {
    val parts = text.split(":")
    if (parts.size != 2) return null
    val h = parts[0].trim().toIntOrNull() ?: return null
    val m = parts[1].trim().toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return h * 60 + m
}

private fun trimDouble(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()

private fun toast(ctx: android.content.Context, msg: String) {
    Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
}

/** 提醒时间：用系统滚轮时间选择器，直接选具体几点几分 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    val parts = initial.split(":")
    val state = rememberTimePickerState(
        initialHour = (parts.getOrNull(0)?.toIntOrNull() ?: 20).coerceIn(0, 23),
        initialMinute = (parts.getOrNull(1)?.toIntOrNull() ?: 0).coerceIn(0, 59),
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = SgType.cardTitle) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onSave(state.hour * 60 + state.minute) }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
