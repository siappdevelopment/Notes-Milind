package com.notebook.notes.checklist.CallFunction.Adapter;

import android.content.Context;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.notebook.notes.checklist.CallFunction.fragment.CallShowFragment;
import com.notebook.notes.checklist.CallFunction.fragment.MoreShowFragment;
import com.notebook.notes.checklist.CallFunction.fragment.NotificationShowFragment;
import com.notebook.notes.checklist.CallFunction.fragment.ReminderMessageFragment;


public class CallShowTabAdapter extends FragmentPagerAdapter {
    private Context context;
    private int totalTabs;

    public CallShowTabAdapter(Context context, FragmentManager fragmentManager, int i) {
        super(fragmentManager);
        this.context = context;
        this.totalTabs = i;
    }

    @Override 
    public Fragment getItem(int i) {
        if (i != 0) {
            if (i != 1) {
                if (i != 2) {
                    if (i == 3) {
                        return new MoreShowFragment();
                    }
                    return getItem(i);
                }
                return new NotificationShowFragment();

            }
            return new ReminderMessageFragment();
        }
        return new CallShowFragment();
    }

    @Override
    public int getCount() {
        return this.totalTabs;
    }
}
