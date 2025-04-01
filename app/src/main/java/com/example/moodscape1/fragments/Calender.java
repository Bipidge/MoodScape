package com.example.moodscape1.fragments;

import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.moodscape1.R;
import com.example.moodscape1.model.MoodEntry;
import com.example.moodscape1.util.EventDecorator; // Import decorator
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;


import java.util.Calendar;
import java.util.HashSet;
import java.util.TimeZone;

public class Calender extends Fragment {

    private static final String TAG = "CalendarFragment";

    private MaterialCalendarView calendarView;
    private ProgressBar progressBarCalendar;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private HashSet<CalendarDay> datesWithEntries = new HashSet<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_calender, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize UI elements
        calendarView = view.findViewById(R.id.calendarView);
        progressBarCalendar = view.findViewById(R.id.progressBarCalendar);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // --- Explicitly Set Initial Date Selection to Today (Local TZ) ---
        try {
            // Create CalendarDay using device's default timezone for "today"
            Calendar todayCal = Calendar.getInstance(TimeZone.getDefault());
            CalendarDay today = CalendarDay.from(todayCal);
            calendarView.setCurrentDate(today, true); // Show month containing today & animate if needed
            calendarView.setDateSelected(today, true); // Select today visually
            Log.i(TAG, "Initial setup: Set calendar view to month of and selected: " + today.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error setting initial date selection", e);
            // Fallback: Default selection without specifying today might occur
        }
        // ---------------------------------------------------------------

        // --- Set Date Tap Listener ---
        calendarView.setOnDateChangedListener(new OnDateSelectedListener() {
            @Override
            public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date, boolean selected) {
                if (selected) {
                    // Log extensively when a date is tapped
                    Log.i(TAG, "-- Date Tapped --");
                    Log.d(TAG, "onDateSelected - Raw CalendarDay Object: " + date.toString());
                    Log.d(TAG, "onDateSelected - Extracted Components: Year=" + date.getYear() + ", Month(1-12)=" + date.getMonth() + ", Day=" + date.getDay());
                    navigateToDayEntries(date); // Proceed to navigate
                } else {
                    // Optional: Log or handle deselection if necessary
                    Log.d(TAG, "onDateSelected - Date Deselected: " + date.toString());
                }
            }
        });
        // ----------------------------

