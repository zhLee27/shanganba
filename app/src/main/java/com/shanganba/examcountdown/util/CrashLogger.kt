package com.shanganba.examcountdown.util

import android.content.Context
import android.os.Build
import com.shanganba.examcountdown.update.UpdateManager
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 崩溃自记录：闪退时把堆栈写到 filesDir/last_crash.txt，装完 App 就能从「我的」里导出 */
object CrashLogger {

    private const val FILE_NAME = "last_crash.txt"

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                write(app, throwable)
            } catch (_: Throwable) {
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun write(context: Context, throwable: Throwable) {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val header = buildString {
            append("时间：")
            append(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(Date()))
            append('\n')
            append("版本：")
            append(UpdateManager.currentVersionName(context))
            append(" (versionCode ")
            append(UpdateManager.currentVersionCode(context))
            append(")\n")
            append("系统：Android ")
            append(Build.VERSION.RELEASE)
            append(" / API ")
            append(Build.VERSION.SDK_INT)
            append('\n')
            append("机型：")
            append(Build.MANUFACTURER)
            append(' ')
            append(Build.MODEL)
            append("\n\n")
            append(sw.toString())
        }
        File(context.filesDir, FILE_NAME).writeText(header, Charsets.UTF_8)
    }

    fun read(context: Context): String? = try {
        File(context.filesDir, FILE_NAME).takeIf { it.exists() }?.readText(Charsets.UTF_8)
    } catch (_: Exception) {
        null
    }

    fun clear(context: Context) {
        try {
            File(context.filesDir, FILE_NAME).delete()
        } catch (_: Exception) {
        }
    }
}
