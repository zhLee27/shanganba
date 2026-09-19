package com.shanganba.examcountdown.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun SgCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = LocalSgColors.current
    val base = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(24.dp))
        .background(c.surface)
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
        .padding(16.dp)
    Column(modifier = base, verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
}

@Composable
fun SgSectionHeader(title: String, right: String? = null, rightStyle: TextStyle? = null) {
    val c = LocalSgColors.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(title, style = SgType.cardTitle, color = c.inkTitle)
        Spacer(Modifier.weight(1f))
        if (right != null) {
            Text(right, style = rightStyle ?: SgType.meta, color = c.inkMuted)
        }
    }
}

@Composable
fun SgProgress(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color? = null,
    track: Color? = null,
    height: Int = 7
) {
    val c = LocalSgColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(50))
            .background(track ?: c.surface2)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(height.dp)
                .clip(RoundedCornerShape(50))
                .background(color ?: c.accent)
        )
    }
}

@Composable
fun SgChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 11.dp, vertical = 5.dp)
    ) {
        Text(text, style = SgType.chip, color = color)
    }
}

@Composable
fun SgPrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val c = LocalSgColors.current
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(50),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = c.accent,
            contentColor = c.onAccent,
            disabledContainerColor = c.surface2,
            disabledContentColor = c.inkFaint
        )
    ) {
        Text(text, style = SgType.button)
    }
}

@Composable
fun SgWideButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val c = LocalSgColors.current
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = c.accent,
            contentColor = c.onAccent,
            disabledContainerColor = c.surface2,
            disabledContentColor = c.inkFaint
        )
    ) {
        Text(text, style = SgType.button)
    }
}

@Composable
fun SgSoftButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = LocalSgColors.current
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(50),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = c.surface2, contentColor = c.ink)
    ) {
        Text(text, style = SgType.button)
    }
}

@Composable
fun SgDot(color: Color, size: Int = 9) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun SgHeroHeader(label: String, sticker: String?) {
    val c = LocalSgColors.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = SgType.meta, color = c.heroInk.copy(alpha = 0.94f))
        Spacer(Modifier.weight(1f))
        if (sticker != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFFFFFFF).copy(alpha = 0.92f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(sticker, style = SgType.chip, color = Color(0xFFC24A20))
            }
        }
    }
}

@Composable
fun SgDivider(modifier: Modifier = Modifier) {
    val c = LocalSgColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(c.line)
    )
}

@Composable
fun SgGradientBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val c = LocalSgColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(c.heroStart, c.heroEnd)))
    ) { content() }
}

@Composable
fun SgLabelValue(label: String, value: String, valueColor: Color? = null, suffix: String = "") {
    val c = LocalSgColors.current
    Column {
        Text(label, style = SgType.meta, color = c.inkMuted)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = SgType.statValue, color = valueColor ?: c.inkTitle)
            if (suffix.isNotEmpty()) {
                Spacer(Modifier.width(2.dp))
                Text(suffix, style = SgType.meta, color = c.inkMuted)
            }
        }
    }
}

@Composable
fun SgScreenTitle(title: String, right: @Composable RowScope.() -> Unit = {}) {
    val c = LocalSgColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 10.dp)
    ) {
        Text(title, style = SgType.pageTitle, color = c.inkTitle)
        Spacer(Modifier.weight(1f))
        right()
    }
}

/**
 * 统一输入框：填色背景 + 明显边框 + 高亮标签，让人一眼看出这里可以输入。
 * 之前直接用 OutlinedTextField，边框颜色太浅，看着像普通文字。
 */
@Composable
fun SgTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    password: Boolean = false
) {
    val c = LocalSgColors.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = SgType.meta) },
        placeholder = { Text("点这里输入", style = SgType.meta, color = c.inkFaint) },
        singleLine = singleLine,
        minLines = minLines,
        textStyle = SgType.body,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = c.surface2,
            unfocusedContainerColor = c.surface2,
            focusedBorderColor = c.accent,
            unfocusedBorderColor = c.inkFaint.copy(alpha = 0.5f),
            focusedLabelColor = c.accent,
            unfocusedLabelColor = c.inkMuted,
            cursorColor = c.accent,
            focusedTextColor = c.ink,
            unfocusedTextColor = c.ink,
            focusedPlaceholderColor = c.inkFaint,
            unfocusedPlaceholderColor = c.inkFaint
        ),
        modifier = modifier.fillMaxWidth()
    )
}

/** 加减步进按钮 */
@Composable
fun SgStepButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalSgColors.current
    Box(
        modifier = modifier
            .height(42.dp)
            .width(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(c.surface2)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = SgType.body, color = c.ink)
    }
}

/** 统一样式的分段切换（行测 / 申论），胶囊底色 + 选中填充主题色 */
@Composable
fun SgTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = LocalSgColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.surface2)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tabs.forEachIndexed { i, label ->
            val on = i == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (on) c.accent else Color.Transparent)
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = SgType.button,
                    color = if (on) c.onAccent else c.inkMuted
                )
            }
        }
    }
}

/** 统一的二次确认弹窗：删除这类不可撤销的操作都走它 */
@Composable
fun SgConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "删除",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val c = LocalSgColors.current
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = SgType.cardTitle) },
        text = { Text(message, style = SgType.bodyLong, color = c.inkMuted) },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirmText, color = c.accent2)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
