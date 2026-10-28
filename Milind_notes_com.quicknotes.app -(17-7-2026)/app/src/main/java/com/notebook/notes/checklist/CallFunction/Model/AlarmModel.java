package com.notebook.notes.checklist.CallFunction.Model;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseBooleanArray;


public final class AlarmModel implements Parcelable {

    public static final Creator<AlarmModel> CREATOR = new Creator<AlarmModel>() {

        @Override
        public AlarmModel createFromParcel(Parcel in) {
            return new AlarmModel(in);
        }


        @Override
        public AlarmModel[] newArray(int size) {
            return new AlarmModel[size];
        }
    };

    private final long id;
    private SparseBooleanArray allDays;
    private boolean isEnabled;
    private String label;
    private long time;

    private AlarmModel(Parcel in) {
        this.id = in.readLong();
        this.time = in.readLong();
        this.label = in.readString();
        this.allDays = in.readSparseBooleanArray();
        this.isEnabled = in.readByte() != 0;
    }

    public AlarmModel() {
        this(-1L);
    }

    public AlarmModel(long id) {
        this(id, System.currentTimeMillis(), new int[0]);
    }

    public AlarmModel(long id, long time, int... days) {
        this(id, time, null, days);
    }

    public AlarmModel(long id, long time, String label, int... days) {
        this.id = id;
        this.time = time;
        this.label = label;
        this.allDays = buildDaysArray(days);
    }

    private static SparseBooleanArray buildDaysArray(int... days) {
        SparseBooleanArray buildBaseDaysArray = buildBaseDaysArray();
        for (int i : days) {
            buildBaseDaysArray.append(i, true);
        }
        return buildBaseDaysArray;
    }

    private static SparseBooleanArray buildBaseDaysArray() {
        SparseBooleanArray sparseBooleanArray = new SparseBooleanArray(7);
        sparseBooleanArray.put(1, false);
        sparseBooleanArray.put(2, false);
        sparseBooleanArray.put(3, false);
        sparseBooleanArray.put(4, false);
        sparseBooleanArray.put(5, false);
        sparseBooleanArray.put(6, false);
        sparseBooleanArray.put(7, false);
        return sparseBooleanArray;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(id);
        parcel.writeLong(time);
        parcel.writeString(label);
        parcel.writeSparseBooleanArray(allDays);
        parcel.writeByte(isEnabled ? (byte) 1 : (byte) 0);
    }

    public long getId() {
        return id;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setDay(int day, boolean isAlarmed) {
        allDays.append(day, isAlarmed);
    }

    public SparseBooleanArray getDays() {
        return allDays;
    }

    public boolean getDay(int day) {
        return allDays.get(day);
    }

    public void setIsEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public int notificationId() {
        long id = getId();
        return (int) (id ^ (id >>> 32));
    }

    public String toString() {
        return "Alarm{id=" + id + ", time=" + time + ", label='" + label + "', allDays=" + allDays + ", isEnabled=" + isEnabled + '}';
    }

    public int hashCode() {
        long j = id;
        long j2 = time;
        int hashCode = ((((527 + ((int) (j ^ (j >>> 32)))) * 31) + ((int) (j2 ^ (j2 >>> 32)))) * 31) + label.hashCode();
        for (int i = 0; i < allDays.size(); i++) {
            hashCode = (hashCode * 31) + (allDays.valueAt(i) ? 1 : 0);
        }
        return hashCode;
    }

}
