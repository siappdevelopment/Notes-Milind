package com.notebook.notes.checklist.CallFunction.Adapter;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;


import com.notebook.notes.checklist.CallFunction.Model.ReminderStrModel;
import com.notebook.notes.checklist.CallFunction.Other.RemindContractUtil;
import com.notebook.notes.checklist.CallFunction.Other.ReminderUtilMain;
import com.notebook.notes.checklist.R;

import java.text.SimpleDateFormat;

public class RemindListAdapter extends RemindAdapterCursor<RemindListAdapter.ReminderViewHolder> {

    private Context context;

    public RemindListAdapter(Context context) {
        super(null);
        this.context = context;
    }

    @Override
    public ReminderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ReminderViewHolder(LayoutInflater.from(context).inflate(R.layout.layout_item_reminder, parent, false));
    }

    @Override
    public void onBindViewHolderCursor(ReminderViewHolder holder, Cursor cursor) {
        String format;
        String format2;
        final ReminderStrModel fromCursor = ReminderStrModel.fromCursor(cursor);
        holder.reminderTitleTextView.setText(fromCursor.getReminderTitle().replace("\n", " "));

        TextView textView = holder.reminderDateTimeTextView1;
        if (fromCursor.getReminderDateTime().getTimeInMillis() == 0) {
            format = "";
        } else {
            format = (DateFormat.is24HourFormat(context) ? new SimpleDateFormat("h:mm") : new SimpleDateFormat("h:mm a")).format(fromCursor.getReminderDateTime().getTime());
        }
        textView.setText(format);

        TextView textView2 = holder.reminderDateTimeTextView2;
        if (fromCursor.getReminderDateTime().getTimeInMillis() == 0) {
            format2 = "";
        } else {
            format2 = (DateFormat.is24HourFormat(context) ? new SimpleDateFormat("MMMM dd, yyyy") : new SimpleDateFormat("MMMM dd, yyyy")).format(fromCursor.getReminderDateTime().getTime());
        }
        textView2.setText(format2);

        if (fromCursor.getColor().replace("\n", " ").equals("1")) {
            holder.imageColor.setColorFilter(Color.parseColor("#62CA6E"), PorterDuff.Mode.SRC_IN);

        } else if (fromCursor.getColor().replace("\n", " ").equals("2")) {
            holder.imageColor.setColorFilter(Color.parseColor("#78DCC7"), PorterDuff.Mode.SRC_IN);

        } else if (fromCursor.getColor().replace("\n", " ").equals("3")) {
            holder.imageColor.setColorFilter(Color.parseColor("#FF2ED9"), PorterDuff.Mode.SRC_IN);

        } else if (fromCursor.getColor().replace("\n", " ").equals("4")) {
            holder.imageColor.setColorFilter(Color.parseColor("#FF6A00"), PorterDuff.Mode.SRC_IN);

        } else if (fromCursor.getColor().replace("\n", " ").equals("5")) {
            holder.imageColor.setColorFilter(Color.parseColor("#8F0FFF"), PorterDuff.Mode.SRC_IN);

        } else if (fromCursor.getColor().replace("\n", " ").equals("6")) {
            holder.imageColor.setColorFilter(Color.parseColor("#FFC300"), PorterDuff.Mode.SRC_IN);

        } else if (fromCursor.getColor().replace("\n", " ").equals("7")) {
            holder.imageColor.setColorFilter(Color.parseColor("#00B2FF"), PorterDuff.Mode.SRC_IN);

        } else {
            holder.imageColor.setColorFilter(Color.parseColor("#62CA6E"), PorterDuff.Mode.SRC_IN);

        }


        holder.reminder_delete.setOnClickListener(view -> {


            ContentValues contentValues = new ContentValues();
            contentValues.put(RemindContractUtil.ReminderEntry.COLUMN_REMINDER_DONE, 1);
            int rowsUpdated = context.getContentResolver().update(
                ContentUris.withAppendedId(RemindContractUtil.ReminderEntry.CONTENT_URI, fromCursor.getReminderId().longValue()),
                contentValues,
                null,
                null
            );

            if (rowsUpdated > 0) {
                ReminderUtilMain.refreshWidget(context);

                Cursor query = context.getContentResolver().query(
                    ContentUris.withAppendedId(RemindContractUtil.ReminderEntry.CONTENT_URI, fromCursor.getReminderId().longValue()),
                    null,
                    null,
                    null,
                    null
                );
                if (query != null) {
                    query.moveToFirst();
                    query.close();
                }

                notifyItemChanged(holder.getAdapterPosition());
            }

        });
    }

    public class ReminderViewHolder extends RecyclerView.ViewHolder {

        TextView reminderDateTimeTextView1;
        TextView reminderDateTimeTextView2;
        TextView reminderTitleTextView;
        ImageView reminder_delete;
        ImageView imageColor;

        public ReminderViewHolder(View itemView) {
            super(itemView);
            reminderTitleTextView = itemView.findViewById(R.id.reminder_title_text_view_item);
            reminderDateTimeTextView1 = itemView.findViewById(R.id.reminder_date_time_text_view_item1);
            reminderDateTimeTextView2 = itemView.findViewById(R.id.reminder_date_time_text_view_item2);
            reminder_delete = itemView.findViewById(R.id.reminder_delete);
            imageColor = itemView.findViewById(R.id.imageColor);
        }
    }
}
