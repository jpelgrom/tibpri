package nl.jpelgrom.tibpri.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface EnergyPriceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(prices: List<HourlyEnergyPrice>)

    @Query("DELETE FROM prices WHERE startsAt IN (:ids)")
    suspend fun delete(ids: List<String>)

    @Query("DELETE FROM prices")
    fun deleteAll()

    @Transaction
    suspend fun replaceAll(newData: List<HourlyEnergyPrice>) {
        deleteAll()
        insertAll(newData)
    }

    @Query("SELECT * FROM prices")
    suspend fun getAll(): List<HourlyEnergyPrice>

    @Query("SELECT * FROM prices ORDER BY startsAt DESC LIMIT 1")
    suspend fun getLatest(): HourlyEnergyPrice?
}