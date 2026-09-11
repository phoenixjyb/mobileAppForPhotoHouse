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
@Composable internal fun TvImage(bytes: ByteArray?, description: String, modifier: Modifier = Modifier, missing: String, maxPixels: Int = 8_847_360) {
    key(bytes) {
        val result by produceState(DecodeResult(complete = bytes == null)) {
            if (bytes != null) value = DecodeResult(true, withContext(Dispatchers.Default) {
                decodePermit.withPermit { decodeTvPhoto(bytes, maxPixels) }
            })
        }
        Box(modifier.clipToBounds(), contentAlignment = Alignment.Center) {
            val current = result.photo
            if (!result.complete) CircularProgressIndicator()
            else if (current == null) Text(missing)
            else Image(current.bitmap.asImageBitmap(), description, Modifier.matchParentSize().testTag("tv-image"), contentScale = ContentScale.Fit)
        }
    }
}
