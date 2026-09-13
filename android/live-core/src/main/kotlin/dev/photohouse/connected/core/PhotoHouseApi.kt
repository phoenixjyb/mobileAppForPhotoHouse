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
    val photoDeliveryEnabled: Boolean get() = false
    suspend fun displayPhoto(token: Bearer, library: String, assetId: String): ByteArray = throw ApiFailure(FailureKind.INVALID_INPUT)
    val discoveryEnabled: Boolean get() = false
    suspend fun facets(token: Bearer, library: String, facet: PhoneFacet, page: Int = 1, binding: String? = null): PhoneFacetPage = throw ApiFailure(FailureKind.INVALID_INPUT)
    suspend fun search(token: Bearer, library: String, binding: String, filters: PhoneFilters, page: Int = 1, fingerprint: String? = null): PhoneSearchPage = throw ApiFailure(FailureKind.INVALID_INPUT)
    suspend fun login(phone: String, password: String): SessionToken
    suspend fun register(phone: String, password: String, code: String): SessionToken
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
}

object Admission {
    fun phone(value: String): String {
        val result = value.filterNot { it in " ()-" }
        require(result.matches(Regex("\\+[1-9][0-9]{7,14}"))) { "Use an international phone login" }
        return result
    }
    fun password(value: String) { require(Wire.passwordLengthValid(value)) { "Password length must be 15 to 128 code points" } }
}

fun retryAfterMillis(value: String?, nowMillis: Long = System.currentTimeMillis()): Long {
    // Honor long server cooldowns without overflow; there is never an automatic retry.
    value?.toLongOrNull()?.let { return it.coerceIn(0, Long.MAX_VALUE / 1000) * 1000 }
    val date = runCatching { ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() }.getOrNull()
    return if (date == null) 5000 else (date - nowMillis).coerceAtLeast(0)
}

/** Internal transport result, not a new wire DTO. */
data class VideoChunk(val start: Long, val total: Long, val bytes: ByteArray)
