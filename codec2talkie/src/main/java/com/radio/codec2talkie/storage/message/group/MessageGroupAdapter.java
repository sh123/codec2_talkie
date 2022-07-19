package com.radio.codec2talkie.storage.message.group;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

public class MessageGroupAdapter extends ListAdapter<String, MessageGroupHolder> {

    private View.OnLongClickListener _longClickListener;

    public MessageGroupAdapter(@NonNull DiffUtil.ItemCallback<String> diffCallback) {
        super(diffCallback);
    }

    public void setLongClickListener(View.OnLongClickListener longClickListener) {
        _longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public MessageGroupHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return MessageGroupHolder.create(parent);
    }

    @Override
    public void onBindViewHolder(MessageGroupHolder holder, int position) {
        String current = getItem(position);
        holder.itemView.setLongClickable(true);
        holder.itemView.setOnLongClickListener(_longClickListener);
        holder.bind(current);
    }

    static class MessageGroupDiff extends DiffUtil.ItemCallback<String> {

        @Override
        public boolean areItemsTheSame(@NonNull String oldItem, @NonNull String newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areContentsTheSame(@NonNull String oldItem, @NonNull String newItem) {
            return oldItem.equals(newItem);
        }
    }
}

