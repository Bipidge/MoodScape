package com.example.moodscape1.fragments;

import android.Manifest; // Import Manifest for permissions
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration; // Import Configuration for checking current UI mode
import android.os.Build; // Import Build for version checks
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton; // Import for listener
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate; // Import for theme control
import androidx.core.content.ContextCompat; // Import for permission check
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity; // Import for safe activity access

import com.example.moodscape1.AboutUs; // Import the About Us screen
import com.example.moodscape1.MainActivity; // Import MainActivity for logout
import com.example.moodscape1.R; // Your R file
import com.example.moodscape1.notifications.NotificationScheduler; // Import the scheduler
import com.example.moodscape1.util.SettingsManager; // Import the settings manager
import com.google.android.material.switchmaterial.SwitchMaterial; // Import the Switch widget


public class Profile extends Fragment {

    private static final String TAG = "ProfileFragment";

    // UI Elements
    private Button logoutButton, buttonAboutUs;
    private SwitchMaterial switchTheme, switchNotifications;

    // Helpers
    private SettingsManager settingsManager;
    private Context appContext; // Application context to prevent leaks

    // Activity Result Launcher for Notification Permission
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (appContext == null) return; // Check if context detached

                if (isGranted) {
                    Log.i(TAG, "Notification permission granted by user.");
                    // Now that permission is granted, schedule the reminders
                    scheduleReminders();
                    // Make sure the saved state reflects 'enabled'
                    settingsManager.saveNotificationsEnabled(true);
                } else {
                    Log.w(TAG, "Notification permission denied by user.");
                    // Explain why the feature won't work and update UI/Settings
                    Toast.makeText(appContext, "Reminders cannot be shown without notification permission.", Toast.LENGTH_LONG).show();
                    if (switchNotifications != null) {
                        switchNotifications.setChecked(false); // Ensure switch reflects the denied state
                    }
                    settingsManager.saveNotificationsEnabled(false); // Save disabled state
                }
            });

    // --- Lifecycle Methods ---

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Get Application context to avoid holding activity context too long
        appContext = context.getApplicationContext();
        settingsManager = new SettingsManager(appContext); // Initialize here safely
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Theme is applied by MainActivity before fragment is attached,
        // but reading it here ensures consistency if Profile is the first fragment shown (unlikely).
        // AppCompatDelegate.setDefaultNightMode(settingsManager.getThemeMode()); // Applied in MainActivity

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find Views using their IDs from fragment_profile.xml
        logoutButton = view.findViewById(R.id.buttonLogout);
        buttonAboutUs = view.findViewById(R.id.buttonAboutUs);
        switchTheme = view.findViewById(R.id.switchTheme);
        switchNotifications = view.findViewById(R.id.switchNotifications);

        // Load current settings and update the UI elements visually
        loadAndApplyCurrentSettings();

        // --- Set Click & Change Listeners ---
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> handleLogout());
        } else {
            Log.e(TAG, "Logout Button not found in layout!");
        }

        if (buttonAboutUs != null) {
            buttonAboutUs.setOnClickListener(v -> navigateToAboutUs());
        } else {
            Log.e(TAG, "About Us Button not found in layout!");
        }

        // Listeners for switches are attached *after* initial state is set in loadAndApplyCurrentSettings
        if (switchTheme == null) {
            Log.e(TAG, "Theme Switch not found in layout!");
        }
        if (switchNotifications == null) {
            Log.e(TAG, "Notifications Switch not found in layout!");
        }

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up view references to avoid memory leaks
        logoutButton = null;
        buttonAboutUs = null;
        switchTheme = null;
        switchNotifications = null;
        Log.d(TAG, "Views nulled out in onDestroyView");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        appContext = null; // Release application context reference
        settingsManager = null; // Release SettingsManager
        Log.d(TAG, "Context nulled out in onDetach");
    }

    // --- Helper Methods ---

    /**
     * Reads settings from SharedPreferences and updates the switches accordingly.
     * Attaches listeners *after* setting the initial state.
     */
    private void loadAndApplyCurrentSettings() {
        if (settingsManager == null) {
            Log.e(TAG, "SettingsManager is null in loadAndApplyCurrentSettings!");
            return;
        }

        // Notifications Switch State
        boolean isNotificationsEnabled = settingsManager.areNotificationsEnabled();
        if (switchNotifications != null) {
            switchNotifications.setOnCheckedChangeListener(null); // Detach listener first
            switchNotifications.setChecked(isNotificationsEnabled);
            switchNotifications.setOnCheckedChangeListener(notificationsSwitchListener); // Re-attach listener
            Log.d(TAG, "Notification switch initial state set to: " + isNotificationsEnabled);
        }

        // Theme Switch State
        int currentThemeMode = settingsManager.getThemeMode();
        if (switchTheme != null) {
            switchTheme.setOnCheckedChangeListener(null); // Detach listener
            boolean isCurrentlyDark;
            if (currentThemeMode == AppCompatDelegate.MODE_NIGHT_YES) {
                isCurrentlyDark = true;
            } else if (currentThemeMode == AppCompatDelegate.MODE_NIGHT_NO) {
                isCurrentlyDark = false;
            } else { // MODE_NIGHT_FOLLOW_SYSTEM or default
                // Check the actual current system configuration for accurate display
                int systemUiMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                isCurrentlyDark = (systemUiMode == Configuration.UI_MODE_NIGHT_YES);
                // Consider visually disabling switch or adding 3rd state if system default is primary
                Log.d(TAG,"Theme is SYSTEM, current dark mode: " + isCurrentlyDark);
            }
            switchTheme.setChecked(isCurrentlyDark);
            switchTheme.setOnCheckedChangeListener(themeSwitchListener); // Re-attach listener
            Log.d(TAG, "Theme switch initial state set based on mode " + currentThemeMode + ": " + isCurrentlyDark);
        }
    }

    // --- Listener Implementations ---

    /**
     * Listener for the Dark Theme switch. Applies the theme and saves the preference.
     */
    private final CompoundButton.OnCheckedChangeListener themeSwitchListener = (buttonView, isChecked) -> {
        if (settingsManager == null) return; // Safety check

        Log.d(TAG, "Theme switch toggled: " + isChecked);
        int newMode = isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;

        // Only apply and save if the mode actually changes from preference (prevents loops)
        // Note: This comparison might be tricky if current setting is system default.
        // Simpler: Just apply and save based on the switch toggle directly.
        if (AppCompatDelegate.getDefaultNightMode() != newMode) {
            AppCompatDelegate.setDefaultNightMode(newMode); // Apply change immediately
            settingsManager.saveThemeMode(newMode); // Save the user's explicit choice

            // Note: Applying theme might require activity recreation for full effect across all UI elements.
            // getActivity().recreate(); // Uncomment this if subtle visual glitches occur, but it restarts the activity.
            if (appContext != null) {
                Toast.makeText(appContext, "Theme changed.", Toast.LENGTH_SHORT).show();
            }

        } else {
            Log.d(TAG,"Theme switch toggled but mode ("+ newMode + ") is already set.");
        }

    };

    /**
     * Listener for the Notifications switch. Handles permissions and scheduling/cancelling.
     */
    private final CompoundButton.OnCheckedChangeListener notificationsSwitchListener = (buttonView, isChecked) -> {
        if (settingsManager == null || appContext == null) {
            Log.e(TAG, "Notifications listener called but SettingsManager or Context is null!");
            if(buttonView.isPressed()){ // Prevent programmatic change issues
                buttonView.setChecked(!isChecked); // Revert visual state
            }
            return;
        }

        Log.d(TAG, "Notifications switch toggled: " + isChecked);

        if (isChecked) {
            // Trying to turn ON: Check permissions first.
            Log.d(TAG,"Attempting to enable notifications, checking permission...");
            checkNotificationPermissionAndSchedule();
            // State saved after permission result
        } else {
            // Turning OFF: Cancel alarms and save state immediately.
            Log.d(TAG,"Disabling notifications, cancelling alarms...");
            NotificationScheduler.cancelReminderNotifications(appContext);
            settingsManager.saveNotificationsEnabled(false); // Save disabled state
        }
    };


    // --- Action Methods ---

    /**
     * Checks for POST_NOTIFICATIONS permission (Android 13+) and schedules reminders if granted,
     * otherwise launches the permission request flow.
     */
    private void checkNotificationPermissionAndSchedule() {
        if (appContext == null) {
            Log.e(TAG, "Cannot check permission or schedule, appContext is null.");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33+ needs runtime permission
            if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                // Permission already granted
                Log.i(TAG, "POST_NOTIFICATIONS permission already granted. Scheduling reminders.");
                scheduleReminders();
                settingsManager.saveNotificationsEnabled(true); // Ensure state is saved as enabled
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // Need to show rationale - user previously denied without "never ask again"
                Log.w(TAG, "Need to show rationale for POST_NOTIFICATIONS permission.");
                Toast.makeText(appContext, "Please grant Notification permission for daily reminders.", Toast.LENGTH_LONG).show();
                // TODO: Consider showing a Dialog explaining why the permission is needed
                // After showing rationale (or instead of), request permission again
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                // First time asking or user chose "Don't ask again" before
                Log.i(TAG, "Requesting POST_NOTIFICATIONS permission...");
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            // Below Android 13 (API 33), permission is granted at install time
            Log.i(TAG, "OS below Android 13, runtime notification permission not required. Scheduling reminders.");
            scheduleReminders();
            settingsManager.saveNotificationsEnabled(true); // Save enabled state
        }
    }

    /**
     * Calls the NotificationScheduler to set up the reminder alarms.
     * Should only be called after necessary permissions are confirmed.
     */
    private void scheduleReminders() {
        if(appContext == null) {
            Log.e(TAG, "Cannot schedule reminders, appContext is null.");
            return;
        }
        Log.d(TAG, "Calling NotificationScheduler.scheduleReminderNotifications().");
        NotificationScheduler.scheduleReminderNotifications(appContext);
    }

    /**
     * Handles the logout button click by calling the logout method in MainActivity.
     */
    private void handleLogout() {
        FragmentActivity activity = getActivity(); // Get hosting Activity
        if (activity instanceof MainActivity) {
            Log.d(TAG, "Logout button clicked. Requesting logout via MainActivity.");
            ((MainActivity) activity).logoutUser();
        } else {
            Log.e(TAG, "Cannot logout: Hosting Activity is not MainActivity or is null.");
            if(appContext != null) {
                Toast.makeText(appContext, "Logout failed (Internal Error).", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Starts the AboutUsActivity.
     */
    private void navigateToAboutUs() {
        if (getActivity() == null) {
            Log.e(TAG, "Cannot navigate to About Us: Fragment not attached to an Activity.");
            return;
        }
        Intent intent = new Intent(getActivity(), AboutUs.class);
        startActivity(intent);
        Log.d(TAG,"Started AboutUsActivity.");
    }

}