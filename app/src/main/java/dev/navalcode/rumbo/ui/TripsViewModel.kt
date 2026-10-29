package dev.navalcode.rumbo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.navalcode.rumbo.core.Booking
import dev.navalcode.rumbo.core.Catalog
import dev.navalcode.rumbo.core.Category
import dev.navalcode.rumbo.core.SyncStatus
import dev.navalcode.rumbo.core.TripRepository
import dev.navalcode.rumbo.data.DemoScenario
import dev.navalcode.rumbo.data.DemoTripSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TripsState(
    val catalog: Catalog = Catalog(),
    val favorites: Set<String> = emptySet(),
    val bookings: List<Booking> = emptyList(),
    val sync: SyncStatus = SyncStatus.Idle,
)

class TripsViewModel(private val repository: TripRepository, private val source: DemoTripSource) : ViewModel() {
    private var refreshJob: Job? = null
    private val mutableQuery = MutableStateFlow("")
    val query = mutableQuery.asStateFlow()
    private val mutableCategory = MutableStateFlow<Category?>(null)
    val category = mutableCategory.asStateFlow()
    val scenario = source.scenario
    val state = combine(repository.catalog, repository.favorites, repository.bookings, repository.sync) {
        catalog, favorites, bookings, sync -> TripsState(catalog, favorites, bookings, sync)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TripsState())

    init { refresh() }

    fun queryChanged(value: String) { mutableQuery.value = value }

    fun categoryChanged(value: Category?) { mutableCategory.value = value }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch { repository.refresh() }
    }

    fun scenarioChanged(value: DemoScenario) {
        source.setScenario(value)
        refresh()
    }

    fun favorite(tripId: String, saved: Boolean) {
        viewModelScope.launch { repository.setFavorite(tripId, saved) }
    }

    fun reserve(tripId: String) {
        viewModelScope.launch { repository.reserve(tripId) }
    }
}
