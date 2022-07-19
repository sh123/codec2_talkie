package com.radio.codec2talkie.storage.message;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

public class MessageItemAdapter extends ListAdapter<MessageItem, MessageItemHolder> {

    public MessageItemAdapter(@NonNull DiffUtil.ItemCallback<MessageItem> diffCallback) {
        super(diffCallback);
    }

    @NonNull
    @Override
    public MessageItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return MessageItemHolder.create(parent);
    }

    @Override
    public void onBindViewHolder(MessageItemHolder holder, int position) {
        MessageItem current = getItem(position);
        holder.bind(current.getTimestampEpoch(), current.getSrcCallsign(), current.getMessage(), current.getIsTransmit());
    }

    static class MessageItemDiff extends DiffUtil.ItemCallback<MessageItem> {

        @Override
        public boolean areItemsTheSame(@NonNull MessageItem oldItem, @NonNull MessageItem newItem) {
            return oldItem == newItem;
        }

        @Override
        public boolean areContentsTheSame(@NonNull MessageItem oldItem, @NonNull MessageItem newItem) {
            return oldItem.getMessage().equals(newItem.getMessage());
        }
    }
}

