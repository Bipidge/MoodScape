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
import com.example.moodscape1.adapter.MoodEntryAdapter;
import com.example.moodscape1.model.MoodEntry;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.DateFormatSymbols; // Import for manual date formatting
import java.text.SimpleDateFormat; // Keep for potential fallback or other uses
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone; // Ensure this import is present


public class DayEntries extends Fragment {

    // TAG and Argument Keys
    private static final String TAG = "DayEntriesFragment"; // Log Tag
    public static final String ARG_YEAR = "selected_year";
    public static final String ARG_MONTH = "selected_month"; // Expects 0-11 index
    public static final String ARG_DAY = "selected_day";

    // UI Elements
    private RecyclerView recyclerViewDayEntries;
    private MoodEntryAdapter moodEntryAdapter;
    private List<MoodEntry> moodEntryList;
    private ProgressBar progressBarDayEntries;
    private TextView textViewNoEntriesForDay;
    private TextView textViewDayEntriesTitle;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // Date Handling
    private int selectedYear;
    private int selectedMonth; // Stored as 0-11 index
    private int selectedDay;
    // SimpleDateFormat is kept mainly as a fallback if manual formatting fails
    private SimpleDateFormat fallbackTitleFormat = new SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault());


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fallbackTitleFormat.setTimeZone(TimeZone.getDefault()); // Configure fallback formatter timezone

        // Retrieve arguments passed from Calendar Fragment
        if (getArguments() != null) {
            selectedYear = getArguments().getInt(ARG_YEAR);
            selectedMonth = getArguments().getInt(ARG_MONTH); // Expecting 0-11 index
            selectedDay = getArguments().getInt(ARG_DAY);
            Log.i(TAG, "Received arguments: Year=" + selectedYear + ", Month(0-11)=" + selectedMonth + ", Day=" + selectedDay);
        } else {
            Log.e(TAG, "CRITICAL: No arguments received for selected date! Cannot proceed.");
            Toast.makeText(getContext(), "Error: No date specified.", Toast.LENGTH_SHORT).show();
            if (isAdded() && getParentFragmentManager() != null) {
                getParentFragmentManager().popBackStack();
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_day_entries, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase instances
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // --- Initialize Views ---
        recyclerViewDayEntries = view.findViewById(R.id.recyclerViewDayEntries);
        progressBarDayEntries = view.findViewById(R.id.progressBarDayEntries);
        textViewNoEntriesForDay = view.findViewById(R.id.textViewNoEntriesForDay);
        textViewDayEntriesTitle = view.findViewById(R.id.textViewDayEntriesTitle);

        // --- Setup RecyclerView ---
        moodEntryList = new ArrayList<>();
        if (getContext() != null) { // Ensure context is available
            moodEntryAdapter = new MoodEntryAdapter(getContext(), moodEntryList);
            recyclerViewDayEntries.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerViewDayEntries.setAdapter(moodEntryAdapter);
        } else {
            Log.e(TAG, "Context is null during RecyclerView setup in onViewCreated!");
            // Display error state if context is missing
            textViewNoEntriesForDay.setText(R.string.error_loading_display); // Use string resource
            textViewNoEntriesForDay.setVisibility(View.VISIBLE);
            progressBarDayEntries.setVisibility(View.GONE);
            recyclerViewDayEntries.setVisibility(View.GONE);
            textViewDayEntriesTitle.setText(R.string.error_title); // Use string resource
            return; // Stop further execution
        }

        // --- Set Title Text Directly and Robustly ---
        try {
            // Use a Calendar instance *only* to determine the Day of the Week reliably
            Calendar titleHelperCal = Calendar.getInstance(TimeZone.getDefault()); // Use device's default timezone
            titleHelperCal.clear(); // Start clean!
            // Use the received 0-11 month index directly with Calendar.set
            titleHelperCal.set(selectedYear, selectedMonth, selectedDay);
            int dayOfWeekIndex = titleHelperCal.get(Calendar.DAY_OF_WEEK); // Gets index (e.g., Calendar.SATURDAY)

            // Get month and weekday names using Locale-aware symbols
            DateFormatSymbols symbols = new DateFormatSymbols(Locale.getDefault());
            String shortWeekdayName = symbols.getShortWeekdays()[dayOfWeekIndex]; // e.g., "Sat", "Sun"
            // Ensure month index is valid before accessing array
            String shortMonthName = (selectedMonth >= 0 && selectedMonth < symbols.getShortMonths().length)
                    ? symbols.getShortMonths()[selectedMonth] // e.g., "Jan", "Feb" (uses 0-11)
                    : "???"; // Fallback if month index is weird

            // Construct the title string manually using retrieved components
            String titleString = String.format(Locale.getDefault(), "Entries for %s, %s %02d, %d",
                    shortWeekdayName, shortMonthName, selectedDay, selectedYear);

            textViewDayEntriesTitle.setText(titleString);
            Log.i(TAG, "Successfully constructed title: " + titleString);
            // Also log the date object used for getting dayOfWeek for verification
            Log.d(TAG, "Helper Calendar Date obj: " + titleHelperCal.getTime() + " [TZ: " + titleHelperCal.getTimeZone().getID() + "]");

        } catch (Exception e) {
            Log.e(TAG, "Error constructing date title manually, attempting fallback.", e);
            // Fallback to SimpleDateFormat if manual method fails
            try {
                Calendar fallbackCal = Calendar.getInstance(TimeZone.getDefault());
                fallbackCal.clear();
                fallbackCal.set(selectedYear, selectedMonth, selectedDay);
                textViewDayEntriesTitle.setText(String.format("Entries for %s", fallbackTitleFormat.format(fallbackCal.getTime())));
            } catch (Exception fe) {
                Log.e(TAG, "Fallback date formatting failed as well.", fe);
                textViewDayEntriesTitle.setText(R.string.entries_for_selected_date); // Generic title
            }
        }
        // ----------------------------------------

        // --- Fetch Data for the Day ---
        fetchEntriesForSelectedDay();
    }

    /**
     * Fetches mood entries from Firestore for the specific selected date.
     * Uses UTC for constructing the query range.
     */
    private void fetchEntriesForSelectedDay() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        // Check user and context again before proceeding with fetch
        if (currentUser == null || getContext() == null) {
            if (currentUser == null) Log.w(TAG, "fetchEntries: User not logged in.");
            if (getContext() == null) Log.w(TAG, "fetchEntries: Context is null.");
            // Set UI to appropriate state if we can't fetch
            Toast.makeText(getContext(), R.string.login_required_to_load, Toast.LENGTH_LONG).show();
            progressBarDayEntries.setVisibility(View.GONE);
            textViewNoEntriesForDay.setText(R.string.login_or_error_loading);
            textViewNoEntriesForDay.setVisibility(View.VISIBLE);
            recyclerViewDayEntries.setVisibility(View.GONE);
            return;
        }
        String userId = currentUser.getUid();

        // --- Calculate Query Timestamp Range using UTC ---
        // Start of the selected day in UTC
        Calendar startCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        startCal.clear(); // Start clean
        startCal.set(selectedYear, selectedMonth, selectedDay, 0, 0, 0); // Use 0-11 month index
        startCal.set(Calendar.MILLISECOND, 0);
        Date startDate = startCal.getTime();

        // Start of the *next* day in UTC (exclusive end boundary)
        Calendar endCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        endCal.setTime(startDate); // Set to start of selected day UTC
        endCal.add(Calendar.DAY_OF_MONTH, 1); // Move to start of next day UTC
        Date endDate = endCal.getTime();

        Timestamp startTimestamp = new Timestamp(startDate);
        Timestamp endTimestamp = new Timestamp(endDate);
        // --- End Timestamp Calculation ---

        Log.i(TAG, "Querying Firestore for user [" + userId + "]");
        Log.d(TAG, "Query Range UTC Start: " + startDate + " (TS Secs: " + startTimestamp.getSeconds() + ")");
        Log.d(TAG, "Query Range UTC End:   " + endDate + " (TS Secs: " + endTimestamp.getSeconds() + ")");

        // --- Update UI: Show Loading ---
        progressBarDayEntries.setVisibility(View.VISIBLE);
        textViewNoEntriesForDay.setVisibility(View.GONE);
        recyclerViewDayEntries.setVisibility(View.GONE);

        // --- Execute Firestore Query ---
        db.collection("mood_entries")
                .whereEqualTo("uid", userId)
                .whereGreaterThanOrEqualTo("timestamp", startTimestamp)
                .whereLessThan("timestamp", endTimestamp)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    // Check fragment/context validity *before* touching UI elements
                    if (!isAdded() || getContext() == null) {
                        Log.w(TAG, "fetchEntries: Firestore task completed, but fragment/context no longer valid.");
                        return; // Stop processing to avoid crash
                    }

                    // --- Hide loading indicator ---
                    progressBarDayEntries.setVisibility(View.GONE);

                    if (task.isSuccessful() && task.getResult() != null) {
                        List<MoodEntry> fetchedEntries = new ArrayList<>();
                        Log.d(TAG, "fetchEntries: Firestore query successful. Found " + task.getResult().size() + " documents in range.");
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                MoodEntry entry = document.toObject(MoodEntry.class);
                                // Verbose log for timestamp verification
                                if (entry.getTimestamp() != null) {
                                    Log.v(TAG, "  Retrieved Entry: '" + entry.getEmotion() + "' at Firestore TS: " + entry.getTimestamp().toDate() + " (UTC)");
                                } else {
                                    Log.w(TAG, "  Retrieved Entry Document " + document.getId() + " has null 'timestamp'.");
                                }
                                fetchedEntries.add(entry);
                            } catch (Exception e) {
                                Log.e(TAG, "  Error parsing Firestore document [" + document.getId() + "]", e);
                            }
                        }

                        // Update adapter (which should be non-null if initial setup passed)
                        if (moodEntryAdapter != null) {
                            moodEntryAdapter.updateEntries(fetchedEntries);
                        }

                        // Show/hide "No entries" message or RecyclerView
                        if (fetchedEntries.isEmpty()) {
                            textViewNoEntriesForDay.setText(R.string.no_entries_for_day); // Use string resource
                            textViewNoEntriesForDay.setVisibility(View.VISIBLE);
                            recyclerViewDayEntries.setVisibility(View.GONE);
                            Log.d(TAG, "fetchEntries: Displaying 'No Entries' message.");
                        } else {
                            textViewNoEntriesForDay.setVisibility(View.GONE);
                            recyclerViewDayEntries.setVisibility(View.VISIBLE);
                            Log.d(TAG, "fetchEntries: Displaying " + fetchedEntries.size() + " entries in RecyclerView.");
                        }

                    } else {
                        // Handle Firestore query failure
                        Log.e(TAG, "fetchEntries: Error fetching documents.", task.getException());
                        Toast.makeText(getContext(), R.string.error_loading_entries, Toast.LENGTH_SHORT).show(); // Use string resource
                        textViewNoEntriesForDay.setText(R.string.error_loading_entries); // Use string resource
                        textViewNoEntriesForDay.setVisibility(View.VISIBLE);
                        recyclerViewDayEntries.setVisibility(View.GONE);
                    }
                });
    }
}