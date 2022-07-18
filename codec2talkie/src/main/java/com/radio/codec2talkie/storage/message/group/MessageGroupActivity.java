package com.radio.codec2talkie.storage.message.group;

import android.os.Bundle;
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

public class MessageGroupActivity extends AppCompatActivity {

    private MessageGroupViewModel _messageGroupViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_groups_view);
        setTitle(R.string.messages_group_view_title);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        Button sendToButton = findViewById(R.id.messages_send_to);
        sendToButton.setOnClickListener(_onSendToButtonListener);

        RecyclerView recyclerView = findViewById(R.id.message_groups_recyclerview);
        recyclerView.setHasFixedSize(true);
        final MessageGroupAdapter adapter = new MessageGroupAdapter(new MessageGroupAdapter.MessageGroupDiff());
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL));

        _messageGroupViewModel = new ViewModelProvider(this).get(MessageGroupViewModel.class);
        _messageGroupViewModel.getGroups().observe(this, adapter::submitList);
    }

    private final View.OnClickListener _onSendToButtonListener = v -> {
        MessageGroupDialogSendTo dialogSendTo = new MessageGroupDialogSendTo(MessageGroupActivity.this);
        dialogSendTo.show();
    };

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
}
