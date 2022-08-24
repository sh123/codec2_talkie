package com.radio.codec2talkie.storage.log;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

public class LogItemAdapter extends ListAdapter<LogItem, LogItemHolder> {

    private final boolean _isClickable;

    public LogItemAdapter(@NonNull DiffUtil.ItemCallback<LogItem> diffCallback, boolean isClickable) {
        super(diffCallback);
        _isClickable = isClickable;
    }

    @NonNull
    @Override
    public LogItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return LogItemHolder.create(parent, _isClickable);
    }

    @Override
    public void onBindViewHolder(LogItemHolder holder, int position) {
        LogItem current = getItem(position);
        holder.bind(current.getTimestampEpoch(), current.getSrcCallsign(), current.getLogLine(), current.getIsTransmit());
    }

    static class LogItemDiff extends DiffUtil.ItemCallback<LogItem> {

        @Override
        public boolean areItemsTheSame(@NonNull LogItem oldItem, @NonNull LogItem newItem) {
            return oldItem.getTimestampEpoch() == newItem.getTimestampEpoch() &&
                    oldItem.getSrcCallsign().equals(newItem.getSrcCallsign());
        }

        @Override
        public boolean areContentsTheSame(@NonNull LogItem oldItem, @NonNull LogItem newItem) {
            return oldItem.getTimestampEpoch() == newItem.getTimestampEpoch() &&
                    oldItem.getSrcCallsign().equals(newItem.getSrcCallsign());
        }
    }
}

