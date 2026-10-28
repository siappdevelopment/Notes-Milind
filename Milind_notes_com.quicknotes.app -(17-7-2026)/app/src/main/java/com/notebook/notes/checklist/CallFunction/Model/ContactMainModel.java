package com.notebook.notes.checklist.CallFunction.Model;

import android.graphics.Color;


public class ContactMainModel {
    public static final String[] COLOR_CODES = {"#7386FF", "#0087FF", "#F057EE", "#A469FF", "#ED8D00", "#0D931C"};
    private long GroupId;
    private String GroupTitle;
    private String Note;
    private String _id;
    private String company;
    private String contactId;
    private String displayName;
    private String email;
    private String firstName;
    private boolean isSelected;
    private String middleName;
    private String number;
    private String surname;
    private String userThumbnail;
    private int color = -1;
    private int contactsCount = 0;
    private int colorIndex = -1;

    public ContactMainModel() {
    }

    public boolean hasThumbnail() {
        return this.userThumbnail != null;
    }

    public ContactMainModel(String str) {
        this.GroupTitle = str;
    }

    public String get_id() {
        return this._id;
    }

    public void set_id(String str) {
        this._id = str;
    }

    public String getContactId() {
        return this.contactId;
    }

    public void setContactId(String str) {
        this.contactId = str;
        setColorIndexBasedOnId(str);
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public void setDisplayName(String str) {
        this.displayName = str;
    }

    public String getFirstName() {
        return this.firstName;
    }

    public void setFirstName(String str) {
        this.firstName = str;
    }

    public String getMiddleName() {
        return this.middleName;
    }

    public void setMiddleName(String str) {
        this.middleName = str;
    }

    public String getSurname() {
        return this.surname;
    }

    public void setSurname(String str) {
        this.surname = str;
    }

    public String getCompany() {
        return this.company;
    }

    public void setCompany(String str) {
        this.company = str;
    }

    public String getNumber() {
        return this.number;
    }

    public void setNumber(String str) {
        this.number = str;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String str) {
        this.email = str;
    }

    public String getUserThumbnail() {
        return this.userThumbnail;
    }

    public void setUserThumbnail(String str) {
        this.userThumbnail = str;
    }

    public String getNote() {
        return this.Note;
    }

    public void setNote(String str) {
        this.Note = str;
    }

    public long getGroupId() {
        return this.GroupId;
    }

    public void setGroupId(long j) {
        this.GroupId = j;
    }

    public String getGroupTitle() {
        return this.GroupTitle;
    }

    public void setGroupTitle(String str) {
        this.GroupTitle = str;
    }

    public int getContactsCount() {
        return this.contactsCount;
    }

    public void setContactsCount(int i) {
        this.contactsCount = i;
    }

    public int getColorIndex() {
        return this.colorIndex;
    }

    public void setColorIndex(int i) {
        this.colorIndex = i;
        String[] strArr = COLOR_CODES;
        this.colorIndex = Math.max(0, Math.min(i, strArr.length - 1));
        try {
            this.color = Color.parseColor(strArr[i]);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            this.color = -1;
        }
    }

    public int getColor() {
        return this.color;
    }

    public void setColor(int i) {
        this.color = i;
    }

    public boolean isSelected() {
        return this.isSelected;
    }

    public void setSelected(boolean z) {
        this.isSelected = z;
    }

    private void setColorIndexBasedOnId(String str) {
        if (str != null) {
            setColorIndex(Math.abs(str.hashCode()) % COLOR_CODES.length);
        }
    }
}
