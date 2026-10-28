package com.notebook.notes.checklist.CallFunction.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.notebook.notes.checklist.R;


public class ReminderMessageFragment extends Fragment {

    private EditText editTextEditor;
    private ImageView imageSendFive, imageSendOne, imageSendThree, imageSendTwo;
    private CheckBox checkBoxIntro1, checkBoxIntro2, checkBoxIntro3;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_remind_msg, container, false);

        checkBoxIntro1 = view.findViewById(R.id.checkBoxIntro1);
        checkBoxIntro2 = view.findViewById(R.id.checkBoxIntro2);
        checkBoxIntro3 = view.findViewById(R.id.checkBoxIntro3);
        editTextEditor = view.findViewById(R.id.edittxt);
        imageSendOne = view.findViewById(R.id.send_one);
        imageSendTwo = view.findViewById(R.id.send_two);
        imageSendThree = view.findViewById(R.id.send_three);
        imageSendFive = view.findViewById(R.id.send_five);

        checkBoxIntro1.setOnClickListener(v -> {
            checkBoxIntro1.setChecked(true);
            checkBoxIntro2.setChecked(false);
            checkBoxIntro3.setChecked(false);
            imageSendOne.setVisibility(View.VISIBLE);
            imageSendTwo.setVisibility(View.GONE);
            imageSendThree.setVisibility(View.GONE);
        });

        checkBoxIntro2.setOnClickListener(v -> {
            checkBoxIntro1.setChecked(false);
            checkBoxIntro2.setChecked(true);
            checkBoxIntro3.setChecked(false);
            imageSendOne.setVisibility(View.GONE);
            imageSendTwo.setVisibility(View.VISIBLE);
            imageSendThree.setVisibility(View.GONE);
        });

        checkBoxIntro3.setOnClickListener(v -> {
            checkBoxIntro1.setChecked(false);
            checkBoxIntro2.setChecked(false);
            checkBoxIntro3.setChecked(true);
            imageSendOne.setVisibility(View.GONE);
            imageSendTwo.setVisibility(View.GONE);
            imageSendThree.setVisibility(View.VISIBLE);
        });

        imageSendOne.setOnClickListener(v -> sendMessage("Sorry, I can’t talk right now"));
        imageSendTwo.setOnClickListener(v -> sendMessage("Can I call you later"));
        imageSendThree.setOnClickListener(v -> sendMessage("I'm on my way"));
        imageSendFive.setOnClickListener(v -> sendCustomMessage());

        return view;
    }

    private void sendMessage(String message) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("smsto:"));
        intent.putExtra("sms_body", message);
        startActivity(intent);
        requireActivity().finish();
    }

    private void sendCustomMessage() {
        String message = editTextEditor.getText().toString();
        if (!message.isEmpty()) {
            sendMessage(message);
        } else {
            Toast.makeText(requireContext(), "Please enter a message", Toast.LENGTH_SHORT).show();
        }
    }
}
