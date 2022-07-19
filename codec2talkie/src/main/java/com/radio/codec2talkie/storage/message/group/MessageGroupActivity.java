package com.radio.codec2talkie.storage.message.group;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.radio.codec2talkie.R;
import com.radio.codec2talkie.ui.AppCompatActivityWithServiceConnection;

public class MessageGroupActivity extends AppCompatActivityWithServiceConnection implements View.OnClickListener {

    private static final String TAG = MessageGroupActivity.class.getSimpleName();

    private MessageGroupViewModel _messageGroupViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");

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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.messages_group_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.messages_group_menu_clear) {
            deleteAll();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        MessageGroupDialogSendTo dialogSendTo = new MessageGroupDialogSendTo(MessageGroupActivity.this, getService());
        dialogSendTo.show();
    }

    private void deleteAll() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.messages_group_activity_delete_all_confirmation_title))
                .setPositiveButton(getString(R.string.yes), _deleteAllDialogClickListener)
                .setNegativeButton(getString(R.string.no), _deleteAllDialogClickListener).show();
    }

    private final DialogInterface.OnClickListener _deleteAllDialogClickListener = (dialog, which) -> {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            _messageGroupViewModel.deleteAll();
        }
    };
}
