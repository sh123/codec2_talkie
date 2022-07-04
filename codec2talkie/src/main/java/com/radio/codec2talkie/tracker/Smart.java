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

public class Smart implements Tracker {
    private static final String TAG = Periodic.class.getSimpleName();

    private boolean _isTracking = false;
    private TrackerCallback _trackerCallback;
    private Context _context;
    private LocationManager _locationManager;

    private int _fastSpeed;
    private int _fastRate;
    private int _slowSpeed;
    private int _slowRate;
    private int _minTurnTime;
    private int _minTurnAngle;
    private int _turnSlope;

    @Override
    public void initialize(Context context, TrackerCallback trackerCallback) {
        _context = context;
        _trackerCallback  = trackerCallback;
        _locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        _fastSpeed = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.APRS_LOCATION_SOURCE_SMART_FAST_SPEED, "90"));
        _fastRate = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.APRS_LOCATION_SOURCE_SMART_FAST_RATE, "60"));
        _slowSpeed = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.APRS_LOCATION_SOURCE_SMART_SLOW_SPEED, "5"));
        _slowRate = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.APRS_LOCATION_SOURCE_SMART_SLOW_RATE, "1200"));
        _minTurnTime = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.APRS_LOCATION_SOURCE_SMART_MIN_TURN_TIME, "15"));
        _minTurnTime = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.APRS_LOCATION_SOURCE_SMART_MIN_TURN_ANGLE, "10"));
        _turnSlope = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.APRS_LOCATION_SOURCE_SMART_TURN_SLOPE, "240"));
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
                5000L,
                5,
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

    private void processNewLocation(Location location) {
        // TODO, add Smartbeaconing logic
    }

    private final LocationListener _locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            processNewLocation(location);
        }
    };
}
