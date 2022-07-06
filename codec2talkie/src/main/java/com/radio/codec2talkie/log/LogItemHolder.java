package com.radio.codec2talkie.log;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.radio.codec2talkie.R;
import com.radio.codec2talkie.tools.DateTools;
import com.radio.codec2talkie.tools.TextTools;

public class LogItemHolder extends RecyclerView.ViewHolder {
    private final TextView logItemViewTitle;
    private final TextView logItemViewMessage;

    private LogItemHolder(View itemView) {
        super(itemView);
        logItemViewTitle = itemView.findViewById(R.id.log_view_item_title);
        logItemViewMessage = itemView.findViewById(R.id.log_view_item_message);
    }

    public void bind(long timestamp, String srcCallsign, String text, boolean isTransmitting) {
        logItemViewTitle.setText(String.format("%s %s %s",
                DateTools.epochToIso8601(timestamp),
                isTransmitting ? "→" : "←",
                srcCallsign));
        logItemViewMessage.setText(TextTools.addZeroWidthSpaces(text));
    }

    static LogItemHolder create(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.activity_log_view_item, parent, false);
        return new LogItemHolder(view);
    }
}
