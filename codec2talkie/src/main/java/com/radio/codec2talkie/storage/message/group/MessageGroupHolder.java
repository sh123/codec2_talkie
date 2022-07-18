package com.radio.codec2talkie.storage.message.group;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.radio.codec2talkie.R;

public class MessageGroupHolder extends RecyclerView.ViewHolder {

    private final TextView messageGroupsViewItemName;
    private final TextView messageGroupsViewItemMessage;

    private MessageGroupHolder(View itemView) {
        super(itemView);
        messageGroupsViewItemName = itemView.findViewById(R.id.message_groups_view_item_name);
        messageGroupsViewItemMessage = itemView.findViewById(R.id.message_groups_item_message);
    }

    public void bind(String text) {
        messageGroupsViewItemMessage.setText(text);
    }

    static MessageGroupHolder create(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.activity_message_groups_view_item, parent, false);
        return new MessageGroupHolder(view);
    }
}
