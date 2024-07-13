package nl.jpelgrom.tibpri.data

import nl.jpelgrom.tibpri.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response

class ApolloAuthorizationInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .apply {
                addHeader("Authorization", "Bearer ${BuildConfig.TIBBER_TOKEN}")
            }
            .build()
        return chain.proceed(request)
    }
}