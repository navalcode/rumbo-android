package dev.navalcode.rumbo.data

import dev.navalcode.rumbo.core.SourceOfflineException
import dev.navalcode.rumbo.core.SourceUnavailableException
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class TripCatalogApiTest {
    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient
    private lateinit var api: TripCatalogApi
    private val valid = """[{"id":"alba","title":"Valle del Alba","region":"Sierra Clara","category":"MOUNTAIN","days":3,"priceEuros":145,"distanceKm":18,"artwork":0}]"""

    @Before fun setUp() {
        server = MockWebServer().also { it.start() }
        client = OkHttpClient.Builder().retryOnConnectionFailure(false).build()
        api = Retrofit.Builder().baseUrl(server.url("/"))
            .client(client).addConverterFactory(GsonConverterFactory.create())
            .build().create(TripCatalogApi::class.java)
    }

    @After fun tearDown() {
        client.dispatcher.cancelAll()
        client.connectionPool.evictAll()
        server.shutdown()
        client.dispatcher.executorService.shutdown()
    }

    @Test fun decodesTheRealHttpResponseAndSendsTheContract() = runBlocking {
        server.enqueue(MockResponse().setBody(valid).setHeader("Content-Type", "application/json"))
        val trips = api.fetchTrips(DemoScenario.CONNECTED)
        assertEquals("alba", trips.single().id)
        assertEquals(145, trips.single().priceEuros)
        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/v1/trips", request.path)
        assertEquals("CONNECTED", request.getHeader("X-Demo-Scenario"))
    }

    @Test fun mapsHttpServiceFailure() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(503).setBody("{}"))
        expectUnavailable { api.fetchTrips(DemoScenario.CONNECTED) }
    }

    @Test fun rejectsMissingFieldsRatherThanCachingPartialModels() = runBlocking {
        server.enqueue(MockResponse().setBody("""[{"id":"alba"}]"""))
        expectUnavailable { api.fetchTrips(DemoScenario.CONNECTED) }
    }

    @Test fun rejectsUnknownCategory() = runBlocking {
        server.enqueue(MockResponse().setBody(valid.replace("MOUNTAIN", "UNKNOWN")))
        expectUnavailable { api.fetchTrips(DemoScenario.CONNECTED) }
    }

    @Test fun rejectsOutOfRangeValues() = runBlocking {
        server.enqueue(MockResponse().setBody(valid.replace("\"days\":3", "\"days\":0")))
        expectUnavailable { api.fetchTrips(DemoScenario.CONNECTED) }
    }

    @Test fun rejectsDuplicateTripIds() = runBlocking {
        server.enqueue(MockResponse().setBody("[${valid.removeSurrounding("[", "]")},${valid.removeSurrounding("[", "]")}]"))
        expectUnavailable { api.fetchTrips(DemoScenario.CONNECTED) }
    }

    @Test fun rejectsNullItemsAndNoContent() = runBlocking {
        server.enqueue(MockResponse().setBody("[null]"))
        expectUnavailable { api.fetchTrips(DemoScenario.CONNECTED) }
        server.enqueue(MockResponse().setResponseCode(204))
        expectUnavailable { api.fetchTrips(DemoScenario.CONNECTED) }
    }

    @Test fun mapsTransportIoToOffline() = runBlocking {
        val broken = object : TripCatalogApi {
            override suspend fun catalog(scenario: String): Response<List<TripDto?>> = throw IOException("Synthetic transport failure")
        }
        try { broken.fetchTrips(DemoScenario.CONNECTED); fail("Expected offline") }
        catch (_: SourceOfflineException) { }
    }

    @Test fun cancellationCancelsTheRetrofitCall() = runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val loading = async { api.fetchTrips(DemoScenario.CONNECTED) }
        withTimeout(5_000) {
            while (client.dispatcher.runningCallsCount() == 0) kotlinx.coroutines.yield()
        }
        loading.cancel()
        try { loading.await(); fail("Expected cancellation") }
        catch (_: CancellationException) { }
        withTimeout(5_000) {
            while (client.dispatcher.runningCallsCount() != 0) kotlinx.coroutines.yield()
        }
    }

    @Test fun bundledTransportExercisesSuccessOfflineAnd503() = runBlocking {
        val demo = createDemoApi()
        assertEquals(6, demo.fetchTrips(DemoScenario.CONNECTED).size)
        try { demo.fetchTrips(DemoScenario.OFFLINE); fail("Expected offline") }
        catch (_: SourceOfflineException) { }
        expectUnavailable { demo.fetchTrips(DemoScenario.SERVICE_ERROR) }
    }

    private suspend fun expectUnavailable(block: suspend () -> Unit) {
        try { block(); fail("Expected unavailable") }
        catch (_: SourceUnavailableException) { }
    }
}
