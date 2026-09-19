package com.shanganba.examcountdown

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnitType
import com.shanganba.examcountdown.ui.SgType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 回归测试：字距只能用 sp。
 * Material3 输入框聚焦时会把调用方的 TextStyle 和默认样式做 lerp，
 * em 与 sp 混用会抛 "Cannot perform operation for Em and Sp" 直接闪退。
 */
class TypographyUnitTest {

    @Test
    fun all_letter_spacing_use_sp() {
        val styles = mapOf(
            "hero" to SgType.hero,
            "pageTitle" to SgType.pageTitle,
            "cardTitle" to SgType.cardTitle,
            "button" to SgType.button,
            "chip" to SgType.chip,
            "statValue" to SgType.statValue,
            "bigStat" to SgType.bigStat,
            "heroNumber" to SgType.heroNumber,
            "body" to SgType.body,
            "bodyLong" to SgType.bodyLong,
            "caption" to SgType.caption,
            "meta" to SgType.meta
        )
        styles.forEach { (name, style) ->
            assertTrue(
                "$name 的字距不能是 em（em 与 sp 混用会让输入框闪退），现在=${style.letterSpacing.type}",
                style.letterSpacing.type != TextUnitType.Em
            )
        }
    }

    @Test
    fun material_typography_mapping_is_also_sp() {
        val mapped = listOf(SgType.body, SgType.bodyLong, SgType.pageTitle, SgType.cardTitle, SgType.meta)
        mapped.forEach { style: TextStyle ->
            assertTrue(
                "映射到 Material3 的样式字距不能是 em，现在=${style.letterSpacing.type}",
                style.letterSpacing.type != TextUnitType.Em
            )
        }
    }
}
