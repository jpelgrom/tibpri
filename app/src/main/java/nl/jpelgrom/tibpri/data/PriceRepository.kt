package nl.jpelgrom.tibpri.data

import android.util.Log
import com.apollographql.apollo.ApolloClient
import nl.jpelgrom.tibpri.BuildConfig
import nl.jpelgrom.tibpri.EnergyPriceInfoQuery
import nl.jpelgrom.tibpri.data.database.HourlyEnergyPrice
import nl.jpelgrom.tibpri.data.database.TibpriDatabase
import nl.jpelgrom.tibpri.data.database.todayAndTomorrow
import java.time.Instant
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import javax.inject.Inject


class PriceRepository @Inject constructor(
    private val apolloClient: ApolloClient,
    private val database: TibpriDatabase,
    private val preferences: TibpriPreferences
) {

    companion object {
        private const val TAG = "PriceRepository"
    }

    /** @return a list of [HourlyEnergyPrice] for today and tomorrow (if available) */
    suspend fun getPrices(): List<HourlyEnergyPrice> {
        // Update the price info from the API if:
        // - the last update was more than 6 hours ago (in case of any changes)
        // - it is after 13:00 CET and we don't have prices for tomorrow
        val lastFetchedData = preferences.getLastFetchedData()
        if (
            lastFetchedData < Instant.now().minus(6, ChronoUnit.HOURS).toEpochMilli() ||
            (LocalTime.now(ZoneId.of("Europe/Amsterdam")).hour >= 13 && !latestPriceIsForTomorrow())
        ) {
            Log.i(TAG, "Trying to update price data from the API (last update: $lastFetchedData)")
            getAndCachePriceInfo()
        } else {
            Log.i(TAG, "Using cached data, last update was recent enough ($lastFetchedData)")
        }

        clearOldPriceInfo()
        return database.energyPriceDao().getAll()
    }

    /** Get the latest prices from the API and if successful store them in the database*/
    private suspend fun getAndCachePriceInfo() {
        val data = apolloClient.query(EnergyPriceInfoQuery()).execute().data
        val priceInfo = data?.viewer?.homes?.get(0)?.currentSubscription?.priceInfo

        if (priceInfo != null) {
            val allPrices = priceInfo.todayAndTomorrow()

            val dao = database.energyPriceDao()
            val oldPrices = dao.getAll().filter { stored ->
                allPrices.none { fetched -> fetched.startsAt == stored.startsAt }
            }
            dao.delete(oldPrices.map { it.startsAt })
            dao.insertAll(allPrices)
            preferences.setLastFetchedData()
        } // else no data due to network error, API error, ...
    }

    /** Delete any prices stored in the database before today (at the home's time zone) */
    private suspend fun clearOldPriceInfo() {
        val nowAtHome = ZonedDateTime.now(ZoneId.of(BuildConfig.TIBBER_HOME_TZ))
        val startOfDay = nowAtHome.toLocalDate().atStartOfDay(nowAtHome.zone).toOffsetDateTime()

        val dao = database.energyPriceDao()
        dao.getAll().filter {
            OffsetDateTime.parse(it.startsAt).isBefore(startOfDay)
        }.map { it.startsAt }.ifEmpty { null }?.let { dao.delete(it) }
    }

    private suspend fun latestPriceIsForTomorrow(): Boolean {
        val latestPrice = database.energyPriceDao().getLatest() ?: return false
        val nowAtHome = OffsetDateTime.now(ZoneId.of(BuildConfig.TIBBER_HOME_TZ))
        return try {
            OffsetDateTime.parse(latestPrice.startsAt).toLocalDate()
                .isEqual(nowAtHome.toLocalDate().plusDays(1))
        } catch (e: DateTimeParseException) {
            false
        }
    }

}