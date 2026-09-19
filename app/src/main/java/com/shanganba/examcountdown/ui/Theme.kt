package com.shanganba.examcountdown.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.shanganba.examcountdown.R

enum class Preset { FRESH, POP, STICKER;
    companion object {
        fun of(value: String?): Preset = when (value) {
            "pop" -> POP
            "sticker" -> STICKER
            else -> FRESH
        }
    }
}

data class SgColors(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val ink: Color,
    val inkTitle: Color,
    val inkMuted: Color,
    val inkFaint: Color,
    val accent: Color,
    val accent2: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val heroInk: Color,
    val glow: Color,
    val onAccent: Color
)

val LocalSgColors = staticCompositionLocalOf { freshColors(false) }

val ModuleColors = listOf(
    Color(0xFF4C8DFF), // 言语 蓝
    Color(0xFFFF9248), // 数量 橙
    Color(0xFF8B7CF6), // 判断 紫
    Color(0xFF2ED3A0), // 资料 青
    Color(0xFFFFC53D), // 常识 黄
    Color(0xFFF2789F)  // 申论 粉
)

fun moduleColor(index: Int): Color = ModuleColors[Math.floorMod(index, ModuleColors.size)]

private val moduleIdToColor = mapOf(
    "m_zhengzhi" to ModuleColors[1],
    "m_yanyu" to ModuleColors[0],
    "m_shuliang" to ModuleColors[1],
    "m_tuxing" to ModuleColors[2],
    "m_dingyi" to ModuleColors[2],
    "m_leibi" to ModuleColors[2],
    "m_luoji" to ModuleColors[2],
    "m_panduan" to ModuleColors[2],
    "m_ziliao" to ModuleColors[3],
    "m_changshi" to ModuleColors[4],
    "m_guina" to ModuleColors[5],
    "m_zonghe" to ModuleColors[5],
    "m_duice" to ModuleColors[5],
    "m_guanche" to ModuleColors[5],
    "m_zuowen" to ModuleColors[5]
    ,
    // 第三级小题型跟随所属大题型配色
    "m_zz_dz" to ModuleColors[1], "m_zz_jh" to ModuleColors[1], "m_zz_wj" to ModuleColors[1],
    "m_zz_xf" to ModuleColors[1], "m_zz_ah" to ModuleColors[1],
    "m_cs_fl" to ModuleColors[4], "m_cs_rw" to ModuleColors[4], "m_cs_ls" to ModuleColors[4],
    "m_cs_dl" to ModuleColors[4], "m_cs_kj" to ModuleColors[4], "m_cs_jj" to ModuleColors[4],
    "m_yy_ljtk" to ModuleColors[0], "m_yy_pdyd" to ModuleColors[0], "m_yy_yjbd" to ModuleColors[0],
    "m_sl_yunsuan" to ModuleColors[1],
    "m_zl_zzl" to ModuleColors[3], "m_zl_bz" to ModuleColors[3],
    "m_zl_pjs" to ModuleColors[3], "m_zl_bs" to ModuleColors[3],
    "m_gn_wt" to ModuleColors[5], "m_gn_cx" to ModuleColors[5],
    "m_gc_gkx" to ModuleColors[5], "m_gc_dp" to ModuleColors[5],
    "m_gc_dybg" to ModuleColors[5], "m_gc_gzjb" to ModuleColors[5]
)

fun moduleColorOf(moduleId: String, fallbackIndex: Int = 0): Color =
    moduleIdToColor[moduleId]
        ?: listOf(
            "m_zz" to 1, "m_cs" to 4, "m_yy" to 0, "m_sl" to 1, "m_tx" to 2,
            "m_dy" to 2, "m_lb" to 2, "m_lj" to 2, "m_zl" to 3, "m_gn" to 5, "m_gc" to 5
        ).firstOrNull { moduleId.startsWith(it.first) }?.let { moduleColor(it.second) }
        ?: moduleColor(fallbackIndex)

