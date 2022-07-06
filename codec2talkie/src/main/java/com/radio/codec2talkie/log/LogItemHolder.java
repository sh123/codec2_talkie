package com.radio.codec2talkie.log;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.radio.codec2talkie.R;
import com.radio.codec2talkie.tools.DateTools;

public class LogItemHolder extends RecyclerView.ViewHolder {
    private final TextView logItemView;

    private LogItemHolder(View itemView) {
        super(itemView);
        logItemView = itemView.findViewById(R.id.log_view_item);
    }

    public void bind(long timestamp, String srcCallsign, String text, boolean isTransmitting) {
        logItemView.setText(String.format("%s %s\n%s",
                DateTools.epochToIso8601(timestamp),
                isTransmitting ? "TX" : "RX",
                text));
    }

    static LogItemHolder create(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.activity_log_view_item, parent, false);
        return new LogItemHolder(view);
    }
}
