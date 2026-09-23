package dev.photohouse.connected

import android.content.SharedPreferences
import dev.photohouse.connected.core.*
import org.json.JSONArray
import org.json.JSONObject

/** App-private metadata only; media bytes remain behind the persisted SAF grant. */
internal class SharedPreferencesUploadQueuePersistence(private val prefs: SharedPreferences) : UploadQueuePersistence {
    override fun load(account: String): List<UploadQueueRecord> = runCatching {
        val array = JSONArray(prefs.getString("upload_queue_$account", "[]"))
        (0 until array.length()).map { i -> val o = array.getJSONObject(i)
            UploadQueueRecord(o.getString("local"), o.getString("request"), o.getString("batch"), o.getString("filename"), o.optString("locator").takeIf { it.isNotEmpty() }, o.getLong("bytes"), UploadKind.valueOf(o.getString("kind")), o.getString("sha"), o.optString("upload").takeIf { it.isNotEmpty() }, o.getLong("offset"), o.getString("status"), o.optString("error").takeIf { it.isNotEmpty() })
        }
    }.getOrDefault(emptyList())
    override fun save(account: String, records: List<UploadQueueRecord>) {
        val array = JSONArray(); records.forEach { r -> array.put(JSONObject().apply {
            put("local", r.localId); put("request", r.requestId); put("batch", r.batch); put("filename", r.filename); put("locator", r.locator); put("bytes", r.bytes); put("kind", r.kind.name); put("sha", r.sha256); put("upload", r.uploadId); put("offset", r.offset); put("status", r.status); put("error", r.error)
        }) }; prefs.edit().putString("upload_queue_$account", array.toString()).apply()
    }
}
