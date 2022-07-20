package com.radio.codec2talkie.storage.message.group;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.radio.codec2talkie.R;
import com.radio.codec2talkie.storage.message.MessageItemActivity;

public class MessageGroupHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    private final TextView _messageGroupsViewItemName;
    private final TextView _messageGroupsViewItemMessage;

    private MessageGroupHolder(View itemView) {
        super(itemView);
        _messageGroupsViewItemName = itemView.findViewById(R.id.message_groups_view_item_name);
        _messageGroupsViewItemMessage = itemView.findViewById(R.id.message_groups_item_message);
        itemView.setOnClickListener(this);
    }

    public void bind(String text) {
        _messageGroupsViewItemName.setText(text);
    }

    static MessageGroupHolder create(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.activity_message_groups_view_item, parent, false);
        return new MessageGroupHolder(view);
    }

    @Override
    public void onClick(View v) {
        String groupName = _messageGroupsViewItemName.getText().toString();
        Intent messagesIntent = new Intent(v.getContext(), MessageItemActivity.class);
        messagesIntent.putExtra("groupName", groupName);
        v.getContext().startActivity(messagesIntent);
    }
}
