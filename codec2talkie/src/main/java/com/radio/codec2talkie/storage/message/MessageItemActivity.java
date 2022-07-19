package com.radio.codec2talkie.storage.message;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.radio.codec2talkie.R;
import com.radio.codec2talkie.storage.message.group.MessageGroupAdapter;
import com.radio.codec2talkie.storage.message.group.MessageGroupViewModel;

public class MessageItemActivity extends AppCompatActivity {

    private MessageItemViewModel _messageViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_view);
        setTitle(R.string.messages_view_title);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        RecyclerView recyclerView = findViewById(R.id.message_recyclerview);
        recyclerView.setHasFixedSize(true);
        final MessageItemAdapter adapter = new MessageItemAdapter(new MessageItemAdapter.MessageItemDiff());
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL));

        String groupName = (String) getIntent().getExtras().get("groupName");
        _messageViewModel = new ViewModelProvider(this).get(MessageItemViewModel.class);
        _messageViewModel.getMessages(groupName).observe(this, adapter::submitList);
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
