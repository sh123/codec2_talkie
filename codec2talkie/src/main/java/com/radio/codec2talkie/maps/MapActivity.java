package com.radio.codec2talkie.maps;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.radio.codec2talkie.R;
import com.radio.codec2talkie.protocol.aprs.tools.AprsSymbolTable;
import com.radio.codec2talkie.settings.PreferenceKeys;
import com.radio.codec2talkie.storage.log.LogItemViewModel;
import com.radio.codec2talkie.storage.log.group.LogItemGroup;
import com.radio.codec2talkie.tools.DateTools;
import com.radio.codec2talkie.tools.UnitTools;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.infowindow.InfoWindow;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class MapActivity extends AppCompatActivity {
    private static final String TAG = MapActivity.class.getSimpleName();

    private MapView _map;
    private IMapController _mapController;
    private CompassOverlay _compassOverlay;
    private MyLocationNewOverlay _myLocationNewOverlay;
    private ItemizedIconOverlay<OverlayItem> _objectOverlay;
    private final HashMap<String, Marker> _objectOverlayItems = new HashMap<>();

    private LogItemViewModel _logItemViewModel;
    private AprsSymbolTable _aprsSymbolTable;

    private String _mySymbolCode;
    private boolean _rotateMap = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.menu_aprs_map);
        setContentView(R.layout.activity_map_view);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        Context context = getApplicationContext();
        Configuration.getInstance().setUserAgentValue("C2T");
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        _aprsSymbolTable = AprsSymbolTable.getInstance(context);
        _mySymbolCode = sharedPreferences.getString(PreferenceKeys.APRS_SYMBOL, "/[");

        // map
        _map = findViewById(R.id.map);
        _map.setTileSource(TileSourceFactory.MAPNIK);
        _map.setMultiTouchControls(true);

        // controller
        _mapController = _map.getController();
        _mapController.zoomTo(5.0);

        // compass
        InternalCompassOrientationProvider compassOrientationProvider = new InternalCompassOrientationProvider(context) {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if (_rotateMap) {
                    _map.setMapOrientation(-sensorEvent.values[0]);
                }
                super.onSensorChanged(sensorEvent);
            }
        };
        _compassOverlay = new CompassOverlay(context, compassOrientationProvider, _map);
        _compassOverlay.enableCompass();
        _map.getOverlays().add(_compassOverlay);

        // my location
        _myLocationNewOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(context), _map);
        Bitmap myBitmapIcon = _aprsSymbolTable.bitmapFromSymbol(_mySymbolCode, true);
        _myLocationNewOverlay.setDirectionIcon(myBitmapIcon);
        _myLocationNewOverlay.setPersonIcon(myBitmapIcon);

        // my location overlay
        _myLocationNewOverlay.enableMyLocation();
        _myLocationNewOverlay.runOnFirstFix(() -> runOnUiThread(() -> {
            _mapController.setCenter(_myLocationNewOverlay.getMyLocation());
            _mapController.animateTo(_myLocationNewOverlay.getMyLocation());
        }));
        _map.getOverlays().add(_myLocationNewOverlay);

        // add data listener
        _logItemViewModel = new ViewModelProvider(this).get(LogItemViewModel.class);
        _logItemViewModel.getGroups().observe(this, logItemGroups -> {
            for (LogItemGroup group : logItemGroups) {
                if (!addIcon(group)) {
                    Log.e(TAG, "Failed to add APRS icon");
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.map_menu, menu);
        return true;
    }

    private boolean addIcon(LogItemGroup group) {
        String callsign = group.getSrcCallsign();
        Marker marker = null;

        // find old marker
        if (_objectOverlayItems.containsKey(callsign)) {
            marker = _objectOverlayItems.get(callsign);
            assert marker != null;
        }

        // create new marker
        if (marker == null) {
            // icon from symbol
            Bitmap bitmapIcon = _aprsSymbolTable.bitmapFromSymbol(group.getSymbolCode(), false);
            if (bitmapIcon == null) return false;
            Bitmap bitmapInfoIcon = _aprsSymbolTable.bitmapFromSymbol(group.getSymbolCode(), true);
            if (bitmapInfoIcon == null) return false;

            // construct and calculate bounds
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.FILL);
            Rect bounds = new Rect();
            paint.getTextBounds(callsign, 0, callsign.length(), bounds);
            int width = Math.max(bitmapIcon.getWidth(), bounds.width());
            int height = bitmapIcon.getHeight() + bounds.height();

            // create overlay bitmap
            Bitmap bitmap = Bitmap.createBitmap(width, height, null);
            bitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);

            // draw APRS icon
            Canvas canvas = new Canvas(bitmap);
            float bitmapLeft = width > bitmapIcon.getWidth() ? width / 2.0f - bitmapIcon.getWidth() / 2.0f : 0;
            canvas.drawBitmap(bitmapIcon, bitmapLeft, 0, null);

            // draw background
            paint.setColor(Color.WHITE);
            paint.setAlpha(120);
            bounds.set(0, bitmapIcon.getHeight(), width, height);
            canvas.drawRect(bounds, paint);

            // draw text
            paint.setColor(Color.BLACK);
            paint.setAlpha(255);
            paint.setTextSize(12);
            paint.setFlags(Paint.ANTI_ALIAS_FLAG);
            canvas.drawText(callsign, 0, height, paint);

            // add marker
            BitmapDrawable drawableText = new BitmapDrawable(getResources(), bitmap);
            BitmapDrawable drawableInfoIcon = new BitmapDrawable(getResources(), bitmapInfoIcon);
            marker = new Marker(_map);
            marker.setIcon(drawableText);
            marker.setImage(drawableInfoIcon);

            _map.getOverlays().add(marker);
            _objectOverlayItems.put(callsign, marker);
        }
        marker.setPosition(new GeoPoint(group.getLatitude(), group.getLongitude()));
        marker.setTitle(DateTools.epochToIso8601(group.getTimestampEpoch()) + " " + callsign);
        marker.setSnippet(getStatus(group));

        return true;
    }

    private String getStatus(LogItemGroup group) {
        return String.format(Locale.US, "%s %f %f<br>%03d° %03dkm/h %04dm<br>%s %s",
                group.getMaidenHead(),
                group.getLatitude(),
                group.getLongitude(),
                (int)group.getBearingDegrees(),
                UnitTools.metersPerSecondToKilometersPerHour((int)group.getSpeedMetersPerSecond()),
                (int)group.getAltitudeMeters(),
                group.getStatus(),
                group.getComment());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.map_menu_clear_cache) {
            _map.getTileProvider().clearTileCache();
            return true;
        } else if (itemId == R.id.map_menu_rotate_map) {
            if (item.isChecked()) {
                item.setChecked(false);
                _rotateMap = false;
                _map.setMapOrientation(0);
            } else {
                item.setChecked(true);
                _rotateMap = true;
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
