package nl.jpelgrom.tibpri.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class TibpriPreferences @Inject constructor(@ApplicationContext val context: Context) {

    private val Context.dataStore by preferencesDataStore("tibpri.datastore")

    companion object {
        private val LAST_FETCHED_DATA = longPreferencesKey("last_fetched_data")
    }

    /** @return the last time data was fetched, or -1 if never fetched */
    suspend fun getLastFetchedData() =
        context.dataStore.data.firstOrNull()?.get(LAST_FETCHED_DATA) ?: -1

    /** Update the last time data was fetched */
    suspend fun setLastFetchedData(time: Long = System.currentTimeMillis()) {
        context.dataStore.edit { it[LAST_FETCHED_DATA] = time }
    }
}