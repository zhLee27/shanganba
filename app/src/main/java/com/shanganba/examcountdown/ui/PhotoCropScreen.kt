package com.shanganba.examcountdown.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private fun loadScaled(path: String, maxEdge: Int): Bitmap? = try {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    var sample = 1
    while (bounds.outWidth / sample > maxEdge || bounds.outHeight / sample > maxEdge) sample *= 2
    val decoded = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    // 依据照片的 EXIF 方向自动摆正，横拍竖拍都不会躺倒
    val orientation = try {
        ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } catch (e: Exception) {
        ExifInterface.ORIENTATION_NORMAL
    }
    val degrees = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    if (degrees == 0f) decoded else Bitmap.createBitmap(
        decoded, 0, 0, decoded.width, decoded.height,
        Matrix().apply { postRotate(degrees) }, true
    )
} catch (e: Exception) {
    null
}

/** 图片按 contain 方式摆放后的显示区域 */
private fun displayRect(bmp: Bitmap, viewport: Size): Rect {
    val fit = min(viewport.width / bmp.width, viewport.height / bmp.height)
    val w = bmp.width * fit
    val h = bmp.height * fit
    val left = (viewport.width - w) / 2f
    val top = (viewport.height - h) / 2f
    return Rect(left, top, left + w, top + h)
}

/**
 * 裁剪页：图片固定，拖动裁剪框（框内移动、四角拖动改大小），框里的内容会被保留。
 */
@Composable
fun PhotoCropScreen(sourcePath: String, onCancel: () -> Unit, onDone: (String) -> Unit) {
    val c = LocalSgColors.current
    val ctx = LocalContext.current
    var bitmap by remember(sourcePath) { mutableStateOf(loadScaled(sourcePath, 1600)) }
    val image = remember(bitmap) { bitmap?.asImageBitmap() }
    var viewport by remember { mutableStateOf(Size.Zero) }
    var frame by remember { mutableStateOf<Rect?>(null) }
    val handleSlop = 34f

    fun initFrame(vp: Size, bmp: Bitmap) {
        if (vp.width <= 0f) return
        val d = displayRect(bmp, vp)
        val w = d.width * 0.86f
        val h = d.height * 0.86f
        frame = Rect(d.left + (d.width - w) / 2f, d.top + (d.height - h) / 2f, d.left + (d.width + w) / 2f, d.top + (d.height + h) / 2f)
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        SgScreenTitle("裁剪照片", right = { SgSoftButton("返回") { onCancel() } })
        Text(
            "拖动方框选中题目区域：框内拖动位置，拖四个角改大小。",
            style = SgType.meta, color = c.inkMuted
        )
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black)
                .onSizeChanged { size ->
                    val vp = Size(size.width.toFloat(), size.height.toFloat())
                    viewport = vp
                    bitmap?.let { if (frame == null) initFrame(vp, it) }
                }
                .pointerInput(bitmap, viewport) {
                    val bmp = bitmap ?: return@pointerInput
                    detectDragGestures(
                        onDragStart = { start ->
                            val d = displayRect(bmp, viewport)
                            val f = frame ?: return@detectDragGestures
                            val nearLeft = abs(start.x - f.left) < handleSlop
                            val nearRight = abs(start.x - f.right) < handleSlop
                            val nearTop = abs(start.y - f.top) < handleSlop
                            val nearBottom = abs(start.y - f.bottom) < handleSlop
                            dragMode = when {
                                (nearLeft || nearRight) && (nearTop || nearBottom) ->
                                    (if (nearTop) "n" else "s") + (if (nearLeft) "w" else "e")
                                start.x in f.left..f.right && start.y in f.top..f.bottom -> "move"
                                else -> "move"
                            }
                            dragBounds = d
                        },
                        onDragEnd = { dragMode = "" },
                        onDrag = { change, amount ->
                            change.consume()
                            val f = frame ?: return@detectDragGestures
                            val d = dragBounds
                            val minSize = 60f
                            var l = f.left
                            var t = f.top
                            var r = f.right
                            var b = f.bottom
                            when (dragMode) {
                                "move" -> {
                                    val dx = amount.x.coerceIn(d.left - l, d.right - r)
                                    val dy = amount.y.coerceIn(d.top - t, d.bottom - b)
                                    l += dx; r += dx; t += dy; b += dy
                                }
                                else -> {
                                    if (dragMode.contains("w")) l = (l + amount.x).coerceIn(d.left, r - minSize)
                                    if (dragMode.contains("e")) r = (r + amount.x).coerceIn(l + minSize, d.right)
                                    if (dragMode.contains("n")) t = (t + amount.y).coerceIn(d.top, b - minSize)
                                    if (dragMode.contains("s")) b = (b + amount.y).coerceIn(t + minSize, d.bottom)
                                }
                            }
                            frame = Rect(l, t, r, b)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val bmp = bitmap ?: return@Canvas
                val d = displayRect(bmp, size)
                drawImage(
                    image = image ?: return@Canvas,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(bmp.width, bmp.height),
                    dstOffset = IntOffset(d.left.roundToInt(), d.top.roundToInt()),
                    dstSize = IntSize(d.width.roundToInt(), d.height.roundToInt())
                )
                val f = frame ?: return@Canvas
                // 四角手柄
                val hs = 16f
                listOf(f.topLeft, f.topRight, f.bottomLeft, f.bottomRight).forEach { p ->
                    drawCircle(Color.White, radius = hs / 2f, center = p)
                    drawCircle(c.accent, radius = hs / 2f - 3f, center = p)
                }
                drawRect(
                    color = Color.White,
                    topLeft = Offset(f.left, f.top),
                    size = Size(f.width, f.height),
                    style = Stroke(width = 4f)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            SgSoftButton("取消", modifier = Modifier.weight(1f)) { onCancel() }
            SgSoftButton("旋转", modifier = Modifier.weight(1f)) {
                val cur = bitmap ?: return@SgSoftButton
                val rotated = Bitmap.createBitmap(
                    cur, 0, 0, cur.width, cur.height,
                    Matrix().apply { postRotate(90f) }, true
                )
                cur.recycle()
                bitmap = rotated
                // 旋转后图片比例变了，重新摆一个裁剪框
                frame = null
                initFrame(viewport, rotated)
            }
            SgWideButton("用这块", modifier = Modifier.weight(1f)) {
                val bmp = bitmap ?: return@SgWideButton
                val f = frame ?: return@SgWideButton
                try {
                    val d = displayRect(bmp, viewport)
                    val sx = ((f.left - d.left) / d.width * bmp.width).toInt().coerceIn(0, bmp.width - 1)
                    val sy = ((f.top - d.top) / d.height * bmp.height).toInt().coerceIn(0, bmp.height - 1)
                    val sw = (f.width / d.width * bmp.width).toInt().coerceIn(1, bmp.width - sx)
                    val sh = (f.height / d.height * bmp.height).toInt().coerceIn(1, bmp.height - sy)
                    val cropped = Bitmap.createBitmap(bmp, sx, sy, sw, sh)
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

private var dragMode = "move"
private var dragBounds = Rect(0f, 0f, 1f, 1f)
