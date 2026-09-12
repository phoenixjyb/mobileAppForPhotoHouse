package dev.photohouse.home

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Only the JVM test friend source set may inject a synthetic TLS client. */
class HttpsCatalogApi internal constructor(private val origin: HomeOrigin, client: OkHttpClient) : HomeApi {
    constructor(origin: HomeOrigin) : this(origin, OkHttpClient())
    constructor(origin: HomeOrigin, address: HomeLanAddress) : this(origin, homeLanClient(origin, address))
    private val client = client.newBuilder().followRedirects(false).followSslRedirects(false)
        .retryOnConnectionFailure(false).cookieJar(CookieJar.NO_COOKIES).cache(null)
        .authenticator(Authenticator.NONE).proxyAuthenticator(Authenticator.NONE)
        .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS))
        .connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).callTimeout(20, TimeUnit.SECONDS).build()

    private suspend fun get(path: String, limit: Int, type: String, missing: Boolean = false, range: LongRange? = null, total: Long? = null): ByteArray? {
        require(path.startsWith("/home/v2/") && !path.contains('\\'))
        val url = requireNotNull(origin.url.resolve(path))
        require(url.scheme == "https" && url.host == origin.url.host && url.port == origin.url.port)
        val request = Request.Builder().url(url).header("Accept", type)
            .header("Accept-Encoding", "identity").header("Cache-Control", "no-store")
            .apply { if (range != null) header("Range", "bytes=${range.first}-${range.last}") }.build()
        return suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(HomeFailure(if (e is SSLException) HomeError.TLS else HomeError.OFFLINE))
                }
                override fun onResponse(call: Call, response: Response) {
                    try {
                        val value = response.use { r ->
                            when (r.code) {
                                200 -> if (range != null) throw HomeFailure(HomeError.INVALID)
                                206 -> if (range == null) throw HomeFailure(HomeError.INVALID)
                                403 -> throw HomeFailure(HomeError.DENIED)
                                404 -> if (missing) return@use null else throw HomeFailure(HomeError.INVALID)
                                409 -> throw HomeFailure(HomeError.CHANGED)
                                429 -> throw HomeFailure(HomeError.BUSY, HttpsHomeApi.retryAfter(r.header("Retry-After")))
                                503 -> throw HomeFailure(HomeError.UNAVAILABLE)
                                else -> throw HomeFailure(HomeError.INVALID)
                            }
                            if (r.header("Content-Type")?.substringBefore(';')?.trim()?.lowercase() != type ||
                                r.header("Content-Encoding")?.lowercase() !in listOf(null, "identity") ||
                                r.header("Cache-Control")?.split(',')?.none { it.trim().equals("no-store", true) } != false)
                                throw HomeFailure(HomeError.INVALID)
                            if (range != null && (r.headers.values("Content-Range") != listOf("bytes ${range.first}-${range.last}/$total") ||
                                r.headers.values("Content-Length") != listOf(limit.toString()))) throw HomeFailure(HomeError.INVALID)
                            val body = r.body ?: throw HomeFailure(HomeError.INVALID)
                            if (body.contentLength() > limit) throw HomeFailure(HomeError.INVALID)
                            val out = ByteArrayOutputStream()
                            body.byteStream().use { stream ->
                                val buffer = ByteArray(8192)
                                while (true) {
                                    val n = stream.read(buffer); if (n < 0) break
                                    if (out.size() + n > limit) throw HomeFailure(HomeError.INVALID)
                                    out.write(buffer, 0, n)
                                }
                            }
                            if (range != null && out.size() != limit) throw HomeFailure(HomeError.INVALID)
                            out.toByteArray()
                        }
                        if (continuation.isActive) continuation.resume(value)
                    } catch (e: Exception) {
                        if (continuation.isActive) continuation.resumeWithException(when (e) {
                            is HomeFailure -> e
                            is SSLException -> HomeFailure(HomeError.TLS)
                            is IOException -> HomeFailure(HomeError.OFFLINE)
                            else -> HomeFailure(HomeError.INVALID)
                        })
                    }
                }
            })
        }
    }
    override val catalogVersion = 2
    override suspend fun feed(page: Int): HomeFeed = feed(page, null)
    override suspend fun feed(page: Int, revision: Int?): HomeFeed {
        require(page in 1..100000 && (revision == null || revision > 0) && (page == 1 || revision != null))
        val path = "/home/v2/catalog?page=$page&page_size=50" + (revision?.let { "&revision=$it" } ?: "")
        val bytes = requireNotNull(get(path, HomeLimits.JSON, "application/json"))
        return withContext(Dispatchers.Default) { CatalogWire.feed(bytes, page, revision) }
    }
    override suspend fun preview(asset: HomeAsset, variant: Variant, revision: Int): ByteArray? {
        val p = asset.preview(variant) ?: return null
        val path = CatalogWire.previewPath(asset.id, variant, revision)
        if (asset.id <= 0 || revision <= 0 || p.url != path || p.bytes !in 1..variant.bytes) throw HomeFailure(HomeError.INVALID)
        val bytes = get(path, p.bytes, "image/jpeg", missing = true) ?: return null
        return withContext(Dispatchers.Default) { HomeWire.verifyPreview(bytes, p, variant); bytes }
    }
    override fun video(asset: HomeAsset, revision: Int, failed: (Exception) -> Unit): HomeVideoSource {
        val v = asset.video ?: throw HomeFailure(HomeError.INVALID)
        val path = CatalogWire.videoPath(asset.id, revision)
        if (asset.kind != AssetKind.VIDEO || asset.id <= 0 || revision <= 0 || v.url != path || v.bytes !in 1..CatalogWire.VIDEO_MAX_BYTES) throw HomeFailure(HomeError.INVALID)
        return HomeVideoReader(v.bytes, CatalogWire.READ_BYTES, { start, count ->
            if (start < 0 || count !in 1..CatalogWire.READ_BYTES || start >= v.bytes || count > v.bytes - start) throw HomeFailure(HomeError.INVALID)
            get(path, count, "video/mp4", missing = true, range = start..(start + count - 1), total = v.bytes)
                ?: throw IOException("Video unavailable")
        }, failed)
    }
}
