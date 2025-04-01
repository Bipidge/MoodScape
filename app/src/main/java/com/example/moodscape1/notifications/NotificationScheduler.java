package com.example.moodscape1.notifications;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import java.util.Calendar;

public class NotificationScheduler {
    private static final String TAG = "NotificationScheduler";
    // Unique request codes for each distinct alarm
    private static final int RC_MORNING = 101;
    private static final int RC_AFTERNOON = 102;
    private static final int RC_EVENING = 103;

    public static void scheduleReminderNotifications(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is null, cannot schedule notifications.");
            return;
        }

        // Check for permission (Android 12+) - may prevent setting alarms
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Cannot schedule exact alarms. Notifications might be delayed or not fire.");
                // Consider guiding user to settings: new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                Toast.makeText(context, "App needs permission to schedule notifications accurately.", Toast.LENGTH_LONG).show();
                // Continue with inexact alarms or stop? For now, let's try inexact.
            }
        }


        // Define times (Adjust as needed)
        int morningHour = 9;
        int afternoonHour = 15; // 3 PM
        int eveningHour = 20; // 8 PM

        // Schedule three separate repeating alarms
        scheduleSingleAlarm(context, alarmManager, morningHour, RC_MORNING);
        scheduleSingleAlarm(context, alarmManager, afternoonHour, RC_AFTERNOON);
        scheduleSingleAlarm(context, alarmManager, eveningHour, RC_EVENING);

        Log.i(TAG, "Scheduled daily reminders for morning, afternoon, and evening.");
        Toast.makeText(context, "Daily reminders scheduled", Toast.LENGTH_SHORT).show();
    }

    private static void scheduleSingleAlarm(Context context, AlarmManager alarmManager, int hour, int requestCode) {
        Intent intent = new Intent(context, ReminderBroadcastReceiver.class);
        // Optional: Add action or data to differentiate intents if needed
        intent.setAction("REMINDER_ALARM_" + requestCode);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // If the time has already passed for today, schedule it for tomorrow
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            Log.d(TAG, "Scheduling first trigger for request " + requestCode + " for tomorrow: " + calendar.getTime());
        } else {
            Log.d(TAG, "Scheduling first trigger for request " + requestCode + " for today: " + calendar.getTime());
        }


        // Use setInexactRepeating for better battery performance.
        // AlarmManager fires the alarm sometime around the specified time.
        alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP, // Wake device up if needed
                calendar.getTimeInMillis(), // First trigger time
                AlarmManager.INTERVAL_DAY, // Repeat interval (daily)
                pendingIntent
        );

        // If exact timing is needed (and permission granted):
         /*
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
             alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
             // Note: For repeating exact alarms, you'd need to reschedule in the receiver.
          } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
             alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
          } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
             alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
          } else {
             alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
          }
          // And manually reschedule in onReceive for repeating exact
          */
    }

    public static void cancelReminderNotifications(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is null, cannot cancel notifications.");
            return;
        }

        // Cancel all three alarms using the same pending intents used to schedule them
        cancelSingleAlarm(context, alarmManager, RC_MORNING);
        cancelSingleAlarm(context, alarmManager, RC_AFTERNOON);
        cancelSingleAlarm(context, alarmManager, RC_EVENING);

        Log.i(TAG, "Cancelled scheduled daily reminders.");
        Toast.makeText(context, "Daily reminders cancelled", Toast.LENGTH_SHORT).show();
    }

    private static void cancelSingleAlarm(Context context, AlarmManager alarmManager, int requestCode) {
        Intent intent = new Intent(context, ReminderBroadcastReceiver.class);
        intent.setAction("REMINDER_ALARM_" + requestCode); // Must match the action used for scheduling

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE // Same flags
        );
        alarmManager.cancel(pendingIntent);
        Log.d(TAG, "Cancelled pending intent with request code: " + requestCode);
    }
}

