package com.jobtracker.data.local.secure

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypted (AES-256-GCM) local cache for the user's extracted CV text.
 *
 * CV content is personally identifiable — full name, contact details, address,
 * and work history — so it is stored via EncryptedSharedPreferences rather than
 * plain SharedPreferences or DataStore, so it doesn't sit as plaintext on disk
 * between analysis sessions. Keys are encrypted with AES-256-SIV, values with
 * AES-256-GCM.
 */
@Singleton
class SecureCvCache @Inject constructor(
    @ApplicationContext context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_cv_cache",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /** Persist the most recently extracted/entered CV text, encrypted at rest. */
    fun saveLastCvText(text: String) {
        prefs.edit()
            .putString(KEY_CV_TEXT, text)
            .putLong(KEY_SAVED_AT, System.currentTimeMillis())
            .apply()
    }

    /** Returns the last cached CV text, or null if none has been saved yet. */
    fun getLastCvText(): String? = prefs.getString(KEY_CV_TEXT, null)

    fun getLastSavedAt(): Long? = prefs.getLong(KEY_SAVED_AT, -1L).takeIf { it > 0 }

    /** Wipes the cached CV text — called on sign-out so it isn't left behind for the next user. */
    fun clear() = prefs.edit().clear().apply()

    companion object {
        private const val KEY_CV_TEXT = "last_cv_text"
        private const val KEY_SAVED_AT = "last_cv_saved_at"
    }
}
