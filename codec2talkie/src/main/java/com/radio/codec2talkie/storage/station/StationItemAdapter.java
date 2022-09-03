package com.radio.codec2talkie.storage.station;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import java.util.Objects;

public class StationItemAdapter extends ListAdapter<StationItem, StationItemHolder> {

    private View.OnClickListener _clickListener;

    public StationItemAdapter(@NonNull DiffUtil.ItemCallback<StationItem> diffCallback) {
        super(diffCallback);
    }

    public void setClickListener(View.OnClickListener clickListener) {
        _clickListener = clickListener;
    }

    @NonNull
    @Override
    public StationItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return StationItemHolder.create(parent);
    }

    @Override
    public void onBindViewHolder(StationItemHolder holder, int position) {
        StationItem current = getItem(position);
        holder.itemView.setOnClickListener(_clickListener);
        holder.bind(current);
    }

    public static class StationItemDiff extends DiffUtil.ItemCallback<StationItem> {

        @Override
        public boolean areItemsTheSame(@NonNull StationItem oldItem, @NonNull StationItem newItem) {
            return oldItem.getSrcCallsign().equals(newItem.getSrcCallsign());
        }

        @Override
        public boolean areContentsTheSame(@NonNull StationItem oldItem, @NonNull StationItem newItem) {
            return oldItem.getSrcCallsign().equals(newItem.getSrcCallsign()) &&
                    oldItem.getLatitude() == newItem.getLatitude() &&
                    oldItem.getLongitude() == newItem.getLongitude() &&
                    Objects.equals(oldItem.getComment(), newItem.getComment());
        }
    }
}

