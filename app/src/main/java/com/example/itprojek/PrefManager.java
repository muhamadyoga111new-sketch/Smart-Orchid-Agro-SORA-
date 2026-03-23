package com.example.itprojek;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * PrefManager — Helper class untuk SharedPreferences.
 * Satu instance bisa digunakan di semua Activity.
 *
 * Penggunaan:
 *   PrefManager pref = new PrefManager(this);
 *   pref.saveBoolean("STATUS_POMPA", true);
 *   boolean status = pref.getBoolean("STATUS_POMPA", false);
 */
public class PrefManager {

    private static final String PREF_NAME = "APP_PREF";
    private final SharedPreferences pref;

    public PrefManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // ==================== Boolean ====================

    public void saveBoolean(String key, boolean value) {
        pref.edit().putBoolean(key, value).apply();
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return pref.getBoolean(key, defaultValue);
    }

    // ==================== String ====================

    public void saveString(String key, String value) {
        pref.edit().putString(key, value).apply();
    }

    public String getString(String key, String defaultValue) {
        return pref.getString(key, defaultValue);
    }

    // ==================== Int ====================

    public void saveInt(String key, int value) {
        pref.edit().putInt(key, value).apply();
    }

    public int getInt(String key, int defaultValue) {
        return pref.getInt(key, defaultValue);
    }

    // ==================== Float ====================

    public void saveFloat(String key, float value) {
        pref.edit().putFloat(key, value).apply();
    }

    public float getFloat(String key, float defaultValue) {
        return pref.getFloat(key, defaultValue);
    }

    // ==================== Clear ====================

    public void clearAll() {
        pref.edit().clear().apply();
    }

    public void remove(String key) {
        pref.edit().remove(key).apply();
    }
}
