@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package dev.navalcode.rumbo.ui

import androidx.lifecycle.ViewModelStore
import dev.navalcode.rumbo.core.*
import dev.navalcode.rumbo.data.*
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class TripsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val models = ViewModelStore()
    private val store = MemoryStore()
    private val item = TripDto("alba", "Alba", "Sierra", "MOUNTAIN", 3, 100, 12, 0)
    private var action: suspend (String) -> Response<List<TripDto?>> = { Response.success(listOf(item)) }
    private val api = object : TripCatalogApi {
        override suspend fun catalog(scenario: String) = action(scenario)
    }

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { models.clear(); Dispatchers.resetMain() }

    private fun connectedModel(): TripsViewModel {
        val source = DemoTripSource(api)
        return TripsViewModel(TripRepository(source, store, { 42L }), source)
            .also { models.put("test", it) }
    }

    @Test fun initialLoadExposesUpdatingAndThenCachedCatalog() = runTest {
        val model = connectedModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.state.collect {} }
        runCurrent()
        assertEquals(SyncStatus.Updating, model.state.value.sync)
        advanceUntilIdle()
        assertEquals(SyncStatus.Ready, model.state.value.sync)
        assertEquals("alba", model.state.value.catalog.trips.single().id)
    }

    @Test fun userInputDoesNotStartNetworkWork() = runTest {
        var requests = 0
        action = { requests++; Response.success(listOf(item)) }
        val model = connectedModel()
        advanceUntilIdle()
        model.queryChanged("Alba")
        model.categoryChanged(Category.FOREST)
        runCurrent()
        assertEquals("Alba", model.query.value)
        assertEquals(Category.FOREST, model.category.value)
        assertEquals(1, requests)
    }

    @Test fun newerRefreshCancelsPreviousWorkAndWins() = runTest {
        var cancelled = false
        action = {
            try { delay(10_000); Response.success(listOf(item)) }
            finally { cancelled = true }
        }
        val model = connectedModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.state.collect {} }
        advanceTimeBy(700); runCurrent()
        action = { Response.success(listOf(item.copy(title = "Latest"))) }
        model.refresh()
        advanceUntilIdle()
        assertTrue(cancelled)
        assertEquals(SyncStatus.Ready, model.state.value.sync)
        assertEquals("Latest", model.state.value.catalog.trips.single().title)
    }

    @Test fun failedRefreshKeepsCacheAndRecoveryUpdatesState() = runTest {
        val model = connectedModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.state.collect {} }
        advanceUntilIdle()
        action = { throw IOException("Synthetic offline") }
        model.scenarioChanged(DemoScenario.OFFLINE)
        advanceUntilIdle()
        assertEquals(SyncStatus.Failed(SyncProblem.OFFLINE), model.state.value.sync)
        assertEquals("alba", model.state.value.catalog.trips.single().id)
        assertEquals(DemoScenario.OFFLINE, model.scenario.value)
        action = { Response.success(listOf(item)) }
        model.scenarioChanged(DemoScenario.CONNECTED)
        advanceUntilIdle()
        assertEquals(SyncStatus.Ready, model.state.value.sync)
    }

    @Test fun firstLoadFailureHasNoInventedCatalog() = runTest {
        action = { Response.error(503, "{}".toResponseBody("application/json".toMediaType())) }
        val model = connectedModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.state.collect {} }
        advanceUntilIdle()
        assertTrue(model.state.value.catalog.trips.isEmpty())
        assertEquals(SyncStatus.Failed(SyncProblem.UNAVAILABLE), model.state.value.sync)
    }

    @Test fun favoriteAndRepeatedReservationAreObserved() = runTest {
        val model = connectedModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.state.collect {} }
        advanceUntilIdle()
        model.favorite("alba", true)
        model.reserve("alba")
        model.reserve("alba")
        advanceUntilIdle()
        assertEquals(setOf("alba"), model.state.value.favorites)
        assertEquals(1, model.state.value.bookings.size)
        assertEquals("DEMO-ALBA", model.state.value.bookings.single().reference)
    }
}

private class MemoryStore : TripStore {
    override val catalog = MutableStateFlow(Catalog())
    override val favorites = MutableStateFlow<Set<String>>(emptySet())
    override val bookings = MutableStateFlow<List<Booking>>(emptyList())
    override suspend fun replaceCatalog(trips: List<Trip>, updatedAt: Long) { catalog.value = Catalog(trips, updatedAt) }
    override suspend fun setFavorite(tripId: String, saved: Boolean) {
        favorites.value = if (saved) favorites.value + tripId else favorites.value - tripId
    }
    override suspend fun saveBooking(booking: Booking): Booking {
        bookings.value.find { it.tripId == booking.tripId }?.let { return it }
        bookings.value += booking
        return booking
    }
}
