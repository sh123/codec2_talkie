package com.radio.codec2talkie.storage.message;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.radio.codec2talkie.R;
import com.radio.codec2talkie.app.AppService;
import com.radio.codec2talkie.protocol.message.TextMessage;
import com.radio.codec2talkie.storage.message.group.MessageGroupActivity;
import com.radio.codec2talkie.storage.message.group.MessageGroupAdapter;
import com.radio.codec2talkie.storage.message.group.MessageGroupViewModel;

public class MessageItemActivity extends AppCompatActivity implements ServiceConnection, View.OnClickListener {

    private static final String TAG = MessageItemActivity.class.getSimpleName();

    private MessageItemViewModel _messageViewModel;

    private String _groupName;

    private AppService _appService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindAppService();

        setContentView(R.layout.activity_message_view);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        RecyclerView recyclerView = findViewById(R.id.message_recyclerview);
        recyclerView.setHasFixedSize(true);
        final MessageItemAdapter adapter = new MessageItemAdapter(new MessageItemAdapter.MessageItemDiff());
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL));

        _groupName = (String) getIntent().getExtras().get("groupName");
        _messageViewModel = new ViewModelProvider(this).get(MessageItemViewModel.class);
        _messageViewModel.getMessages(_groupName).observe(this, adapter::submitList);

        setTitle(String.format("%s to %s", getString(R.string.messages_view_title), _groupName));

        Button sendButton = findViewById(R.id.messages_send);
        assert sendButton != null;
        sendButton.setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy()");
        unbindAppService();
    }

    private void bindAppService() {
        if (!bindService(new Intent(this, AppService.class), this, Context.BIND_AUTO_CREATE)) {
            Log.e(TAG, "Service does not exists or no access");
        }
    }

    private void unbindAppService() {
        unbindService(this);
    }

    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
        Log.i(TAG, "Connected to app service");
        _appService = ((AppService.AppServiceBinder)service).getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
        Log.i(TAG, "Disconnected from app service");
        _appService = null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.messages_send) {
            EditText messageEdit = findViewById(R.id.messages_edit);
            assert messageEdit != null;
            TextMessage textMessage = new TextMessage();
            textMessage.dst = _groupName;
            textMessage.text = messageEdit.getText().toString();
            _appService.sendTextMessage(textMessage);
            messageEdit.setText("");
        }
    }
}
