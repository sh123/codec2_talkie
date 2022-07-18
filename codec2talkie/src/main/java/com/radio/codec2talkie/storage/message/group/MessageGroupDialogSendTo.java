package com.radio.codec2talkie.storage.message.group;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.radio.codec2talkie.R;

public class MessageGroupDialogSendTo extends AlertDialog implements View.OnClickListener {

    public MessageGroupDialogSendTo(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(.getString(R.string.activity_send_message_to_title));
        setContentView(R.layout.activity_send_message_to);

        Button sendButton = findViewById(R.id.send_message_to_btn_ok);
        assert sendButton != null;
        sendButton.setOnClickListener(this);
        Button cancelButton = findViewById(R.id.send_message_to_btn_cancel);
        assert cancelButton != null;
        cancelButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.send_message_to_btn_ok) {
        }  else if (id == R.id.send_message_to_btn_cancel) {
            dismiss();
        }
    }
}
