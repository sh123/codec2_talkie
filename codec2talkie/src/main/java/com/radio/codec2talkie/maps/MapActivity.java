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
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.radio.codec2talkie.BuildConfig;
import com.radio.codec2talkie.R;
import com.radio.codec2talkie.protocol.Aprs;
import com.radio.codec2talkie.protocol.aprs.tools.AprsSymbolTable;
import com.radio.codec2talkie.settings.PreferenceKeys;
import com.radio.codec2talkie.tools.UnitTools;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.modules.SqlTileWriter;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.Locale;

public class MapActivity extends AppCompatActivity {
    private static final String TAG = MapActivity.class.getSimpleName();

    private static final double MAP_STARTUP_ZOOM = 5.0;
    private static final double MAP_FOLLOW_ZOOM = 14.0;

    private MapView _mapView;
    private IMapController _mapController;
    private MyLocationNewOverlay _myLocationNewOverlay;

    private MapStations _mapStations;
    private boolean _rotateMap = false;
    private boolean _shouldFollowLocation = false;

    private String _positionInfo;
    private double _prevBearing = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.menu_aprs_map);
        setContentView(R.layout.activity_map_view);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        Context context = getApplicationContext();
        Configuration.getInstance().setUserAgentValue(Aprs.APRS_ID + " " + BuildConfig.VERSION_NAME);

        // map
        _mapView = findViewById(R.id.map);
        _mapView.setTileSource(TileSourceFactory.MAPNIK);
        _mapView.setMultiTouchControls(true);

        // controller
        _mapController = _mapView.getController();
        _mapController.zoomTo(MAP_STARTUP_ZOOM);

        // compass
        InternalCompassOrientationProvider compassOrientationProvider = new InternalCompassOrientationProvider(context) {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if (_rotateMap) {
                    _mapView.setMapOrientation(-sensorEvent.values[0]);
                }
                super.onSensorChanged(sensorEvent);
            }
        };
        CompassOverlay compassOverlay = new CompassOverlay(context, compassOrientationProvider, _mapView);
        compassOverlay.enableCompass();
        _mapView.getOverlays().add(compassOverlay);

        // my location
        _myLocationNewOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(context), _mapView) {
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

                if (_prevBearing > 180 && currentBearing <= 180)
                    updateMyIcon(false);
                else if (_prevBearing <= 180 && currentBearing > 180)
                    updateMyIcon(true);

                _prevBearing = currentBearing;
            }

            @Override
            public void draw(Canvas pCanvas, MapView pMapView, boolean pShadow) {
                super.draw(pCanvas, pMapView, pShadow);
                if (_positionInfo == null || !_shouldFollowLocation) return;

                // create paint
                Paint paint = new Paint();
                paint.setStyle(Paint.Style.FILL);
                paint.setTextSize(24);

                // query bounds from text
                Rect bounds = new Rect();
                paint.getTextBounds(_positionInfo, 0, _positionInfo.length(), bounds);

                // draw background
                paint.setColor(Color.WHITE);
                paint.setAlpha(200);
                pCanvas.drawRect(pCanvas.getWidth() - bounds.width(),
                        0,
                        pCanvas.getWidth(),
                        bounds.height(),
                        paint);

                // draw text
                paint.setColor(Color.BLACK);
                paint.setAlpha(255);
                paint.setFlags(Paint.ANTI_ALIAS_FLAG);
                pCanvas.drawText(_positionInfo, pCanvas.getWidth() - bounds.width(), bounds.height(), paint);
            }
        };

        // set own icon
        updateMyIcon(false);

        // my location overlay
        _myLocationNewOverlay.enableMyLocation();
        _myLocationNewOverlay.runOnFirstFix(() -> runOnUiThread(() -> {
            _mapController.setCenter(_myLocationNewOverlay.getMyLocation());
            _mapController.animateTo(_myLocationNewOverlay.getMyLocation());
        }));
        _mapView.getOverlays().add(_myLocationNewOverlay);

        // stations
        _mapStations = new MapStations(context, _mapView, this);
    }

    public void updateMyIcon(boolean shouldFlip) {
        Context context = getApplicationContext();
        Configuration.getInstance().setUserAgentValue(Aprs.APRS_ID + " " + BuildConfig.VERSION_NAME);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        // my symbol
        AprsSymbolTable aprsSymbolTable = AprsSymbolTable.getInstance(context);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.map_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.map_menu_clear_cache) {
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
            boolean showCircles = false;
            if (item.isChecked()) {
                item.setChecked(false);
            } else {
                item.setChecked(true);
                showCircles = true;
            }
            _mapStations.showRangeCircles(showCircles);
            return true;
        } else if (itemId == R.id.map_menu_show_moving) {
            boolean showMoving = false;
            if (item.isChecked()) {
                item.setChecked(false);
            } else {
                item.setChecked(true);
                showMoving = true;
            }
            _mapStations.showMovingStations(showMoving);
            return true;
        } else if (itemId == R.id.map_menu_move_map) {
            if (item.isChecked()) {
                item.setChecked(false);
                _shouldFollowLocation = false;
                _myLocationNewOverlay.disableFollowLocation();
            } else {
                item.setChecked(true);
                _shouldFollowLocation = true;
                _myLocationNewOverlay.enableFollowLocation();
                _mapController.zoomTo(MAP_FOLLOW_ZOOM);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
