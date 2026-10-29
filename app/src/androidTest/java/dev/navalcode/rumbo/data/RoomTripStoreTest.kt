package dev.navalcode.rumbo.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import dev.navalcode.rumbo.core.Booking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomTripStoreTest {
    @Test fun catalogFavoritesAndBookingSurviveDatabaseReopen() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "persistence-test.db"
        context.deleteDatabase(name)
        var db = Room.databaseBuilder(context, TripDatabase::class.java, name).build()
        try {
            RoomTripStore(db).apply {
                replaceCatalog(demoTrips, 1_234)
                setFavorite("alba", true)
                saveBooking(Booking("alba", "DEMO-ALBA", 2_000))
            }
            db.close()
            db = Room.databaseBuilder(context, TripDatabase::class.java, name).build()
            val reopened = RoomTripStore(db)
            assertEquals(demoTrips, reopened.catalog.first().trips)
            assertEquals(1_234L, reopened.catalog.first().updatedAt)
            assertEquals(setOf("alba"), reopened.favorites.first())
            assertEquals(listOf(Booking("alba", "DEMO-ALBA", 2_000)), reopened.bookings.first())
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun duplicateBookingKeepsOriginalReferenceAndTimestamp() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TripDatabase::class.java).build()
        try {
            val store = RoomTripStore(db)
            val first = store.saveBooking(Booking("alba", "DEMO-ALBA", 1_000))
            val repeated = store.saveBooking(Booking("alba", "OTHER-REFERENCE", 9_000))
            assertEquals(first, repeated)
            assertEquals(1, store.bookings.first().size)
        } finally { db.close() }
    }

    @Test fun cacheReplacementPreservesFavoritesAndBookings() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TripDatabase::class.java).build()
        try {
            val store = RoomTripStore(db)
            store.replaceCatalog(demoTrips, 1_000)
            store.setFavorite("alba", true)
            store.saveBooking(Booking("alba", "DEMO-ALBA", 1_100))
            store.replaceCatalog(demoTrips.reversed(), 2_000)
            assertEquals(demoTrips.reversed(), store.catalog.first().trips)
            assertEquals(setOf("alba"), store.favorites.first())
            assertEquals(1, store.bookings.first().size)
        } finally { db.close() }
    }
}
