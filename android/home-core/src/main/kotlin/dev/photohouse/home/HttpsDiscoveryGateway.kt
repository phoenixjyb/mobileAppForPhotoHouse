package dev.photohouse.home

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Same-origin discovery only. Synthetic TLS injection is restricted to the JVM friend test set. */
class HttpsDiscoveryGateway internal constructor(private val origin: HomeOrigin, client: OkHttpClient) : DiscoveryGateway {
    constructor(origin: HomeOrigin) : this(origin, OkHttpClient())
    constructor(origin: HomeOrigin, address: HomeLanAddress) : this(origin, homeLanClient(origin, address))
    private val client = client.newBuilder().followRedirects(false).followSslRedirects(false)
        .retryOnConnectionFailure(false).cookieJar(CookieJar.NO_COOKIES).cache(null)
        .authenticator(Authenticator.NONE).proxyAuthenticator(Authenticator.NONE)
        .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS))
        .connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).callTimeout(20, TimeUnit.SECONDS).build()
    private val media = HttpsCatalogApi(origin, this.client)

    private suspend fun json(path: String, body: ByteArray? = null): ByteArray {
        if (!path.startsWith("/home/discovery/v1/") || path.contains('\\')) throw HomeFailure(HomeError.INVALID)
        val url = origin.url.resolve(path) ?: throw HomeFailure(HomeError.INVALID)
        if (url.scheme != "https" || url.host != origin.url.host || url.port != origin.url.port) throw HomeFailure(HomeError.INVALID)
        val request = Request.Builder().url(url).header("Accept", "application/json")
            .header("Accept-Encoding", "identity").header("Cache-Control", "no-store")
            .apply { if (body != null) post(body.toRequestBody("application/json".toMediaType())) }.build()
        return suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(HomeFailure(if (e is SSLException) HomeError.TLS else HomeError.OFFLINE))
                }
                override fun onResponse(call: Call, response: Response) {
                    try {
                        val bytes = response.use { r ->
                            when (r.code) {
                                200 -> Unit
                                403 -> throw HomeFailure(HomeError.DENIED)
                                409 -> throw HomeFailure(HomeError.CHANGED)
                                429 -> throw HomeFailure(HomeError.BUSY, HttpsHomeApi.retryAfter(r.header("Retry-After")))
                                503 -> throw HomeFailure(HomeError.UNAVAILABLE)
                                else -> throw HomeFailure(HomeError.INVALID)
                            }
                            if (r.headers.values("Content-Type").size != 1 ||
                                r.header("Content-Type")?.substringBefore(';')?.trim()?.lowercase() != "application/json" ||
                                r.headers.values("Content-Encoding").let { it.isNotEmpty() && (it.size != 1 || !it.single().equals("identity", true)) } ||
                                r.headers.values("Cache-Control").flatMap { it.split(',') }.none { it.trim().equals("no-store", true) })
                                throw HomeFailure(HomeError.INVALID)
                            val responseBody = r.body ?: throw HomeFailure(HomeError.INVALID)
                            if (responseBody.contentLength() > HomeLimits.JSON) throw HomeFailure(HomeError.INVALID)
                            val out = ByteArrayOutputStream()
                            responseBody.byteStream().use { stream ->
                                val buffer = ByteArray(8192)
                                while (true) {
                                    val count = stream.read(buffer); if (count < 0) break
                                    if (out.size() + count > HomeLimits.JSON) throw HomeFailure(HomeError.INVALID)
                                    out.write(buffer, 0, count)
                                }
                            }
                            out.toByteArray()
                        }
                        if (continuation.isActive) continuation.resume(bytes)
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
    private suspend fun facet(field: DiscoveryField, page: Int, revision: Int?): DiscoveryWire.Facets {
        if (page !in 1..5000 || revision != null && revision <= 0 || page > 1 && revision == null) throw HomeFailure(HomeError.INVALID)
        val path = "/home/discovery/v1/facets?facet=${DiscoveryWire.facetName(field)}&page=$page&page_size=50" + (revision?.let { "&revision=$it" } ?: "")
        val bytes = json(path)
        return withContext(Dispatchers.Default) { DiscoveryWire.facets(bytes, field, page).also {
            if (revision != null && it.snapshot.revision != revision) throw HomeFailure(HomeError.INVALID)
        } }
    }
    override suspend fun load(): DiscoverySnapshot {
        var snapshot: DiscoverySnapshot? = null
        for (field in listOf(DiscoveryField.PEOPLE, DiscoveryField.TAGS, DiscoveryField.PLACES)) {
            val response = facet(field, 1, snapshot?.revision)
            snapshot = DiscoveryWire.merge(snapshot, response)
        }
        return requireNotNull(snapshot)
    }
    override suspend fun more(snapshot: DiscoverySnapshot, field: DiscoveryField): DiscoverySnapshot {
        val page = snapshot.nextPages[field] ?: throw HomeFailure(HomeError.INVALID)
        if (snapshot.binding == null || page <= 1 || field !in snapshot.facetTotals) throw HomeFailure(HomeError.INVALID)
        return DiscoveryWire.merge(snapshot, facet(field, page, snapshot.revision))
    }
    override fun results(snapshot: DiscoverySnapshot, draft: DiscoveryDraft): HomeApi {
        // Validate before constructing a result store, and copy the exact immutable request selection.
        val normalized = draft.normalized().copy(people = draft.people.toSet(), tags = draft.tags.toSet(),
            themes = draft.themes.toSet(), topics = draft.topics.toSet())
        DiscoveryWire.request(snapshot, normalized, 1)
        return object : HomeApi {
            override val catalogVersion = 2
            override val retryRevisionChanges = false
            private val lock = Mutex()
            private var fingerprint: String? = null
            private var total: Int? = null
            override suspend fun feed(page: Int): HomeFeed = feed(page, null)
            override suspend fun feed(page: Int, revision: Int?): HomeFeed = lock.withLock {
                if (revision != null && revision != snapshot.catalogRevision || page > 1 && (revision == null || fingerprint == null)) throw HomeFailure(HomeError.INVALID)
                val request = DiscoveryWire.request(snapshot, normalized, page)
                val bytes = json("/home/discovery/v1/search", request)
                val parsed = withContext(Dispatchers.Default) { DiscoveryWire.search(bytes, snapshot, page, fingerprint, total) }
                fingerprint = parsed.fingerprint; total = parsed.feed.total
                parsed.feed
            }
            override suspend fun preview(asset: HomeAsset, variant: Variant, revision: Int): ByteArray? {
                if (revision != snapshot.catalogRevision) throw HomeFailure(HomeError.INVALID)
                return media.preview(asset, variant, revision)
            }
            override fun video(asset: HomeAsset, revision: Int, failed: (Exception) -> Unit): HomeVideoSource {
                if (revision != snapshot.catalogRevision) throw HomeFailure(HomeError.INVALID)
                return media.video(asset, revision, failed)
            }
        }
    }
}
