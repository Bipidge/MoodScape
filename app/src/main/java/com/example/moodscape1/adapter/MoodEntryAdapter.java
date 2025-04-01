package com.example.moodscape1.adapter;

import android.content.Context;
import android.graphics.Color; // Import Color
import android.graphics.drawable.GradientDrawable; // For gradient backgrounds
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView; // Import CardView if using it as root
import androidx.core.content.ContextCompat; // For colors
import androidx.recyclerview.widget.RecyclerView;

import com.example.moodscape1.R;
import com.example.moodscape1.model.MoodEntry;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MoodEntryAdapter extends RecyclerView.Adapter<MoodEntryAdapter.MoodEntryViewHolder> {
    private List<MoodEntry> entryList;
    private Context context;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());


    public MoodEntryAdapter(Context context, List<MoodEntry> entryList) {
        this.context = context;
        this.entryList = entryList;
    }

    @NonNull
    @Override
    public MoodEntryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_mood_entry, parent, false);
        return new MoodEntryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MoodEntryViewHolder holder, int position) {
        MoodEntry entry = entryList.get(position);

        holder.textViewEmotion.setText(entry.getEmotion());
        holder.textViewEntryText.setText(entry.getEntryText());

        // Format and set Timestamp
        Timestamp ts = entry.getTimestamp();
        if (ts != null) {
            holder.textViewTimestampSide.setText(
                    String.format("%s\n%s", timeFormat.format(ts.toDate()), dateFormat.format(ts.toDate()))
            );
        } else {
            holder.textViewTimestampSide.setText("No Date");
        }

        // Set background color based on emotion
        holder.setCardBackgroundColor(entry.getEmotion());
    }

    @Override
    public int getItemCount() {
        return entryList.size();
    }

    // Method to update the list
    public void updateEntries(List<MoodEntry> newEntries) {
        this.entryList.clear();
        this.entryList.addAll(newEntries);
        notifyDataSetChanged(); // Notify adapter data has changed
    }


    // --- ViewHolder Class ---
    public static class MoodEntryViewHolder extends RecyclerView.ViewHolder {
        TextView textViewEmotion, textViewEntryText, textViewTimestampSide;
        CardView cardViewEntry; // Reference to the card or root layout of the item
        LinearLayout linearLayoutCardContent; // Reference to the inner layout

        public MoodEntryViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewEmotion = itemView.findViewById(R.id.textViewEmotion);
            textViewEntryText = itemView.findViewById(R.id.textViewEntryText);
            textViewTimestampSide = itemView.findViewById(R.id.textViewTimestampSide);
            cardViewEntry = itemView.findViewById(R.id.cardViewEntry); // Get card view
            linearLayoutCardContent = itemView.findViewById(R.id.linearLayoutCardContent); // Get inner layout
        }

        // Helper method to set background based on emotion
        public void setCardBackgroundColor(String emotion) {
            int startColor;
            int endColor;
            Context ctx = itemView.getContext(); // Get context from the item view

            switch (emotion.toLowerCase()) {
                case "happy":
                    startColor = ContextCompat.getColor(ctx, R.color.emotion_happy_start); // Define these in colors.xml
                    endColor = ContextCompat.getColor(ctx, R.color.emotion_happy_end);
                    break;
                case "calm":
                    startColor = ContextCompat.getColor(ctx, R.color.emotion_calm_start);
                    endColor = ContextCompat.getColor(ctx, R.color.emotion_calm_end);
                    break;
                case "sad":
                    startColor = ContextCompat.getColor(ctx, R.color.emotion_sad_start);
                    endColor = ContextCompat.getColor(ctx, R.color.emotion_sad_end);
                    break;
                case "angry":
                    startColor = ContextCompat.getColor(ctx, R.color.emotion_angry_start);
                    endColor = ContextCompat.getColor(ctx, R.color.emotion_angry_end);
                    break;
                default:
                    startColor = ContextCompat.getColor(ctx, R.color.emotion_default_start);
                    endColor = ContextCompat.getColor(ctx, R.color.emotion_default_end);
            }

            // Create a GradientDrawable
            GradientDrawable gradientDrawable = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM, // Gradient direction
                    new int[]{startColor, endColor}); // Colors

            // Set the corner radius (optional, CardView already has corners)
            // float cornerRadius = ctx.getResources().getDimension(R.dimen.card_corner_radius); // Define in dimens.xml if needed
            // gradientDrawable.setCornerRadius(cornerRadius);

            // Apply the gradient background to the LinearLayout inside the CardView
            if (linearLayoutCardContent != null) {
                linearLayoutCardContent.setBackground(gradientDrawable);
                // Ensure CardView background is transparent or white so gradient shows through
                if(cardViewEntry != null) cardViewEntry.setCardBackgroundColor(Color.TRANSPARENT);
            }
            // Or apply directly to CardView if you prefer (might override corner radius effect slightly)
            // if (cardViewEntry != null) {
            //     cardViewEntry.setBackground(gradientDrawable);
            // }
        }
    }
}
