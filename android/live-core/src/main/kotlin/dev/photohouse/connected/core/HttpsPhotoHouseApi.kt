package dev.photohouse.connected.core

import dev.photohouse.protocol.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.KSerializer
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TrustedOrigin private constructor(internal val url: HttpUrl) {
    override fun toString() = "TrustedOrigin([configured])"
    companion object {
        fun parse(raw: String): TrustedOrigin {
            require(raw == raw.trim() && raw.none { it.isWhitespace() || it == '\\' })
            val url = raw.toHttpUrl()
            require(url.scheme == "https" && url.username.isEmpty() && url.password.isEmpty())
            require(url.encodedPath == "/" && url.query == null && url.fragment == null) { "Configure an HTTPS origin without credentials, path, query or fragment" }
            return TrustedOrigin(url)
        }
    }
}

/** Application construction always uses platform trust and hostname validation.
 * The internal overload is visible only to this module's JVM test friend source set.
 */
class HttpsPhotoHouseApi internal constructor(private val origin: TrustedOrigin, client: OkHttpClient, private val detailPreviewSize: Int = 256) : PhotoHouseApi {
    constructor(origin: TrustedOrigin, detailPreviewSize: Int = 256) : this(origin, OkHttpClient(), detailPreviewSize)
    init { require(detailPreviewSize in 64..1024) }
    private val client = client.newBuilder()
        .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false)
        .cookieJar(CookieJar.NO_COOKIES).cache(null)
        .authenticator(Authenticator.NONE).proxyAuthenticator(Authenticator.NONE)
        .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS))
        .connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).callTimeout(20, TimeUnit.SECONDS)
        .build()
    private data class Packet(val code: Int, val contentType: String?, val bytes: ByteArray, val total: Long = 0)

    private fun url(path: String, library: String? = null, page: Int? = null): HttpUrl {
        require(path.startsWith('/') && !path.startsWith("//"))
        return origin.url.newBuilder().encodedPath(path).apply {
            library?.let { require(it.isNotBlank() && it.length <= 256); addQueryParameter("library", it) }
            page?.let { require(it in 1..100000); addQueryParameter("page", it.toString()); addQueryParameter("page_size", "50") }
        }.build()
    }
    private fun assetId(id: String): String {
        require(id.matches(Regex("[1-9][0-9]{0,18}")) && id.toLongOrNull() != null)
        return id
    }
    private suspend fun packet(url: HttpUrl, token: Bearer?, body: String? = null, limit: Int = JSON_LIMIT, missingAllowed: Boolean = false, accept: String = "application/json", rangeStart: Long? = null): Packet {
        require(url.scheme == "https" && url.host == origin.url.host && url.port == origin.url.port)
        val bytes = body?.toByteArray(Charsets.UTF_8)
        if (bytes != null && bytes.size > 2048) throw ApiFailure(FailureKind.INVALID_INPUT)
        val request = Request.Builder().url(url).header("Accept", accept)
            .header("Cache-Control", "no-store")
            .apply { rangeStart?.let { header("Range", "bytes=$it-${it + limit - 1}"); header("Accept-Encoding", "identity") } }
            .apply { token?.let { header("Authorization", it.header()) } }
            .apply { if (bytes != null) post(bytes.toRequestBody("application/json; charset=utf-8".toMediaType())) }
            .build()
        return suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(ApiFailure(if (e is SSLException) FailureKind.TLS else FailureKind.OFFLINE))
                }
                override fun onResponse(call: Call, response: Response) {
                    try {
                        val result = response.use {
                            if (it.code !in 200..299 && !(missingAllowed && it.code == 404)) {
                                throw ApiFailure(FailureKind.HTTP, it.code, if (it.code == 429) retryAfterMillis(it.header("Retry-After")) else 0)
                            }
                            if (it.code == 404) return@use Packet(404, null, byteArrayOf())
                            var total = 0L
                            var expectedBytes = -1L
                            if (rangeStart != null) {
                                if (it.code != 206 || it.header("Content-Encoding")?.lowercase() !in listOf(null, "identity") ||
                                    it.header("Content-Type")?.substringBefore(';')?.trim()?.lowercase() !in setOf("video/mp4", "video/webm"))
                                    throw ApiFailure(FailureKind.INVALID_RESPONSE)
                                val parts = Regex("bytes ([0-9]+)-([0-9]+)/([0-9]+)").matchEntire(it.header("Content-Range").orEmpty())
                                    ?.groupValues ?: throw ApiFailure(FailureKind.INVALID_RESPONSE)
                                val start = parts[1].toLongOrNull() ?: throw ApiFailure(FailureKind.INVALID_RESPONSE)
                                val end = parts[2].toLongOrNull() ?: throw ApiFailure(FailureKind.INVALID_RESPONSE)
                                total = parts[3].toLongOrNull() ?: throw ApiFailure(FailureKind.INVALID_RESPONSE)
                                if (total > VIDEO_FILE_LIMIT) throw ApiFailure(FailureKind.TOO_LARGE)
                                if (total <= 0 || start != rangeStart || end != minOf(start + limit - 1, total - 1) || end < start)
                                    throw ApiFailure(FailureKind.INVALID_RESPONSE)
                                expectedBytes = end - start + 1
                            }
                            val responseBody = it.body ?: throw ApiFailure(FailureKind.INVALID_RESPONSE)
                            if (responseBody.contentLength() > limit) throw ApiFailure(FailureKind.TOO_LARGE)
                            val out = ByteArrayOutputStream()
                            responseBody.byteStream().use { stream ->
                                val buffer = ByteArray(8192)
                                while (true) {
                                    val count = stream.read(buffer)
                                    if (count < 0) break
                                    if (out.size() + count > limit) throw ApiFailure(FailureKind.TOO_LARGE)
                                    out.write(buffer, 0, count)
                                }
                            }
                            if (expectedBytes >= 0 && out.size().toLong() != expectedBytes) throw ApiFailure(FailureKind.INVALID_RESPONSE)
                            Packet(it.code, it.header("Content-Type"), out.toByteArray(), total)
                        }
                        if (continuation.isActive) continuation.resume(result)
                    } catch (e: Exception) {
                        if (continuation.isActive) continuation.resumeWithException(when (e) {
                            is ApiFailure -> e
                            is SSLException -> ApiFailure(FailureKind.TLS)
                            is IOException -> ApiFailure(FailureKind.OFFLINE)
                            else -> ApiFailure(FailureKind.INVALID_RESPONSE)
                        })
                    }
                }
            })
        }
    }
    private suspend fun <T> json(url: HttpUrl, serializer: KSerializer<T>, token: Bearer? = null, body: String? = null): T {
        val packet = packet(url, token, body)
        if (packet.contentType?.substringBefore(';')?.trim()?.lowercase() != "application/json") throw ApiFailure(FailureKind.INVALID_RESPONSE)
        return try { Wire.json.decodeFromString(serializer, packet.bytes.toString(Charsets.UTF_8)) }
        catch (_: Exception) { throw ApiFailure(FailureKind.INVALID_RESPONSE) }
    }
    override suspend fun login(phone: String, password: String): SessionToken {
        Admission.password(password)
        return json(url("/auth/login"), SessionToken.serializer(), body = Wire.json.encodeToString(LoginRequest.serializer(), LoginRequest(Admission.phone(phone), password)))
    }
    override suspend fun register(phone: String, password: String, code: String): SessionToken {
        Admission.password(password); require(code.isNotBlank())
        return json(url("/auth/register"), SessionToken.serializer(), body = Wire.json.encodeToString(RegisterRequest.serializer(), RegisterRequest(Admission.phone(phone), password, code)))
    }
    override suspend fun session(token: Bearer) = json(url("/auth/session"), Session.serializer(), token)
    override suspend fun acceptInvitation(token: Bearer, code: String) {
        require(code.isNotBlank())
        if (!json(url("/auth/invitations/accept"), Ok.serializer(), token, Wire.json.encodeToString(AcceptRequest.serializer(), AcceptRequest(code))).ok) throw ApiFailure(FailureKind.INVALID_RESPONSE)
    }
    override suspend fun logout(token: Bearer) { if (!json(url("/auth/logout"), Ok.serializer(), token, "{}").ok) throw ApiFailure(FailureKind.INVALID_RESPONSE) }
    override suspend fun gallery(token: Bearer, library: String, page: Int) = json(url("/assets", library, page), Gallery.serializer(), token)
    override suspend fun detail(token: Bearer, library: String, assetId: String) = json(url("/assets/detail/${assetId(assetId)}", library), Detail.serializer(), token)
    override suspend fun captions(token: Bearer, library: String, assetId: String) = json(url("/assets/${assetId(assetId)}/captions", library), Captions.serializer(), token)
    override suspend fun thumbnail(token: Bearer, library: String, asset: Asset): ByteArray? {
        return cachedPreview(token, library, asset, 256)
    }
    override suspend fun detailPreview(token: Bearer, library: String, asset: Asset): ByteArray? {
        val preview = cachedPreview(token, library, asset, detailPreviewSize)
        // Only a missing cached larger derivative can fall back to a cached thumbnail.
        // Denial, TLS, size and other failures propagate; originals are never a fallback.
        return if (preview == null && detailPreviewSize != 256) thumbnail(token, library, asset) else preview
    }
    private suspend fun cachedPreview(token: Bearer, library: String, asset: Asset, size: Int): ByteArray? {
        val expected = url("/assets/${assetId(asset.id)}/thumbnail", library)
        // A response cannot turn a bearer-protected thumbnail into an arbitrary URL.
        require(asset.thumbnail_url.startsWith('/') && !asset.thumbnail_url.startsWith("//") && '\\' !in asset.thumbnail_url)
        require(origin.url.resolve(asset.thumbnail_url) == expected) { "Unexpected scoped thumbnail reference" }
        val target = if (size == 256) expected else expected.newBuilder().addQueryParameter("size", size.toString()).build()
        val packet = packet(target, token, limit = IMAGE_LIMIT, missingAllowed = true, accept = "image/*")
        if (packet.code == 404) return null
        if (packet.contentType?.substringBefore(';')?.lowercase() !in setOf("image/jpeg", "image/png", "image/webp")) throw ApiFailure(FailureKind.INVALID_RESPONSE)
        return packet.bytes
    }
    override suspend fun originalPhoto(token: Bearer, library: String, assetId: String): ByteArray {
        // Construct the protected route; never accept a URL from metadata or UI.
        val packet = packet(url("/assets/${assetId(assetId)}/media", library), token,
            limit = ORIGINAL_LIMIT, accept = "image/jpeg, image/png, image/webp")
        if (packet.code != 200 || packet.bytes.isEmpty() || packet.contentType?.substringBefore(';')?.trim()?.lowercase()
            !in setOf("image/jpeg", "image/png", "image/webp")) throw ApiFailure(FailureKind.INVALID_RESPONSE)
        return packet.bytes
    }
    override suspend fun videoRange(token: Bearer, library: String, assetId: String, start: Long, length: Int): VideoChunk {
        require(start in 0 until VIDEO_FILE_LIMIT && length in 1..VIDEO_CHUNK_LIMIT)
        val packet = packet(url("/assets/${assetId(assetId)}/media", library), token,
            limit = length, accept = "video/mp4, video/webm", rangeStart = start)
        return VideoChunk(start, packet.total, packet.bytes)
    }
    companion object {
        const val VIDEO_CHUNK_LIMIT = 256 * 1024
        const val VIDEO_FILE_LIMIT = 32L * 1024 * 1024 * 1024
        const val JSON_LIMIT = 524288; const val IMAGE_LIMIT = 1048576; const val ORIGINAL_LIMIT = 12 * 1024 * 1024 }
}
