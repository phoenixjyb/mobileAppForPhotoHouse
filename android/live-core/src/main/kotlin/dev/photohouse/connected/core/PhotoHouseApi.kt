package dev.photohouse.connected.core

import dev.photohouse.protocol.*
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** Bearer credentials never appear in state, URLs, logs or toString. */
class Bearer private constructor(private val value: String) {
    internal fun header() = "Bearer $value"
    override fun toString() = "Bearer([redacted])"
    companion object {
        fun from(response: SessionToken): Bearer {
            require(response.token_type == "Bearer" && response.expires_in == 86400L)
            require(response.access_token.matches(Regex("[A-Za-z0-9_-]{43}")))
            require(response.access_token != "F".repeat(43)) { "Fixture credentials are not accepted" }
            return Bearer(response.access_token)
        }
    }
}

enum class FailureKind { HTTP, OFFLINE, TLS, INVALID_RESPONSE, INVALID_INPUT, TOO_LARGE }
class ApiFailure(val kind: FailureKind, val status: Int? = null, val retryAfterMillis: Long = 0) : Exception("PhotoHouse request failed")

interface PhotoHouseApi {
    /** Protected family stories are opt-in until the integration owner enables the route. */
    val protectedNativeV2Enabled: Boolean get() = false
    val photoDeliveryEnabled: Boolean get() = false
    suspend fun displayPhoto(token: Bearer, library: String, assetId: String): ByteArray = throw ApiFailure(FailureKind.INVALID_INPUT)
    val discoveryEnabled: Boolean get() = false
    suspend fun facets(token: Bearer, library: String, facet: PhoneFacet, page: Int = 1, binding: String? = null): PhoneFacetPage = throw ApiFailure(FailureKind.INVALID_INPUT)
    suspend fun search(token: Bearer, library: String, binding: String, filters: PhoneFilters, page: Int = 1, fingerprint: String? = null): PhoneSearchPage = throw ApiFailure(FailureKind.INVALID_INPUT)
    suspend fun login(phone: String, password: String): SessionToken
    suspend fun register(phone: String, password: String, code: String): SessionToken
    suspend fun registerNamed(phone: String, password: String, code: String, name: String): SessionToken = throw ApiFailure(FailureKind.INVALID_INPUT)
    suspend fun session(token: Bearer): Session
    suspend fun acceptInvitation(token: Bearer, code: String)
    suspend fun logout(token: Bearer)
    suspend fun gallery(token: Bearer, library: String, page: Int): Gallery
    suspend fun detail(token: Bearer, library: String, assetId: String): Detail
    suspend fun captions(token: Bearer, library: String, assetId: String): Captions
    suspend fun thumbnail(token: Bearer, library: String, asset: Asset): ByteArray?
    /** Larger cached detail preview; default preserves existing phone and test adapters. */
    suspend fun detailPreview(token: Bearer, library: String, asset: Asset): ByteArray? = thumbnail(token, library, asset)
    suspend fun videoRange(token: Bearer, library: String, assetId: String, start: Long, length: Int): VideoChunk
    suspend fun originalPhoto(token: Bearer, library: String, assetId: String): ByteArray
    suspend fun stories(token: Bearer, library: String, assetId: String, page: Int): ProtectedStoryPage = throw ApiFailure(FailureKind.INVALID_INPUT)
}

object Admission {
    fun phone(value: String): String {
        val result = value.filterNot { it in " ()-" }
        require(result.matches(Regex("\\+[1-9][0-9]{7,14}"))) { "Use an international phone login" }
        return result
    }
    fun password(value: String, protectedNativeV2: Boolean = false, registration: Boolean = false) {
        require(value.toUtf8Strict()) { "Password contains malformed Unicode" }
        val points = value.codePointCount(0, value.length)
        val valid = if (!protectedNativeV2) points in 15..128 else points in (if (registration) 8..128 else 1..128)
        require(valid) { if (protectedNativeV2) "Password length is invalid" else "Password length must be 15 to 128 code points" }
    }

    /** Match Python str.split whitespace, counting Unicode code points after collapse. */
    fun displayName(value: String): String {
        require(value.toUtf8Strict())
        val collapsed = value.split(Regex("[\\u0009-\\u000D\\u001C-\\u0020\\u0085\\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000]+"))
            .filter { it.isNotEmpty() }.joinToString(" ")
        require(collapsed.codePointCount(0, collapsed.length) in 1..64)
        require(collapsed.none { it < ' ' || it == '\u007f' })
        return collapsed
    }

    fun invitationCode(value: String): String {
        require(value.isNotBlank() && value.toUtf8Strict())
        return value
    }

    private fun String.toUtf8Strict(): Boolean = runCatching {
        Charsets.UTF_8.newEncoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
            .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT).encode(java.nio.CharBuffer.wrap(this))
    }.isSuccess
}

fun retryAfterMillis(value: String?, nowMillis: Long = System.currentTimeMillis()): Long {
    // Honor long server cooldowns without overflow; there is never an automatic retry.
    value?.toLongOrNull()?.let { return it.coerceIn(0, Long.MAX_VALUE / 1000) * 1000 }
    val date = runCatching { ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() }.getOrNull()
    return if (date == null) 5000 else (date - nowMillis).coerceAtLeast(0)
}

/** Internal transport result, not a new wire DTO. */
data class VideoChunk(val start: Long, val total: Long, val bytes: ByteArray)
