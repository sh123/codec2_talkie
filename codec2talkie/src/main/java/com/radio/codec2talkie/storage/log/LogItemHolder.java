package com.radio.codec2talkie.storage.log;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.radio.codec2talkie.R;
import com.radio.codec2talkie.tools.DateTools;
import com.radio.codec2talkie.tools.TextTools;

public class LogItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    private final TextView _logItemViewTitle;
    private final TextView _logItemViewMessage;
    private String _srcCallsign;
    private final boolean _isClickable;
    private final Context _context;

    private LogItemHolder(View itemView, Context context, boolean isClickable) {
        super(itemView);
        _context = context;
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
        if (text != null)
            _logItemViewMessage.setText(TextTools.addZeroWidthSpaces(text));
    }

    static LogItemHolder create(ViewGroup parent, Context context, boolean isClickable) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.activity_log_view_item, parent, false);
        return new LogItemHolder(view, context, isClickable);
    }

    @Override
    public void onClick(View v) {
        if (!_isClickable) return;
        LogItemFragment logItemFragment = LogItemFragment.newInstance(_srcCallsign);
        FragmentTransaction transaction = ((AppCompatActivity) _context)
                .getSupportFragmentManager()
                .beginTransaction();
        transaction.replace(R.id.fragmentMain, logItemFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
