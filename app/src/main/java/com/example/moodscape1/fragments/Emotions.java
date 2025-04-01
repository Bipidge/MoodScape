package com.example.moodscape1.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.moodscape1.R;

public class Emotions extends Fragment implements View.OnClickListener {

    private static final String TAG = "EmotionsFragment";
    public static final String ARG_SELECTED_EMOTION = "selected_emotion"; // Key for Bundle argument

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_emotions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find buttons and set click listener to this fragment
        view.findViewById(R.id.buttonHappy).setOnClickListener(this);
        view.findViewById(R.id.buttonCalm).setOnClickListener(this);
        view.findViewById(R.id.buttonSad).setOnClickListener(this);
        view.findViewById(R.id.buttonAngry).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        String selectedEmotion = "";
        int id = v.getId();

        if (id == R.id.buttonHappy) {
            selectedEmotion = "Happy";
        } else if (id == R.id.buttonCalm) {
            selectedEmotion = "Calm";
        } else if (id == R.id.buttonSad) {
            selectedEmotion = "Sad";
        } else if (id == R.id.buttonAngry) {
            selectedEmotion = "Angry";
        }

        if (!selectedEmotion.isEmpty()) {
            Log.d(TAG, "Emotion selected: " + selectedEmotion);
            navigateToEntryFragment(selectedEmotion);
        }
    }

    private void navigateToEntryFragment(String emotion) {
        // Create the EntryFragment instance
        Entry entryFragment = new Entry();

        // Create a Bundle to pass data
        Bundle args = new Bundle();
        args.putString(ARG_SELECTED_EMOTION, emotion); // Use the constant key
        entryFragment.setArguments(args);

        // Navigate using FragmentManager
        FragmentManager fragmentManager = getParentFragmentManager(); // Use parent manager (MainActivity's)
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.main_frame, entryFragment); // Replace content in MainActivity's frame
        transaction.addToBackStack(null); // Add transaction to back stack to allow back navigation
        transaction.commit();
        Log.d(TAG, "Navigating to EntryFragment with emotion: " + emotion);
    }
}