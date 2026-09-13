package dev.photohouse.connected.core

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class VideoReaderTest {
    @Test fun largeFileSeeksAndEofKeepAllocationsBoundedAndReauthorizeRepeatReads() {
        val total = 8193114694L
        val calls = mutableListOf<Pair<Long, Int>>()
        var deny = false
        val reader = VideoReader({ start, count ->
            calls += start to count
            if (deny) throw ApiFailure(FailureKind.HTTP, 403)
            VideoChunk(start, total, ByteArray(minOf(count.toLong(), total - start).toInt()) { ((start + it) % 251).toByte() })
        }, Long.MAX_VALUE, { 0 }) { }
        reader.use {
            val buffer = ByteArray(16) { 77 }
            assertEquals(16, it.readAt(4294967296L, buffer, 0, 16))
            assertEquals((4294967296L % 251).toByte(), buffer[0])
            assertEquals(4, it.readAt(total - 4, buffer, 3, 12))
            assertEquals(-1, it.readAt(total, buffer, 0, 1))
            assertEquals(3, calls.size)
            deny = true; buffer.fill(77)
            assertTrue(runCatching { it.readAt(4294967296L, buffer, 0, 16) }.isFailure)
            assertArrayEquals(ByteArray(16) { 77 }, buffer)
            assertTrue(it.isClosed)
            assertEquals(4, calls.size)
        }
    }
    @Test fun randomReadsAreBoundedAndEofAndZeroLengthDoNotFetch() {
        val calls = mutableListOf<Pair<Long, Int>>()
        val reader = VideoReader({ start, length -> calls += start to length; VideoChunk(start, 800000, ByteArray(length) { ((start + it) % 251).toByte() }) }, Long.MAX_VALUE, { 0 }) { fail("Unexpected failure") }
        reader.use {
            assertEquals(0, it.readAt(0, byteArrayOf(), 0, 0)); assertTrue(calls.isEmpty())
            assertEquals(800000L, it.size())
            val buffer = ByteArray(400000)
            assertEquals(262144, it.readAt(0, buffer, 0, buffer.size))
            assertEquals(5, it.readAt(799995, buffer, 10, 20))
            assertEquals((799995 % 251).toByte(), buffer[10])
            val before = calls.size; assertEquals(-1, it.readAt(800000, buffer, 0, 1)); assertEquals(before, calls.size)
            assertEquals(20, it.readAt(123, buffer, 0, 20)); assertEquals(20, it.readAt(123, buffer, 0, 20))
            assertEquals(5, calls.size) // Repeated ranges reauthorize, no chunk cache.
        }
        assertTrue(reader.isClosed)
        assertTrue(runCatching { reader.size() }.exceptionOrNull() is IOException)
    }
    @Test fun closeCancelsInflightReadAndNeverCopiesLateBytes() = runBlocking {
        val entered = CountDownLatch(1); val cancelled = CountDownLatch(1); val errors = AtomicInteger()
        val reader = VideoReader({ _, _ -> entered.countDown(); try { awaitCancellation() } finally { cancelled.countDown() } }, Long.MAX_VALUE, { 0 }) { errors.incrementAndGet() }
        val buffer = ByteArray(10) { 9 }
        val worker = async(Dispatchers.IO) { runCatching { reader.readAt(0, buffer, 0, 10) }.exceptionOrNull() }
        assertTrue(entered.await(5, TimeUnit.SECONDS))
        reader.close(); reader.close()
        assertTrue(withTimeout(2000) { worker.await() } is IOException)
        assertTrue(cancelled.await(2, TimeUnit.SECONDS)); assertEquals(0, errors.get())
        assertArrayEquals(ByteArray(10) { 9 }, buffer)
    }
    @Test fun changedSizeMalformedChunkExpiryAndDenialFailClosedOnlyOnce() {
        for (scenario in listOf("size", "bytes", "expiry", "denial")) {
            var clock = 0L; var count = 0; val failures = mutableListOf<Exception>(); var closes = 0
            val reader = VideoReader({ start, length ->
                count++
                if (count > 1 && scenario == "denial") throw ApiFailure(FailureKind.HTTP, 401)
                VideoChunk(start, if (count > 1 && scenario == "size") 99 else 100,
                    ByteArray(if (count > 1 && scenario == "bytes") length - 1 else length))
            }, 1000, { clock }) { failures += it }
            reader.onClose { closes++ }; assertEquals(100L, reader.size())
            if (scenario == "expiry") clock = 1000
            assertTrue(runCatching { reader.readAt(0, ByteArray(10), 0, 10) }.isFailure)
            assertTrue(reader.isClosed); assertEquals(1, failures.size); assertEquals(1, closes)
            runCatching { reader.size() }; assertEquals(1, failures.size)
            reader.onClose { closes++ }; assertEquals(2, closes)
        }
    }
}
