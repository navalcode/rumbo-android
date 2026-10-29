package dev.navalcode.rumbo.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TripRepositoryTest {
    private val trip = Trip("alba", "Valle del Alba", "Sierra Clara", Category.MOUNTAIN, 3, 145, 18, 0)

    @Test fun `successful refresh persists data and timestamp`() = runTest {
        val store = MemoryTripStore()
        val repository = TripRepository(source { listOf(trip) }, store, { 1_234 })
        repository.refresh()
        assertEquals(Catalog(listOf(trip), 1_234), store.catalog.value)
        assertEquals(SyncStatus.Ready, repository.sync.value)
    }

    @Test fun `offline refresh preserves previously saved catalog and timestamp`() = runTest {
        val store = MemoryTripStore(Catalog(listOf(trip), 100))
        val repository = TripRepository(source { throw SourceOfflineException() }, store, { 200 })
        repository.refresh()
        assertEquals(Catalog(listOf(trip), 100), store.catalog.value)
        assertEquals(SyncStatus.Failed(SyncProblem.OFFLINE), repository.sync.value)
    }

    @Test fun `unavailable source without cache exposes recoverable failure`() = runTest {
        val store = MemoryTripStore()
        val repository = TripRepository(source { throw SourceUnavailableException() }, store, { 200 })
        repository.refresh()
        assertTrue(store.catalog.value.trips.isEmpty())
        assertEquals(SyncStatus.Failed(SyncProblem.UNAVAILABLE), repository.sync.value)
    }

    @Test fun `timeout cancels source and retains cache`() = runTest {
        var sourceCancelled = false
        val store = MemoryTripStore(Catalog(listOf(trip), 100))
        val repository = TripRepository(source {
            try { delay(9_000); emptyList() } finally { sourceCancelled = true }
        }, store, { 200 }, timeoutMillis = 50)
        repository.refresh()
        assertTrue(sourceCancelled)
        assertEquals(Catalog(listOf(trip), 100), store.catalog.value)
        assertEquals(SyncStatus.Failed(SyncProblem.TIMEOUT), repository.sync.value)
    }

    @Test fun `caller cancellation does not become a service failure`() = runTest {
        val store = MemoryTripStore()
        val repository = TripRepository(source { delay(9_000); listOf(trip) }, store, { 200 })
        val job = launch { repository.refresh() }
        runCurrent()
        assertEquals(SyncStatus.Updating, repository.sync.value)
        job.cancelAndJoin()
        assertTrue(job.isCancelled)
        assertEquals(SyncStatus.Idle, repository.sync.value)
        assertTrue(store.catalog.value.trips.isEmpty())
    }

    @Test fun `recovery after a failure replaces the old cache`() = runTest {
        var offline = true
        val store = MemoryTripStore()
        val repository = TripRepository(source {
            if (offline) throw SourceOfflineException() else listOf(trip)
        }, store, { 200 })
        repository.refresh()
        offline = false
        repository.refresh()
        assertEquals(SyncStatus.Ready, repository.sync.value)
        assertEquals(listOf(trip), store.catalog.value.trips)
    }

    @Test fun `caller timeout propagates instead of being mistaken for source timeout`() = runTest {
        val store = MemoryTripStore()
        val repository = TripRepository(source { delay(9_000); listOf(trip) }, store, { 200 })
        try {
            withTimeout(20) { repository.refresh() }
            fail("Expected caller cancellation to propagate")
        } catch (_: TimeoutCancellationException) {
            assertEquals(SyncStatus.Idle, repository.sync.value)
            assertTrue(store.catalog.value.trips.isEmpty())
        }
    }

    @Test fun `concurrent duplicate reservations return the same stored booking`() = runTest {
        var time = 100L
        val store = MemoryTripStore(Catalog(listOf(trip), 10))
        val repository = TripRepository(source { emptyList() }, store, { time++ })
        val results = List(8) { async { repository.reserve(trip.id) } }.awaitAll()
        assertEquals(1, store.bookings.value.size)
        assertEquals(1, results.distinct().size)
        assertEquals("DEMO-ALBA", results.first().reference)
    }

    @Test fun `unknown trip cannot produce a booking`() = runTest {
        val store = MemoryTripStore()
        val repository = TripRepository(source { emptyList() }, store, { 100 })
        try {
            repository.reserve("missing")
            fail("Expected an invalid trip to be rejected")
        } catch (_: IllegalArgumentException) {
            assertTrue(store.bookings.value.isEmpty())
        }
    }

    @Test fun `favorites can be changed while source is offline`() = runTest {
        val store = MemoryTripStore(Catalog(listOf(trip), 100))
        val repository = TripRepository(source { throw SourceOfflineException() }, store, { 200 })
        repository.refresh()
        repository.setFavorite(trip.id, true)
        assertEquals(setOf("alba"), store.favorites.value)
        repository.setFavorite(trip.id, false)
        assertTrue(store.favorites.value.isEmpty())
    }

    private fun source(fetch: suspend () -> List<Trip>) = object : TripSource {
        override suspend fun fetch() = fetch.invoke()
    }
}

private class MemoryTripStore(initial: Catalog = Catalog()) : TripStore {
    override val catalog = MutableStateFlow(initial)
    override val favorites = MutableStateFlow<Set<String>>(emptySet())
    override val bookings = MutableStateFlow<List<Booking>>(emptyList())
    override suspend fun replaceCatalog(trips: List<Trip>, updatedAt: Long) { catalog.value = Catalog(trips, updatedAt) }
    override suspend fun setFavorite(tripId: String, saved: Boolean) {
        favorites.value = if (saved) favorites.value + tripId else favorites.value - tripId
    }
    override suspend fun saveBooking(booking: Booking): Booking {
        val existing = bookings.value.find { it.tripId == booking.tripId }
        if (existing != null) return existing
        bookings.value += booking
        return booking
    }
}
