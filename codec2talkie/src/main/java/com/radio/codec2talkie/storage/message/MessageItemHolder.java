package com.radio.codec2talkie.storage.message;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.radio.codec2talkie.R;
import com.radio.codec2talkie.tools.DateTools;

public class MessageItemHolder extends RecyclerView.ViewHolder {

    private final TextView _messageItemViewTitle;
    private final TextView _messageItemViewMessage;

    private MessageItemHolder(View itemView) {
        super(itemView);
        _messageItemViewTitle = itemView.findViewById(R.id.message_view_item_name);
        _messageItemViewMessage = itemView.findViewById(R.id.message_item_message);
    }

    public void bind(long timestamp, String srcCallsign, String text, boolean isTransmitting) {
        _messageItemViewTitle.setText(String.format("%s %s %s",
                DateTools.epochToIso8601(timestamp),
                isTransmitting ? "→" : "←",
                srcCallsign));
        _messageItemViewMessage.setText(text);
    }

    static MessageItemHolder create(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.activity_message_view_item, parent, false);
        return new MessageItemHolder(view);
    }
}
