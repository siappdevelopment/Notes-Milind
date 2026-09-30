package com.notebook.notes.checklist.CallFunction.Adapter;

import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Handler;
import android.widget.FilterQueryProvider;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

public abstract class RemindAdapterCursor<VH extends ViewHolder> extends RecyclerView.Adapter<VH> {

    ChangeObserver changeObserver;
    Cursor cursor;
    DataSetObserver dataSetObserver;
    boolean dataValid;
    FilterQueryProvider filterQueryProvider;
    int rowIdColumn;

    public RemindAdapterCursor(Cursor cursor) {
        initialize(cursor);
    }

    public abstract void onBindViewHolderCursor(VH holder, Cursor cursor);

    protected void onContentChanged() {
    }

    void initialize(Cursor c) {
        boolean z = c != null;
        this.cursor = c;
        this.dataValid = z;
        this.rowIdColumn = z ? c.getColumnIndexOrThrow("_id") : -1;
        this.changeObserver = new ChangeObserver();
        this.dataSetObserver = new DataSetObserverImpl();
        if (z) {
            ChangeObserver changeObserver = this.changeObserver;
            if (changeObserver != null) {
                c.registerContentObserver(changeObserver);
            }
            DataSetObserver dataSetObserver = this.dataSetObserver;
            if (dataSetObserver != null) {
                c.registerDataSetObserver(dataSetObserver);
            }
        }
    }

    @Override
    public void onBindViewHolder(VH holder, int i) {
        if (!dataValid) {
            throw new IllegalStateException("this should only be called when the cursor is valid");
        }
        if (!cursor.moveToPosition(i)) {
            throw new IllegalStateException("couldn't move cursor to position " + i);
        }
        onBindViewHolderCursor(holder, cursor);
    }

    @Override
    public int getItemCount() {
        Cursor cursor;
        if (!dataValid || (cursor = this.cursor) == null) {
            return 0;
        }
        return cursor.getCount();
    }

    @Override
    public long getItemId(int position) {
        Cursor cursor;
        if (dataValid && (cursor = this.cursor) != null && cursor.moveToPosition(position)) {
            return this.cursor.getLong(rowIdColumn);
        }
        return 0L;
    }

    public Cursor swapCursor(Cursor newCursor) {
        Cursor cursor = this.cursor;
        if (newCursor == cursor) {
            return null;
        }
        if (cursor != null) {
            ChangeObserver changeObserver = this.changeObserver;
            if (changeObserver != null) {
                cursor.unregisterContentObserver(changeObserver);
            }
            DataSetObserver dataSetObserver = this.dataSetObserver;
            if (dataSetObserver != null) {
                cursor.unregisterDataSetObserver(dataSetObserver);
            }
        }
        this.cursor = newCursor;
        if (newCursor != null) {
            ChangeObserver changeObserver2 = changeObserver;
            if (changeObserver2 != null) {
                newCursor.registerContentObserver(changeObserver2);
            }
            DataSetObserver dataSetObserver2 = dataSetObserver;
            if (dataSetObserver2 != null) {
                newCursor.registerDataSetObserver(dataSetObserver2);
            }
            rowIdColumn = newCursor.getColumnIndexOrThrow("_id");
            dataValid = true;
            notifyDataSetChanged();
        } else {
            rowIdColumn = -1;
            dataValid = false;
            notifyItemRangeRemoved(0, getItemCount());
        }
        return cursor;
    }

    public void setFilterQueryProvider(FilterQueryProvider filterQueryProvider) {
        this.filterQueryProvider = filterQueryProvider;
    }


    private class ChangeObserver extends ContentObserver {
        public ChangeObserver() {
            super(new Handler());
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            onContentChanged();
        }
    }


    private class DataSetObserverImpl extends DataSetObserver {
        private DataSetObserverImpl() {
        }

        @Override
        public void onChanged() {
            dataValid = true;
            notifyDataSetChanged();
        }

        @Override
        public void onInvalidated() {
            dataValid = false;
            RemindAdapterCursor.this.notifyItemRangeRemoved(0, getItemCount());
        }
    }
}
