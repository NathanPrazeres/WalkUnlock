package com.gmail.nathanprazeres.walkunlock.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gmail.nathanprazeres.walkunlock.models.LockedApp
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.ByteArrayOutputStream

private val Context.dataStore by preferencesDataStore(name = "locked_apps_prefs")

class BitmapTypeAdapter : TypeAdapter<Bitmap>() {
    override fun write(out: JsonWriter, value: Bitmap?) {
        if (value == null) {
            out.nullValue()
            return
        }

        val byteArrayOutputStream = ByteArrayOutputStream()
        value.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        val base64String = Base64.encodeToString(byteArray, Base64.DEFAULT)
        out.value(base64String)
    }

    override fun read(reader: JsonReader): Bitmap? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }

        val base64String = reader.nextString()
        val byteArray = Base64.decode(base64String, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }
}

class LockedAppManager(private val context: Context) {

    private val gson = GsonBuilder()
        .registerTypeAdapter(Bitmap::class.java, BitmapTypeAdapter())
        .create()

    private val lockedAppsKey = stringPreferencesKey("locked_apps")

    val lockedAppsFlow: Flow<List<LockedApp>> = context.dataStore.data
        .map { preferences ->
            preferences[lockedAppsKey]?.let { json ->
                try {
                    val type = object : TypeToken<List<LockedApp>>() {}.type
                    gson.fromJson(json, type)
                } catch (_: Exception) {
                    // If deserialization fails, return empty list and clear corrupted data
                    emptyList()
                }
            } ?: emptyList()
        }

    suspend fun addLockedApp(newApp: LockedApp) {
        context.dataStore.edit { preferences ->
            val currentApps = preferences[lockedAppsKey]?.let { json ->
                try {
                    gson.fromJson<List<LockedApp>>(
                        json,
                        object : TypeToken<List<LockedApp>>() {}.type
                    )
                } catch (_: Exception) {
                    emptyList()
                }
            } ?: emptyList()

            // Avoid duplicates package name
            val updatedApps = currentApps.toMutableList()
                .apply {
                    removeAll { it.packageName == newApp.packageName }
                    add(newApp)
                }

            preferences[lockedAppsKey] = gson.toJson(updatedApps)
        }
    }

    suspend fun removeLockedApp(packageName: String) {
        context.dataStore.edit { preferences ->
            val currentApps = preferences[lockedAppsKey]?.let { json ->
                try {
                    gson.fromJson<List<LockedApp>>(
                        json,
                        object : TypeToken<List<LockedApp>>() {}.type
                    )
                } catch (_: Exception) {
                    emptyList()
                }
            } ?: emptyList()

            val updatedApps = currentApps.filterNot { it.packageName == packageName }
            preferences[lockedAppsKey] = gson.toJson(updatedApps)
        }
    }

    // TODO: remove this method, only for testing
    suspend fun clearLockedApps() {
        context.dataStore.edit { preferences ->
            preferences.remove(lockedAppsKey)
        }
    }
}
