package com.hbde.courseschedule.importer.webview

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.cookieDataStore: DataStore<Preferences> by preferencesDataStore(name = "web_cookies")

/**
 * 基于 DataStore 的 Cookie 持久化存储
 */
@Singleton
class DataStoreCookieStore @Inject constructor(
    @ApplicationContext private val context: Context
) : CookieStore {

    private val dataStore = context.cookieDataStore

    override suspend fun saveCookies(domain: String, cookies: String) {
        val key = stringPreferencesKey("cookies_$domain")
        dataStore.edit { preferences ->
            preferences[key] = cookies
        }
    }

    override suspend fun getCookies(domain: String): String? {
        val key = stringPreferencesKey("cookies_$domain")
        return dataStore.data.map { preferences ->
            preferences[key]
        }.first()
    }

    override suspend fun clearCookies(domain: String) {
        val key = stringPreferencesKey("cookies_$domain")
        dataStore.edit { preferences ->
            preferences.remove(key)
        }
    }

    /**
     * 清除所有保存的 Cookies
     */
    suspend fun clearAllCookies() {
        dataStore.edit { preferences ->
            preferences.asMap().keys.filter { it.name.startsWith("cookies_") }
                .forEach { preferences.remove(it) }
        }
    }
}
