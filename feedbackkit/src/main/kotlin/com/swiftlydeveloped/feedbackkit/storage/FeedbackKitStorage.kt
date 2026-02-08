package com.swiftlydeveloped.feedbackkit.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.feedbackKitDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "feedbackkit_preferences"
)

/**
 * Persistent storage for FeedbackKit SDK data.
 */
class FeedbackKitStorage internal constructor(
    private val context: Context
) {
    private val dataStore: DataStore<Preferences> = context.feedbackKitDataStore

    /**
     * Flow of the stored user ID.
     */
    val userIdFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[USER_ID_KEY]
    }

    /**
     * Flow of the stored user email.
     */
    val userEmailFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[USER_EMAIL_KEY]
    }

    /**
     * Flow of the stored user name.
     */
    val userNameFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[USER_NAME_KEY]
    }

    /**
     * Get the stored user ID synchronously.
     */
    suspend fun getUserId(): String? = userIdFlow.first()

    /**
     * Get the stored user email synchronously.
     */
    suspend fun getUserEmail(): String? = userEmailFlow.first()

    /**
     * Get the stored user name synchronously.
     */
    suspend fun getUserName(): String? = userNameFlow.first()

    /**
     * Save the user ID.
     */
    suspend fun setUserId(userId: String?) {
        dataStore.edit { preferences ->
            if (userId != null) {
                preferences[USER_ID_KEY] = userId
            } else {
                preferences.remove(USER_ID_KEY)
            }
        }
    }

    /**
     * Save the user email.
     */
    suspend fun setUserEmail(email: String?) {
        dataStore.edit { preferences ->
            if (email != null) {
                preferences[USER_EMAIL_KEY] = email
            } else {
                preferences.remove(USER_EMAIL_KEY)
            }
        }
    }

    /**
     * Save the user name.
     */
    suspend fun setUserName(name: String?) {
        dataStore.edit { preferences ->
            if (name != null) {
                preferences[USER_NAME_KEY] = name
            } else {
                preferences.remove(USER_NAME_KEY)
            }
        }
    }

    /**
     * Save user info.
     */
    suspend fun setUserInfo(userId: String?, email: String?, name: String?) {
        dataStore.edit { preferences ->
            if (userId != null) {
                preferences[USER_ID_KEY] = userId
            } else {
                preferences.remove(USER_ID_KEY)
            }

            if (email != null) {
                preferences[USER_EMAIL_KEY] = email
            } else {
                preferences.remove(USER_EMAIL_KEY)
            }

            if (name != null) {
                preferences[USER_NAME_KEY] = name
            } else {
                preferences.remove(USER_NAME_KEY)
            }
        }
    }

    /**
     * Clear all stored data.
     */
    suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    companion object {
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
    }
}
