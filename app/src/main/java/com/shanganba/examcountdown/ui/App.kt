package com.shanganba.examcountdown.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.updateAll
import com.shanganba.examcountdown.notify.Reminders
import com.shanganba.examcountdown.update.UpdateManager
import com.shanganba.examcountdown.update.UpdateManifest
import com.shanganba.examcountdown.widget.CountdownWidget
import kotlinx.coroutines.delay

private enum class Tab(val emoji: String, val label: String) {
    HOME("🏠", "首页"),
    PRACTICE("⏱", "刷题"),
    WRONG("📝", "错题"),
    ANALYSIS("🎯", "分析"),
    MINE("⚙️", "我的")
}

private enum class Overlay { NODES, TASKS, KNOWLEDGE, HISTORY, CHECKIN, QUESTION, LOGIN, PROFILE_EDIT }

/** 关掉点击时的半透明水波纹：这个 App 里所有点击反馈都靠颜色变化，不要那层灰 */
private object NoIndication : androidx.compose.foundation.IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): androidx.compose.ui.node.DelegatableNode =
        object : androidx.compose.ui.Modifier.Node() { }

    override fun equals(other: Any?): Boolean = other === this
    override fun hashCode(): Int = -1
}

@Composable
fun SgApp(vm: AppViewModel) {
    val state by vm.state.collectAsState()
    val preset = Preset.of(state.settings.theme)
    val ctx = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    SgTheme(preset, state.settings.darkMode) {
        // 注意：必须放在 SgTheme 里面，MaterialTheme 自己会提供带水波纹的 LocalIndication
        CompositionLocalProvider(LocalIndication provides NoIndication) {
        val c = LocalSgColors.current
        var tab by remember { mutableIntStateOf(0) }
        var overlay by remember { mutableStateOf<Overlay?>(null) }
        var questionId by remember { mutableStateOf("") }
        var showResult by remember { mutableStateOf(false) }
        var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
        var updateManifest by remember { mutableStateOf<UpdateManifest?>(null) }
        var confirmQuitTimer by remember { mutableStateOf(false) }
        var loginPrompt by remember { mutableStateOf(false) }
        // 刷题页的行测/申论切换放在这里，计时结束后回来仍在原来那一页
        var practiceSubject by remember { mutableStateOf("XINGCE") }
        var practiceModuleId by remember { mutableStateOf("") }
        var practiceExpanded by remember { mutableStateOf(setOf<String>()) }
        // 刷题页的滚动位置提到这里，计时结束回来还在原来的地方
        val practiceScroll = rememberScrollState()

        // 手机滑动返回：优先关弹窗 → 关子页面 → 回首页 → 才退出应用
        BackHandler {
            when {
                updateManifest != null -> updateManifest = null
                showResult -> showResult = false
                state.activeTimer != null -> confirmQuitTimer = true
                overlay != null -> overlay = null
                tab != 0 -> tab = 0
                else -> (ctx as? android.app.Activity)?.finish()
            }
        }

        if (confirmQuitTimer) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { confirmQuitTimer = false },
                title = { Text("要放弃本次计时吗？", style = SgType.cardTitle) },
                text = { Text("放弃后这次计时不会保存。", style = SgType.bodyLong, color = c.inkMuted) },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        confirmQuitTimer = false
                        showResult = false
                        vm.cancelTimer()
                    }) { Text("放弃") }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { confirmQuitTimer = false }) { Text("继续计时") }
                }
            )
        }

        LaunchedEffect(Unit) {
            while (true) {
                now = System.currentTimeMillis()
                delay((1000 - (System.currentTimeMillis() % 1000)).coerceAtLeast(50))
            }
        }

        LaunchedEffect(Unit) {
            if (Build.VERSION.SDK_INT >= 33 && !Reminders.canNotify(ctx)) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        LaunchedEffect(
            state.settings.dailyReminderOn,
            state.settings.dailyReminderMinute,
            state.settings.dailyReminderDays,
            state.settings.nodeReminderOn,
            state.settings.nodeReminderMinute,
            state.nodes
        ) {
            try {
                Reminders.rescheduleAll(ctx, state)
            } catch (_: Exception) {
            }
        }

        LaunchedEffect(state.activeTimer) {
            val timer = state.activeTimer
            try {
                if (timer != null && timer.mode == "COUNT_DOWN" && timer.endAlarmAt > 0L) {
                    val name = state.modules.firstOrNull { it.id == timer.moduleId }?.name ?: "刷题"
                    Reminders.scheduleTimerEnd(ctx, timer.endAlarmAt, name)
                } else {
                    Reminders.cancelTimerEnd(ctx)
                }
            } catch (_: Exception) {
            }
        }

        LaunchedEffect(state) {
            try {
                CountdownWidget().updateAll(ctx)
            } catch (_: Exception) {
            }
        }

        // 启动自动检测更新：发现新版就弹窗，可选择立即更新或稍后更新（稍后 = 本次不装，下次启动还会提醒）
        LaunchedEffect(state.settings.autoCheckUpdate, state.settings.updateUrl) {
            if (!state.settings.autoCheckUpdate) return@LaunchedEffect
            val url = state.settings.effectiveUpdateUrl()
            if (url.isBlank()) return@LaunchedEffect
            try {
                UpdateManager.fetch(url).onSuccess { manifest ->
                    val current = UpdateManager.currentVersionCode(ctx)
                    if (manifest.versionCode > current) {
                        updateManifest = manifest
                    }
                }
            } catch (_: Exception) {
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(c.bg)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            val activeTimer = state.activeTimer
            when {
                activeTimer != null && showResult -> {
                    PracticeResultScreen(vm = vm, state = state, onDone = { showResult = false })
                }
                activeTimer != null -> {
                    FocusTimerScreen(
                        vm = vm,
                        state = state,
                        onFinishRequest = { showResult = true },
                        onQuit = { showResult = false }
                    )
                }
                overlay != null -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        when (overlay) {
                            Overlay.NODES -> NodesScreen(vm, state, onBack = { overlay = null })
                            Overlay.TASKS -> TasksScreen(vm, state, onBack = { overlay = null })
                            Overlay.KNOWLEDGE -> KnowledgeScreen(vm, state, onBack = { overlay = null })
                            Overlay.HISTORY -> PracticeHistoryScreen(state, onBack = { overlay = null })
                            Overlay.CHECKIN -> CheckinScreen(state, onBack = { overlay = null })
                            Overlay.QUESTION -> QuestionDetailScreen(vm, state, questionId, onBack = { overlay = null })
                            Overlay.LOGIN -> LoginScreen(vm, state, onBack = { overlay = null })
                            Overlay.PROFILE_EDIT -> ProfileEditScreen(vm, state, onBack = { overlay = null })
                            null -> Unit
                        }
                    }
                }
                else -> {
                    Scaffold(
                        containerColor = c.bg,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        bottomBar = {
                            NavigationBar(containerColor = c.surface, tonalElevation = 0.dp) {
                                Tab.entries.forEachIndexed { index, item ->
                                    NavigationBarItem(
                                        selected = tab == index,
                                        onClick = { tab = index },
                                        icon = { Text(item.emoji, style = SgType.cardTitle) },
                                        label = { Text(item.label, style = SgType.meta) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = c.accent,
                                            selectedTextColor = c.accent,
                                            unselectedIconColor = c.inkFaint,
                                            unselectedTextColor = c.inkFaint,
                                            indicatorColor = c.accent.copy(alpha = 0.14f)
                                        )
                                    )
                                }
                            }
                        }
                    ) { inner ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(inner)
                        ) {
                            androidx.compose.animation.AnimatedContent(
                                targetState = tab,
                                transitionSpec = {
                                    val forward = targetState > initialState
                                    (androidx.compose.animation.slideInHorizontally { if (forward) it else -it } +
                                        androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220))) togetherWith
                                        (androidx.compose.animation.slideOutHorizontally { if (forward) -it / 3 else it / 3 } +
                                            androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(160)))
                                },
                                label = "tab"
                            ) { currentTab ->
                            when (currentTab) {
                                0 -> {
                                    SgScreenTitle("上岸吧") {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            SgSoftButton("打卡记录") { overlay = Overlay.CHECKIN }
                                            Spacer(Modifier.width(8.dp))
                                            SgSoftButton("任务模板") { overlay = Overlay.TASKS }
                                        }
                                    }
                                    HomeScreen(
                                        vm = vm,
                                        state = state,
                                        now = now,
                                        onQuick = { target ->
                                            when (target) {
                                                QuickTarget.PRACTICE -> tab = 1
                                                QuickTarget.WRONG -> tab = 2
                                                QuickTarget.ANALYSIS -> tab = 3
                                                QuickTarget.KNOWLEDGE -> overlay = Overlay.KNOWLEDGE
                                            }
                                        },
                                        onOpenNodes = { overlay = Overlay.NODES }
                                    )
                                }
                                1 -> PracticeTab(
                                    vm = vm,
                                    state = state,
                                    subject = practiceSubject,
                                    onSubjectChange = { practiceSubject = it },
                                    savedModuleId = practiceModuleId,
                                    onModuleChange = { practiceModuleId = it },
                                    savedExpanded = practiceExpanded,
                                    onExpandedChange = { practiceExpanded = it },
                                    scrollState = practiceScroll,
                                    onStart = { vm.startTimer(it); showResult = false },
                                    onOpenHistory = { overlay = Overlay.HISTORY }
                                )
                                2 -> WrongBookTab(
                                    vm = vm,
                                    state = state,
                                    onOpenDetail = { id ->
                                        questionId = id
                                        overlay = Overlay.QUESTION
                                    }
                                )
                                3 -> AnalysisTab(state)
                                else -> SettingsScreen(
                                    vm = vm,
                                    state = state,
                                    onOpenTasks = { overlay = Overlay.TASKS },
                                    onOpenKnowledge = { overlay = Overlay.KNOWLEDGE },
                                    onOpenLogin = { overlay = Overlay.LOGIN },
                                    onOpenEditProfile = { overlay = Overlay.PROFILE_EDIT },
                                    onLogout = { vm.logout() }
                                )
                            }
                            }
                        }
                    }
                }
            }
        }

        updateManifest?.let { manifest ->
            UpdateAvailableDialog(manifest) {
                updateManifest = null
            }
        }

        // 未登录：只能浏览，点任何功能都提示登录（「我的」页除外，那里有登录入口）
        val loggedIn = state.profile.loggedIn
        if (!loggedIn && overlay == null && tab != 4) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { loginPrompt = true }
            )
        }
        if (loginPrompt) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { loginPrompt = false },
                title = { Text("需要登录", style = SgType.cardTitle) },
                text = {
                    Text(
                        "登录后才能使用功能，现在只能浏览页面。要现在去登录吗？",
                        style = SgType.bodyLong,
                        color = c.inkMuted
                    )
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        loginPrompt = false
                        overlay = Overlay.LOGIN
                    }) { Text("去登录") }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { loginPrompt = false }) { Text("先看看") }
                }
            )
        }
    }
    }
}
