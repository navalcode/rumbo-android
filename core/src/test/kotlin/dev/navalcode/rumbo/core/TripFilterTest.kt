package dev.navalcode.rumbo.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TripFilterTest {
    private val trips = listOf(
        Trip("a", "Cumbres de Ámbar", "Sierra Clara", Category.MOUNTAIN, 4, 210, 26, 0),
        Trip("b", "Costa Serena", "Bahía del Sol", Category.COAST, 2, 110, 8, 1),
        Trip("c", "Bosque Azul", "Valle Verde", Category.FOREST, 2, 95, 12, 2),
    )
    @Test fun `query ignores accents case and outer spaces`() {
        assertEquals(listOf(trips[0]), filterTrips(trips, "  AMBAR  ", null))
    }
    @Test fun `region is searchable`() {
        assertEquals(listOf(trips[1]), filterTrips(trips, "bahia", null))
    }
    @Test fun `category and query are combined`() {
        assertTrue(filterTrips(trips, "costa", Category.MOUNTAIN).isEmpty())
    }
    @Test fun `favorites restrict results without changing their order`() {
        assertEquals(listOf(trips[0], trips[2]), filterTrips(trips, "", null, setOf("c", "a")))
    }
    @Test fun `clearing filters restores the full catalog`() {
        assertEquals(trips, filterTrips(trips, "", null))
    }
    @Test fun `empty favorite set returns no results`() {
        assertTrue(filterTrips(trips, "", null, emptySet()).isEmpty())
    }
}
