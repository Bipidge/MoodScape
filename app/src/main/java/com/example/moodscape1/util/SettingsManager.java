package com.example.moodscape1.util;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class SettingsManager {
    private static final String PREFS_NAME = "MoodscapeSettings";
    private static final String KEY_THEME = "theme_mode";
    private static final String KEY_NOTIFICATIONS = "notifications_enabled";

    private final SharedPreferences sharedPreferences;

    public SettingsManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // --- Theme ---
    public void saveThemeMode(@AppCompatDelegate.NightMode int mode) {
        sharedPreferences.edit().putInt(KEY_THEME, mode).apply();
    }

    public int getThemeMode() {
        // Default to system theme if nothing saved
        return sharedPreferences.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    // --- Notifications ---
    public void saveNotificationsEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply();
    }

    public boolean areNotificationsEnabled() {
        // Default to false if nothing saved
        return sharedPreferences.getBoolean(KEY_NOTIFICATIONS, false);
    }
}
