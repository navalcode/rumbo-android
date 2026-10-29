package dev.navalcode.rumbo.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import dev.navalcode.rumbo.core.Booking
import dev.navalcode.rumbo.core.Catalog
import dev.navalcode.rumbo.core.Category
import dev.navalcode.rumbo.core.Trip
import dev.navalcode.rumbo.core.TripStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey val id: String,
    val title: String,
    val region: String,
    val category: String,
    val days: Int,
    val priceEuros: Int,
    val distanceKm: Int,
    val artwork: Int,
    val updatedAt: Long,
    val position: Int,
)

@Entity(tableName = "favorites")
data class FavoriteEntity(@PrimaryKey val tripId: String)

@Entity(tableName = "bookings", indices = [Index(value = ["tripId"], unique = true)])
data class BookingEntity(@PrimaryKey val reference: String, val tripId: String, val createdAt: Long)

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY position") fun observeTrips(): Flow<List<TripEntity>>
    @Query("SELECT tripId FROM favorites") fun observeFavorites(): Flow<List<String>>
    @Query("SELECT * FROM bookings ORDER BY createdAt DESC, reference") fun observeBookings(): Flow<List<BookingEntity>>
    @Query("DELETE FROM trips") suspend fun clearTrips()
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTrips(trips: List<TripEntity>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertFavorite(favorite: FavoriteEntity)
    @Query("DELETE FROM favorites WHERE tripId = :tripId") suspend fun deleteFavorite(tripId: String)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertBooking(booking: BookingEntity)
    @Query("SELECT * FROM bookings WHERE tripId = :tripId") suspend fun getBooking(tripId: String): BookingEntity
}

@Database(entities = [TripEntity::class, FavoriteEntity::class, BookingEntity::class], version = 1, exportSchema = true)
abstract class TripDatabase : RoomDatabase() { abstract fun trips(): TripDao }

class RoomTripStore(private val database: TripDatabase) : TripStore {
    private val dao = database.trips()
    override val catalog = dao.observeTrips().map { rows ->
        Catalog(rows.map { it.toTrip() }, rows.firstOrNull()?.updatedAt)
    }
    override val favorites = dao.observeFavorites().map { it.toSet() }
    override val bookings = dao.observeBookings().map { rows -> rows.map { it.toBooking() } }

    override suspend fun replaceCatalog(trips: List<Trip>, updatedAt: Long) = database.withTransaction {
        dao.clearTrips()
        dao.insertTrips(trips.mapIndexed { index, trip ->
            TripEntity(trip.id, trip.title, trip.region, trip.category.name, trip.days,
                trip.priceEuros, trip.distanceKm, trip.artwork, updatedAt, index)
        })
    }

    override suspend fun setFavorite(tripId: String, saved: Boolean) {
        if (saved) dao.insertFavorite(FavoriteEntity(tripId)) else dao.deleteFavorite(tripId)
    }

    override suspend fun saveBooking(booking: Booking): Booking = database.withTransaction {
        dao.insertBooking(BookingEntity(booking.reference, booking.tripId, booking.createdAt))
        dao.getBooking(booking.tripId).toBooking()
    }
}

private fun TripEntity.toTrip() = Trip(id, title, region, Category.valueOf(category), days, priceEuros, distanceKm, artwork)
private fun BookingEntity.toBooking() = Booking(tripId, reference, createdAt)
