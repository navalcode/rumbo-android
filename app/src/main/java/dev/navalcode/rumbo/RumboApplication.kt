package dev.navalcode.rumbo

import android.app.Application
import androidx.room.Room
import dev.navalcode.rumbo.core.TripRepository
import dev.navalcode.rumbo.data.DemoTripSource
import dev.navalcode.rumbo.data.RoomTripStore
import dev.navalcode.rumbo.data.TripDatabase

class RumboApplication : Application() {
    val source by lazy { DemoTripSource() }
    val database by lazy {
        Room.databaseBuilder(this, TripDatabase::class.java, "rumbo.db").build()
    }
    val repository by lazy { TripRepository(source, RoomTripStore(database), System::currentTimeMillis) }
}
