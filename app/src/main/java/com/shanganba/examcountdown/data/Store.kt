package com.shanganba.examcountdown.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class AppStore(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }
    private val file = File(context.filesDir, "data.json")
    private val mutex = Mutex()
    private val _state = MutableStateFlow(readFromDisk())
    val state: StateFlow<PersistedState> = _state.asStateFlow()

    fun readFromDisk(): PersistedState {
        val raw = try {
            if (file.exists()) file.readText(Charsets.UTF_8) else null
        } catch (e: Exception) {
            null
        }
        if (raw.isNullOrBlank()) return freshState()
        return try {
            migrate(json.decodeFromString(PersistedState.serializer(), raw))
        } catch (e: Exception) {
            backupBroken(raw)
            freshState()
        }
    }

    private fun freshState() = PersistedState(
        settings = Settings(firstLaunchAt = System.currentTimeMillis())
    )

    /** 1.5.0 起行测改成新题型结构（政治理论独立、判断推理拆四块），保留刷题与错题记录 */
    private fun migrate(state: PersistedState): PersistedState {
        if (state.schemaVersion >= 2) return state
        val remap = mapOf("m_panduan" to "m_tuxing", "m_duice" to "m_zonghe")
        fun fix(id: String) = remap[id] ?: id
        return state.copy(
            schemaVersion = 2,
            modules = defaultModules(),
            scoreConfigs = defaultScoreConfigs(),
            sessions = state.sessions.map { it.copy(moduleId = fix(it.moduleId)) },
            questions = state.questions.map { it.copy(moduleId = fix(it.moduleId)) },
            templates = state.templates.map { it.copy(moduleId = fix(it.moduleId)) }
        )
    }

    private fun backupBroken(raw: String) {
        try {
            File(context.filesDir, "data.broken.${System.currentTimeMillis()}.json")
                .writeText(raw, Charsets.UTF_8)
        } catch (_: Exception) {
        }
    }

    /** 写入失败不会抛出，最多是这次改动没保存，避免把整个 App 带崩 */
    suspend fun update(block: (PersistedState) -> PersistedState): Result<PersistedState> = mutex.withLock {
        val next = try {
            block(_state.value)
        } catch (e: Exception) {
            return@withLock Result.failure(e)
        }
        try {
            writeToDisk(next)
        } catch (e: Exception) {
            try {
                writeToDiskPlain(next)
            } catch (e2: Exception) {
                return@withLock Result.failure(e2)
            }
        }
        _state.value = next
        Result.success(next)
    }

    private suspend fun writeToDisk(state: PersistedState) = withContext(Dispatchers.IO) {
        val text = json.encodeToString(PersistedState.serializer(), state)
        val tmp = File(file.parentFile, "data.json.tmp")
        tmp.writeText(text, Charsets.UTF_8)
        Files.move(
            tmp.toPath(), file.toPath(),
            StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE
        )
    }

    /** 有些设备的文件系统不支持原子移动，退回普通写法 */
    private suspend fun writeToDiskPlain(state: PersistedState) = withContext(Dispatchers.IO) {
        val text = json.encodeToString(PersistedState.serializer(), state)
        val tmp = File(file.parentFile, "data.json.tmp")
        tmp.writeText(text, Charsets.UTF_8)
        Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

    fun exportText(): String = json.encodeToString(PersistedState.serializer(), _state.value)

    suspend fun importText(text: String): Result<Unit> = try {
        val parsed = json.decodeFromString(PersistedState.serializer(), text)
        update { parsed.copy(schemaVersion = 1) }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