fun freshColors(dark: Boolean): SgColors = if (dark) SgColors(
    bg = Color(0xFF0A1511), surface = Color(0xFF131C18), surface2 = Color(0xFF1B2620),
    line = Color(0xFF243029), ink = Color(0xFFE8F2EC), inkTitle = Color(0xFFEFF6F1),
    inkMuted = Color(0xFF9DB3A6), inkFaint = Color(0xFF7E9187),
    accent = Color(0xFF2ED3A0), accent2 = Color(0xFF3AB8E8),
    heroStart = Color(0xFF2AB98C), heroEnd = Color(0xFF2E9ECB), heroInk = Color(0xFFF2FFFA),
    glow = Color(0x662ED3A0), onAccent = Color(0xFF06251A)
) else SgColors(
    bg = Color(0xFFF3FBF7), surface = Color(0xFFFFFFFF), surface2 = Color(0xFFE9F6EF),
    line = Color(0xFFDCEDE4), ink = Color(0xFF333B36), inkTitle = Color(0xFF2C332F),
    inkMuted = Color(0xFF7C8A82), inkFaint = Color(0xFF9BA8A0),
    accent = Color(0xFF2ED3A0), accent2 = Color(0xFF3AB8E8),
    heroStart = Color(0xFF34D8A4), heroEnd = Color(0xFF35B7E8), heroInk = Color(0xFFF4FFF9),
    glow = Color(0x572ED3A0), onAccent = Color(0xFFFFFFFF)
)

fun popColors(dark: Boolean): SgColors = if (dark) SgColors(
    bg = Color(0xFF141210), surface = Color(0xFF1B1815), surface2 = Color(0xFF241F19),
    line = Color(0xFF2E2822), ink = Color(0xFFF5EDE2), inkTitle = Color(0xFFFBF4EA),
    inkMuted = Color(0xFFB9A98F), inkFaint = Color(0xFF9C8C74),
    accent = Color(0xFFFF8A5B), accent2 = Color(0xFFFFC53D),
    heroStart = Color(0xFFE4794C), heroEnd = Color(0xFFE0A63A), heroInk = Color(0xFF2A1508),
    glow = Color(0x59FF7A59), onAccent = Color(0xFF3A1A0A)
) else SgColors(
    bg = Color(0xFFFFFCF5), surface = Color(0xFFFFFFFF), surface2 = Color(0xFFFFF3E2),
    line = Color(0xFFF2E4D2), ink = Color(0xFF3A3028), inkTitle = Color(0xFF2A2118),
    inkMuted = Color(0xFF8A7A66), inkFaint = Color(0xFFA99681),
    accent = Color(0xFFFF7A59), accent2 = Color(0xFFFFC53D),
    heroStart = Color(0xFFFF8A5B), heroEnd = Color(0xFFFFC53D), heroInk = Color(0xFF43210E),
    glow = Color(0x5CFF7A59), onAccent = Color(0xFFFFFFFF)
)

fun stickerColors(dark: Boolean): SgColors = if (dark) SgColors(
    bg = Color(0xFF151110), surface = Color(0xFF1C1815), surface2 = Color(0xFF241E1A),
    line = Color(0xFF3A2F26), ink = Color(0xFFF3EAE2), inkTitle = Color(0xFFFBF3EA),
    inkMuted = Color(0xFFB3A196), inkFaint = Color(0xFF9A887C),
    accent = Color(0xFF5FBF7F), accent2 = Color(0xFFF2994A),
    heroStart = Color(0xFF3E7A5B), heroEnd = Color(0xFF2F6C7A), heroInk = Color(0xFFEAF6EE),
    glow = Color(0x4D5FBF7F), onAccent = Color(0xFF10301F)
) else SgColors(
    bg = Color(0xFFFFF7EF), surface = Color(0xFFFFFFFF), surface2 = Color(0xFFFDF0E2),
    line = Color(0xFFE7CDB2), ink = Color(0xFF3B302A), inkTitle = Color(0xFF2C2320),
    inkMuted = Color(0xFF8C7A6E), inkFaint = Color(0xFFA8978B),
    accent = Color(0xFF5FBF7F), accent2 = Color(0xFFF2994A),
    heroStart = Color(0xFF7FD1A0), heroEnd = Color(0xFF4FB3D9), heroInk = Color(0xFF123024),
    glow = Color(0x4D5FBF7F), onAccent = Color(0xFFFFFFFF)
)

