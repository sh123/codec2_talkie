package com.radio.codec2talkie.storage.log.group;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.radio.codec2talkie.R;
import com.radio.codec2talkie.tools.UnitTools;

import java.util.Locale;

public class LogItemGroupHolder extends RecyclerView.ViewHolder {

    private final TextView _logItemViewTitle;
    private final TextView _logItemViewMessage;

    private LogItemGroupHolder(View itemView) {
        super(itemView);
        _logItemViewTitle = itemView.findViewById(R.id.log_view_group_item_title);
        _logItemViewMessage = itemView.findViewById(R.id.log_view_group_item_message);
    }

    public void bind(LogItemGroup group) {
        _logItemViewTitle.setText(String.format(Locale.US, "%s",
                group.getSrcCallsign()));
        _logItemViewMessage.setText(String.format(Locale.US, "%s %s %.4f %.4f %d° %dkm/h %dm %s %s",
                group.getSymbolCode(),
                group.getMaidenHead(),
                group.getLatitude(),
                group.getLongitude(),
                (int)group.getBearingDegrees(),
                UnitTools.metersPerSecondToKilometersPerHour((int)group.getSpeedMetersPerSecond()),
                (int)group.getAltitudeMeters(),
                group.getStatus(),
                group.getComment()));
    }

    static LogItemGroupHolder create(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.activity_log_view_group_item, parent, false);
        return new LogItemGroupHolder(view);
    }
}
