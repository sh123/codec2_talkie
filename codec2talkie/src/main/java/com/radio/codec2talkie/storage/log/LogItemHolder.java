package com.radio.codec2talkie.storage.log;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.radio.codec2talkie.R;
import com.radio.codec2talkie.tools.DateTools;
import com.radio.codec2talkie.tools.TextTools;

public class LogItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    private final TextView _logItemViewTitle;
    private final TextView _logItemViewMessage;
    private String _srcCallsign;
    private final boolean _isClickable;

    private LogItemHolder(View itemView, boolean isClickable) {
        super(itemView);
        _logItemViewTitle = itemView.findViewById(R.id.log_view_item_title);
        _logItemViewMessage = itemView.findViewById(R.id.log_view_item_message);
        _isClickable = isClickable;
        itemView.setOnClickListener(this);
    }

    public void bind(long timestamp, String srcCallsign, String text, boolean isTransmitting) {
        _srcCallsign = srcCallsign;
        _logItemViewTitle.setText(String.format("%s %s %s",
                DateTools.epochToIso8601(timestamp),
                isTransmitting ? "→" : "←",
                srcCallsign));
        _logItemViewMessage.setText(TextTools.addZeroWidthSpaces(text));
    }

    static LogItemHolder create(ViewGroup parent, boolean isClickable) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.activity_log_view_item, parent, false);
        return new LogItemHolder(view, isClickable);
    }

    @Override
    public void onClick(View v) {
        if (!_isClickable) return;
        Intent logItemIntent = new Intent(v.getContext(), LogItemActivity.class);
        logItemIntent.putExtra("stationName", _srcCallsign);
        v.getContext().startActivity(logItemIntent);
    }
}
