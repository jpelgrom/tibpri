package nl.jpelgrom.tibpri.data

import nl.jpelgrom.tibpri.EnergyPriceInfoQuery
import nl.jpelgrom.tibpri.type.PriceLevel

data class HourlyEnergyPrice(
    val total: Double?,
    val energy: Double?,
    val tax: Double?,
    val startsAt: String?,
    val level: PriceLevel?,
    val currency: String?
)

fun EnergyPriceInfoQuery.PriceInfo.todayAndTomorrow(): List<HourlyEnergyPrice> =
    today.mapNotNull { it?.asGeneric() } + tomorrow.mapNotNull { it?.asGeneric() }

fun EnergyPriceInfoQuery.Today.asGeneric() =
    HourlyEnergyPrice(total, energy, tax, startsAt, level, currency)

fun EnergyPriceInfoQuery.Tomorrow.asGeneric() =
    HourlyEnergyPrice(total, energy, tax, startsAt, level, currency)
