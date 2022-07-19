package com.radio.codec2talkie.storage.message.group;

import android.app.Service;
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

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.radio.codec2talkie.R;
import com.radio.codec2talkie.app.AppService;

public class MessageGroupActivity extends AppCompatActivity implements ServiceConnection, View.OnClickListener {

    private static final String TAG = MessageGroupActivity.class.getSimpleName();

    private AppService _appService;

    private MessageGroupViewModel _messageGroupViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");
        bindAppService();

        setContentView(R.layout.activity_message_groups_view);
        setTitle(R.string.messages_group_view_title);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        Button sendToButton = findViewById(R.id.messages_send_to);
        sendToButton.setOnClickListener(this);

        RecyclerView recyclerView = findViewById(R.id.message_groups_recyclerview);
        recyclerView.setHasFixedSize(true);
        final MessageGroupAdapter adapter = new MessageGroupAdapter(new MessageGroupAdapter.MessageGroupDiff());
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL));

        _messageGroupViewModel = new ViewModelProvider(this).get(MessageGroupViewModel.class);
        _messageGroupViewModel.getGroups().observe(this, adapter::submitList);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy()");
        unbindAppService();
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
    public void onClick(View v) {
        MessageGroupDialogSendTo dialogSendTo = new MessageGroupDialogSendTo(MessageGroupActivity.this, _appService);
        dialogSendTo.show();
    }
}
