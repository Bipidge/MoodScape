package com.example.moodscape1.notifications;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.moodscape1.MainActivity;
import com.example.moodscape1.R; // Your R file
import com.example.moodscape1.util.SettingsManager;

public class ReminderBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "ReminderReceiver";
    public static final String CHANNEL_ID = "MOODSCAPE_REMINDER_CHANNEL";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Broadcast received for reminder notification.");

        // Check if notifications are still enabled by the user
        SettingsManager settingsManager = new SettingsManager(context);
        if (!settingsManager.areNotificationsEnabled()) {
            Log.d(TAG, "Notifications disabled in settings, skipping reminder.");
            // Optional: Consider cancelling future alarms here if needed
            // NotificationScheduler.cancelReminderNotifications(context);
            return;
        }

        // Intent to launch MainActivity when notification is tapped
        Intent mainActivityIntent = new Intent(context, MainActivity.class);
        mainActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0, // Request code (can be unique if needed)
                mainActivityIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT // Recommended flags
        );

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_add_24) // Replace with your app's icon
                .setContentTitle("Moodscape Reminder")
                .setContentText("Don't forget to make an entry on your emotions.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent) // Set the action to perform on tap
                .setAutoCancel(true); // Dismiss notification on tap

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // Check for POST_NOTIFICATIONS permission before notifying (Android 13+)
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED)
        {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Cannot show notification.");
            // Consider logging this or handling it if permission might be revoked after scheduling
            return;
        }

        // Notify - Use a consistent ID or generate unique ones if needed
        int notificationId = 1001; // Simple static ID for the reminder
        notificationManager.notify(notificationId, builder.build());
        Log.d(TAG, "Reminder notification posted.");
    }
}

