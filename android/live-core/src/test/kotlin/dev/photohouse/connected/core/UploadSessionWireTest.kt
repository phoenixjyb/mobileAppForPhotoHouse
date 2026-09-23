package dev.photohouse.connected.core

import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class UploadSessionWireTest {
    private val body = """{"upload_id":"${"a".repeat(32)}","bytes":8,"offset":0,"chunk_bytes":4194304,"state":"uploading","asset_id":null}"""
    @Test fun parsesExactSessionAndEncodesExactRequest() {
        val parsed = UploadSessionWire.parse(body.toByteArray()); assertEquals(8, parsed.bytes); assertEquals(4194304, parsed.chunkBytes)
        val encoded = UploadSessionWire.request(UploadSessionRequest("1".repeat(32), "2".repeat(32), "x.mp4", 8, "3".repeat(64), UploadKind.VIDEO))
        assertEquals(setOf("request_id", "batch", "filename", "bytes", "sha256", "kind"), kotlinx.serialization.json.Json.parseToJsonElement(encoded).jsonObject.keys)
    }
    @Test fun rejectsDuplicateKeysBoundsAndInvalidState() {
        assertThrows(ApiFailure::class.java) { UploadSessionWire.parse(body.replace("\"offset\":0", "\"offset\":0,\"offset\":1").toByteArray()) }
        assertThrows(ApiFailure::class.java) { UploadSessionWire.parse(body.replace("\"bytes\":8", "\"bytes\":0").toByteArray()) }
        assertThrows(ApiFailure::class.java) { UploadSessionWire.parse(body.replace("\"state\":\"uploading\"", "\"state\":\"complete\"").toByteArray()) }
    }
}
