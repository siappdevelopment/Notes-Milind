package com.notebook.notes.checklist.CallFunction.fragment;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.notebook.notes.checklist.R;


public class MoreShowFragment extends Fragment {
    private LinearLayout linearContacts;
    private LinearLayout linearMessage;
    private LinearLayout linearSendMail;
    private LinearLayout linearWeb;
    private LinearLayout linearCalender;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_more_event, container, false);

        linearContacts = view.findViewById(R.id.linearContacts);
        linearMessage = view.findViewById(R.id.linearMessage);
        linearSendMail = view.findViewById(R.id.linearSendMail);
        linearWeb = view.findViewById(R.id.linearWeb);
        linearCalender = view.findViewById(R.id.linearCalender);

        linearContacts.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                startActivity(intent);
                requireActivity().finish();
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Unable to open dialer", Toast.LENGTH_SHORT).show();
            }
        });

        linearCalender.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("content://com.android.calendar/time"));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(requireActivity(), "No Calendar app found!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(requireActivity(), "Error opening calendar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            requireActivity().finish();
        });

        linearMessage.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("smsto:"));
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(requireActivity(), "No Messaging app found!", Toast.LENGTH_SHORT).show();
            }
            requireActivity().finish();

        });

        linearSendMail.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"));
                startActivity(intent);
                requireActivity().finish();
            } catch (Exception e) {
                Toast.makeText(requireContext(), "No Email app found", Toast.LENGTH_SHORT).show();
            }
        });

        linearWeb.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.google.com"));
                startActivity(intent);
                requireActivity().finish();
            } catch (Exception e) {
                Toast.makeText(requireContext(), "No Browser found", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }
}
