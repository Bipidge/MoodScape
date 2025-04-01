package com.example.moodscape1;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View; // Required for View class
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
// import androidx.appcompat.widget.Toolbar; // If using a toolbar
import androidx.core.graphics.Insets; // Required for Insets
import androidx.core.view.ViewCompat; // Required for ViewCompat
import androidx.core.view.WindowInsetsCompat; // Required for WindowInsetsCompat
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.moodscape1.authentication.SignIn;
import com.example.moodscape1.fragments.Calender;
import com.example.moodscape1.fragments.CheckIn;
import com.example.moodscape1.fragments.Home;
import com.example.moodscape1.fragments.Profile;
import com.example.moodscape1.notifications.ReminderBroadcastReceiver;
import com.example.moodscape1.util.SettingsManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
// import com.google.android.material.navigation.NavigationBarView; // Not needed
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private BottomNavigationView bottomNavigationView;
    private FrameLayout frameLayout; // Container for fragments
    private FirebaseAuth mAuth;
    private SettingsManager settingsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Apply theme setting BEFORE setting the content view
        settingsManager = new SettingsManager(getApplicationContext());
        AppCompatDelegate.setDefaultNightMode(settingsManager.getThemeMode());

        // Enable Edge-to-Edge display
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_main); // Set the activity layout

        // Initialize Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        // Create the Notification Channel needed for API 26+
        createNotificationChannel();

        // --- Verify User Login ---
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "User not logged in. Redirecting to SignIn screen.");
            navigateToSignIn();
            return; // Exit onCreate early if not logged in
        } else {
            Log.d(TAG, "User " + currentUser.getUid() + " is logged in.");
        }
        // -------------------------

        // --- Initialize UI Components ---
        bottomNavigationView = findViewById(R.id.bottomNavView); // Get BottomNavigationView
        frameLayout = findViewById(R.id.main_frame);             // Get Fragment container

        if (bottomNavigationView == null || frameLayout == null) {
            Log.e(TAG, "Critical UI components (BottomNav or FrameLayout) not found! Check activity_main.xml IDs.");
            Toast.makeText(this, "Error initializing UI.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        // -----------------------------

        // --- Load Initial Fragment (Only on first creation) ---
        if (savedInstanceState == null) {
            Log.d(TAG, "Loading initial fragment (Home)");
            loadFragment(new Home(), true);
            bottomNavigationView.setSelectedItemId(R.id.home); // Select "Home" visually (assuming it's first now)
        } else {
            Log.d(TAG, "Activity recreated, FragmentManager restoring fragments.");
        }
        // ------------------------------------------------------

        // --- Set up Bottom Navigation Listener ---
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Fragment selectedFragment = null;

            // Navigation logic based on menu item IDs (order in XML doesn't matter here)
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
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment, false); // Replace current fragment
                return true; // Indicate navigation was handled
            }
            return false; // Indicate item selection was not handled
        });
        // ----------------------------------------

        // --- Apply Window Insets for Correct Padding (REFINED) ---
        // Ensure R.id.main_container is the ID of your root ConstraintLayout in activity_main.xml
        View rootLayout = findViewById(R.id.main_container);
        if (rootLayout != null && bottomNavigationView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, windowInsets) -> {
                Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

                // Apply padding ONLY for TOP and sides to the ROOT view
                // This accounts for status bar and side display cutouts.
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0); // No bottom padding on root

                // Apply BOTTOM padding specifically to the BottomNavigationView itself
                // This increases the space *below* the icons/text *inside* the NavView's background.
                // It doesn't move the whole NavView up, avoiding the "gap".
                bottomNavigationView.setPadding(
                        bottomNavigationView.getPaddingLeft(), // Keep existing horizontal padding
                        bottomNavigationView.getPaddingTop(),  // Keep existing top padding
                        bottomNavigationView.getPaddingRight(),// Keep existing horizontal padding
                        systemBars.bottom // Apply system navigation bar height as bottom padding
                );
                Log.d(TAG, "Applied bottom padding to BottomNavView: " + systemBars.bottom);


                // --- FrameLayout Padding Adjustment ---
                // The FrameLayout sits above the BottomNavigationView area now due to its constraints.
                // It doesn't need specific bottom padding related to system bars.
                if (frameLayout != null) {
                    // Clear any potential pre-existing bottom padding if it was set before
                    frameLayout.setPadding(frameLayout.getPaddingLeft(), frameLayout.getPaddingTop(), frameLayout.getPaddingRight(), 0);
                }
                // ----------------------------------

                // We've manually handled padding based on insets, consume them.
                return WindowInsetsCompat.CONSUMED;
            });
        } else {
            Log.e(TAG, "Root layout (R.id.main_container) or BottomNavView not found. Cannot apply refined window insets.");
        }
        // --------------------------------------------

    } // End of onCreate

    /**
     * Replaces the content of the main FrameLayout with the given fragment.
     * @param fragment The Fragment to display.
     * @param isInitial True if this is the first fragment being loaded.
     */
    private void loadFragment(Fragment fragment, boolean isInitial) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // Set animations (optional)
        // transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);

        String fragmentTag = fragment.getClass().getSimpleName(); // Use class name as tag

        if (isInitial) {
            // Only add if the container is truly empty
            if (fragmentManager.findFragmentById(R.id.main_frame) == null) {
                fragmentTransaction.add(R.id.main_frame, fragment, fragmentTag);
                Log.d(TAG, "Adding initial fragment: " + fragmentTag);
            } else {
                // Fallback if container wasn't empty (shouldn't happen with correct logic)
                fragmentTransaction.replace(R.id.main_frame, fragment, fragmentTag);
                Log.w(TAG, "Replacing fragment during initial load (container wasn't empty?): " + fragmentTag);
            }
        } else {
            fragmentTransaction.replace(R.id.main_frame, fragment, fragmentTag);
            Log.d(TAG, "Replacing fragment with: " + fragmentTag);
            // fragmentTransaction.addToBackStack(fragmentTag); // Uncomment if fragment back navigation needed
        }

        fragmentTransaction.commit(); // Prefer commit() over commitAllowingStateLoss()
    }

    /**
     * Handles logging the user out via Firebase and returning to the SignIn screen.
     */
    public void logoutUser() {
        Log.i(TAG, "Logout requested by Profile fragment.");
        mAuth.signOut(); // Sign out from Firebase
        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
        navigateToSignIn(); // Go back to login screen
    }

    /**
     * Navigates to the SignIn Activity and clears the activity stack.
     */
    private void navigateToSignIn() {
        Intent intent = new Intent(MainActivity.this, SignIn.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Close MainActivity so user cannot navigate back to it
    }

    /**
     * Creates the Notification Channel required for Android 8.0+ reminders.
     */
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.reminder_channel_name);
            String description = getString(R.string.reminder_channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(ReminderBroadcastReceiver.CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this.
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.i(TAG, "Notification channel registered: ID=" + ReminderBroadcastReceiver.CHANNEL_ID);
            } else {
                Log.e(TAG, "NotificationManager service is null. Cannot create notification channel!");
            }
        } else {
            // Log that channel isn't needed for older versions
            Log.d(TAG, "Android version < Oreo. No notification channel creation necessary.");
        }
    }
}