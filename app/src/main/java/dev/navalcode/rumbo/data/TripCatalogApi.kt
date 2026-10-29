package dev.navalcode.rumbo.data

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.annotations.SerializedName
import dev.navalcode.rumbo.core.Category
import dev.navalcode.rumbo.core.SourceOfflineException
import dev.navalcode.rumbo.core.SourceUnavailableException
import dev.navalcode.rumbo.core.Trip
import java.io.IOException
import kotlinx.coroutines.CancellationException
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header

/** Fictional REST contract. The header controls only the bundled demo transport. */
interface TripCatalogApi {
    @GET("v1/trips")
    suspend fun catalog(@Header("X-Demo-Scenario") scenario: String): Response<List<TripDto?>>
}

data class TripDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("region") val region: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("days") val days: Int? = null,
    @SerializedName("priceEuros") val priceEuros: Int? = null,
    @SerializedName("distanceKm") val distanceKm: Int? = null,
    @SerializedName("artwork") val artwork: Int? = null,
) {
    fun toTrip(): Trip {
        val safeId = requireNotNull(id).also { require(it.matches(Regex("[a-z0-9-]{1,40}"))) }
        val safeTitle = requireNotNull(title).also { require(it.isNotBlank() && it.length <= 120) }
        val safeRegion = requireNotNull(region).also { require(it.isNotBlank() && it.length <= 120) }
        return Trip(safeId, safeTitle, safeRegion, Category.valueOf(requireNotNull(category)),
            requireNotNull(days).also { require(it in 1..30) },
            requireNotNull(priceEuros).also { require(it in 0..100_000) },
            requireNotNull(distanceKm).also { require(it in 0..10_000) },
            requireNotNull(artwork).also { require(it in 0..5) })
    }
}

/** Converts transport/contract failures at the boundary; cancellation stays cancellation. */
suspend fun TripCatalogApi.fetchTrips(scenario: DemoScenario): List<Trip> = try {
    val response = catalog(scenario.name)
    if (!response.isSuccessful) throw SourceUnavailableException()
    val items = response.body() ?: throw SourceUnavailableException()
    require(items.size <= 100)
    val trips = items.map { requireNotNull(it).toTrip() }
    require(trips.map { it.id }.distinct().size == trips.size)
    trips
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: IOException) {
    throw SourceOfflineException()
} catch (_: JsonParseException) {
    throw SourceUnavailableException()
} catch (_: IllegalArgumentException) {
    throw SourceUnavailableException()
}

/** Every request is intercepted. No socket, account, API key or production endpoint. */
internal fun createDemoApi(): TripCatalogApi {
    val json = Gson().toJson(demoTrips.map {
        TripDto(it.id, it.title, it.region, it.category.name, it.days, it.priceEuros, it.distanceKm, it.artwork)
    })
    val transport = Interceptor { chain ->
        val request = chain.request()
        check(request.url.host == "demo.rumbo.invalid" && request.url.encodedPath == "/v1/trips")
        if (request.header("X-Demo-Scenario") == DemoScenario.OFFLINE.name) {
            throw IOException("Fictional offline scenario")
        }
        val code = if (request.header("X-Demo-Scenario") == DemoScenario.SERVICE_ERROR.name) 503 else 200
        okhttp3.Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
            .code(code).message(if (code == 200) "OK" else "Demo unavailable")
            .body((if (code == 200) json else "{}").toResponseBody("application/json".toMediaType()))
            .build()
    }
    return Retrofit.Builder().baseUrl("https://demo.rumbo.invalid/")
        .client(OkHttpClient.Builder().addInterceptor(transport).retryOnConnectionFailure(false).build())
        .addConverterFactory(GsonConverterFactory.create()).build().create(TripCatalogApi::class.java)
}
