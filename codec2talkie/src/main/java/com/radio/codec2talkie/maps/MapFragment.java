package com.radio.codec2talkie.maps;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.hardware.SensorEvent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.radio.codec2talkie.BuildConfig;
import com.radio.codec2talkie.R;
import com.radio.codec2talkie.protocol.Aprs;
import com.radio.codec2talkie.protocol.aprs.tools.AprsSymbolTable;
import com.radio.codec2talkie.settings.PreferenceKeys;
import com.radio.codec2talkie.tools.UnitTools;
import com.radio.codec2talkie.ui.FragmentMenuHandler;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.DelayedMapListener;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.modules.SqlTileWriter;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.Locale;

public class MapFragment extends Fragment implements FragmentMenuHandler {
    private static final String TAG = MapFragment.class.getSimpleName();

    private static final int AUTO_RESUME_FOLLOW_LOCATION_MS = 2000;
    private static final int AUTO_RESUME_CHECK_TRIGGER_MS = 250;

    private static final float MAP_FOLLOW_ZOOM = 18.0f;
    private static final float MAP_MAX_ZOOM = 20.0f;
    private static final int MAP_ANIMATION_DURATION_MS=500;

    private MapView _mapView;
    private IMapController _mapController;
    private MyLocationNewOverlay _myLocationNewOverlay;
    private CompassOverlay _compassOverlay;
    private TextView _locationInfoTextView;

    private final Handler _handler = new Handler(Looper.getMainLooper());
    private final Runnable _autoResumeRunnable = this::resumeFollow;

    private MapStations _mapStations;

    private SharedPreferences _sharedPreferences;
    private boolean _shouldFollowLocation = false;
    private boolean _rotateMapGps = false;
    private boolean _rotateMapCompass = false;
    private boolean _showMoving = false;
    private boolean _showCircles = false;
    private float _zoomLevel = MAP_FOLLOW_ZOOM;

    private String _positionInfo;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_map_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Configuration.getInstance().setUserAgentValue(Aprs.APRS_ID + " " + BuildConfig.VERSION_NAME);

        // config
        _sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        loadSettings();

        // location info label
        _locationInfoTextView = view.findViewById(R.id.location_info);

        // map
        _mapView = view.findViewById(R.id.map);
        _mapView.setTileSource(TileSourceFactory.MAPNIK);
        _mapView.setMultiTouchControls(true);

        // controller
        _mapController = _mapView.getController();
        _mapController.zoomTo(_zoomLevel);

        // compass
        _compassOverlay = getCompassOverlay(requireActivity());
        _mapView.getOverlays().add(_compassOverlay);

        // my location
        _myLocationNewOverlay = getLocationOverlay();

        // set own icon
        updateMyIcon(false);

        // my location overlay
        _myLocationNewOverlay.enableMyLocation();
        _myLocationNewOverlay.setEnableAutoStop(true);
        if (_shouldFollowLocation) {
            _myLocationNewOverlay.enableFollowLocation();
            _mapController.setZoom(_zoomLevel);
        }
        _myLocationNewOverlay.runOnFirstFix(() -> requireActivity().runOnUiThread(() -> {
            GeoPoint myLocation = _myLocationNewOverlay.getMyLocation();
            if (myLocation != null) {
                _mapController.setCenter(_myLocationNewOverlay.getMyLocation());
                _mapController.animateTo(_myLocationNewOverlay.getMyLocation());
            }
            if (_shouldFollowLocation) {
                _myLocationNewOverlay.enableFollowLocation();
            }
            _mapController.zoomTo(_zoomLevel);
        }));
        _mapView.getOverlays().add(_myLocationNewOverlay);


        // delayed user interaction for map follow updates
        _mapView.addMapListener(new DelayedMapListener(new MapListener() {
            @Override public boolean onScroll(ScrollEvent event) {
                onUserInteraction();
                return false;
            }
            @Override public boolean onZoom(ZoomEvent event) {
                onUserInteraction();
                return false;
            }
        }, AUTO_RESUME_CHECK_TRIGGER_MS));

