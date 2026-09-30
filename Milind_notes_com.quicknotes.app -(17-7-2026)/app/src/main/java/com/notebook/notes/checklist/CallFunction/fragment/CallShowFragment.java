package com.notebook.notes.checklist.CallFunction.fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;


import com.notebook.notes.checklist.R;

import java.util.Calendar;
import java.util.Random;

public class CallShowFragment extends Fragment {
    private TextView textViewDay1;
    private TextView textViewDay2;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_call_event, container, false);
        textViewDay1 = view.findViewById(R.id.textViewDay1);
        textViewDay2 = view.findViewById(R.id.textViewDay2);
        showCurrentDateTime();
        showGreeting();

        return view;
    }

    private void showCurrentDateTime() {
        Random random = new Random();
        int sunriseMinute = random.nextInt(60);
        int sunsetMinute = random.nextInt(60);

        String sunriseTime = "6:" + String.format("%02d", sunriseMinute);
        String sunsetTime = "18:" + String.format("%02d", sunsetMinute);

        textViewDay2.setText("Today, the sun rises at " + sunriseTime + " and sets at " + sunsetTime);
    }

    private void showGreeting() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        String greeting;
        if (hour >= 5 && hour < 12) {
            greeting = "Good Morning!";
        } else if (hour >= 12 && hour < 18) {
            greeting = "Good Afternoon!";
        } else {
            greeting = "Good Night!";
        }

        textViewDay1.setText(greeting);
    }
}
