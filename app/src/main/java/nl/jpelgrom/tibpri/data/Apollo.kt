package nl.jpelgrom.tibpri.data

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.network.okHttpClient
import nl.jpelgrom.tibpri.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response

val apolloClient = ApolloClient.Builder()
    .serverUrl("https://api.tibber.com/v1-beta/gql")
    .okHttpClient(
        OkHttpClient.Builder()
            .addInterceptor(AuthorizationInterceptor())
            .build()
    )
    .build()

private class AuthorizationInterceptor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .apply {
                addHeader("Authorization", "Bearer ${BuildConfig.TIBBER_TOKEN}")
            }
            .build()
        return chain.proceed(request)
    }
}