        // stations
        _mapStations = new MapStations(requireActivity(), _mapView, this);
    }

    @NonNull
    private MyLocationNewOverlay getLocationOverlay() {
         return new MyLocationNewOverlay(new GpsMyLocationProvider(requireActivity()), _mapView) {
            @Override
            public void onLocationChanged(Location location, IMyLocationProvider source) {
                super.onLocationChanged(location, source);
                _positionInfo = String.format(Locale.US, "%dkm/h, %d°, %dm, %s, %f, %f",
                        UnitTools.metersPerSecondToKilometersPerHour((int)location.getSpeed()),
                        (int)location.getBearing(),
                        (int)location.getAltitude(),
                        UnitTools.decimalToMaidenhead(location.getLatitude(), location.getLongitude()),
                        location.getLatitude(),
                        location.getLongitude()
                );

                if (!location.hasBearing()) return;
                double currentBearing = location.getBearing();
                if (_rotateMapGps) {
                    boolean shouldRotate = (location.hasSpeed() && location.getSpeed() > 0) || !_rotateMapCompass;
                    if (shouldRotate) {
                        float mapOrientation = (360f - (float)currentBearing) % 360f;
                        rotateMapSmoothly(mapOrientation);
                    }
                } else {
                    boolean shouldFlip = (currentBearing > 180f && currentBearing < 360f);
                    updateMyIcon(shouldFlip);
                }
            }

            @Override
            public void draw(Canvas pCanvas, MapView pMapView, boolean pShadow) {
                super.draw(pCanvas, pMapView, pShadow);
                if (_positionInfo == null || !_shouldFollowLocation) return;
                _locationInfoTextView.setText(_positionInfo);
            }
        };
    }

    @NonNull
    private CompassOverlay getCompassOverlay(Context context) {
        InternalCompassOrientationProvider compassOrientationProvider = new InternalCompassOrientationProvider(context) {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if (_rotateMapCompass && sensorEvent.values.length > 0) {
                    Location lastLocation = _myLocationNewOverlay.getLastFix();
                    if (lastLocation == null || lastLocation.getSpeed() == 0 || !_rotateMapGps) {
                        // normalize azimuth into [0, 360)
                        float azimuth = sensorEvent.values[0];
                        float azimuthNorm = (azimuth % 360f + 360f) % 360f;
                        // map orientation = -azimuth (normalized to [0,360))
                        float mapOrientation = (360f - azimuthNorm) % 360f;
                        _mapView.setMapOrientation(mapOrientation);
                    }
                }
                super.onSensorChanged(sensorEvent);
            }
        };
        CompassOverlay compassOverlay = new CompassOverlay(context, compassOrientationProvider, _mapView);
        compassOverlay.enableCompass();
        return compassOverlay;
    }

    public void rotateMapSmoothly(float targetOrientation) {
        float currentOrientation = _mapView.getMapOrientation();
        float delta = targetOrientation - currentOrientation;
        if (delta > 180) {
            delta -= 360;
        } else if (delta < -180) {
            delta += 360;
        }
        ValueAnimator animator = ValueAnimator.ofFloat(currentOrientation, currentOrientation + delta);
        animator.setDuration(MAP_ANIMATION_DURATION_MS);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            float animatedValue = (float) animation.getAnimatedValue();
            _mapView.setMapOrientation(animatedValue);
        });
        animator.start();
    }

    private void loadSettings() {
        _shouldFollowLocation = _sharedPreferences.getBoolean(PreferenceKeys.APRS_MAP_MOVE_WITH_POSITION, false);
        _rotateMapGps = _sharedPreferences.getBoolean(PreferenceKeys.APRS_MAP_ROTATE_WITH_GPS, false);
        _rotateMapCompass = _sharedPreferences.getBoolean(PreferenceKeys.APRS_MAP_ROTATE_WITH_COMPASS, false);
        _showMoving = _sharedPreferences.getBoolean(PreferenceKeys.APRS_MAP_SHOW_MOVING_STATIONS, false);
        _showCircles = _sharedPreferences.getBoolean(PreferenceKeys.APRS_MAP_SHOW_RANGE_CIRCLES, false);
        _zoomLevel = _sharedPreferences.getFloat(PreferenceKeys.APRS_MAP_ZOOM_LEVEL, MAP_FOLLOW_ZOOM);
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = _sharedPreferences.edit();
        editor.putBoolean(PreferenceKeys.APRS_MAP_MOVE_WITH_POSITION, _shouldFollowLocation);
        editor.putBoolean(PreferenceKeys.APRS_MAP_ROTATE_WITH_GPS, _rotateMapGps);
        editor.putBoolean(PreferenceKeys.APRS_MAP_ROTATE_WITH_COMPASS, _rotateMapCompass);
        editor.putBoolean(PreferenceKeys.APRS_MAP_SHOW_MOVING_STATIONS, _showMoving);
        editor.putBoolean(PreferenceKeys.APRS_MAP_SHOW_RANGE_CIRCLES, _showCircles);
        editor.putFloat(PreferenceKeys.APRS_MAP_ZOOM_LEVEL, (float)_mapView.getZoomLevelDouble());
        editor.apply();
    }

    private void onUserInteraction() {
        if (_shouldFollowLocation && AUTO_RESUME_FOLLOW_LOCATION_MS > 0) {
            _handler.removeCallbacks(_autoResumeRunnable);
            _handler.postDelayed(_autoResumeRunnable, AUTO_RESUME_FOLLOW_LOCATION_MS);
        }
        saveSettings();
    }

    private void resumeFollow() {
        _handler.removeCallbacks(_autoResumeRunnable);
        GeoPoint last = _myLocationNewOverlay.getMyLocation();
        if (last != null) _mapController.animateTo(last);
        _myLocationNewOverlay.enableFollowLocation();
    }

    @Override
    public void onResume() {
        super.onResume();
        _mapView.onResume();
        _myLocationNewOverlay.onResume();
        _myLocationNewOverlay.enableMyLocation();
        if (_shouldFollowLocation) {
            _myLocationNewOverlay.enableFollowLocation();
            _mapController.setZoom(MAP_FOLLOW_ZOOM);
        }
        _compassOverlay.enableCompass();
    }

    @Override
    public void onPause() {
        super.onPause();
        _compassOverlay.disableCompass();
        _myLocationNewOverlay.disableMyLocation();
        _myLocationNewOverlay.onPause();
        _mapView.onPause();
    }

    public void updateMyIcon(boolean shouldFlip) {
        Configuration.getInstance().setUserAgentValue(Aprs.APRS_ID + " " + BuildConfig.VERSION_NAME);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());

        // my symbol
        AprsSymbolTable aprsSymbolTable = AprsSymbolTable.getInstance(requireActivity());
        String mySymbolCode = sharedPreferences.getString(PreferenceKeys.APRS_SYMBOL, "/[");

        Bitmap myBitmapIcon = aprsSymbolTable.bitmapFromSymbol(mySymbolCode, true);
        if (AprsSymbolTable.needsRotation(mySymbolCode)) {
            Matrix matrix = new Matrix();
            matrix.postRotate(-90, myBitmapIcon.getWidth() / 2f, myBitmapIcon.getHeight() / 2f);
            if (shouldFlip) {
                matrix.postScale(-1, 1, myBitmapIcon.getWidth() / 2f, myBitmapIcon.getHeight() / 2f);
            }
            myBitmapIcon = Bitmap.createBitmap(myBitmapIcon, 0, 0, myBitmapIcon.getWidth(), myBitmapIcon.getHeight(), matrix, true);
        }
        _myLocationNewOverlay.setDirectionIcon(myBitmapIcon);
        _myLocationNewOverlay.setDirectionAnchor(0.5f, 0.5f);
        _myLocationNewOverlay.setPersonIcon(myBitmapIcon);
        _myLocationNewOverlay.setPersonAnchor(0.5f, 0.5f);
    }

    @Override
    public boolean handleMenuItemClick(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.map_menu_clear_cache) {
            new Thread(() -> {
                _mapView.getTileProvider().clearTileCache();
                SqlTileWriter sqlTileWriter = new SqlTileWriter();
                boolean isCleared = sqlTileWriter.purgeCache(_mapView.getTileProvider().getTileSource().name());
                if (isCleared)
                    Log.i(TAG, "Cleanup completed");
                else
                    Log.e(TAG, "Cache was not cleared");
            }).start();
            return true;
        } else if (itemId == R.id.map_menu_rotate_map) {
            if (item.isChecked()) {
                item.setChecked(false);
                _rotateMapGps = false;
                _mapView.setMapOrientation(0);
            } else {
                item.setChecked(true);
                _rotateMapGps = true;
            }
            saveSettings();
            return true;
        } else if (itemId == R.id.map_menu_rotate_map_compass) {
            if (item.isChecked()) {
                item.setChecked(false);
                _rotateMapCompass = false;
                _mapView.setMapOrientation(0);
            } else {
                item.setChecked(true);
                _rotateMapCompass = true;
            }
            saveSettings();
            return true;
        } else if (itemId == R.id.map_menu_show_range) {
            if (item.isChecked()) {
                _showCircles = false;
                item.setChecked(false);
            } else {
                item.setChecked(true);
                _showCircles = true;
            }
            item.setChecked(_showCircles);
            _mapStations.showRangeCircles(_showCircles);
            saveSettings();
            return true;
        } else if (itemId == R.id.map_menu_show_moving) {
            if (item.isChecked()) {
                _showMoving = false;
                item.setChecked(false);
            } else {
                item.setChecked(true);
                _showMoving = true;
            }
            _mapStations.showMovingStations(_showMoving);
            saveSettings();
            return true;
        } else if (itemId == R.id.map_menu_move_map) {
            if (item.isChecked()) {
                item.setChecked(false);
                _shouldFollowLocation = false;
                _myLocationNewOverlay.disableFollowLocation();
                _locationInfoTextView.setText("");
            } else {
                item.setChecked(true);
                _shouldFollowLocation = true;
                _myLocationNewOverlay.enableFollowLocation();
                _mapController.setCenter(_myLocationNewOverlay.getMyLocation());
                _mapController.animateTo(_myLocationNewOverlay.getMyLocation());
                _mapController.zoomTo(_zoomLevel);
            }
            saveSettings();
            return true;
        } else if (itemId == R.id.map_menu_zoom_max) {
            _mapController.zoomTo(MAP_MAX_ZOOM);
            return true;
        }
        return false;
    }

    @Override
    public void handleMenuCreation(Menu menu) {
        MenuItem itemRotateMap = menu.findItem(R.id.map_menu_rotate_map);
        if (itemRotateMap != null) {
            itemRotateMap.setChecked(_rotateMapGps);
        }
        MenuItem itemRotateMapCompass = menu.findItem(R.id.map_menu_rotate_map_compass);
        if (itemRotateMapCompass != null) {
            itemRotateMapCompass.setChecked(_rotateMapCompass);
        }
        MenuItem itemShowRange = menu.findItem(R.id.map_menu_show_range);
        if (itemShowRange != null) {
            itemShowRange.setChecked(_showCircles);
        }
        MenuItem itemShowMoving = menu.findItem(R.id.map_menu_show_moving);
        if (itemShowMoving != null) {
            itemShowMoving.setChecked(_showMoving);
        }
        MenuItem itemMoveMap = menu.findItem(R.id.map_menu_move_map);
        if (itemMoveMap != null) {
            itemMoveMap.setChecked(_shouldFollowLocation);
        }
    }
}
