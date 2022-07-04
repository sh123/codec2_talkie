package com.radio.codec2talkie.tracker;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import com.radio.codec2talkie.protocol.position.Position;
import com.radio.codec2talkie.settings.PreferenceKeys;

public class Periodic implements Tracker {
    private static final String TAG = Periodic.class.getSimpleName();

    private boolean _isTracking = false;

    private Context _context;
    private TrackerCallback _trackerCallback;
    private LocationManager _locationManager;

    private int _updateIntervalMinutes;
    private int _updateRangeKm;

    @Override
    public void initialize(Context context, TrackerCallback trackerCallback) {
        _context = context;
        _trackerCallback = trackerCallback;
        _locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        _updateIntervalMinutes = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.APRS_LOCATION_SOURCE_GPS_UPDATE_TIME, "10"));
        _updateRangeKm = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.APRS_LOCATION_SOURCE_GPS_UPDATE_DISTANCE, "10"));
    }

    @Override
    public void sendPosition() {
        if (ActivityCompat.checkSelfPermission(_context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(_context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "No permissions for location access");
            return;
        }

        _locationManager.requestSingleUpdate(
                LocationManager.GPS_PROVIDER,
                this::sendLocation,
                Looper.myLooper());
    }

    @Override
    public void startTracking() {
        if (ActivityCompat.checkSelfPermission(_context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(_context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "No permissions for location access");
            return;
        }
        _locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                _updateIntervalMinutes * 60L * 1000L,
                _updateRangeKm * 1000,
                _locationListener
        );
        _isTracking = true;
    }

    @Override
    public void stopTracking() {
        _locationManager.removeUpdates(_locationListener);
        _isTracking = false;
    }

    @Override
    public boolean isTracking() {
        return _isTracking;
    }

    private void sendLocation(Location location) {
        Position position = new Position();
        position.latitude = location.getLatitude();
        position.longitude = location.getLongitude();
        position.bearingDegrees = location.getBearing();
        position.speedMetersPerSecond = location.getSpeed();
        position.altitudeMeters = location.getAltitude();
        _trackerCallback.onSendLocation(position);
    }

    private final LocationListener _locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            sendLocation(location);
        }
    };
}
