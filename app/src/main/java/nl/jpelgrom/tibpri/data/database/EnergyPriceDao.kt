package nl.jpelgrom.tibpri.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface EnergyPriceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(prices: List<HourlyEnergyPrice>)

    @Query("DELETE FROM prices WHERE startsAt IN (:ids)")
    suspend fun delete(ids: List<String>)

    @Query("SELECT * FROM prices")
    suspend fun getAll(): List<HourlyEnergyPrice>

}