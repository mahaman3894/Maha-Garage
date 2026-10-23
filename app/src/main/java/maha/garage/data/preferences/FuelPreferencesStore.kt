package maha.garage.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.fuelDataStore: DataStore<Preferences> by preferencesDataStore(name = "maha_fuel_preferences")

/**
 * 실주행 연비&주유 화면의 차량 고정(Pinning) 및 마지막 선택 차량 상태를 보존하는 DataStore 저장소
 */
class FuelPreferencesStore(private val context: Context) {

    companion object {
        val KEY_PINNED_VEHICLE_ID = stringPreferencesKey("pinned_vehicle_id")
        val KEY_SELECTED_VEHICLE_ID = stringPreferencesKey("selected_vehicle_id")

        @Volatile
        private var INSTANCE: FuelPreferencesStore? = null

        fun getInstance(context: Context): FuelPreferencesStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FuelPreferencesStore(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    val pinnedVehicleIdFlow: Flow<String?> = context.fuelDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs -> prefs[KEY_PINNED_VEHICLE_ID] }

    val selectedVehicleIdFlow: Flow<String?> = context.fuelDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs -> prefs[KEY_SELECTED_VEHICLE_ID] }

    suspend fun setPinnedVehicleId(vehicleId: String?) {
        context.fuelDataStore.edit { prefs ->
            if (vehicleId != null) {
                prefs[KEY_PINNED_VEHICLE_ID] = vehicleId
            } else {
                prefs.remove(KEY_PINNED_VEHICLE_ID)
            }
        }
    }

    suspend fun togglePin(vehicleId: String) {
        context.fuelDataStore.edit { prefs ->
            val current = prefs[KEY_PINNED_VEHICLE_ID]
            if (current == vehicleId) {
                prefs.remove(KEY_PINNED_VEHICLE_ID)
            } else {
                prefs[KEY_PINNED_VEHICLE_ID] = vehicleId
            }
        }
    }

    suspend fun setSelectedVehicleId(vehicleId: String) {
        context.fuelDataStore.edit { prefs ->
            prefs[KEY_SELECTED_VEHICLE_ID] = vehicleId
        }
    }
}
