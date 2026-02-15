package com.radio.codec2talkie.storage.message;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import java.util.List;
import java.util.Objects;

public class MessageItemAdapter extends ListAdapter<MessageItem, MessageItemHolder> {

    public MessageItemAdapter(@NonNull DiffUtil.ItemCallback<MessageItem> diffCallback) {
        super(diffCallback);
        //setHasStableIds(true);
    }

    @NonNull
    @Override
    public MessageItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return MessageItemHolder.create(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageItemHolder holder, int position) {
        MessageItem current = getItem(position);
        holder.bind(current.getTimestampEpoch(), current.getSrcCallsign(), current.getDstCallsign(),
                current.getMessage(), current.getAckId(), current.getRetryCnt(), current.getIsAcknowledged());
    }

    static class MessageItemDiff extends DiffUtil.ItemCallback<MessageItem> {

        @Override
        public boolean areItemsTheSame(@NonNull MessageItem oldItem, @NonNull MessageItem newItem) {
            if (oldItem.getId() != 0 && newItem.getId() != 0) {
                return oldItem.getId() == newItem.getId();
            }
            return Objects.equals(oldItem.getSrcCallsign(), newItem.getSrcCallsign())
                    && oldItem.getIsTransmit() == newItem.getIsTransmit()
                    && Objects.equals(oldItem.getDstCallsign(), newItem.getDstCallsign())
                    && oldItem.getTimestampEpoch() == newItem.getTimestampEpoch()
                    && Objects.equals(oldItem.getAckId(), newItem.getAckId())
                    && Objects.equals(oldItem.getMessage(), newItem.getMessage());
        }

        @Override
        public boolean areContentsTheSame(@NonNull MessageItem oldItem, @NonNull MessageItem newItem) {
            return Objects.equals(oldItem.getMessage(), newItem.getMessage())
                    && Objects.equals(oldItem.getSrcCallsign(), newItem.getSrcCallsign())
                    && oldItem.getTimestampEpoch() == newItem.getTimestampEpoch()
                    && oldItem.getIsTransmit() == newItem.getIsTransmit()
                    && oldItem.getRetryCnt() == newItem.getRetryCnt()
                    && oldItem.getNeedsRetry() == newItem.getNeedsRetry()
                    && Objects.equals(oldItem.getAckId(), newItem.getAckId())
                    && oldItem.getIsAcknowledged() == newItem.getIsAcknowledged();
        }
    }
}

