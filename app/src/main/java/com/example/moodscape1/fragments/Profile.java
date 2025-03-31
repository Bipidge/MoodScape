package com.example.moodscape1.fragments;

import android.os.Bundle;
import android.util.Log; // Import Log
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button; // Import Button
import android.widget.Toast; // Import Toast for error feedback

import androidx.annotation.NonNull; // Import NonNull
import androidx.annotation.Nullable; // Import Nullable
import androidx.fragment.app.Fragment;

import com.example.moodscape1.MainActivity; // Import MainActivity
import com.example.moodscape1.R;


public class Profile extends Fragment {

    // Declare the button
    private Button logoutButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment first
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find the button within the fragment's view
        logoutButton = view.findViewById(R.id.buttonLogout); // Make sure this ID exists in fragment_profile.xml

        if (logoutButton == null) {
            Log.e("ProfileFragment", "Logout button not found in layout!");
            return; // Exit if the button isn't found
        }

        // Set the click listener
        logoutButton.setOnClickListener(v -> {
            // Get the hosting Activity
            if (getActivity() instanceof MainActivity) {
                Log.d("ProfileFragment", "Logout button clicked. Calling logoutUser in MainActivity.");
                // Cast the activity to MainActivity and call its public logout method
                ((MainActivity) getActivity()).logoutUser();
            } else {
                // Handle the case where the activity is not MainActivity (shouldn't happen in this setup)
                Log.e("ProfileFragment", "Cannot logout: Activity is not an instance of MainActivity.");
                Toast.makeText(getContext(), "Logout failed.", Toast.LENGTH_SHORT).show();
            }
        });

        // --- Add any other setup for your Profile fragment here ---
        // For example, find TextViews to display user info, etc.

    }
}