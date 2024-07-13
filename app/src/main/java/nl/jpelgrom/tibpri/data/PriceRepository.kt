package nl.jpelgrom.tibpri.data

import com.apollographql.apollo.ApolloClient
import nl.jpelgrom.tibpri.EnergyPriceInfoQuery
import javax.inject.Inject

class PriceRepository @Inject constructor(
    private val apolloClient: ApolloClient
) {

    suspend fun getPriceInfo(): EnergyPriceInfoQuery.PriceInfo? {
        val data = apolloClient.query(EnergyPriceInfoQuery()).execute().data
        val priceInfo = data?.viewer?.homes?.get(0)?.currentSubscription?.priceInfo
        return priceInfo
    }

}