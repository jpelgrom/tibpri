package nl.jpelgrom.tibpri.data

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.network.okHttpClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    fun provideApolloClient(): ApolloClient {
        return ApolloClient.Builder()
            .serverUrl("https://api.tibber.com/v1-beta/gql")
            .okHttpClient(
                OkHttpClient.Builder()
                    .addInterceptor(ApolloAuthorizationInterceptor())
                    .build()
            )
            .build()
    }
}