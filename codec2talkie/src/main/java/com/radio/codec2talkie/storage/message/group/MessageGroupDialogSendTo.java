package com.radio.codec2talkie.storage.message.group;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;

import com.radio.codec2talkie.R;

public class MessageGroupDialogSendTo extends Dialog implements View.OnClickListener {

    public MessageGroupDialogSendTo(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Send message to...");
        setContentView(R.layout.activity_send_message_to);
    }

    @Override
    public void onClick(View v) {
    }
}
