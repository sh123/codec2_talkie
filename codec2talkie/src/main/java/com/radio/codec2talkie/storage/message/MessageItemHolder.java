package com.radio.codec2talkie.storage.message;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.radio.codec2talkie.R;
import com.radio.codec2talkie.tools.DateTools;
import com.radio.codec2talkie.tools.TextTools;

public class MessageItemHolder extends RecyclerView.ViewHolder {

    private final TextView messageItemViewTitle;
    private final TextView messageItemViewMessage;

    private MessageItemHolder(View itemView) {
        super(itemView);
        messageItemViewTitle = itemView.findViewById(R.id.message_view_item_name);
        messageItemViewMessage = itemView.findViewById(R.id.message_item_message);
    }

    public void bind(long timestamp, String srcCallsign, String text) {
        messageItemViewTitle.setText(String.format("%s %s",
                DateTools.epochToIso8601(timestamp),
                srcCallsign));
        messageItemViewMessage.setText(text);
    }

    static MessageItemHolder create(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.activity_message_view_item, parent, false);
        return new MessageItemHolder(view);
    }
}
