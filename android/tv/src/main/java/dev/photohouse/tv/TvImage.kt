package dev.photohouse.tv

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlin.math.max
import kotlin.math.min
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import dev.photohouse.home.HomeLimits
import dev.photohouse.home.HomeWire
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

internal data class DecodedPhoto(val bitmap: Bitmap, val downsampled: Boolean)

/** Header-first decoding bounds the displayed bitmap to 8,847,360 pixels.
 * No URI, file, EXIF location, thumbnail fallback, or persistent image cache.
 */
internal fun decodeTvPhoto(bytes: ByteArray, maxPixels: Int = 8_847_360): DecodedPhoto? {
    if (bytes.isEmpty() || bytes.size > HomeLimits.DISPLAY_BYTES) return null
    return try {
        val expected = HomeWire.jpegDimensions(bytes)
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        val width = options.outWidth; val height = options.outHeight
        if (width != expected.first || height != expected.second || width !in 1..4096 || height !in 1..4096 || width.toLong() * height > HomeLimits.DISPLAY_PIXELS) return null
        var sample = 1
        while (((width.toLong() + sample - 1) / sample) * ((height.toLong() + sample - 1) / sample) > maxPixels) sample *= 2
        options.inJustDecodeBounds = false
        options.inSampleSize = sample
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
        if (decoded.width.toLong() * decoded.height > maxPixels) { decoded.recycle(); return null }
        DecodedPhoto(decoded, sample > 1)
    } catch (_: Exception) { null }
      catch (_: OutOfMemoryError) { null }
}

private data class DecodeResult(val complete: Boolean = false, val photo: DecodedPhoto? = null)
private val decodePermit = Semaphore(1)
/** One decode at a time, off the UI thread. A new byte identity cannot show an old photo. */
@Composable internal fun TvImage(bytes: ByteArray?, description: String, modifier: Modifier = Modifier, missing: String, maxPixels: Int = 8_847_360, transform: PhotoTransform = PhotoTransform()) {
    key(bytes) {
        val result by produceState(DecodeResult(complete = bytes == null)) {
            if (bytes != null) value = DecodeResult(true, withContext(Dispatchers.Default) {
                decodePermit.withPermit { decodeTvPhoto(bytes, maxPixels) }
            })
        }
        var viewport by remember { mutableStateOf(IntSize.Zero) }
        Box(modifier.onSizeChanged { viewport = it }.clipToBounds(), contentAlignment = Alignment.Center) {
            val current = result.photo
            if (!result.complete) CircularProgressIndicator()
            else if (current == null) Text(missing)
            else {
                val width = current.bitmap.width.toFloat(); val height = current.bitmap.height.toFloat()
                val fit = min(viewport.width / width, viewport.height / height)
                val fill = max(viewport.width / width, viewport.height / height)
                val scale = (if (transform.fill) fill else fit) * transform.zoom
                val x = max(0f, (width * scale - viewport.width) / 2) * transform.panX
                val y = max(0f, (height * scale - viewport.height) / 2) * transform.panY
                Image(current.bitmap.asImageBitmap(), description,
                    Modifier.matchParentSize().graphicsLayer {
                        scaleX = transform.zoom; scaleY = transform.zoom
                        translationX = x; translationY = y
                    }.testTag("tv-image"), contentScale = if (transform.fill) ContentScale.Crop else ContentScale.Fit)
            }
        }
    }
}

/** Pan is a fraction of the actual overflow; portrait and landscape cannot pan into blank edges. */
internal data class PhotoTransform(val fill: Boolean = false, val zoom: Float = 1f, val panX: Float = 0f, val panY: Float = 0f) {
    fun nextZoom() = copy(zoom = when { zoom < 2f -> 2f; zoom < 4f -> 4f; else -> 1f }, panX = 0f, panY = 0f)
    fun pan(dx: Float, dy: Float) = copy(panX = (panX + dx).coerceIn(-1f, 1f), panY = (panY + dy).coerceIn(-1f, 1f))
}
