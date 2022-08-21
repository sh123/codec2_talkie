package com.radio.codec2talkie.storage.log.group;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.radio.codec2talkie.R;
import com.radio.codec2talkie.protocol.aprs.tools.AprsSymbolTable;
import com.radio.codec2talkie.protocol.position.Position;
import com.radio.codec2talkie.tools.UnitTools;

import java.util.Locale;

public class LogItemGroupHolder extends RecyclerView.ViewHolder {

    private static final String TAG = RecyclerView.class.getSimpleName();

    private final TextView _logItemViewTitle;
    private final TextView _logItemViewDistance;
    private final TextView _logItemViewMessage;
    private final ImageView _symbolIcon;
    private final LocationManager _locationManager;
    private final AprsSymbolTable _symbolTable;

    private LogItemGroupHolder(View itemView) {
        super(itemView);
        _logItemViewTitle = itemView.findViewById(R.id.log_view_group_item_title);
        _logItemViewDistance = itemView.findViewById(R.id.log_view_group_item_distance);
        _logItemViewMessage = itemView.findViewById(R.id.log_view_group_item_message);
        _symbolIcon = itemView.findViewById(R.id.log_view_group_item_icon);
        _symbolTable = AprsSymbolTable.getInstance(itemView.getContext());
        _locationManager = (LocationManager) itemView.getContext().getSystemService(Context.LOCATION_SERVICE);
    }

    public void bind(LogItemGroup group) {
        @SuppressLint("MissingPermission") Location loc = _locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (loc == null) {
            _logItemViewDistance.setText("");
        } else {
            double distanceKm = Position.distanceTo(loc.getLatitude(), loc.getLongitude(), loc.getAltitude(),
                    group.getLatitude(), group.getLongitude(), group.getAltitudeMeters()) / 1000.0;
            String bearing = Position.bearing(loc.getLatitude(), loc.getLongitude(), group.getLatitude(), group.getLongitude());
            _logItemViewDistance.setText(String.format(Locale.US, "%s %.1f km", bearing, distanceKm));
        }
        _logItemViewTitle.setText(String.format(Locale.US, "%s",
                group.getSrcCallsign()));
        _logItemViewMessage.setText(String.format(Locale.US, "%s %f %f %03d° %03dkm/h %04dm %s %s",
                group.getMaidenHead(),
                group.getLatitude(),
                group.getLongitude(),
                (int)group.getBearingDegrees(),
                UnitTools.metersPerSecondToKilometersPerHour((int)group.getSpeedMetersPerSecond()),
                (int)group.getAltitudeMeters(),
                group.getStatus(),
                group.getComment()));
        String symbol = group.getSymbolCode();
        Bitmap iconBitmap = _symbolTable.bitmapFromSymbol(symbol == null ? "/." : symbol, false);
        if (iconBitmap == null) {
            Log.w(TAG, "Cannot load bitmap for " + symbol);
        } else {
            _symbolIcon.setImageBitmap(iconBitmap);
        }
    }

    static LogItemGroupHolder create(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.activity_log_view_group_item, parent, false);
        return new LogItemGroupHolder(view);
    }
}
