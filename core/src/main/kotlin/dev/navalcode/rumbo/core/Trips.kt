package dev.navalcode.rumbo.core

import java.text.Normalizer
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

enum class Category { MOUNTAIN, COAST, FOREST }

data class Trip(
    val id: String,
    val title: String,
    val region: String,
    val category: Category,
    val days: Int,
    val priceEuros: Int,
    val distanceKm: Int,
    val artwork: Int,
)

data class Booking(val tripId: String, val reference: String, val createdAt: Long)
data class Catalog(val trips: List<Trip> = emptyList(), val updatedAt: Long? = null)

interface TripSource { suspend fun fetch(): List<Trip> }

interface TripStore {
    val catalog: Flow<Catalog>
    val favorites: Flow<Set<String>>
    val bookings: Flow<List<Booking>>
    suspend fun replaceCatalog(trips: List<Trip>, updatedAt: Long)
    suspend fun setFavorite(tripId: String, saved: Boolean)
    suspend fun saveBooking(booking: Booking): Booking
}

class SourceOfflineException : Exception()
class SourceUnavailableException : Exception()
enum class SyncProblem { OFFLINE, UNAVAILABLE, TIMEOUT }
sealed interface SyncStatus {
    data object Idle : SyncStatus
    data object Updating : SyncStatus
    data object Ready : SyncStatus
    data class Failed(val problem: SyncProblem) : SyncStatus
}

class TripRepository(
    private val source: TripSource,
    private val store: TripStore,
    private val clock: () -> Long,
    private val timeoutMillis: Long = 3_500,
) {
    private val refreshLock = Mutex()
    private val bookingLock = Mutex()
    private val mutableSync = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val sync: StateFlow<SyncStatus> = mutableSync.asStateFlow()
    val catalog = store.catalog
    val favorites = store.favorites
    val bookings = store.bookings

    suspend fun refresh() = refreshLock.withLock {
        mutableSync.value = SyncStatus.Updating
        try {
            val trips = withTimeout(timeoutMillis) { source.fetch() }
            store.replaceCatalog(trips, clock())
            mutableSync.value = SyncStatus.Ready
        } catch (timeout: TimeoutCancellationException) {
            if (!currentCoroutineContext().isActive) {
                mutableSync.value = SyncStatus.Idle
                throw timeout
            }
            mutableSync.value = SyncStatus.Failed(SyncProblem.TIMEOUT)
        } catch (cancelled: CancellationException) {
            mutableSync.value = SyncStatus.Idle
            throw cancelled
        } catch (_: SourceOfflineException) {
            mutableSync.value = SyncStatus.Failed(SyncProblem.OFFLINE)
        } catch (_: SourceUnavailableException) {
            mutableSync.value = SyncStatus.Failed(SyncProblem.UNAVAILABLE)
        } catch (failure: Exception) {
            mutableSync.value = SyncStatus.Idle
            throw failure
        }
    }

    suspend fun setFavorite(tripId: String, saved: Boolean) = store.setFavorite(tripId, saved)

    suspend fun reserve(tripId: String): Booking = bookingLock.withLock {
        require(store.catalog.first().trips.any { it.id == tripId }) { "Unknown demo trip" }
        store.saveBooking(Booking(tripId, "DEMO-${tripId.uppercase(Locale.ROOT)}", clock()))
    }
}

fun filterTrips(
    trips: List<Trip>,
    query: String,
    category: Category?,
    favoriteIds: Set<String>? = null,
): List<Trip> {
    val needle = query.searchKey()
    return trips.filter { trip ->
        (category == null || trip.category == category) &&
            (favoriteIds == null || trip.id in favoriteIds) &&
            (needle.isBlank() || "${trip.title} ${trip.region}".searchKey().contains(needle))
    }
}

private fun String.searchKey() = Normalizer.normalize(trim(), Normalizer.Form.NFD)
    .replace("\\p{M}+".toRegex(), "").lowercase(Locale.ROOT)
