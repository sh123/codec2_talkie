package com.radio.codec2talkie.storage.message.group;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.radio.codec2talkie.R;
import com.radio.codec2talkie.settings.PreferenceKeys;
import com.radio.codec2talkie.settings.SettingsWrapper;
import com.radio.codec2talkie.ui.FragmentMenuHandler;
import com.radio.codec2talkie.ui.FragmentWithServiceConnection;

public class MessageGroupFragment extends FragmentWithServiceConnection implements FragmentMenuHandler {

    private static final String TAG = MessageGroupFragment.class.getSimpleName();

    private MessageGroupViewModel _messageGroupViewModel;

    private EditText _editTextFilter;

    private SharedPreferences _sharedPreferences;
    private boolean _isAckEnabled;
    private long _msgRetryCount;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        _sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        _isAckEnabled = SettingsWrapper.isMessageAckEnabled(_sharedPreferences);
        _msgRetryCount = _sharedPreferences.getLong(PreferenceKeys.APRS_IS_MSG_RETRY_CNT, 3);
        return inflater.inflate(R.layout.activity_message_groups_view, container, false);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.i(TAG, "onCreateView()");

        RecyclerView recyclerView = view.findViewById(R.id.message_groups_recyclerview);
        recyclerView.setHasFixedSize(true);

        final MessageGroupAdapter adapter = new MessageGroupAdapter(new MessageGroupAdapter.MessageGroupDiff());
        adapter.setLongClickListener(v -> {
            TextView itemView = v.findViewById(R.id.message_groups_view_item_name);
            deleteGroup(itemView.getText().toString());
            return true;
        });
        recyclerView.setAdapter(adapter);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));
        recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL));

        _messageGroupViewModel = new ViewModelProvider(this).get(MessageGroupViewModel.class);
        _messageGroupViewModel.getGroups().observe(requireActivity(), adapter::submitList);

        _editTextFilter = view.findViewById(R.id.message_groups_filter);
        _editTextFilter.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        _editTextFilter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    _editTextFilter.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.baseline_clear_24, 0);
                } else {
                    _editTextFilter.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                }
                _messageGroupViewModel.getFilteredGroups(s.toString()).observe(requireActivity(), adapter::submitList);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        _editTextFilter.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (_editTextFilter.getCompoundDrawables()[2] != null) {
                    if (event.getRawX() >= (_editTextFilter.getRight() - _editTextFilter.getCompoundDrawables()[2].getBounds().width())) {
                        _editTextFilter.setText("");
                        return true;
                    }
                }
            }
            return false;
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void deleteAll() {
        DialogInterface.OnClickListener deleteAllDialogClickListener = (dialog, which) -> {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                _messageGroupViewModel.deleteAll();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setMessage(getString(R.string.messages_group_activity_delete_all_confirmation_title))
                .setPositiveButton(getString(R.string.yes), deleteAllDialogClickListener)
                .setNegativeButton(getString(R.string.no), deleteAllDialogClickListener).show();
    }

    private void deleteGroup(String groupName) {
        if (groupName == null) {
            Log.e(TAG, "Cannot delete group, it is null");
            return;
        }
        DialogInterface.OnClickListener deleteGroupDialogClickListener = (dialog, which) -> {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                _messageGroupViewModel.deleteGroup(groupName);
            }
        };
        String message = getString(R.string.messages_group_activity_delete_group_confirmation_title);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setMessage(String.format(message, groupName))
                .setPositiveButton(getString(R.string.yes), deleteGroupDialogClickListener)
                .setNegativeButton(getString(R.string.no), deleteGroupDialogClickListener).show();
    }

    @Override
    public boolean handleMenuItemClick(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == R.id.messages_group_menu_clear) {
            deleteAll();
            return true;
        }
        else if (itemId == R.id.messages_group_menu_add) {
            MessageGroupDialogSendTo dialogSendTo = new MessageGroupDialogSendTo(requireActivity());
            dialogSendTo.show();
            return true;
        }
        else if (itemId == R.id.messages_group_menu_ack_enable) {
            if (menuItem.isChecked()) {
                menuItem.setChecked(false);
                _isAckEnabled = false;
            } else {
                menuItem.setChecked(true);
                _isAckEnabled = true;
            }
            saveSettings();
            return true;
        }
        else if (itemId == R.id.messages_group_menu_msg_retry_cnt_1) {
            _msgRetryCount = 1;
            saveSettings();
            return true;
        }
        else if (itemId == R.id.messages_group_menu_msg_retry_cnt_3) {
            _msgRetryCount = 3;
            saveSettings();
            return true;
        }
        else if (itemId == R.id.messages_group_menu_msg_retry_cnt_5) {
            _msgRetryCount = 5;
            saveSettings();
            return true;
        }
        else if (itemId == R.id.messages_group_menu_msg_retry_cnt_7) {
            _msgRetryCount = 7;
            saveSettings();
            return true;
        }
        else if (itemId == R.id.messages_group_menu_msg_clear_1h) {
            deleteMsgItems(1);
            return true;
        }
        else if (itemId == R.id.messages_group_menu_msg_clear_12h) {
            deleteMsgItems(12);
            return true;
        }
        else if (itemId == R.id.messages_group_menu_msg_clear_1d) {
            deleteMsgItems(24);
            return true;
        }
        else if (itemId == R.id.messages_group_menu_msg_clear_7d) {
            deleteMsgItems(24 * 7);
            return true;
        }
        return false;
    }

    private void deleteMsgItems(int hours) {
        DialogInterface.OnClickListener deleteAllDialogClickListener = (dialog, which) -> {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                _messageGroupViewModel.deleteOlderThanHours(hours);
            }
        };
        String alertMessage = String.format(getString(R.string.log_item_activity_delete_hours_title), hours);
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setMessage(alertMessage)
                .setPositiveButton(getString(R.string.yes), deleteAllDialogClickListener)
                .setNegativeButton(getString(R.string.no), deleteAllDialogClickListener).show();
    }

    public void saveSettings() {
        SharedPreferences.Editor editor = _sharedPreferences.edit();
        editor.putBoolean(PreferenceKeys.APRS_IS_MSG_ACK_ENABLED, _isAckEnabled);
        editor.putLong(PreferenceKeys.APRS_IS_MSG_RETRY_CNT, _msgRetryCount);
        editor.apply();
    }

    @Override
    public void handleMenuCreation(Menu menu) {
        MenuItem itemRotateMapCompass = menu.findItem(R.id.messages_group_menu_ack_enable);
        if (itemRotateMapCompass != null) {
            itemRotateMapCompass.setChecked(_isAckEnabled);
        }
        MenuItem itemRetryCnt1 = menu.findItem(R.id.messages_group_menu_msg_retry_cnt_1);
        if (itemRetryCnt1 != null && _msgRetryCount == 1) {
            itemRetryCnt1.setChecked(true);
        }
        MenuItem itemRetryCnt3 = menu.findItem(R.id.messages_group_menu_msg_retry_cnt_3);
        if (itemRetryCnt3 != null && _msgRetryCount == 3) {
            itemRetryCnt3.setChecked(true);
        }
        MenuItem itemRetryCnt5 = menu.findItem(R.id.messages_group_menu_msg_retry_cnt_5);
        if (itemRetryCnt5 != null && _msgRetryCount == 5) {
            itemRetryCnt5.setChecked(true);
        }
        MenuItem itemRetryCnt7 = menu.findItem(R.id.messages_group_menu_msg_retry_cnt_7);
        if (itemRetryCnt7 != null && _msgRetryCount == 7) {
            itemRetryCnt7.setChecked(true);
        }
    }
}
