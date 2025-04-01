package com.example.moodscape1;

import android.app.NotificationChannel; // Import NotificationChannel
import android.app.NotificationManager; // Import NotificationManager
import android.content.Intent;
import android.os.Build; // Import Build
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem; // Keep existing imports
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate; // Import for theme
// import androidx.appcompat.widget.Toolbar; // Import if using a Toolbar
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.moodscape1.authentication.SignIn;
import com.example.moodscape1.fragments.Calender;
import com.example.moodscape1.fragments.CheckIn;
import com.example.moodscape1.fragments.Home;
import com.example.moodscape1.fragments.Profile;
import com.example.moodscape1.notifications.ReminderBroadcastReceiver; // Import constant
import com.example.moodscape1.util.SettingsManager; // Import for reading initial theme
import com.google.android.material.bottomnavigation.BottomNavigationView;
//import com.google.android.material.navigation.NavigationBarView; // Needed only if listener set differently
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import android.view.View;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private BottomNavigationView bottomNavigationView;
    private FrameLayout frameLayout;
    private FirebaseAuth mAuth;
    private SettingsManager settingsManager; // Add SettingsManager

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- Apply Theme BEFORE setting content view ---
        settingsManager = new SettingsManager(getApplicationContext());
        AppCompatDelegate.setDefaultNightMode(settingsManager.getThemeMode());
        // -----------------------------------------------

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main); // Ensure this layout exists and is correct

        mAuth = FirebaseAuth.getInstance();

        // --- Create Notification Channel (Needs to be done once per app install) ---
        createNotificationChannel();
        // ---------------------------------------------------------------------

        // Check if user is actually logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "User is null, redirecting to SignIn.");
            navigateToSignIn();
            return; // Stop further execution of onCreate
        } else {
            Log.d(TAG, "User " + currentUser.getUid() + " is logged in.");
        }

        // Initialize UI Components
        bottomNavigationView = findViewById(R.id.bottomNavView);
        frameLayout = findViewById(R.id.main_frame);

        // --- Load the initial fragment ---
        if (savedInstanceState == null) {
            Log.d(TAG, "Loading initial fragment: Home");
            loadFragment(new Home(), true);
            bottomNavigationView.setSelectedItemId(R.id.home); // Highlight Home item in Nav
        } else {
            Log.d(TAG, "Activity recreated, FragmentManager will restore state.");
            // Optionally, you could try to re-select the bottom nav item based on the restored fragment
        }

        // --- Set up Bottom Navigation Listener ---
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Fragment selectedFragment = null;
            boolean loadSuccess = true; // Flag to indicate if selection should be consumed

            if (itemId == R.id.home) {
                selectedFragment = new Home();
                Log.d(TAG, "Navigating to Home fragment");
            } else if (itemId == R.id.checkIn) {
                selectedFragment = new CheckIn();
                Log.d(TAG, "Navigating to CheckIn fragment");
            } else if (itemId == R.id.calender) {
                selectedFragment = new Calender();
                Log.d(TAG, "Navigating to Calendar fragment");
            } else if (itemId == R.id.profile) {
                selectedFragment = new Profile();
                Log.d(TAG, "Navigating to Profile fragment");
            } else {
                loadSuccess = false; // No valid fragment matched
            }


            if (selectedFragment != null) {
                loadFragment(selectedFragment, false); // Replace fragment
            }
            return loadSuccess; // Indicate selection handled (or not)
        });

        // Handle window insets
        View rootLayout = findViewById(R.id.main_container); // Use the root ID from activity_main.xml
        if (rootLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                // Apply padding to root to avoid drawing under status/nav bars
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0); // Adjust bottom as needed

                // Adjust FrameLayout bottom padding to be above the BottomNavigationView
                if (frameLayout != null) {
                    frameLayout.setPadding(0, 0, 0, systemBars.bottom);
                }

                return WindowInsetsCompat.CONSUMED; // Consume the insets
            });
        } else {
            Log.e(TAG, "Root layout (R.id.main_container) not found for WindowInsets listener.");
        }
    }

    /**
     * Loads the specified fragment into the main content frame.
     * @param fragment The fragment instance to load.
     * @param isInitial Boolean indicating if this is the first fragment loaded.
     */
    private void loadFragment(Fragment fragment, boolean isInitial) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        if (isInitial) {
            // Only add if container is empty
            if (fragmentManager.findFragmentById(R.id.main_frame) == null) {
                fragmentTransaction.add(R.id.main_frame, fragment);
                Log.d(TAG, "Adding initial fragment: " + fragment.getClass().getSimpleName());
            } else {
                fragmentTransaction.replace(R.id.main_frame, fragment);
                Log.w(TAG, "Replacing fragment despite isInitial=true (container not empty): " + fragment.getClass().getSimpleName());
            }
        } else {
            fragmentTransaction.replace(R.id.main_frame, fragment);
            Log.d(TAG, "Replacing fragment with: " + fragment.getClass().getSimpleName());
            // Optional: Add to back stack if you want fragment navigation history
            // fragmentTransaction.addToBackStack(null);
        }
        fragmentTransaction.commitAllowingStateLoss(); // Use commit() if state loss isn't a concern
    }

    /**
     * Logs the current user out using Firebase Authentication and navigates to the SignIn screen.
     */
    public void logoutUser() {
        Log.d(TAG, "logoutUser called.");
        mAuth.signOut();
        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
        navigateToSignIn();
    }

    /**
     * Navigates to the SignIn activity, clearing the back stack.
     */
    private void navigateToSignIn() {
        Intent intent = new Intent(MainActivity.this, SignIn.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Finish MainActivity
    }

    /**
     * Creates the Notification Channel required for displaying notifications on Android 8.0 (API 26) and above.
     * This should be called once when the app starts, e.g., in the main activity's onCreate.
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Use values from strings.xml
            CharSequence name = getString(R.string.reminder_channel_name);
            String description = getString(R.string.reminder_channel_description);
            // Set Importance Level (IMPORTANCE_DEFAULT is standard)
            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            // Create the channel object
            NotificationChannel channel = new NotificationChannel(ReminderBroadcastReceiver.CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Optional: Configure other channel settings like lights, vibration, sound here if needed
            // channel.enableLights(true);
            // channel.setLightColor(Color.RED);
            // channel.enableVibration(true);
            // channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});

            // Get the NotificationManager system service
            NotificationManager notificationManager = getSystemService(NotificationManager.class);

            // Register the channel with the system
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.i(TAG, "Notification channel created: ID=" + ReminderBroadcastReceiver.CHANNEL_ID);
            } else {
                Log.e(TAG, "NotificationManager system service is null, cannot create notification channel.");
            }
        } else {
            Log.d(TAG, "OS version < Oreo (API 26). No notification channel needed.");
        }
    }

    // Optional: Add logic to handle back press if using fragment back stack
     /*
     @Override
     public void onBackPressed() {
          if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
               getSupportFragmentManager().popBackStack();
          } else {
               super.onBackPressed(); // Default behaviour
          }
     }
     */
}