package nl.jpelgrom.tibpri.data

import com.apollographql.apollo.ApolloClient
import nl.jpelgrom.tibpri.BuildConfig
import nl.jpelgrom.tibpri.EnergyPriceInfoQuery
import nl.jpelgrom.tibpri.data.database.HourlyEnergyPrice
import nl.jpelgrom.tibpri.data.database.TibpriDatabase
import nl.jpelgrom.tibpri.data.database.todayAndTomorrow
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject


class PriceRepository @Inject constructor(
    private val apolloClient: ApolloClient,
    private val database: TibpriDatabase
) {

    /** @return a list of [HourlyEnergyPrice] for today and tomorrow (if available) */
    suspend fun getPrices(): List<HourlyEnergyPrice> {
        getAndCachePriceInfo()
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
        } // else no data due to network error, API error, ...
    }

    /** Delete any prices stored in the database from before today (at the home's time zone) */
    private suspend fun clearOldPriceInfo() {
        val nowAtHome = ZonedDateTime.now(ZoneId.of(BuildConfig.TIBBER_HOME_TZ))
        val startOfDay = nowAtHome.toLocalDate().atStartOfDay(nowAtHome.zone).toOffsetDateTime()

        val dao = database.energyPriceDao()
        dao.getAll().filter {
            OffsetDateTime.parse(it.startsAt).isBefore(startOfDay)
        }.map { it.startsAt }.ifEmpty { null }?.let { dao.delete(it) }
    }

}