package com.radio.codec2talkie.maps;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.SensorEvent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.modules.SqlTileWriter;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.Locale;

public class MapFragment extends Fragment implements FragmentMenuHandler {
    private static final String TAG = MapFragment.class.getSimpleName();

    private static final double MAP_STARTUP_ZOOM = 5.0;
    private static final double MAP_FOLLOW_ZOOM = 14.0;

    private MapView _mapView;
    private IMapController _mapController;
    private MyLocationNewOverlay _myLocationNewOverlay;
    private TextView _locationInfoTextView;

    private MapStations _mapStations;
    private boolean _rotateMap = false;
    private boolean _shouldFollowLocation = false;
    private boolean _showMoving = false;
    private boolean _showCircles = false;

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

        // location info label
        _locationInfoTextView = view.findViewById(R.id.location_info);

        // map
        _mapView = view.findViewById(R.id.map);
        _mapView.setTileSource(TileSourceFactory.MAPNIK);
        _mapView.setMultiTouchControls(true);

        // controller
        _mapController = _mapView.getController();
        _mapController.zoomTo(MAP_STARTUP_ZOOM);

        // compass
        CompassOverlay compassOverlay = getCompassOverlay(requireActivity());
        _mapView.getOverlays().add(compassOverlay);

        // my location
        _myLocationNewOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireActivity()), _mapView) {
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

                double currentBearing = location.getBearing();
                boolean shouldFlip = (currentBearing > 90f && currentBearing < 270f);
                updateMyIcon(shouldFlip);
            }

            @Override
            public void draw(Canvas pCanvas, MapView pMapView, boolean pShadow) {
                super.draw(pCanvas, pMapView, pShadow);
                if (_positionInfo == null || !_shouldFollowLocation) return;
                _locationInfoTextView.setText(_positionInfo);
            }
        };

        // set own icon
        updateMyIcon(false);

        // my location overlay
        _myLocationNewOverlay.enableMyLocation();
        _myLocationNewOverlay.runOnFirstFix(() -> requireActivity().runOnUiThread(() -> {
            _mapController.setCenter(_myLocationNewOverlay.getMyLocation());
            _mapController.animateTo(_myLocationNewOverlay.getMyLocation());
        }));
        _mapView.getOverlays().add(_myLocationNewOverlay);

        // stations
        _mapStations = new MapStations(requireActivity(), _mapView, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        _myLocationNewOverlay.enableMyLocation();

        if (_shouldFollowLocation) {
            _myLocationNewOverlay.enableFollowLocation();
            _mapController.setZoom(MAP_FOLLOW_ZOOM);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        _myLocationNewOverlay.disableMyLocation();
    }

    @NonNull
    private CompassOverlay getCompassOverlay(Context context) {
        InternalCompassOrientationProvider compassOrientationProvider = new InternalCompassOrientationProvider(context) {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if (_rotateMap) {
                    // normalize azimuth into [0, 360)
                    float azimuth = sensorEvent.values[0];
                    float azimuthNorm = (azimuth % 360f + 360f) % 360f;
                    // map orientation = -azimuth (normalized to [0,360))
                    float mapOrientation = (360f - azimuthNorm) % 360f;
                    _mapView.setMapOrientation(mapOrientation);
                }
                super.onSensorChanged(sensorEvent);
            }
        };
        CompassOverlay compassOverlay = new CompassOverlay(context, compassOrientationProvider, _mapView);
        compassOverlay.enableCompass();
        return compassOverlay;
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
            matrix.postRotate(-90);
            if (shouldFlip) {
                matrix.postScale(-1, 1, myBitmapIcon.getWidth() / 2f, myBitmapIcon.getHeight() / 2f);
            }
            myBitmapIcon = Bitmap.createBitmap(myBitmapIcon, 0, 0, myBitmapIcon.getWidth(), myBitmapIcon.getHeight(), matrix, true);
        }
        _myLocationNewOverlay.setDirectionIcon(myBitmapIcon);
        _myLocationNewOverlay.setPersonIcon(myBitmapIcon);
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
                _rotateMap = false;
                _mapView.setMapOrientation(0);
            } else {
                item.setChecked(true);
                _rotateMap = true;
            }
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
                _mapController.zoomTo(MAP_FOLLOW_ZOOM);
            }
            return true;
        }
        return false;
    }

    @Override
    public void handleMenuCreation(Menu menu) {
        MenuItem itemRotateMap = menu.findItem(R.id.map_menu_rotate_map);
        if (itemRotateMap != null) {
            itemRotateMap.setChecked(_rotateMap);
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
