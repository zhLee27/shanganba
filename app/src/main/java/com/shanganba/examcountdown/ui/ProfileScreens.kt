package com.shanganba.examcountdown.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.shanganba.examcountdown.data.PersistedState
import java.io.File

private fun avatarFile(ctx: android.content.Context): File {
    val dir = File(ctx.filesDir, "photos").apply { mkdirs() }
    return File(dir, "avatar_${System.currentTimeMillis()}.jpg")
}

/** 「我的」页顶部的个人信息卡 */
@Composable
fun ProfileCard(state: PersistedState, onLogin: () -> Unit, onEdit: () -> Unit, onLogout: () -> Unit) {
    val c = LocalSgColors.current
    val p = state.profile
    SgCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(c.accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                if (p.avatarPath.isNotBlank() && File(p.avatarPath).exists()) {
                    AsyncImage(
                        model = File(p.avatarPath),
                        contentDescription = "头像",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(if (p.loggedIn) "🙂" else "👤", style = SgType.bigStat)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (p.loggedIn) p.nickname else "未登录",
                    style = SgType.cardTitle,
                    color = c.inkTitle
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    if (p.loggedIn) {
                        p.signature.ifBlank { "写句签名给自己打打气 →" }
                    } else {
                        "点右边登录或注册，账号密码只存在本机"
                    },
                    style = SgType.meta,
                    color = c.inkMuted,
                    maxLines = 2
                )
            }
            Spacer(Modifier.width(8.dp))
            if (p.loggedIn) {
                SgSoftButton("编辑资料") { onEdit() }
            } else {
                SgPrimaryButton("登录 / 注册") { onLogin() }
            }
        }
        if (p.loggedIn) {
            Spacer(Modifier.height(8.dp))
            SgDivider()
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("账号：${p.account}", style = SgType.meta, color = c.inkMuted, modifier = Modifier.weight(1f))
                SgSoftButton("注销登录") { onLogout() }
            }
        }
    }
}

/** 登录 / 注册（本机账号，不联网） */
@Composable
fun LoginScreen(vm: AppViewModel, state: PersistedState, onBack: () -> Unit) {
    val c = LocalSgColors.current
    var account by remember { mutableStateOf(state.profile.account) }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val firstTime = state.profile.account.isBlank()

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        SgScreenTitle("登录 / 注册", right = { SgSoftButton("返回") { onBack() } })
        SgCard {
            Text(
                if (firstTime) "第一次使用：填好账号和密码点登录，就等于在本机注册好了。账号密码只保存在这台手机上，不会上传。"
                else "本机已注册账号「${state.profile.account}」，输入密码登录即可。忘记密码只能注销后重新注册。",
                style = SgType.bodyLong,
                color = c.inkMuted
            )
            Spacer(Modifier.height(14.dp))
            SgTextField(value = account, onValueChange = { account = it }, label = "账号（至少 2 个字符）")
            Spacer(Modifier.height(10.dp))
            SgTextField(
                value = password,
                onValueChange = { password = it },
                label = "密码（至少 4 位）",
                password = true
            )
            if (error.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(error, style = SgType.meta, color = c.accent2)
            }
            Spacer(Modifier.height(18.dp))
            SgWideButton(if (firstTime) "注册并登录" else "登录") {
                val err = vm.loginOrRegister(account, password)
                if (err == null) onBack() else error = err
            }
        }
    }
}

/** 编辑个人资料：头像、昵称、签名 */
@Composable
fun ProfileEditScreen(vm: AppViewModel, state: PersistedState, onBack: () -> Unit) {
    val c = LocalSgColors.current
    val ctx = LocalContext.current
    var nickname by remember { mutableStateOf(state.profile.nickname) }
    var signature by remember { mutableStateOf(state.profile.signature) }
    var pendingPhoto by remember { mutableStateOf<File?>(null) }
    var cropTarget by remember { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val file = pendingPhoto
        if (ok && file != null) cropTarget = file.absolutePath
    }
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            try {
                val dest = avatarFile(ctx)
                ctx.contentResolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                }
                cropTarget = dest.absolutePath
            } catch (_: Exception) {
            }
        }
    }

    val cropping = cropTarget
    if (cropping != null) {
        PhotoCropScreen(
            sourcePath = cropping,
            onCancel = { cropTarget = null },
            onDone = { cropped ->
                vm.updateProfile { it.copy(avatarPath = cropped) }
                cropTarget = null
            }
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        SgScreenTitle("编辑资料", right = { SgSoftButton("返回") { onBack() } })
        SgCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(c.accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    val path = state.profile.avatarPath
                    if (path.isNotBlank() && File(path).exists()) {
                        AsyncImage(
                            model = File(path),
                            contentDescription = "头像",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text("🙂", style = SgType.heroNumber)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SgSoftButton("拍照换头像") {
                        val file = avatarFile(ctx)
                        pendingPhoto = file
                        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
                        cameraLauncher.launch(uri)
                    }
                    SgSoftButton("从相册选") {
                        fileLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            SgTextField(value = nickname, onValueChange = { nickname = it.take(16) }, label = "昵称")
            Spacer(Modifier.height(10.dp))
            SgTextField(
                value = signature,
                onValueChange = { signature = it.take(40) },
                label = "个性签名",
                singleLine = false,
                minLines = 2
            )
            Spacer(Modifier.height(18.dp))
            SgWideButton("保存") {
                vm.updateProfile {
                    it.copy(
                        nickname = nickname.trim().ifBlank { "上岸吧用户" },
                        signature = signature.trim()
                    )
                }
                onBack()
            }
        }
    }
}
