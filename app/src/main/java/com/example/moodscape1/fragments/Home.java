package com.example.moodscape1.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moodscape1.R;
import com.example.moodscape1.adapter.MoodEntryAdapter; // Import adapter
import com.example.moodscape1.model.MoodEntry;         // Import model
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration; // For cleanup
import com.google.firebase.firestore.Query;               // For ordering
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class Home extends Fragment {

    private static final String TAG = "HomeFragment";

    private RecyclerView recyclerViewEntries;
    private MoodEntryAdapter moodEntryAdapter;
    private List<MoodEntry> moodEntryList;
    private ProgressBar progressBarHome;
    private TextView textViewNoEntries;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration firestoreListener; // To detach listener later

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        recyclerViewEntries = view.findViewById(R.id.recyclerViewEntries);
        progressBarHome = view.findViewById(R.id.progressBarHome);
        textViewNoEntries = view.findViewById(R.id.textViewNoEntries);

        moodEntryList = new ArrayList<>();
        moodEntryAdapter = new MoodEntryAdapter(getContext(), moodEntryList);

        recyclerViewEntries.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewEntries.setAdapter(moodEntryAdapter);

        // Fetch entries when the view is created
        fetchMoodEntries();
    }

    private void fetchMoodEntries() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Not logged in.", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Cannot fetch entries, user is null.");
            // Consider navigating to login
            progressBarHome.setVisibility(View.GONE);
            textViewNoEntries.setVisibility(View.VISIBLE);
            textViewNoEntries.setText("Please log in to see entries.");
            return;
        }

        String userId = currentUser.getUid();
        Log.d(TAG, "Fetching entries for user: " + userId);
        progressBarHome.setVisibility(View.VISIBLE); // Show loading indicator
        textViewNoEntries.setVisibility(View.GONE);
        recyclerViewEntries.setVisibility(View.GONE);

        // Query Firestore
        Query query = db.collection("mood_entries") // Make sure collection name matches EntryFragment
                .whereEqualTo("uid", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING); // Show newest first

        // Add snapshot listener for real-time updates
        firestoreListener = query.addSnapshotListener((querySnapshot, e) -> {
            progressBarHome.setVisibility(View.GONE); // Hide loading indicator once data arrives/fails

            if (e != null) {
                Log.e(TAG, "Error listening for mood entries:", e);
                Toast.makeText(getContext(), "Error fetching entries.", Toast.LENGTH_SHORT).show();
                textViewNoEntries.setVisibility(View.VISIBLE);
                textViewNoEntries.setText("Error loading entries.");
                recyclerViewEntries.setVisibility(View.GONE);
                return;
            }

            if (querySnapshot != null) {
                Log.d(TAG, "Received " + querySnapshot.size() + " entries from Firestore.");
                List<MoodEntry> fetchedEntries = new ArrayList<>();
                for (QueryDocumentSnapshot document : querySnapshot) {
                    try {
                        MoodEntry entry = document.toObject(MoodEntry.class);
                        // Optional: Add document ID if needed later
                        // entry.setDocumentId(document.getId());
                        fetchedEntries.add(entry);
                        Log.d(TAG, "Entry loaded: Emotion=" + entry.getEmotion() + ", Text=" + entry.getEntryText() + ", Timestamp=" + entry.getTimestamp());
                    } catch (Exception parseError) {
                        Log.e(TAG, "Error parsing document " + document.getId(), parseError);
                    }
                }
                moodEntryAdapter.updateEntries(fetchedEntries);

                // Show/Hide RecyclerView or "No Entries" text
                if (fetchedEntries.isEmpty()) {
                    Log.d(TAG, "No entries found for user.");
                    textViewNoEntries.setVisibility(View.VISIBLE);
                    textViewNoEntries.setText("No mood entries yet.\nUse the Check In button to add one!");
                    recyclerViewEntries.setVisibility(View.GONE);
                } else {
                    Log.d(TAG, "Displaying entries.");
                    textViewNoEntries.setVisibility(View.GONE);
                    recyclerViewEntries.setVisibility(View.VISIBLE);
                }
            } else {
                Log.d(TAG, "Query snapshot was null.");
                textViewNoEntries.setVisibility(View.VISIBLE);
                textViewNoEntries.setText("Could not load entries.");
                recyclerViewEntries.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        // IMPORTANT: Remove the listener when the fragment is stopped/destroyed
        if (firestoreListener != null) {
            Log.d(TAG, "Removing Firestore listener.");
            firestoreListener.remove();
        }
    }
}
