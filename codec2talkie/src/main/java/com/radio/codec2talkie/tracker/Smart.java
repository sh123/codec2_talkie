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
import com.radio.codec2talkie.tools.UnitTools;

public class Smart implements Tracker {
    private static final String TAG = Periodic.class.getSimpleName();

    private boolean _isTracking = false;
    private TrackerCallback _trackerCallback;
    private Context _context;
    private LocationManager _locationManager;

    private int _fastSpeedKmph;
    private int _fastRateSeconds;
    private int _slowSpeedKmph;
    private int _slowRateSeconds;
    private int _minTurnTimeSeconds;
    private int _minTurnAngleDegrees;
    private int _turnSlope;

    private Position _oldPosition;

    @Override
    public void initialize(Context context, TrackerCallback trackerCallback) {
        _context = context;
        _trackerCallback  = trackerCallback;
        _locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        _fastSpeedKmph = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.APRS_LOCATION_SOURCE_SMART_FAST_SPEED, "90"));
        _fastRateSeconds = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.APRS_LOCATION_SOURCE_SMART_FAST_RATE, "60"));
        _slowSpeedKmph = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.APRS_LOCATION_SOURCE_SMART_SLOW_SPEED, "5"));
        _slowRateSeconds = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.APRS_LOCATION_SOURCE_SMART_SLOW_RATE, "1200"));
        _minTurnTimeSeconds = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.APRS_LOCATION_SOURCE_SMART_MIN_TURN_TIME, "15"));
        _minTurnAngleDegrees = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.APRS_LOCATION_SOURCE_SMART_MIN_TURN_ANGLE, "10"));
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
                0,
                0,
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
        _trackerCallback.onSendLocation(Position.fromLocation(location));
    }

    private boolean isCornerPeggingTriggered(Position newPosition, Position oldPosition) {
        long timeDifferenceSeconds = UnitTools.millisToSeconds(newPosition.timestampEpochMs - oldPosition.timestampEpochMs);

        double turnAngleDegrees = Math.abs(newPosition.bearingDegrees - oldPosition.bearingDegrees) % 360;
        turnAngleDegrees = turnAngleDegrees <= 180 ? turnAngleDegrees : 360 - turnAngleDegrees;

        if (!newPosition.hasBearing || newPosition.speedMetersPerSecond == 0)
            return false;
        if (!oldPosition.hasBearing)
            return timeDifferenceSeconds >= _minTurnTimeSeconds;

        double thresholdDegrees = _minTurnAngleDegrees + _turnSlope / UnitTools.metersPerSecondToMilesPerHour(newPosition.speedMetersPerSecond);
        return timeDifferenceSeconds >= _minTurnTimeSeconds && turnAngleDegrees > thresholdDegrees;
    }

    private boolean shouldSendPosition(Position newPosition, Position oldPosition) {
        if (oldPosition == null) return true;
        if (isCornerPeggingTriggered(newPosition, oldPosition)) return true;

        double distanceMeters = oldPosition.distanceTo(newPosition);
        long timeDifferenceSeconds = UnitTools.millisToSeconds(newPosition.timestampEpochMs - oldPosition.timestampEpochMs);
        double maxSpeedMetersPerSecond = Math.max(Math.max(distanceMeters/timeDifferenceSeconds, newPosition.speedMetersPerSecond), oldPosition.speedMetersPerSecond);
        double maxSpeedKilometersPerSecond = UnitTools.kilometersPerSecondToMetersPerSecond(maxSpeedMetersPerSecond);
        double speedRateSeconds;
        if (maxSpeedKilometersPerSecond <= _slowSpeedKmph)
            speedRateSeconds = _slowRateSeconds;
        else if (maxSpeedKilometersPerSecond >= _fastSpeedKmph)
            speedRateSeconds = _fastRateSeconds;
        else
            speedRateSeconds = (_fastRateSeconds + (_slowRateSeconds - _fastRateSeconds) * (_fastSpeedKmph - maxSpeedKilometersPerSecond) / (_fastSpeedKmph - _slowSpeedKmph));

        return timeDifferenceSeconds >= (long)speedRateSeconds;
    }

    private void processNewLocation(Location location) {
        Position newPosition = Position.fromLocation(location);
        if (shouldSendPosition(newPosition, _oldPosition)) {
            _trackerCallback.onSendLocation(newPosition);
            _oldPosition = newPosition;
        }
    }

    private final LocationListener _locationListener = this::processNewLocation;
}
