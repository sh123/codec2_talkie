package com.radio.codec2talkie.storage.log;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

public class LogItemGroupAdapter extends ListAdapter<String, LogItemGroupHolder> {

    private View.OnClickListener _clickListener;

    public LogItemGroupAdapter(@NonNull DiffUtil.ItemCallback<String> diffCallback) {
        super(diffCallback);
    }

    public void setClickListener(View.OnClickListener clickListener) {
        _clickListener = clickListener;
    }

    @NonNull
    @Override
    public LogItemGroupHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return LogItemGroupHolder.create(parent);
    }

    @Override
    public void onBindViewHolder(LogItemGroupHolder holder, int position) {
        String current = getItem(position);
        holder.itemView.setOnClickListener(_clickListener);
        holder.bind(current);
    }

    static class LogItemGroupDiff extends DiffUtil.ItemCallback<String> {

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

