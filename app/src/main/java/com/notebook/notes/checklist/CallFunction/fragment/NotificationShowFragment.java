package com.notebook.notes.checklist.CallFunction.fragment;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.notebook.notes.checklist.CallFunction.Adapter.RemindListAdapter;
import com.notebook.notes.checklist.CallFunction.Notification.ReminderSchedule;
import com.notebook.notes.checklist.CallFunction.Other.RemindContractUtil;
import com.notebook.notes.checklist.CallFunction.Other.ReminderUtilMain;
import com.notebook.notes.checklist.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;


public class NotificationShowFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private LinearLayout linearColor1, linearColor2, linearColor3, linearColor4, linearColor5, linearColor6, linearColor7, SelectDate, linearCreateView;
    private RecyclerView recycleView;
    private TextView buttonReminder, buttonYes, add_date_time_reminder, buttonNo, et_reminder_add;
    private Calendar reminderDateTime;
    private String reminderTitle = "";
    private RemindListAdapter adapterReminder;
    private String color = "1";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notification, container, false);

        linearColor1 = view.findViewById(R.id.linearColor1);
        linearColor2 = view.findViewById(R.id.linearColor2);
        linearColor3 = view.findViewById(R.id.linearColor3);
        linearColor4 = view.findViewById(R.id.linearColor4);
        linearColor5 = view.findViewById(R.id.linearColor5);
        linearColor6 = view.findViewById(R.id.linearColor6);
        linearColor7 = view.findViewById(R.id.linearColor7);
        SelectDate = view.findViewById(R.id.SelectDate);
        recycleView = view.findViewById(R.id.recycleView);
        linearCreateView = view.findViewById(R.id.linearCreateView);
        buttonReminder = view.findViewById(R.id.buttonReminder);
        buttonYes = view.findViewById(R.id.buttonYes);
        add_date_time_reminder = view.findViewById(R.id.add_date_time_reminder);
        buttonNo = view.findViewById(R.id.buttonNo);
        et_reminder_add = view.findViewById(R.id.edittxt);

        buttonNo.setOnClickListener(v -> {
            linearCreateView.setVisibility(View.GONE);
            buttonReminder.setVisibility(View.VISIBLE);
            recycleView.setVisibility(View.VISIBLE);
        });

        adapterReminder = new RemindListAdapter(requireActivity());
        recycleView.setAdapter(adapterReminder);
        recycleView.setLayoutManager(new LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false));

        requireActivity().getSupportLoaderManager().initLoader(0, null, this);

        reminderDateTime = Calendar.getInstance();
        reminderDateTime.add(Calendar.MINUTE, 1);

        SimpleDateFormat dateFormat = DateFormat.is24HourFormat(requireActivity())
            ? new SimpleDateFormat("MMMM dd, yyyy  HH:mm")
            : new SimpleDateFormat("MMMM dd, yyyy  h:mm a");

        add_date_time_reminder.setText(dateFormat.format(reminderDateTime.getTime()));

        buttonReminder.setOnClickListener(v -> {
            linearCreateView.setVisibility(View.VISIBLE);
            buttonReminder.setVisibility(View.GONE);
            recycleView.setVisibility(View.GONE);
            et_reminder_add.setText("");
        });

        buttonYes.setOnClickListener(v -> {
            reminderTitle = et_reminder_add.getText().toString().trim();
            if (!reminderTitle.isEmpty()) {
                ContentValues contentValues = new ContentValues();
                contentValues.put("title", reminderTitle);
                contentValues.put("color", color);
                contentValues.put(RemindContractUtil.ReminderEntry.COLUMN_REMINDER_DATE_TIME, reminderDateTime.getTimeInMillis());
                contentValues.put(RemindContractUtil.ReminderEntry.COLUMN_REMINDER_DONE, 0);

                Uri insert = requireContext().getContentResolver().insert(RemindContractUtil.ReminderEntry.CONTENT_URI, contentValues);
                ReminderUtilMain.refreshWidget(requireActivity());

                if (reminderDateTime.getTimeInMillis() > Calendar.getInstance().getTimeInMillis()) {
                    ReminderSchedule.scheduleReminder(requireActivity(), "Pending Reminder", reminderTitle, reminderDateTime, 102);
                }
            } else {
                et_reminder_add.setError("Please Enter Reminder Title");
            }
            linearCreateView.setVisibility(View.GONE);
            buttonReminder.setVisibility(View.VISIBLE);
            recycleView.setVisibility(View.VISIBLE);
        });

        SelectDate.setOnClickListener(v -> {
            Calendar calendar2 = Calendar.getInstance();
            reminderDateTime = Calendar.getInstance();
            new TimePickerDialog(requireActivity(), (timePicker, hourOfDay, minute) -> {
                reminderDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                reminderDateTime.set(Calendar.MINUTE, minute);
                new DatePickerDialog(requireActivity(), (datePicker, year, month, dayOfMonth) -> {
                    reminderDateTime.set(Calendar.YEAR, year);
                    reminderDateTime.set(Calendar.MONTH, month);
                    reminderDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    add_date_time_reminder.setText(dateFormat.format(reminderDateTime.getTime()));
                }, calendar2.get(Calendar.YEAR), calendar2.get(Calendar.MONTH), calendar2.get(Calendar.DAY_OF_MONTH)).show();
            }, calendar2.get(Calendar.HOUR_OF_DAY), calendar2.get(Calendar.MINUTE), DateFormat.is24HourFormat(requireActivity())).show();
        });
        linearColor1.setOnClickListener(view1 -> {
            unselected();
            selected(linearColor1);
            color = "1";
        });

        linearColor2.setOnClickListener(view1 -> {
            unselected();
            selected(linearColor2);
            color = "2";
        });

        linearColor3.setOnClickListener(view1 -> {
            unselected();
            selected(linearColor3);
            color = "3";
        });

        linearColor4.setOnClickListener(view1 -> {
            unselected();
            selected(linearColor4);
            color = "4";
        });

        linearColor5.setOnClickListener(view1 -> {
            unselected();
            selected(linearColor5);
            color = "5";
        });

        linearColor6.setOnClickListener(view1 -> {
            unselected();
            selected(linearColor6);
            color = "6";
        });

        linearColor7.setOnClickListener(view1 -> {
            unselected();
            selected(linearColor7);
            color = "7";
        });


        return view;
    }
    private void selected(LinearLayout linearColor) {
        linearColor.setBackgroundDrawable(getResources().getDrawable(R.drawable.iv_circle_bac_icon));
    }

    private void unselected() {
        linearColor1.setBackgroundDrawable(getResources().getDrawable(android.R.color.transparent));
        linearColor2.setBackgroundDrawable(getResources().getDrawable(android.R.color.transparent));
        linearColor3.setBackgroundDrawable(getResources().getDrawable(android.R.color.transparent));
        linearColor4.setBackgroundDrawable(getResources().getDrawable(android.R.color.transparent));
        linearColor5.setBackgroundDrawable(getResources().getDrawable(android.R.color.transparent));
        linearColor6.setBackgroundDrawable(getResources().getDrawable(android.R.color.transparent));
        linearColor7.setBackgroundDrawable(getResources().getDrawable(android.R.color.transparent));
    }
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(requireActivity(), RemindContractUtil.ReminderEntry.CONTENT_URI, null, "reminder_done = 0", null, null);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        adapterReminder.swapCursor(null);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        adapterReminder.swapCursor(data);
    }
}
