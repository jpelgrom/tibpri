package nl.jpelgrom.tibpri.data

import android.content.Context
import androidx.room.Room
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.network.okHttpClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import nl.jpelgrom.tibpri.data.database.TibpriDatabase
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
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

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TibpriDatabase =
        Room.databaseBuilder(
            context,
            TibpriDatabase::class.java,
            "tibpri.room.db"
        ).build()

    @Provides
    @Singleton
    fun providePreferences(@ApplicationContext context: Context): TibpriPreferences =
        TibpriPreferences(context)
}