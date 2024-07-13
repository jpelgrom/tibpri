package nl.jpelgrom.tibpri.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import nl.jpelgrom.tibpri.EnergyPriceInfoQuery
import nl.jpelgrom.tibpri.type.PriceLevel

@Entity(tableName = "prices")
data class HourlyEnergyPrice(
    @PrimaryKey val startsAt: String,
    val total: Double,
    val energy: Double,
    val tax: Double,
    val level: PriceLevel,
    val currency: String
)

fun EnergyPriceInfoQuery.PriceInfo.todayAndTomorrow(): List<HourlyEnergyPrice> =
    today.mapNotNull { it?.asGeneric() } + tomorrow.mapNotNull { it?.asGeneric() }

fun EnergyPriceInfoQuery.Today.asGeneric() =
    if (total != null && energy != null && tax != null && startsAt != null && level != null) {
        HourlyEnergyPrice(startsAt, total, energy, tax, level, currency)
    } else {
        null
    }

fun EnergyPriceInfoQuery.Tomorrow.asGeneric() =
    if (total != null && energy != null && tax != null && startsAt != null && level != null) {
        HourlyEnergyPrice(startsAt, total, energy, tax, level, currency)
    } else {
        null
    }
