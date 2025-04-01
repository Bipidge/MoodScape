package com.example.moodscape1.model;

import com.google.firebase.Timestamp; // Import Firebase Timestamp
import com.google.firebase.firestore.ServerTimestamp; // Import annotation

public class MoodEntry {
    private String uid;
    private String emotion;
    private String entryText;
    @ServerTimestamp // Tells Firestore to populate this with server time on creation
    private Timestamp timestamp; // Use Firebase Timestamp

    // IMPORTANT: Default empty constructor required for Firestore deserialization
    public MoodEntry() {}

    public MoodEntry(String uid, String emotion, String entryText, Timestamp timestamp) {
        this.uid = uid;
        this.emotion = emotion;
        this.entryText = entryText;
        this.timestamp = timestamp;
    }

    // --- Getters ---
    public String getUid() {
        return uid;
    }

    public String getEmotion() {
        return emotion;
    }

    public String getEntryText() {
        return entryText;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    // --- Setters (Optional, but good practice, also needed by Firestore sometimes) ---
    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setEmotion(String emotion) {
        this.emotion = emotion;
    }

    public void setEntryText(String entryText) {
        this.entryText = entryText;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}

