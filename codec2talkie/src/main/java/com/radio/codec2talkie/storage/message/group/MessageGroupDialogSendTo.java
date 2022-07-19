package com.radio.codec2talkie.storage.message.group;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.radio.codec2talkie.R;
import com.radio.codec2talkie.app.AppService;
import com.radio.codec2talkie.protocol.message.TextMessage;

public class MessageGroupDialogSendTo extends AlertDialog implements View.OnClickListener {

    private final AppService _appService;

    public MessageGroupDialogSendTo(@NonNull Context context, AppService appService) {
        super(context);
        _appService = appService;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTitle(getContext().getString(R.string.activity_send_message_to_title));
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
            EditText targetEdit = findViewById(R.id.send_message_to_target_edit);
            EditText messageEdit = findViewById(R.id.send_message_to_message_edit);
            assert targetEdit != null;
            assert messageEdit != null;
            TextMessage textMessage = new TextMessage();
            textMessage.dst = targetEdit.getText().toString();
            textMessage.text = messageEdit.getText().toString();
            _appService.sendTextMessage(textMessage);
            dismiss();
        }  else if (id == R.id.send_message_to_btn_cancel) {
            dismiss();
        }
    }
}
