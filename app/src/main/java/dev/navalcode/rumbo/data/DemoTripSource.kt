package dev.navalcode.rumbo.data

import dev.navalcode.rumbo.core.Category
import dev.navalcode.rumbo.core.Trip
import dev.navalcode.rumbo.core.TripSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class DemoScenario { CONNECTED, OFFLINE, SERVICE_ERROR, SLOW }

class DemoTripSource(private val api: TripCatalogApi = createDemoApi()) : TripSource {
    private val mutableScenario = MutableStateFlow(DemoScenario.CONNECTED)
    val scenario = mutableScenario.asStateFlow()
    fun setScenario(value: DemoScenario) { mutableScenario.value = value }

    override suspend fun fetch(): List<Trip> {
        val selected = scenario.value
        delay(if (selected == DemoScenario.SLOW) 7_000 else 650)
        return api.fetchTrips(selected)
    }
}

val demoTrips = listOf(
    Trip("alba", "Valle del Alba", "Sierra Clara", Category.MOUNTAIN, 3, 145, 18, 0),
    Trip("serena", "Costa Serena", "Bahía del Sol", Category.COAST, 2, 110, 8, 1),
    Trip("bruma", "Bosque de Bruma", "Tierras del Norte", Category.FOREST, 2, 95, 12, 2),
    Trip("cumbres", "Cumbres de Ámbar", "Sierra Clara", Category.MOUNTAIN, 4, 210, 26, 3),
    Trip("calas", "Camino de las Calas", "Costa de Nácar", Category.COAST, 3, 165, 16, 4),
    Trip("robles", "Refugio de los Robles", "Valle Verde", Category.FOREST, 2, 125, 10, 5),
)
