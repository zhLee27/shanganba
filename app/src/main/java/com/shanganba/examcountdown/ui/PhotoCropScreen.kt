package com.shanganba.examcountdown.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

private fun loadScaled(path: String, maxEdge: Int): Bitmap? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        while (bounds.outWidth / sample > maxEdge || bounds.outHeight / sample > maxEdge) sample *= 2
        BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    } catch (e: Exception) {
        null
    }
}

private fun cropOf(bitmap: Bitmap, viewport: Size, scale: Float, offset: Offset): IntArray {
    val iw = bitmap.width.toFloat()
    val ih = bitmap.height.toFloat()
    if (viewport.width <= 0f || viewport.height <= 0f) return intArrayOf(0, 0, bitmap.width, bitmap.height)
    val fit = max(viewport.width / iw, viewport.height / ih)
    val k = fit * scale
    val left = viewport.width / 2f - iw * k / 2f + offset.x
    val top = viewport.height / 2f - ih * k / 2f + offset.y
    val sw = viewport.width / k
    val sh = viewport.height / k
    val x = ((0f - left) / k).coerceIn(0f, (iw - sw).coerceAtLeast(0f))
    val y = ((0f - top) / k).coerceIn(0f, (ih - sh).coerceAtLeast(0f))
    val w = sw.toInt().coerceIn(1, bitmap.width - x.toInt())
    val h = sh.toInt().coerceIn(1, bitmap.height - y.toInt())
    return intArrayOf(x.toInt(), y.toInt(), w, h)
}

/** 拍照后裁剪：拖动移动、双指缩放，框内就是最终保留的区域 */
@Composable
fun PhotoCropScreen(sourcePath: String, onCancel: () -> Unit, onDone: (String) -> Unit) {
    val c = LocalSgColors.current
    val ctx = LocalContext.current
    val bitmap = remember(sourcePath) { loadScaled(sourcePath, 1600) }
    val image = remember(bitmap) { bitmap?.asImageBitmap() }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(Size.Zero) }

    fun clampOffset(newScale: Float, raw: Offset): Offset {
        val bmp = bitmap ?: return raw
        if (viewport.width <= 0f) return raw
        val fit = max(viewport.width / bmp.width, viewport.height / bmp.height)
        val k = fit * newScale
        val maxX = ((bmp.width * k) - viewport.width) / 2f
        val maxY = ((bmp.height * k) - viewport.height) / 2f
        return Offset(raw.x.coerceIn(-maxX, maxX), raw.y.coerceIn(-maxY, maxY))
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        SgScreenTitle("裁剪照片", right = { SgSoftButton("返回") { onCancel() } })
        Text(
            "拖动调整位置，双指缩放；方框里显示的就是保留下来的部分。",
            style = SgType.meta, color = c.inkMuted
        )
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(c.surface2)
                .onSizeChanged { viewport = Size(it.width.toFloat(), it.height.toFloat()) }
                .pointerInput(bitmap, viewport) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val ns = (scale * zoom).coerceIn(1f, 6f)
                        scale = ns
                        offset = clampOffset(ns, offset + pan)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val bmp = bitmap ?: return@Canvas
                val iw = bmp.width.toFloat()
                val ih = bmp.height.toFloat()
                val fit = max(size.width / iw, size.height / ih)
                val k = fit * scale
                val dispW = iw * k
                val dispH = ih * k
                val left = size.width / 2f - dispW / 2f + offset.x
                val top = size.height / 2f - dispH / 2f + offset.y
                drawImage(
                    image = image ?: return@Canvas,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(bmp.width, bmp.height),
                    dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                    dstSize = IntSize(dispW.roundToInt(), dispH.roundToInt())
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            SgSoftButton("取消", modifier = Modifier.weight(1f)) { onCancel() }
            SgWideButton("用这块", modifier = Modifier.weight(1f)) {
                val bmp = bitmap ?: return@SgWideButton
                try {
                    val rect = cropOf(bmp, viewport, scale, offset)
                    val cropped = Bitmap.createBitmap(bmp, rect[0], rect[1], rect[2], rect[3])
                    val dir = File(ctx.filesDir, "photos").apply { mkdirs() }
                    val out = File(dir, "crop_${System.currentTimeMillis()}.jpg")
                    out.outputStream().use { cropped.compress(Bitmap.CompressFormat.JPEG, 92, it) }
                    cropped.recycle()
                    onDone(out.absolutePath)
                } catch (e: Exception) {
                    onDone(sourcePath)
                }
            }
        }
    }
}
