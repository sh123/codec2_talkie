package com.radio.codec2talkie.storage.log.group;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

public class StationAdapter extends ListAdapter<Station, StationHolder> {

    private View.OnClickListener _clickListener;

    public StationAdapter(@NonNull DiffUtil.ItemCallback<Station> diffCallback) {
        super(diffCallback);
    }

    public void setClickListener(View.OnClickListener clickListener) {
        _clickListener = clickListener;
    }

    @NonNull
    @Override
    public StationHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return StationHolder.create(parent);
    }

    @Override
    public void onBindViewHolder(StationHolder holder, int position) {
        Station current = getItem(position);
        holder.itemView.setOnClickListener(_clickListener);
        holder.bind(current);
    }

    public static class LogItemGroupDiff extends DiffUtil.ItemCallback<Station> {

        @Override
        public boolean areItemsTheSame(@NonNull Station oldItem, @NonNull Station newItem) {
            return oldItem.getSrcCallsign().equals(newItem.getSrcCallsign());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Station oldItem, @NonNull Station newItem) {
            return oldItem.getSrcCallsign().equals(newItem.getSrcCallsign());
        }
    }
}

