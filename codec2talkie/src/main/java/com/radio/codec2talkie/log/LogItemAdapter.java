package com.radio.codec2talkie.log;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

public class LogItemAdapter extends ListAdapter<LogItem, LogItemHolder> {

    public LogItemAdapter(@NonNull DiffUtil.ItemCallback<LogItem> diffCallback) {
        super(diffCallback);
    }

    @NonNull
    @Override
    public LogItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return LogItemHolder.create(parent);
    }

    @Override
    public void onBindViewHolder(LogItemHolder holder, int position) {
        LogItem current = getItem(position);
        holder.bind(current.getTimestampEpoch(), current.getSrcCallsign(), current.getLogLine(), current.getIsTransmit());
    }

    static class LogItemDiff extends DiffUtil.ItemCallback<LogItem> {

        @Override
        public boolean areItemsTheSame(@NonNull LogItem oldItem, @NonNull LogItem newItem) {
            return oldItem == newItem;
        }

        @Override
        public boolean areContentsTheSame(@NonNull LogItem oldItem, @NonNull LogItem newItem) {
            return oldItem.getLogLine().equals(newItem.getLogLine());
        }
    }
}

