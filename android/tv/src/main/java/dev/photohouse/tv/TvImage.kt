package dev.photohouse.tv

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import dev.photohouse.connected.core.HttpsPhotoHouseApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream

internal data class DecodedPhoto(val bitmap: Bitmap, val downsampled: Boolean)

/** Header-first decoding bounds the displayed bitmap to 8,847,360 pixels.
 * No URI, file, EXIF location, thumbnail fallback, or persistent image cache.
 */
internal fun decodeTvPhoto(bytes: ByteArray, maxPixels: Int = 8_847_360): DecodedPhoto? {
    if (bytes.isEmpty() || bytes.size > HttpsPhotoHouseApi.ORIGINAL_LIMIT) return null
    return try {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        val width = options.outWidth; val height = options.outHeight
        if (width !in 1..32768 || height !in 1..32768) return null
        var sample = 1
        while (((width.toLong() + sample - 1) / sample) * ((height.toLong() + sample - 1) / sample) > maxPixels) sample *= 2
        options.inJustDecodeBounds = false
        options.inSampleSize = sample
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
        if (decoded.width.toLong() * decoded.height > maxPixels) { decoded.recycle(); return null }
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
        Box(modifier, contentAlignment = Alignment.Center) {
            val current = result.photo
            if (!result.complete) CircularProgressIndicator()
            else if (current == null) Text(missing)
            else Image(current.bitmap.asImageBitmap(), description, Modifier.fillMaxSize().testTag("tv-image"), contentScale = ContentScale.Fit)
        }
    }
}
