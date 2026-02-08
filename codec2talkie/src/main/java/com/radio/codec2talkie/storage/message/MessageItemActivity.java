package com.radio.codec2talkie.storage.message;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.radio.codec2talkie.R;
import com.radio.codec2talkie.protocol.message.TextMessage;
import com.radio.codec2talkie.settings.PreferenceKeys;
import com.radio.codec2talkie.settings.SettingsWrapper;
import com.radio.codec2talkie.tools.TextTools;
import com.radio.codec2talkie.ui.AppCompatActivityWithServiceConnection;

import java.util.Locale;
import java.util.Objects;

public class MessageItemActivity extends AppCompatActivityWithServiceConnection implements View.OnClickListener {

    private static final int ACK_LENGTH = 5;
    private static final int MAX_MESSAGE_LEN = 67;

    private MessageItemViewModel _messageViewModel;
    private RecyclerView _recyclerView;
    private MessageItemAdapter _adapter;
    private LinearLayoutManager _layoutManager;
    private String _targetCallSign;
    private SharedPreferences _sharedPreferences;
    private TextView _textViewCharCount;
    private EditText _textEdit;
    public static boolean isPaused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_message_view);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        _sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        _textViewCharCount = findViewById(R.id.messages_char_count);

        _textEdit = findViewById(R.id.messages_edit);
        _textEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                updateCharCount();
            }
            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        updateCharCount();

        _recyclerView = findViewById(R.id.message_recyclerview);
        _recyclerView.setHasFixedSize(true);
        _adapter = new MessageItemAdapter(new MessageItemAdapter.MessageItemDiff());
        _recyclerView.setAdapter(_adapter);

        _layoutManager = new LinearLayoutManager(this);
        _layoutManager.setStackFromEnd(true);
        _recyclerView.setLayoutManager(_layoutManager);
        _recyclerView.addItemDecoration(new DividerItemDecoration(_recyclerView.getContext(), DividerItemDecoration.VERTICAL));

        String groupName = (String) Objects.requireNonNull(getIntent().getExtras()).get("groupName");
        assert groupName != null;

        _messageViewModel = new ViewModelProvider(this).get(MessageItemViewModel.class);
        _messageViewModel.getMessages(groupName).observe(this, messages -> _adapter.submitList(messages, () -> {
            if (shouldAutoScroll()) {
                _recyclerView.scrollToPosition(_adapter.getItemCount() - 1);
            }
        }));

        setTitle(groupName);

        Button sendButton = findViewById(R.id.messages_send);
        assert sendButton != null;

        _targetCallSign = TextMessage.getTargetCallsign(this, groupName);
        if (_targetCallSign == null)
            sendButton.setEnabled(false);

        sendButton.setOnClickListener(this);
    }

    private void updateCharCount() {
        int currentLength = _textEdit.length();
        _textViewCharCount.setText(String.format(Locale.ROOT, "%d/%d", currentLength, MAX_MESSAGE_LEN));
    }

    private boolean shouldAutoScroll() {
        int lastVisible = _layoutManager.findLastCompletelyVisibleItemPosition();
        int itemCount = _adapter.getItemCount();
        if (itemCount == 0) return true;
        return lastVisible == RecyclerView.NO_POSITION || lastVisible >= itemCount - 3;
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
            TextMessage textMessage = new TextMessage();
            textMessage.dst = _targetCallSign;
            textMessage.text = _textEdit.getText().toString();
            textMessage.ackId = SettingsWrapper.isMessageAckEnabled(_sharedPreferences)
                    ? TextTools.generateRandomString(ACK_LENGTH)
                    : null;
            getService().sendTextMessage(textMessage);
            _textEdit.setText("");
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
