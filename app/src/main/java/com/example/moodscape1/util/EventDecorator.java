package com.example.moodscape1.util;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.style.ForegroundColorSpan;

import androidx.core.content.ContextCompat;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.spans.DotSpan;

import java.util.Collection;
import java.util.HashSet;

public class EventDecorator implements DayViewDecorator {
    private final int color;
    private final HashSet<CalendarDay> dates;

    public EventDecorator(int color, Collection<CalendarDay> dates) {
        this.color = color;
        this.dates = new HashSet<>(dates);
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return dates.contains(day); // Check if the day is in our set of days with entries
    }

    @Override
    public void decorate(DayViewFacade view) {
        // Add a dot below the date number
        view.addSpan(new DotSpan(7, color)); // Adjust size (5f) and color as needed
        // Alternatively, you could set a background or change text color
        // view.setSelectionDrawable(ContextCompat.getDrawable(context, R.drawable.my_selector));
        // view.addSpan(new ForegroundColorSpan(Color.BLUE));
    }
}