        // Fetch dates with entries to apply decorators
        fetchEntryDates();
    }

    /**
     * Fetches all mood entry dates for the current user from Firestore
     * and prepares them for highlighting on the calendar.
     */
    private void fetchEntryDates() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        // Check context validity early, needed for decorator color
        if (currentUser == null || getContext() == null) {
            if (currentUser == null) Log.w(TAG, "fetchEntryDates: User is null.");
            if (getContext() == null) Log.w(TAG, "fetchEntryDates: Context is null.");
            Toast.makeText(getContext(), "Error loading calendar data. Check login status.", Toast.LENGTH_LONG).show();
            progressBarCalendar.setVisibility(View.GONE); // Hide progress if shown
            calendarView.setVisibility(View.VISIBLE); // Ensure calendar is visible even on error
            return;
        }
        String userId = currentUser.getUid();

        Log.d(TAG, "Fetching all entry dates for user: " + userId);
        progressBarCalendar.setVisibility(View.VISIBLE); // Show loading indicator
        // Keep calendar visible, loading happens over it
        // calendarView.setVisibility(View.INVISIBLE); // Don't hide if you want user to see loading

        db.collection("mood_entries")
                .whereEqualTo("uid", userId)
                .get() // Get all documents for this user
                .addOnCompleteListener(task -> {
                    // Check if fragment is still attached before processing result
                    if (!isAdded() || getContext() == null) {
                        Log.w(TAG, "fetchEntryDates: Fragment detached or context null after Firestore query completed.");
                        return;
                    }

                    progressBarCalendar.setVisibility(View.GONE); // Hide progress

                    if (task.isSuccessful() && task.getResult() != null) {
                        datesWithEntries.clear(); // Reset the set for fresh highlighting
                        Log.d(TAG, "fetchEntryDates: Successfully fetched " + task.getResult().size() + " total user entries.");
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                MoodEntry entry = document.toObject(MoodEntry.class);
                                Timestamp ts = entry.getTimestamp();
                                if (ts != null) {
                                    // --- Convert Firestore Timestamp to CalendarDay ---
                                    // Use LOCAL timezone Calendar to map UTC timestamp to correct local day
                                    Calendar localMappingCal = Calendar.getInstance(TimeZone.getDefault());
                                    localMappingCal.setTime(ts.toDate()); // Convert Timestamp to Date

                                    // Create CalendarDay from the LOCAL calendar's date components
                                    CalendarDay dayWithEntry = CalendarDay.from(
                                            localMappingCal.get(Calendar.YEAR),
                                            localMappingCal.get(Calendar.MONTH) + 1, // Material uses 1-12 month
                                            localMappingCal.get(Calendar.DAY_OF_MONTH)
                                    );
                                    datesWithEntries.add(dayWithEntry);
                                    // -----------------------------------------------
                                } else {
                                    Log.w(TAG, "fetchEntryDates: Document " + document.getId() + " has null timestamp.");
                                }
                            } catch(Exception e) {
                                Log.e(TAG, "fetchEntryDates: Error parsing document " + document.getId(), e);
                            }
                        }
                        Log.i(TAG, "fetchEntryDates: Found entries on " + datesWithEntries.size() + " unique days.");
                        applyDecorator(); // Apply highlights based on collected dates
                    } else {
                        Log.e(TAG, "fetchEntryDates: Error fetching entry documents.", task.getException());
                        Toast.makeText(getContext(), "Error loading calendar highlights.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Applies the visual decorator (dots) to the calendar for days that have entries.
     */
    private void applyDecorator() {
        // Check context again before accessing resources
        if (getContext() == null) {
            Log.w(TAG, "applyDecorator: Context is null, cannot apply highlights.");
            return;
        }

        // Always remove previous decorators before adding new ones to prevent stacking
        calendarView.removeDecorators();

        if (!datesWithEntries.isEmpty()) {
            // Create and add the decorator only if there are dates to highlight
            calendarView.addDecorator(new EventDecorator(
                    ContextCompat.getColor(requireContext(), R.color.calendar_event_dot_color),
                    datesWithEntries
            ));
            Log.d(TAG,"applyDecorator: Applied dot decorator for " + datesWithEntries.size() + " days.");
        } else {
            Log.d(TAG,"applyDecorator: No dates with entries found, no decorator applied.");
        }
    }

    /**
     * Navigates to the DayEntries fragment, passing the selected date components.
     * @param date The CalendarDay object selected by the user.
     */
    private void navigateToDayEntries(CalendarDay date) {
        DayEntries dayEntriesFragment = new DayEntries(); // Create instance of your DayEntries fragment

        // Extract date components
        int year = date.getYear();
        int monthFromListener = date.getMonth();       // Month is 1-12 from MaterialCalendarView
        int monthForBundle = monthFromListener - 1;   // Convert to 0-11 for standard Java Calendar & bundle
        int day = date.getDay();

        // --- Log extensively before navigating ---
        Log.i(TAG, "-- Navigating to DayEntries --");
        Log.d(TAG, "Source CalendarDay: " + date.toString());
        Log.d(TAG, "Extracted Components: Year=" + year + ", Month(1-12)=" + monthFromListener + ", Day=" + day);
        Log.d(TAG, "Data being passed in Bundle: Year=" + year + ", Month(0-11)=" + monthForBundle + ", Day=" + day);
        // -----------------------------------------

        // Create bundle and put arguments
        Bundle args = new Bundle();
        args.putInt(DayEntries.ARG_YEAR, year);
        args.putInt(DayEntries.ARG_MONTH, monthForBundle); // Pass the crucial 0-11 month index
        args.putInt(DayEntries.ARG_DAY, day);
        dayEntriesFragment.setArguments(args);

        // Perform fragment transaction
        FragmentManager fragmentManager = getParentFragmentManager();
        if (fragmentManager != null) {
            FragmentTransaction transaction = fragmentManager.beginTransaction();

            // Set standard animations (ensure anim files exist or use android.R.anim defaults)
            transaction.setCustomAnimations(
                    R.anim.slide_in,         // enter
                    android.R.anim.fade_out, // exit (standard fade)
                    android.R.anim.fade_in,  // popEnter (standard fade)
                    R.anim.slide_out         // popExit
            );

            transaction.replace(R.id.main_frame, dayEntriesFragment); // Replace the content frame
            transaction.addToBackStack(null); // Allow user to navigate back to calendar
            transaction.commit();
            Log.d(TAG, "Committed FragmentTransaction to display DayEntries.");
        } else {
            Log.e(TAG, "Parent FragmentManager is null, cannot navigate.");
            Toast.makeText(getContext(), "Error navigating.", Toast.LENGTH_SHORT).show();
        }
    }
}