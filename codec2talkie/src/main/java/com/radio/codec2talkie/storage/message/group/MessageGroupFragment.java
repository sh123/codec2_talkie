package com.radio.codec2talkie.storage.message.group;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.radio.codec2talkie.R;
import com.radio.codec2talkie.ui.FragmentMenuHandler;
import com.radio.codec2talkie.ui.FragmentWithServiceConnection;

public class MessageGroupFragment extends FragmentWithServiceConnection implements FragmentMenuHandler {

    private static final String TAG = MessageGroupFragment.class.getSimpleName();

    private MessageGroupViewModel _messageGroupViewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_message_groups_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.i(TAG, "onCreateView()");

        Button sendToButton = view.findViewById(R.id.messages_send_to);
        sendToButton.setOnClickListener(v -> {
            MessageGroupDialogSendTo dialogSendTo = new MessageGroupDialogSendTo(requireActivity());
            dialogSendTo.show();
        });

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
        return false;
    }

    @Override
    public void handleMenuCreation(Menu menu) {
    }
}
