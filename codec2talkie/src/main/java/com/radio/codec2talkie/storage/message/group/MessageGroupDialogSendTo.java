package com.radio.codec2talkie.storage.message.group;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;

import com.radio.codec2talkie.R;
import com.radio.codec2talkie.protocol.message.TextMessage;
import com.radio.codec2talkie.settings.SettingsWrapper;
import com.radio.codec2talkie.storage.message.MessageItemActivity;

import java.util.Locale;

public class MessageGroupDialogSendTo extends AlertDialog implements View.OnClickListener {

    private SharedPreferences _sharedPreferences;

    public MessageGroupDialogSendTo(@NonNull Context context) {
        super(context);
        _sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @SuppressLint("MissingSuperCall")
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
            assert targetEdit != null;
            dismiss();

            String srcCallsign = SettingsWrapper.getMyCallsignWithSsid(_sharedPreferences);
            String dstCallsign = targetEdit.getText().toString();
            String groupId = TextMessage.generateGroupId(srcCallsign, dstCallsign);

            Intent messagesIntent = new Intent(v.getContext(), MessageItemActivity.class);
            messagesIntent.putExtra("groupName", groupId);
            v.getContext().startActivity(messagesIntent);

        }  else if (id == R.id.send_message_to_btn_cancel) {
            dismiss();
        }
    }
}
