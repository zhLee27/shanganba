package com.shanganba.examcountdown.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

@Serializable
data class UpdateManifest(
    val versionCode: Int = 0,
    val versionName: String = "",
    val url: String = "",
    val sha256: String = "",
    val notes: String = "",
    val date: String = "",
    val minSupportedVersionCode: Int = 1
)

object UpdateManager {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetch(url: String): Result<UpdateManifest> = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 15000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "shanganba-updater/1.0")
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            Result.success(json.decodeFromString(UpdateManifest.serializer(), text))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun download(
        context: Context,
        manifest: UpdateManifest,
        onProgress: (Int) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val fileName = manifest.url.substringAfterLast('/').ifBlank { "shanganba-update.apk" }
            val dest = File(context.cacheDir, fileName)
            val conn = (URL(manifest.url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20000
                readTimeout = 60000
                setRequestProperty("User-Agent", "shanganba-updater/1.0")
            }
            val total = conn.contentLength
            var read = 0
            conn.inputStream.use { input ->
                dest.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buffer)
                        if (n <= 0) break
                        output.write(buffer, 0, n)
                        read += n
                        if (total > 0) onProgress((read * 100 / total).coerceIn(0, 100))
                    }
                }
            }
            conn.disconnect()
            if (manifest.sha256.isNotBlank()) {
                val actual = sha256(dest)
                if (!actual.equals(manifest.sha256, ignoreCase = true)) {
                    dest.delete()
                    return@withContext Result.failure(IllegalStateException("安装包校验失败，可能没下完整"))
                }
            }
            Result.success(dest)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n <= 0) break
                digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /** 拉起系统安装器；需要用户允许过“安装未知应用” */
    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun currentVersionCode(context: Context): Int = try {
        context.packageManager.getPackageInfo(context.packageName, 0).let {
            @Suppress("DEPRECATION")
            it.versionCode
        }
    } catch (e: Exception) {
        0
    }

    fun currentVersionName(context: Context): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
    } catch (e: Exception) {
        "1.0.0"
    }
}
