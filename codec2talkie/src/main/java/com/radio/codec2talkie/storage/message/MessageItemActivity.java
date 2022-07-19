package com.radio.codec2talkie.storage.message;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.ActionBar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.radio.codec2talkie.R;
import com.radio.codec2talkie.app.AppService;
import com.radio.codec2talkie.protocol.message.TextMessage;
import com.radio.codec2talkie.ui.AppCompatActivityWithServiceConnection;

public class MessageItemActivity extends AppCompatActivityWithServiceConnection implements View.OnClickListener {

    private MessageItemViewModel _messageViewModel;

    private String _groupName;

    public static boolean isPaused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_message_view);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        RecyclerView recyclerView = findViewById(R.id.message_recyclerview);
        recyclerView.setHasFixedSize(true);
        final MessageItemAdapter adapter = new MessageItemAdapter(new MessageItemAdapter.MessageItemDiff());
        recyclerView.setAdapter(adapter);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);
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
            getService().sendTextMessage(textMessage);
            messageEdit.setText("");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isPaused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        isPaused = false;
    }
}
