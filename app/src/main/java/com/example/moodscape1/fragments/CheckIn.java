package com.example.moodscape1.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button; // Import Button

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.moodscape1.R;


public class CheckIn extends Fragment {

    private static final String TAG = "CheckInFragment";
    private Button buttonStartCheckIn;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_check_in, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        buttonStartCheckIn = view.findViewById(R.id.buttonStartCheckIn);
        if (buttonStartCheckIn != null) {
            buttonStartCheckIn.setOnClickListener(v -> {
                Log.d(TAG, "Check in button clicked. Navigating to EmotionsFragment.");
                // Navigate to Emotions Fragment
                Emotions emotionsFragment = new Emotions();
                FragmentManager fragmentManager = getParentFragmentManager(); // Use parent (Activity) manager
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.replace(R.id.main_frame, emotionsFragment); // Replace content in MainActivity's frame
                transaction.addToBackStack(null); // Allow user to navigate back to CheckIn screen
                transaction.commit();
            });
        } else {
            Log.e(TAG, "buttonStartCheckIn not found in layout!");
        }
    }
}