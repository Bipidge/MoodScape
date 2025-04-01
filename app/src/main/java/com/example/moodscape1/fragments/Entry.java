package com.example.moodscape1.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.moodscape1.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue; // For Timestamp
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;


public class Entry extends Fragment {

    private static final String TAG = "EntryFragment";

    private String selectedEmotion;
    private TextView textViewEntryTitle;
    private EditText editTextEntry;
    private Button buttonSaveEntry;
    private ProgressBar progressBarSave;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retrieve the passed emotion from arguments
        if (getArguments() != null) {
            selectedEmotion = getArguments().getString(Emotions.ARG_SELECTED_EMOTION);
            Log.d(TAG, "Received emotion: " + selectedEmotion);
        } else {
            Log.w(TAG, "No emotion received in arguments.");
            // Handle error - maybe navigate back or default emotion?
        }

        // Initialize Firebase instances
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_entry, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        textViewEntryTitle = view.findViewById(R.id.textViewEntryTitle);
        editTextEntry = view.findViewById(R.id.editTextEntry);
        buttonSaveEntry = view.findViewById(R.id.buttonSaveEntry);
        progressBarSave = view.findViewById(R.id.progressBarSave);

        // Display the selected emotion
        if (selectedEmotion != null) {
            textViewEntryTitle.setText("Feeling: " + selectedEmotion);
        } else {
            textViewEntryTitle.setText("Feeling: Unknown"); // Fallback
        }

        buttonSaveEntry.setOnClickListener(v -> attemptSaveEntry());
    }

    private void attemptSaveEntry() {
        String entryText = editTextEntry.getText().toString().trim();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Basic Validation
        if (currentUser == null) {
            Toast.makeText(getContext(), "Error: Not logged in.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Cannot save entry, user is null.");
            // Optionally navigate back to login
            return;
        }
        if (selectedEmotion == null || selectedEmotion.isEmpty()) {
            Toast.makeText(getContext(), "Error: Emotion not selected.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Cannot save entry, emotion is missing.");
            // Optionally navigate back
            return;
        }
        if (TextUtils.isEmpty(entryText)) {
            editTextEntry.setError("Entry cannot be empty");
            editTextEntry.requestFocus();
            return;
        }

        // Disable button and show progress bar
        buttonSaveEntry.setEnabled(false);
        progressBarSave.setVisibility(View.VISIBLE);

        // Prepare data for Firestore
        Map<String, Object> moodEntry = new HashMap<>();
        moodEntry.put("uid", currentUser.getUid());
        moodEntry.put("emotion", selectedEmotion);
        moodEntry.put("entryText", entryText);
        moodEntry.put("timestamp", FieldValue.serverTimestamp()); // Use server time

        // Save to Firestore
        // Using "mood_entries" collection - create if it doesn't exist
        db.collection("mood_entries")
                .add(moodEntry) // Firestore generates a unique ID for the document
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Mood entry saved successfully with ID: " + documentReference.getId());
                    Toast.makeText(getContext(), "Entry saved!", Toast.LENGTH_SHORT).show();
                    // Navigate back after successful save
                    if (isAdded() && getParentFragmentManager() != null) {
                        getParentFragmentManager().popBackStack(); // Go back to the previous fragment (Emotions)
                        // Optional: Pop twice to go back past Emotions
                        // getParentFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                        // getParentFragmentManager().popBackStack(); // Remove Emotions fragment too
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving mood entry", e);
                    Toast.makeText(getContext(), "Error saving entry: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    // Re-enable button and hide progress bar on failure
                    if(isAdded()) { // Check if fragment is still attached
                        buttonSaveEntry.setEnabled(true);
                        progressBarSave.setVisibility(View.GONE);
                    }
                });
    }
}