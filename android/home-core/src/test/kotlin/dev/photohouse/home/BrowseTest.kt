package dev.photohouse.home

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.mockwebserver.*
import okhttp3.tls.*
import org.junit.*
import org.junit.Assert.*
import java.net.InetAddress

class BrowseTest {
    private fun examples() = Json.parseToJsonElement(javaClass.classLoader.getResourceAsStream("browse-v1.json")!!.use { it.readBytes().toString(Charsets.UTF_8) }).jsonArray
    private fun selection(c: JsonObject) = BrowseSelection(Availability.entries.single { it.wire == c.getValue("availability").jsonPrimitive.content },
        BrowseOrder.entries.single { it.wire == c.getValue("order").jsonPrimitive.content }, BrowseMedia.entries.single { it.wire == c.getValue("media").jsonPrimitive.content })
    private fun parse(c: JsonObject) = CatalogWire.feed(c.getValue("response").toString().toByteArray(), c.getValue("page").jsonPrimitive.int,
        c.getValue("revision").jsonPrimitive.intOrNull, 3, selection(c))
    @Test fun actualProducerPagesPreserveSelectionCountsAndGrouping() {
        val pages = examples().map { parse(it.jsonObject) }
        assertEquals(80, pages[0].total); assertEquals(2, pages[1].total)
        assertEquals(listOf(102,101), pages[1].items.map { it.id })
        val ordered = pages[2].items + pages[3].items
        assertEquals(listOf(102,101,180,179), ordered.take(4).map { it.id })
        assertEquals(80, ordered.map { it.id }.toSet().size)
        assertEquals(BrowseCounts(2,80), pages[2].browseCounts)
        assertEquals(AssetKind.VIDEO, pages[4].items.single().kind)
        assertTrue(pages[5].items.single().display!!.onDemand)
    }
    @Test fun rejectIgnoredSelectionWrongCountsReversedGroupingAndDuplicateIds() {
        val c = examples()[2].jsonObject; val raw = c.getValue("response").jsonObject
        fun reject(body: JsonObject) { assertThrows(HomeFailure::class.java) { CatalogWire.feed(body.toString().toByteArray(),1,null,3,selection(c)) } }
        reject(JsonObject(raw - "browse"))
        val b = raw.getValue("browse").jsonObject
        reject(JsonObject(raw + ("browse" to JsonObject(b + ("order" to JsonPrimitive("catalog"))))))
        reject(JsonObject(raw + ("browse" to JsonObject(b + ("ready_total" to JsonPrimitive(81))))))
        reject(JsonObject(raw + ("browse" to JsonObject(b + ("ready_total" to JsonPrimitive(3))))))
        val items = raw.getValue("items").jsonArray
        reject(JsonObject(raw + ("items" to JsonArray(items.reversed()))))
        reject(JsonObject(raw + ("items" to JsonArray(listOf(items[0]) + items.dropLast(1)))))
        assertThrows(HomeFailure::class.java) { CatalogWire.feed(raw.toString().toByteArray(),1,null,3) }
    }
    @Test fun transportSendsSelectionOnEveryPageWithoutCredentials() = runBlocking {
        val certificate=HeldCertificate.Builder().commonName("home.example").addSubjectAlternativeName("home.example").build()
        val server=MockWebServer();server.useHttps(HandshakeCertificates.Builder().heldCertificate(certificate).build().sslSocketFactory(),false);server.start()
        try {
            val trust=HandshakeCertificates.Builder().addTrustedCertificate(certificate.certificate).build()
            val client=OkHttpClient.Builder().sslSocketFactory(trust.sslSocketFactory(),trust.trustManager)
                .dns(object:Dns { override fun lookup(hostname:String)=listOf(InetAddress.getLoopbackAddress()) }).build()
            val api=HttpsCatalogApi(HomeOrigin.parse("https://home.example:${server.port}"),client,3,true)
            for (index in listOf(2,3)) {
                val c=examples()[index].jsonObject
                server.enqueue(MockResponse().setHeader("Content-Type","application/json").setHeader("Cache-Control","no-store").setBody(c.getValue("response").toString()))
                api.feed(if(index==2) 1 else 2, if(index==2) null else 9,selection(c))
                val r=server.takeRequest()
                assertEquals("ready_first",r.requestUrl!!.queryParameter("order"));assertEquals("1",r.requestUrl!!.queryParameter("browse"))
                assertEquals(if(index==2) null else "9",r.requestUrl!!.queryParameter("revision"))
                assertNull(r.getHeader("Authorization"));assertNull(r.getHeader("Cookie"))
            }
        } finally { server.shutdown() }
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun changingSelectionResetsPageAndDiscardsLatePriorResults() = runTest {
        val calls=mutableListOf<Triple<Int,Int?,BrowseSelection>>()
        val late=CompletableDeferred<HomeFeed>()
        val api=object:HomeApi {
            override val catalogVersion=3;override val browseEnabled=true
            override suspend fun feed(page:Int):HomeFeed=error("Wrong overload")
            override suspend fun feed(page:Int,revision:Int?,selection:BrowseSelection):HomeFeed {
                calls+=Triple(page,revision,selection)
                if(calls.size==2) return withContext(NonCancellable) { late.await() }
                return if(selection.availability==Availability.READY) parse(examples()[1].jsonObject) else parse(examples()[0].jsonObject)
            }
            override suspend fun preview(asset:HomeAsset,variant:Variant,revision:Int):ByteArray?=null
        }
        val store=HomeStore(api,backgroundScope);store.foreground();runCurrent()
        store.loadPage(2);runCurrent()
        val selected=BrowseSelection(Availability.READY)
        store.selectBrowse(selected);runCurrent()
        assertEquals(Triple(1,null,selected),calls.last());assertEquals(2,store.state.value.feed!!.total)
        late.complete(parse(examples()[0].jsonObject));runCurrent()
        assertEquals(2,store.state.value.feed!!.total)
        store.background();assertNull(store.state.value.feed)
        store.foreground();runCurrent();assertEquals(selected,calls.last().third)
        store.background()
    }
}
