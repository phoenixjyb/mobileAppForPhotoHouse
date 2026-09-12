package dev.photohouse.home

import kotlinx.serialization.json.*

/** Frozen home-catalog v2. Strict keys and bounded UTF-8 parsing; no permissive v1 fallback. */
object CatalogWire {
    const val VIDEO_MAX_BYTES = 34359738368L
    const val READ_BYTES = 262144
    private fun bad(): Nothing = throw HomeFailure(HomeError.INVALID)
    private fun check(ok: Boolean) { if (!ok) bad() }
    private fun JsonElement.obj(vararg keys: String): JsonObject {
        val o = this as? JsonObject ?: bad(); check(o.keys == keys.toSet()); return o
    }
    private fun JsonElement.number(low: Long = 1, high: Long = Int.MAX_VALUE.toLong()): Long {
        val p = this as? JsonPrimitive ?: bad()
        check(!p.isString && p.content.matches(Regex("0|[1-9][0-9]*")))
        return p.content.toLongOrNull()?.takeIf { it in low..high } ?: bad()
    }
    private fun JsonElement.str(max: Int): String {
        val p = this as? JsonPrimitive ?: bad(); check(p.isString)
        check(p.content.toByteArray().size <= max && p.content.none { it < ' ' && it != '\n' && it != '\t' })
        return p.content
    }
    private fun JsonElement.bool(): Boolean {
        val p = this as? JsonPrimitive ?: bad(); check(!p.isString); return p.booleanOrNull ?: bad()
    }
    private fun JsonElement.hash(): String = str(64).also { check(it.matches(Regex("[0-9a-f]{64}"))) }
    private fun JsonElement.reason(): MediaUnavailable {
        val o = obj("state", "reason"); check(o.getValue("state").str(20) == "unavailable")
        return when (o.getValue("reason").str(32)) {
            "not_prepared" -> MediaUnavailable.NOT_PREPARED
            "source_missing" -> MediaUnavailable.SOURCE_MISSING
            "unsupported" -> MediaUnavailable.UNSUPPORTED
            "preparation_failed" -> MediaUnavailable.PREPARATION_FAILED
            else -> bad()
        }
    }
    fun previewPath(id: Int, variant: Variant, revision: Int) = "/home/v2/assets/$id/preview?variant=${variant.wire}&revision=$revision"
    fun videoPath(id: Int, revision: Int) = "/home/v2/assets/$id/video?revision=$revision"
    fun feed(bytes: ByteArray, page: Int, requestedRevision: Int? = null): HomeFeed = try {
        check(page in 1..100000 && (page == 1 || requestedRevision != null))
        val o = HomeWire.parse(bytes).obj("version", "revision", "library", "page", "page_size", "total", "has_more", "items")
        check(o.getValue("version").number() == 2L)
        val revision = o.getValue("revision").number().toInt()
        check(requestedRevision == null || revision == requestedRevision)
        check(o.getValue("page").number() == page.toLong() && o.getValue("page_size").number() == 50L)
        val total = o.getValue("total").number(0, 100000).toInt()
        val more = o.getValue("has_more").bool(); check(more == (page * 50 < total))
        val library = o.getValue("library").obj("id", "title")
        val id = library.getValue("id").str(64); check(id.matches(Regex("[a-z0-9-]{1,64}")))
        val items = (o.getValue("items") as? JsonArray ?: bad()).map { element ->
            val a = element.obj("id", "kind", "label", "width", "height", "previews", "video", "originals_allowed")
            check(!a.getValue("originals_allowed").bool())
            for (dimension in listOf("width", "height")) a.getValue(dimension).let { if (it != JsonNull) it.number(1, 1000000) }
            val assetId = a.getValue("id").number().toInt()
            val kind = when (a.getValue("kind").str(16)) {
                "photo" -> AssetKind.PHOTO; "video" -> AssetKind.VIDEO; "unsupported" -> AssetKind.UNSUPPORTED; else -> bad()
            }
            val previews = a.getValue("previews").obj("grid", "display")
            fun preview(v: Variant): Pair<Preview?, MediaUnavailable?> {
                val raw = previews.getValue(v.wire); val p = raw as? JsonObject ?: bad()
                if (p["state"] == JsonPrimitive("unavailable")) return null to raw.reason()
                p.obj("state", "width", "height", "bytes", "sha256", "url")
                check(p.getValue("state").str(20) == "ready")
                val w = p.getValue("width").number(1, v.edge.toLong()).toInt()
                val h = p.getValue("height").number(1, v.edge.toLong()).toInt(); check(w.toLong() * h <= v.pixels)
                val url = p.getValue("url").str(160); check(url == previewPath(assetId, v, revision))
                return Preview(w, h, p.getValue("bytes").number(1, v.bytes.toLong()).toInt(), p.getValue("sha256").hash(), url) to null
            }
            val grid = preview(Variant.GRID); val display = preview(Variant.DISPLAY)
            var video: HomeVideo? = null; var unavailable: MediaUnavailable? = null
            val raw = a.getValue("video")
            if (kind != AssetKind.VIDEO) check(raw == JsonNull)
            else {
                val v = raw as? JsonObject ?: bad()
                if (v["state"] == JsonPrimitive("unavailable")) unavailable = v.reason()
                else {
                    v.obj("state", "mime", "video_codec", "audio_codec", "width", "height", "duration_ms", "bytes", "sha256", "url")
                    check(v.getValue("state").str(20) == "ready" && v.getValue("mime").str(32) == "video/mp4" && v.getValue("video_codec").str(8) == "h264")
                    val audio = v.getValue("audio_codec").let { if (it == JsonNull) null else it.str(8).also { codec -> check(codec == "aac") } }
                    val w = v.getValue("width").number(1, 1920).toInt(); val h = v.getValue("height").number(1, 1920).toInt(); check(w.toLong() * h <= 2073600)
                    val url = v.getValue("url").str(160); check(url == videoPath(assetId, revision))
                    video = HomeVideo(w, h, v.getValue("duration_ms").number(1, 86400000).toInt(), v.getValue("bytes").number(1, VIDEO_MAX_BYTES), v.getValue("sha256").hash(), url, audio)
                }
            }
            HomeAsset(assetId, a.getValue("label").str(256), grid.first, display.first, kind, video, grid.second, display.second, unavailable)
        }
        check(items.size == minOf(50, maxOf(0, total - (page - 1) * 50)))
        check(items.zipWithNext().all { (a, b) -> a.id > b.id })
        HomeFeed(revision, id, library.getValue("title").str(256), page, 50, total, more, items, version = 2)
    } catch (e: HomeFailure) { throw e } catch (_: Exception) { bad() }
}
