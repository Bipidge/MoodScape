package com.example.moodscape1;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem; // Import MenuItem
import android.widget.FrameLayout; // Import FrameLayout
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
// import androidx.appcompat.widget.Toolbar; // Import if using a Toolbar
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment; // Import Fragment
import androidx.fragment.app.FragmentManager; // Import FragmentManager
import androidx.fragment.app.FragmentTransaction; // Import FragmentTransaction

import com.example.moodscape1.authentication.SignIn; // For logout redirection
import com.example.moodscape1.fragments.Calender;   // Import your fragments
import com.example.moodscape1.fragments.CheckIn;
import com.example.moodscape1.fragments.Home;
import com.example.moodscape1.fragments.Profile;
import com.google.android.material.bottomnavigation.BottomNavigationView; // Import BottomNavigationView
import com.google.android.material.navigation.NavigationBarView; // Import NavigationBarView for listener
import com.google.firebase.auth.FirebaseAuth; // Import FirebaseAuth for logout
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private BottomNavigationView bottomNavigationView;
    private FrameLayout frameLayout;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main); // Ensure this layout exists and is correct

        mAuth = FirebaseAuth.getInstance();

        // Check if user is actually logged in (optional, but good practice)
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "User is null, redirecting to SignIn.");
            // Should not happen if navigation is correct, but handle defensively
            navigateToSignIn();
            return; // Stop further execution of onCreate
        } else {
            Log.d(TAG, "User " + currentUser.getUid() + " is logged in.");
        }

        // Optional: Setup Toolbar if you have one in activity_main.xml
        // Toolbar toolbar = findViewById(R.id.toolbar);
        // setSupportActionBar(toolbar);

        bottomNavigationView = findViewById(R.id.bottomNavView); // ID from activity_main.xml
        frameLayout = findViewById(R.id.main_frame);         // ID from activity_main.xml

        // --- Load the initial fragment ---
        if (savedInstanceState == null) { // Only load initial fragment if activity is newly created
            Log.d(TAG, "Loading initial fragment: Home");
            loadFragment(new Home(), true); // Load Home fragment initially
            bottomNavigationView.setSelectedItemId(R.id.home); // Highlight Home item in Nav
        } else {
            // Activity is being recreated (e.g., rotation), FragmentManager handles restoring fragments
            Log.d(TAG, "Activity recreated, FragmentManager will restore state.");
            // Update bottom nav selection based on current fragment (more advanced, optional)
        }


        // --- Set up Bottom Navigation Listener ---
        bottomNavigationView.setOnItemSelectedListener(item -> { // Using lambda
            int itemId = item.getItemId();
            Fragment selectedFragment = null;

            if (itemId == R.id.home) {
                selectedFragment = new Home();
                Log.d(TAG, "Navigating to Home fragment");
            } else if (itemId == R.id.checkIn) {
                selectedFragment = new CheckIn();
                Log.d(TAG, "Navigating to CheckIn fragment");
            } else if (itemId == R.id.calender) { // Corrected ID spelling if necessary
                selectedFragment = new Calender();
                Log.d(TAG, "Navigating to Calendar fragment");
            } else if (itemId == R.id.profile) {
                selectedFragment = new Profile();
                Log.d(TAG, "Navigating to Profile fragment");
                // Consider passing mAuth or setting up logout listener in Profile fragment itself
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment, false); // Replace fragment
                return true; // Indicate selection handled
            }
            return false; // Indicate selection not handled
        });

        // Handle window insets - Apply to the root view of activity_main.xml
        // Ensure the root view in activity_main.xml has android:id="@+id/main_container"
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Apply padding to the root layout to avoid drawing under status/nav bars
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0); // Pad top, left, right

            // Adjust bottom padding for the FrameLayout specifically to be above the BottomNavigationView
            frameLayout.setPadding(0, 0, 0, systemBars.bottom);

            // Optional: Adjust BottomNavigationView padding if needed (often not necessary with CoordinatorLayout)
            // bottomNavigationView.setPadding(0,0,0, systemBars.bottom);

            return WindowInsetsCompat.CONSUMED; // Consume insets
        });
    }

    // Method to load fragments into the FrameLayout
    private void loadFragment(Fragment fragment, boolean isInitial) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // Add or Replace logic
        if (isInitial) {
            // Use add only if the container is empty (protect against multiple adds on config change)
            if (fragmentManager.findFragmentById(R.id.main_frame) == null) {
                fragmentTransaction.add(R.id.main_frame, fragment);
                Log.d(TAG,"Adding initial fragment: " + fragment.getClass().getSimpleName());
            } else {
                // If called with isInitial=true but fragment exists, replace might be safer
                fragmentTransaction.replace(R.id.main_frame, fragment);
                Log.d(TAG,"Replacing existing fragment with initial: " + fragment.getClass().getSimpleName());
            }
        } else {
            fragmentTransaction.replace(R.id.main_frame, fragment);
            Log.d(TAG,"Replacing fragment with: " + fragment.getClass().getSimpleName());
            // Optional: Add to back stack for fragment back navigation
            // fragmentTransaction.addToBackStack(fragment.getClass().getSimpleName());
        }
        // Use commitAllowingStateLoss only if necessary, prefer commit()
        fragmentTransaction.commit();
    }

    // --- Logout Functionality ---
    // Call this method from a button click in Profile fragment or an options menu item
    public void logoutUser() {
        Log.d(TAG, "logoutUser called.");
        mAuth.signOut();
        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
        navigateToSignIn();
    }

    // Navigate to SignIn (used for logout or if user check fails)
    private void navigateToSignIn() {
        Intent intent = new Intent(MainActivity.this, SignIn.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear back stack
        startActivity(intent);
        finish(); // Finish MainActivity
    }
}

    // Optional: Handle back press for fragment back stack if you use addToBackStack
    /*
    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed(); // Default behavior (finish activity)
        }
    }
    */

    // Optional: Add Options Menu for Logout (if not using Profile fragment button)
    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_options_menu, menu); // Create res/menu/main_options_menu.xml
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) { // Define android:id="@+id/action_logout" in menu XML
            logoutUser();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }*/

