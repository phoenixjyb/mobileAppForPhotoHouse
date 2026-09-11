package dev.photohouse.connected

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.photohouse.connected.core.HttpsPhotoHouseApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import kotlin.math.min

internal data class DecodedPhoto(val bitmap: Bitmap, val downsampled: Boolean)
private data class DecodeResult(val complete: Boolean = false, val photo: DecodedPhoto? = null)
private val decodePermit = Semaphore(1)

/** Header-first decoding bounds the displayed bitmap to four million pixels.
 * No URI, file, EXIF location, thumbnail fallback, or persistent image cache.
 */
internal fun decodeOriginalPhoto(bytes: ByteArray): DecodedPhoto? {
    if (bytes.isEmpty() || bytes.size > HttpsPhotoHouseApi.ORIGINAL_LIMIT) return null
    return try {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        val width = options.outWidth; val height = options.outHeight
        if (width !in 1..32768 || height !in 1..32768) return null
        var sample = 1
        while (((width.toLong() + sample - 1) / sample) * ((height.toLong() + sample - 1) / sample) > 4_000_000) sample *= 2
        options.inJustDecodeBounds = false
        options.inSampleSize = sample
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
        if (decoded.width.toLong() * decoded.height > 4_000_000) { decoded.recycle(); return null }
        val orientation = runCatching {
            ExifInterface(ByteArrayInputStream(bytes)).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        DecodedPhoto(orientPhoto(decoded, orientation), sample > 1)
    } catch (_: IllegalArgumentException) { null }
      catch (_: OutOfMemoryError) { null }
}

internal fun orientPhoto(bitmap: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.setRotate(90f); matrix.postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
        ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.setRotate(-90f); matrix.postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
        else -> return bitmap
    }
    val oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)
    if (oriented !== bitmap) bitmap.recycle()
    return oriented
}

@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun OriginalPhotoViewer(bytes: ByteArray?, loading: Boolean, zh: Boolean, onClose: () -> Unit) {
    fun t(en: String, cn: String) = if (zh) cn else en
    val decodedState by produceState(DecodeResult(), bytes) {
        value = DecodeResult()
        if (bytes != null) value = DecodeResult(true, withContext(Dispatchers.Default) {
            decodePermit.withPermit { decodeOriginalPhoto(bytes) }
        })
    }
    val photo = decodedState.photo
    var zoom by remember(bytes) { mutableFloatStateOf(1f) }
    var offset by remember(bytes) { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    fun updateZoom(factor: Float, pan: Offset = Offset.Zero) {
        if (!factor.isFinite() || !pan.x.isFinite() || !pan.y.isFinite()) return
        zoom = (zoom * factor).coerceIn(1f, 5f)
        val bitmap = photo?.bitmap ?: return
        val fit = min(viewport.width.toFloat() / bitmap.width, viewport.height.toFloat() / bitmap.height)
        val maxX = ((bitmap.width * fit * zoom - viewport.width) / 2).coerceAtLeast(0f)
        val maxY = ((bitmap.height * fit * zoom - viewport.height) / 2).coerceAtLeast(0f)
        offset = Offset((offset.x + pan.x).coerceIn(-maxX, maxX), (offset.y + pan.y).coerceIn(-maxY, maxY))
    }
    val transform = rememberTransformableState { zoomChange, panChange, _ -> updateZoom(zoomChange, panChange) }
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp).testTag("original-viewer")) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = onClose) { Text(t("Close photo", "关闭照片")) }
            TextButton(onClick = { updateZoom(1.5f) }, enabled = photo != null && zoom < 5f) { Text(t("Zoom in", "放大")) }
            TextButton(onClick = { updateZoom(1 / 1.5f) }, enabled = photo != null && zoom > 1f) { Text(t("Zoom out", "缩小")) }
            TextButton(onClick = { zoom = 1f; offset = Offset.Zero }, enabled = photo != null) { Text(t("Fit photo", "适合屏幕")) }
        }
        if (photo != null) Text("${(zoom * 100).toInt()}%", Modifier.testTag("photo-zoom"))
        if (photo?.downsampled == true) Text(t("Large photo shown at reduced resolution.", "大图已降低显示分辨率。"))
        Box(Modifier.fillMaxWidth().weight(1f).clipToBounds().onSizeChanged {
            viewport = it; offset = Offset.Zero
        }, contentAlignment = Alignment.Center) {
            val decoded = photo
            if (loading || !decodedState.complete) CircularProgressIndicator()
            else if (decoded == null) Text(t("This image format or size cannot be displayed here.", "此处无法显示该图片格式或尺寸。"))
            if (decoded != null) Image(decoded.bitmap.asImageBitmap(),
                contentDescription = t("Original photo. Pinch or use the zoom controls.", "原始照片。可双指缩放或使用缩放按钮。"),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().testTag("original-image")
                    .graphicsLayer(scaleX = zoom, scaleY = zoom, translationX = offset.x, translationY = offset.y)
                    .transformable(transform)
                    .pointerInput(bytes) { detectTapGestures(onDoubleTap = {
                        zoom = if (zoom > 1f) 1f else 2f; offset = Offset.Zero
                    }) })
        }
    }
}
