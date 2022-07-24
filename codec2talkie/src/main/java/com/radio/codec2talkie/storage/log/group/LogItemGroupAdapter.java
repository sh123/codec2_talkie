package com.radio.codec2talkie.storage.log.group;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

public class LogItemGroupAdapter extends ListAdapter<LogItemGroup, LogItemGroupHolder> {

    private View.OnClickListener _clickListener;

    public LogItemGroupAdapter(@NonNull DiffUtil.ItemCallback<LogItemGroup> diffCallback) {
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
        LogItemGroup current = getItem(position);
        holder.itemView.setOnClickListener(_clickListener);
        holder.bind(current);
    }

    public static class LogItemGroupDiff extends DiffUtil.ItemCallback<LogItemGroup> {

        @Override
        public boolean areItemsTheSame(@NonNull LogItemGroup oldItem, @NonNull LogItemGroup newItem) {
            return oldItem == newItem;
        }

        @Override
        public boolean areContentsTheSame(@NonNull LogItemGroup oldItem, @NonNull LogItemGroup newItem) {
            return oldItem.getSrcCallsign().equals(newItem.getSrcCallsign());
        }
    }
}