fun colorsFor(preset: Preset, dark: Boolean): SgColors = when (preset) {
    Preset.FRESH -> freshColors(dark)
    Preset.POP -> popColors(dark)
    Preset.STICKER -> stickerColors(dark)
}

val DisplayFont = FontFamily(
    Font(R.font.zcool_kuaile, FontWeight.Normal),
    Font(R.font.zcool_kuaile, FontWeight.Medium),
    Font(R.font.zcool_kuaile, FontWeight.Bold)
)

/** 按位置分级的排版：标题/数字用内置圆体，正文用系统字体保证可读性。 */
object SgType {
    // 注意：这里所有 letterSpacing 必须用 sp，不能用 em。
    // Material3 的输入框在聚焦时会对自己和调用方的 TextStyle 做 lerp 插值，
    // 一旦遇到 em 与 sp 混用会抛 "Cannot perform operation for Em and Sp" 直接闪退。
    val hero = TextStyle(
        fontFamily = DisplayFont, fontSize = 76.sp, fontWeight = FontWeight.Medium,
        letterSpacing = (-2.43).sp, lineHeight = 74.sp
    )
    val pageTitle = TextStyle(
        fontFamily = DisplayFont, fontSize = 18.sp, fontWeight = FontWeight.Medium,
        letterSpacing = 0.22.sp
    )
    val cardTitle = TextStyle(
        fontFamily = DisplayFont, fontSize = 15.sp, fontWeight = FontWeight.Medium,
        letterSpacing = 0.15.sp
    )
    val button = TextStyle(
        fontFamily = DisplayFont, fontSize = 15.5.sp, fontWeight = FontWeight.Medium,
        letterSpacing = 0.31.sp
    )
    val chip = TextStyle(
        fontFamily = DisplayFont, fontSize = 12.sp, fontWeight = FontWeight.Medium,
        letterSpacing = 0.24.sp
    )
    val statValue = TextStyle(
        fontFamily = DisplayFont, fontSize = 18.sp, fontWeight = FontWeight.Medium
    )
    val bigStat = TextStyle(
        fontFamily = DisplayFont, fontSize = 30.sp, fontWeight = FontWeight.Medium,
        letterSpacing = (-0.3).sp
    )
    val heroNumber = TextStyle(
        fontFamily = DisplayFont, fontSize = 44.sp, fontWeight = FontWeight.Medium,
        letterSpacing = (-0.88).sp
    )
    val body = TextStyle(
        fontSize = 14.5.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.06.sp,
        lineHeight = 23.5.sp
    )
    val bodyLong = TextStyle(
        fontSize = 13.5.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.05.sp,
        lineHeight = 22.sp
    )
    val caption = TextStyle(fontSize = 12.5.sp, lineHeight = 18.sp)
    val meta = TextStyle(fontSize = 12.sp, lineHeight = 17.sp)
}

@Composable
fun SgTheme(preset: Preset, darkMode: String, content: @Composable () -> Unit) {
    val dark = when (darkMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val c = colorsFor(preset, dark)
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.accent, onPrimary = c.onAccent,
            secondary = c.accent2, background = c.bg, onBackground = c.ink,
            surface = c.surface, onSurface = c.ink, surfaceVariant = c.surface2,
            outline = c.line
        )
    } else {
        lightColorScheme(
            primary = c.accent, onPrimary = c.onAccent,
            secondary = c.accent2, background = c.bg, onBackground = c.ink,
            surface = c.surface, onSurface = c.ink, surfaceVariant = c.surface2,
            outline = c.line
        )
    }
    val typography = Typography(
        bodyLarge = SgType.body,
        bodyMedium = SgType.bodyLong,
        titleLarge = SgType.pageTitle,
        titleMedium = SgType.cardTitle,
        labelMedium = SgType.meta
    )
    CompositionLocalProvider(LocalSgColors provides c) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}
