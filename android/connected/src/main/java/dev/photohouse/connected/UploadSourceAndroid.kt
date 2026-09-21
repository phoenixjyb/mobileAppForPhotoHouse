package dev.photohouse.connected

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.provider.OpenableColumns
import dev.photohouse.connected.core.HttpsPhotoHouseApi
import dev.photohouse.connected.core.UploadNetwork
import dev.photohouse.connected.core.UploadSource

/** Build a bounded source over an ephemeral SAF URI; no media bytes are copied to app storage. */
internal fun uploadSource(context: Context, uri: Uri): UploadSource? {
    if (uri.scheme != "content") return null
    val resolver = context.applicationContext.contentResolver
    val mime = resolver.getType(uri)?.substringBefore(';')?.lowercase() ?: return null
    val extension = when (mime) { "image/jpeg" -> "jpg"; "image/png" -> "png"; else -> return null }
    val bytes = resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        if (!cursor.moveToFirst() || cursor.isNull(0)) null else cursor.getLong(0)
    } ?: return null
    if (bytes !in 1..HttpsPhotoHouseApi.MAX_UPLOAD_BYTES) return null
    // Do not expose provider filenames or EXIF data. The backend treats this as advisory.
    return UploadSource("photo.$extension", bytes) { resolver.openInputStream(uri) ?: error("SAF stream unavailable") }
}

internal fun uploadNetwork(context: Context): UploadNetwork {
    val connectivity = context.getSystemService(ConnectivityManager::class.java)
    val network = connectivity?.activeNetwork ?: return UploadNetwork.UNKNOWN
    val capabilities = connectivity.getNetworkCapabilities(network) ?: return UploadNetwork.UNKNOWN
    if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return UploadNetwork.UNKNOWN
    if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) return UploadNetwork.UNKNOWN
    return if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) UploadNetwork.UNMETERED else UploadNetwork.METERED
}
