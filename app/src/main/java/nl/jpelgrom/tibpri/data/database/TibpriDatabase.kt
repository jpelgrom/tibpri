package nl.jpelgrom.tibpri.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        HourlyEnergyPrice::class
    ],
    version = 1
)
abstract class TibpriDatabase : RoomDatabase() {
    abstract fun energyPriceDao(): EnergyPriceDao
}