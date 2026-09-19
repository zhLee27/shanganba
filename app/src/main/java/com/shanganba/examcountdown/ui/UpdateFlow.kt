package com.shanganba.examcountdown.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.shanganba.examcountdown.AppConfig
import com.shanganba.examcountdown.data.Settings
import com.shanganba.examcountdown.update.UpdateManager
import com.shanganba.examcountdown.update.UpdateManifest
import kotlinx.coroutines.launch

/**
 * 设置里没填地址时用内置地址——它指向 GitHub 上最新一个 Release 的 version.json，
 * 所以以后我发新版本，你手机上的自动检测会立刻看到，不用改任何设置。
 */
fun Settings.effectiveUpdateUrl(): String =
    updateUrl.ifBlank { AppConfig.DEFAULT_UPDATE_URL }

@Composable
fun UpdateAvailableDialog(manifest: UpdateManifest, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val c = LocalSgColors.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = {
            Text(
                buildString {
                    append("发现新版本 ")
                    append(manifest.versionName.ifBlank { "未知版本" })
                    if (manifest.date.isNotBlank()) {
                        append(" · ")
                        append(manifest.date)
                    }
                },
                style = SgType.cardTitle
            )
        },
        text = {
            Column {
                Text(
                    "本次更新内容",
                    style = SgType.chip,
                    color = c.inkMuted
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    manifest.notes.ifBlank { "有新版本可以更新，建议装上。" },
                    style = SgType.bodyLong,
                    color = c.ink
                )
                if (status.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(status, style = SgType.meta, color = c.inkMuted)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "安装时系统可能提示「允许安装未知应用」，允许一次即可，数据不会丢。",
                    style = SgType.meta,
                    color = c.inkFaint
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy,
                onClick = {
                    busy = true
                    status = "正在下载…"
                    scope.launch {
                        UpdateManager.download(ctx, manifest) { p -> status = "正在下载 $p%" }
                            .onSuccess { file ->
                                status = "下载完成，正在拉起安装器"
                                try {
                                    UpdateManager.install(ctx, file)
                                } catch (e: Exception) {
                                    status = "拉起安装器失败：${e.message}"
                                }
                                busy = false
                            }
                            .onFailure {
                                status = "下载失败：${it.message}"
                                busy = false
                            }
                    }
                }
            ) { Text(if (busy) "处理中…" else "立即更新") }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = onDismiss) { Text("稍后更新") }
        }
    )
}
