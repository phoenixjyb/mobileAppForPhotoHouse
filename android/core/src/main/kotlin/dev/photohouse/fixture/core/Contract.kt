package dev.photohouse.fixture.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

// Names intentionally match the frozen wire fields. No derived language/date fields.
@Serializable data class SessionToken(val expires_in: Long, val access_token: String, val token_type: String)
@Serializable data class Session(val account_id: String, val phone_login: String, val memberships: List<Membership>)
@Serializable data class Membership(
    val library_id: String, val status: String, val role: String, val revision: Long,
    val expires_at: Long?, val originals: Int, val available: Boolean,
)
@Serializable data class Asset(
    val id: String, val kind: String, val width: Int?, val height: Int?,
    val duration_sec: Double?, val taken_at: String?, val thumbnail_url: String,
) {
    init { require(id.matches(Regex("[1-9][0-9]{0,18}")) && id.toLongOrNull() != null) }
}
@Serializable data class Gallery(
    val library_id: String, val page: Int, val page_size: Int, val total: Long,
    val originals_allowed: Boolean, val items: List<Asset>,
)
@Serializable data class Detail(val library_id: String, val originals_allowed: Boolean, val asset: Asset)
@Serializable data class Caption(
    val id: String, val text: String, val truncated: Boolean, val user_edited: Boolean,
    val created_at: String?, val updated_at: String?,
)
@Serializable data class Captions(
    val library_id: String, val asset_id: String, val has_more: Boolean, val items: List<Caption>,
)
@Serializable data class Ok(val ok: Boolean)
@Serializable data class LoginRequest(val phone: String, val password: String, val transport: String = "native")
@Serializable data class RegisterRequest(val phone: String, val password: String, val code: String, val transport: String = "native")
@Serializable data class AcceptRequest(val code: String)

@Serializable data class FixtureCase(
    val id: String, val operation_id: String, val status: Int, val body: JsonElement? = null,
    val body_kind: String? = null, val content_type: String? = null,
    val byte_length: Int? = null, val content_range: String? = null,
)
@Serializable data class FixtureFile(
    val contract_version: String, val synthetic_only: Boolean,
    val normalization: String, val cases: List<FixtureCase>,
)
@Serializable data class ClientScenario(val id: String, val action: String, val responses: List<String>, val expected: String)
@Serializable data class ClientScenarios(
    val contract_version: String, val evidence_kind: String, val network_enabled: Boolean,
    val thumbnail_resources: Map<String, String>, val scenarios: List<ClientScenario>,
)

object Contract {
    const val VERSION = "1.0.0-fixture.1"
    val json = Json { ignoreUnknownKeys = false }
    inline fun <reified T> body(response: FixtureCase): T =
        json.decodeFromString(requireNotNull(response.body).toString())
    fun fixtures(text: String) = json.decodeFromString<FixtureFile>(text).also {
        require(it.contract_version == VERSION && it.synthetic_only)
    }
    fun scenarios(text: String) = json.decodeFromString<ClientScenarios>(text).also {
        require(it.contract_version == VERSION && !it.network_enabled)
        require(it.thumbnail_resources.values.all { path -> path in setOf("media/amber.png", "media/blue.png") })
    }
    fun passwordLengthValid(password: String) = password.codePointCount(0, password.length) in 15..128
}
