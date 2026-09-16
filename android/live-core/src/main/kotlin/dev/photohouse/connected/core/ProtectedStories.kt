package dev.photohouse.connected.core

import kotlinx.serialization.json.*
import java.util.UUID

data class ProtectedStory(
    val id: String, val assetId: String, val title: String, val text: String,
    val language: String, val byline: String, val authorId: String, val revision: Long,
    val createdAt: Long, val updatedAt: Long, val canEdit: Boolean, val canViewHistory: Boolean
)

data class ProtectedStoryPage(
    val libraryId: String, val assetId: String, val page: Int,
    val canCreate: Boolean, val hasMore: Boolean, val items: List<ProtectedStory>
)

internal object ProtectedStoriesWire {
    private fun bad(): Nothing = throw ApiFailure(FailureKind.INVALID_RESPONSE)
    private fun check(v: Boolean) { if (!v) bad() }
    private fun obj(v: JsonElement): JsonObject = v as? JsonObject ?: bad()
    private fun str(o: JsonObject, key: String): String = o[key]?.jsonPrimitive?.takeIf { it.isString }?.content ?: bad()
    private fun bool(o: JsonObject, key: String): Boolean = o[key]?.jsonPrimitive?.takeIf { !it.isString }?.booleanOrNull ?: bad()
    private fun long(o: JsonObject, key: String): Long = o[key]?.jsonPrimitive?.takeIf { !it.isString }?.longOrNull ?: bad()
    private fun fields(o: JsonObject, expected: Set<String>) { check(o.keys == expected) }
    private fun uuid(value: String) { check(runCatching { UUID.fromString(value).toString() == value }.getOrDefault(false)) }
    private fun decimalAsset(value: String) { check(value.matches(Regex("[1-9][0-9]{0,18}")) && value.toLongOrNull() != null) }
    private fun text(value: String, bytes: Int, nonblank: Boolean = false) {
        check(value.indexOf('\u0000') < 0 && value.toByteArray(Charsets.UTF_8).size <= bytes)
        if (nonblank) check(value.isNotBlank())
    }
    internal fun parse(bytes: ByteArray, library: String, assetId: String, page: Int): ProtectedStoryPage = try {
        parseInternal(bytes, library, assetId, page)
    } catch (e: ApiFailure) {
        throw e
    } catch (_: Exception) {
        bad()
    }
    private fun parseInternal(bytes: ByteArray, library: String, assetId: String, page: Int): ProtectedStoryPage {
        val root = obj(DiscoveryJson.parse(bytes, HttpsPhotoHouseApi.STORIES_LIMIT))
        fields(root, setOf("asset_id", "library_id", "page", "can_create", "has_more", "items"))
        check(str(root, "library_id") == library); check(str(root, "asset_id") == assetId)
        decimalAsset(str(root, "asset_id"))
        check(long(root, "page") == page.toLong()); val items = root["items"] as? JsonArray ?: bad()
        check(items.size <= 5); val seen = mutableSetOf<String>()
        val stories = items.map { parseStory(obj(it), assetId, seen) }
        return ProtectedStoryPage(library, assetId, page, bool(root, "can_create"), bool(root, "has_more"), stories)
    }
    private fun parseStory(o: JsonObject, assetId: String, seen: MutableSet<String>): ProtectedStory {
        fields(o, setOf("asset_id", "author_id", "byline", "can_edit", "can_view_history", "created_at", "deleted", "id", "language", "revision", "source", "text", "title", "updated_at"))
        val id = str(o, "id"); val actualAsset = str(o, "asset_id"); val author = str(o, "author_id")
        uuid(id); check(author.isNotBlank()); check(seen.add(id)); check(actualAsset == assetId); decimalAsset(actualAsset)
        check(str(o, "source") == "family"); check(!bool(o, "deleted"));
        val language = str(o, "language"); check(language in setOf("en", "zh", "mixed", "und"))
        val title = str(o, "title"); val text = str(o, "text"); val byline = str(o, "byline")
        text(title, 512); text(text, 64 * 1024, nonblank = true); text(byline, 256)
        val revision = long(o, "revision"); val created = long(o, "created_at"); val updated = long(o, "updated_at")
        check(revision > 0 && created >= 0 && updated >= 0)
        return ProtectedStory(id, actualAsset, title, text, language, byline, author, revision, created, updated, bool(o, "can_edit"), bool(o, "can_view_history"))
    }
}
