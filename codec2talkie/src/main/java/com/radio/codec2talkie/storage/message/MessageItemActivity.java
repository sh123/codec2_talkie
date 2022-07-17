package com.radio.codec2talkie.storage.message;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.radio.codec2talkie.R;
import com.radio.codec2talkie.storage.message.group.MessageGroupViewModel;

public class MessageItemActivity extends AppCompatActivity {

    private MessageGroupViewModel _messageGroupViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_view);
        setTitle(R.string.messages_group_view_title);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            finish();
            return true;
        }
        else if (itemId == R.id.messages_send) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
