package dev.photohouse.connected.core

import dev.photohouse.protocol.SessionToken
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.concurrent.TimeUnit

class UploadTransportTest {
    private val token = Bearer.from(SessionToken(86400, "T".repeat(43), "Bearer"))
    private val batch = "0123456789abcdef0123456789abcdef"

    @Test fun uploadSendsProtectedHeadersAndReportsProgress() = runBlocking {
        val cert = HeldCertificate.Builder().addSubjectAlternativeName("localhost").build()
        val server = MockWebServer().apply { useHttps(HandshakeCertificates.Builder().heldCertificate(cert).build().sslSocketFactory(), false) }
        server.enqueue(MockResponse().setResponseCode(201).setHeader("Content-Type", "application/json")
            .setBody("""{"asset_id":"17","library_id":null,"incoming":"Yanbo-a1","batch":"$batch","kind":"image","width":640,"height":480,"sha256":"9f64a747e1b97f131fabb6b447296c9b6f0201e79fb3c5356e6c77e89b6a806a","bytes":4,"tasks_enqueued":5}"""))
        server.start(java.net.InetAddress.getByName("127.0.0.1"), 0)
        try {
            val trust = HandshakeCertificates.Builder().addTrustedCertificate(cert.certificate).build()
            val client = OkHttpClient.Builder().protocols(listOf(okhttp3.Protocol.HTTP_1_1)).sslSocketFactory(trust.sslSocketFactory(), trust.trustManager).build()
            val origin = TrustedOrigin.parse("https://localhost:${server.port}")
            val progress = mutableListOf<Long>()
            val api = HttpsPhotoHouseApi(origin, client, protectedNativeV2Enabled = true, uploadEnabled = true)
            val receipt = api.uploadPhoto(token, UploadSource("camera.jpg", 4) { ByteArrayInputStream(byteArrayOf(1, 2, 3, 4)) }, batch) { progress += it }
            val request = server.takeRequest(2, TimeUnit.SECONDS)!!
            assertEquals("POST", request.method)
            assertEquals("Bearer ${"T".repeat(43)}", request.getHeader("Authorization"))
            assertEquals("camera.jpg", request.getHeader("X-Upload-Filename"))
            assertEquals(batch, request.getHeader("X-Upload-Batch"))
            assertEquals("application/octet-stream", request.getHeader("Content-Type"))
            assertArrayEquals(byteArrayOf(1, 2, 3, 4), request.body.readByteArray())
            assertEquals("17", receipt.assetId); assertEquals(5, receipt.tasksEnqueued)
            assertEquals(listOf(4L), progress)
        } finally { server.shutdown() }
    }

    @Test fun uploadRequiresProtectedOptInAndRejectsMalformedReceipt() {
        runBlocking {
        val cert = HeldCertificate.Builder().addSubjectAlternativeName("localhost").build()
        val server = MockWebServer().apply { useHttps(HandshakeCertificates.Builder().heldCertificate(cert).build().sslSocketFactory(), false) }
        server.enqueue(MockResponse().setResponseCode(201).setHeader("Content-Type", "application/json").setBody("{}"))
        server.start(java.net.InetAddress.getByName("127.0.0.1"), 0)
        try {
            val trust = HandshakeCertificates.Builder().addTrustedCertificate(cert.certificate).build()
            val client = OkHttpClient.Builder().protocols(listOf(okhttp3.Protocol.HTTP_1_1)).sslSocketFactory(trust.sslSocketFactory(), trust.trustManager).build()
            val origin = TrustedOrigin.parse("https://localhost:${server.port}")
            val disabled = HttpsPhotoHouseApi(origin, client, protectedNativeV2Enabled = true)
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { disabled.uploadPhoto(token, UploadSource("x.jpg", 1) { ByteArrayInputStream(byteArrayOf(1)) }, batch) }
            }
            val enabled = HttpsPhotoHouseApi(origin, client, protectedNativeV2Enabled = true, uploadEnabled = true)
            assertThrows(ApiFailure::class.java) {
                runBlocking { enabled.uploadPhoto(token, UploadSource("x.jpg", 1) { ByteArrayInputStream(byteArrayOf(1)) }, batch) }
            }
        } finally { server.shutdown() }
        }
    }
}
