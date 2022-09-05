package com.radio.codec2talkie.maps;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.SensorEvent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.radio.codec2talkie.R;
import com.radio.codec2talkie.protocol.aprs.tools.AprsSymbolTable;
import com.radio.codec2talkie.protocol.position.Position;
import com.radio.codec2talkie.settings.PreferenceKeys;
import com.radio.codec2talkie.storage.position.PositionItem;
import com.radio.codec2talkie.storage.position.PositionItemViewModel;
import com.radio.codec2talkie.storage.station.StationItem;
import com.radio.codec2talkie.storage.station.StationItemViewModel;
import com.radio.codec2talkie.tools.DateTools;
import com.radio.codec2talkie.tools.UnitTools;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.modules.SqlTileWriter;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class MapActivity extends AppCompatActivity {
    private static final String TAG = MapActivity.class.getSimpleName();

    private MapView _map;
    private IMapController _mapController;
    private MyLocationNewOverlay _myLocationNewOverlay;
    private PositionItemViewModel _positionItemViewModel;
    private AprsSymbolTable _aprsSymbolTable;
    private MarkerInfoWindow _infoWindow;

    // live settings
    private boolean _rotateMap = false;
    private boolean _showCircles = false;

    // stations and circles
    private final HashMap<String, Marker> _objectOverlayItems = new HashMap<>();
    private final HashMap<String, Polygon> _objectOverlayRangeCircles = new HashMap<>();

    // track
    private LiveData<List<PositionItem>> _activeTrackLiveData;
    private final HashSet<Long> _activeTrackTimestamps = new HashSet<>();
    private final List<GeoPoint> _activeTrackPoints = new ArrayList<>();
    private final Polyline _activeTrackLine = new Polyline();

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
        String mySymbolCode = sharedPreferences.getString(PreferenceKeys.APRS_SYMBOL, "/[");

        // map
        _map = findViewById(R.id.map);
        _map.setTileSource(TileSourceFactory.MAPNIK);
        _map.setMultiTouchControls(true);
        _infoWindow = new MarkerInfoWindow(R.layout.bonuspack_bubble, _map);

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
        CompassOverlay compassOverlay = new CompassOverlay(context, compassOrientationProvider, _map);
        compassOverlay.enableCompass();
        _map.getOverlays().add(compassOverlay);

        // my location
        _myLocationNewOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(context), _map);
        Bitmap myBitmapIcon = _aprsSymbolTable.bitmapFromSymbol(mySymbolCode, true);
        _myLocationNewOverlay.setDirectionIcon(myBitmapIcon);
        _myLocationNewOverlay.setPersonIcon(myBitmapIcon);

        // my location overlay
        _myLocationNewOverlay.enableMyLocation();
        _myLocationNewOverlay.runOnFirstFix(() -> runOnUiThread(() -> {
            _mapController.setCenter(_myLocationNewOverlay.getMyLocation());
            _mapController.animateTo(_myLocationNewOverlay.getMyLocation());
        }));
        _map.getOverlays().add(_myLocationNewOverlay);

        // position items
        _positionItemViewModel = new ViewModelProvider(this).get(PositionItemViewModel.class);

        // station items, add data listener
        StationItemViewModel _stationItemViewModel = new ViewModelProvider(this).get(StationItemViewModel.class);
        // FIXME, room livedata sends all list if one item changed event with distinctUntilChanged
        _stationItemViewModel.getAllStationItems().observe(this, allStations -> {
            Log.i(TAG, "add stations " + allStations.size());
            for (StationItem station : allStations) {
                //Log.i(TAG, "new position " + station.getLatitude() + " " + station.getLongitude());
                // do not add items without coordinate
                if (station.getMaidenHead() == null) continue;
                if (addStationPositionIcon(station)) {
                    addRangeCircle(station);
                }
            }
        });

        // add track
        Paint p = _activeTrackLine.getOutlinePaint();
        p.setStrokeWidth(8);
        p.setColor(Color.RED);
        p.setStyle(Paint.Style.STROKE);
        p.setPathEffect(new DashPathEffect(new float[] {10f, 10f}, 0f));
        _map.getOverlayManager().add(_activeTrackLine);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.map_menu, menu);
        return true;
    }

    private void showRangeCircles(boolean isVisible) {
        for (Polygon polygon : _objectOverlayRangeCircles.values()) {
            polygon.setVisible(isVisible);
        }
    }

    private void addRangeCircle(StationItem group) {
        if (group.getRangeMiles() == 0) return;
        String callsign = group.getSrcCallsign();
        Polygon polygon = null;

        if (_objectOverlayRangeCircles.containsKey(callsign)) {
            polygon = _objectOverlayRangeCircles.get(callsign);
            assert polygon != null;
        }

        if (polygon == null) {
            polygon = new Polygon();
            polygon.setVisible(_showCircles);

            Paint p = polygon.getOutlinePaint();
            p.setStrokeWidth(1);

            _map.getOverlayManager().add(0, polygon);
            _objectOverlayRangeCircles.put(callsign, polygon);
        }
        ArrayList<GeoPoint> circlePoints = new ArrayList<>();
        for (float f = 0; f < 360; f += 6) {
            circlePoints.add(new GeoPoint(group.getLatitude(), group.getLongitude()).destinationPoint(1000 * UnitTools.milesToKilometers(group.getRangeMiles()), f));
        }
        polygon.setPoints(circlePoints);
    }

    private void addTrack(List<PositionItem> positions) {
        boolean shouldSet = false;
        for (PositionItem trackPoint : positions) {
            if (!_activeTrackTimestamps.contains(trackPoint.getTimestampEpoch())) {
                Log.i(TAG, "addPoint " + trackPoint.getTimestampEpoch() + " " + trackPoint.getLatitude() + " " + trackPoint.getLongitude());
                GeoPoint point = new GeoPoint(trackPoint.getLatitude(), trackPoint.getLongitude());
                _activeTrackPoints.add(point);
                _activeTrackTimestamps.add(trackPoint.getTimestampEpoch());
                shouldSet = true;
            }
        }
        if (shouldSet)
            _activeTrackLine.setPoints(_activeTrackPoints);
    }

    private boolean addStationPositionIcon(StationItem group) {
        String callsign = group.getSrcCallsign();
        Marker marker = null;

        String newTitle = DateTools.epochToIso8601(group.getTimestampEpoch()) + " " + callsign;
        String newSnippet = getStatus(group);

        // find old marker
        if (_objectOverlayItems.containsKey(callsign)) {
            marker = _objectOverlayItems.get(callsign);
            assert marker != null;

            // skip if unchanged
            GeoPoint oldPosition = marker.getPosition();
            if (oldPosition.getLatitude() == group.getLatitude() &&
                oldPosition.getLongitude() == group.getLongitude() &&
                marker.getTitle().equals(newTitle) &&
                marker.getSnippet().equals(newSnippet)) {

                return false;
            }
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
            // do not rotate
            if (group.getBearingDegrees() == 0 || !AprsSymbolTable.needsRotation(group.getSymbolCode())) {
                canvas.drawBitmap(bitmapIcon, bitmapLeft, 0, null);
            // rotate
            } else {
                float rotationDeg = (float) (group.getBearingDegrees() - 90.0f);
                Matrix m = new Matrix();
                // flip/rotate
                if (group.getBearingDegrees() > 180) {
                    m.postScale(-1, 1);
                    m.postTranslate(bitmapIcon.getWidth(), 0);
                    m.postRotate(rotationDeg - 180, bitmapIcon.getWidth() / 2.0f, bitmapIcon.getHeight() / 2.0f);
                // rotate
                } else {
                    m.postRotate(rotationDeg, bitmapIcon.getWidth() / 2.0f, bitmapIcon.getHeight() / 2.0f);
                }
                m.postTranslate(bitmapLeft, 0);
                canvas.drawBitmap(bitmapIcon, m, null);
            }

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
            marker.setId(callsign);
            marker.setIcon(drawableText);
            marker.setImage(drawableInfoIcon);
            marker.setOnMarkerClickListener((monitoredMarker, mapView) -> {
                GeoPoint markerPoint = monitoredMarker.getPosition();
                _infoWindow.open(monitoredMarker, new GeoPoint(markerPoint.getLatitude(), markerPoint.getLongitude()), 0, -2*height);
                if (_activeTrackLiveData != null)
                    _activeTrackLiveData.removeObservers(this);
                _map.getOverlays().remove(_activeTrackLine);
                _activeTrackPoints.clear();
                _activeTrackTimestamps.clear();
                _activeTrackLine.setPoints(_activeTrackPoints);
                _map.getOverlays().add(_activeTrackLine);
                // FIXME, room livedata sends all list if one item changed event with distinctUntilChanged
                _activeTrackLiveData = _positionItemViewModel.getPositionItems(monitoredMarker.getId());
                _activeTrackLiveData.observe(this, this::addTrack);
                return false;
            });
            _map.getOverlays().add(marker);
            _objectOverlayItems.put(callsign, marker);
        }

        marker.setPosition(new GeoPoint(group.getLatitude(), group.getLongitude()));
        marker.setTitle(newTitle);
        marker.setSnippet(newSnippet);

        return true;
    }

    private String getStatus(StationItem station) {
        double range = UnitTools.milesToKilometers(station.getRangeMiles());
        return String.format(Locale.US, "%s<br>%s %f %f<br>%03d° %03dkm/h %04dm %.2fkm<br>%s %s",
                station.getDigipath(),
                station.getMaidenHead(), station.getLatitude(), station.getLongitude(),
                (int)station.getBearingDegrees(),
                UnitTools.metersPerSecondToKilometersPerHour((int)station.getSpeedMetersPerSecond()),
                (int)station.getAltitudeMeters(),
                range == 0 ? UnitTools.milesToKilometers(Position.DEFAULT_RANGE_MILES): range,
                station.getStatus(),
                station.getComment());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.map_menu_clear_cache) {
            new Thread(() -> {
                _map.getTileProvider().clearTileCache();
                SqlTileWriter sqlTileWriter = new SqlTileWriter();
                boolean isCleared = sqlTileWriter.purgeCache(_map.getTileProvider().getTileSource().name());
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
                _map.setMapOrientation(0);
            } else {
                item.setChecked(true);
                _rotateMap = true;
            }
            return true;
        } else if (itemId == R.id.map_menu_show_range) {
            if (item.isChecked()) {
                item.setChecked(false);
                _showCircles = false;
                _map.setMapOrientation(0);
            } else {
                item.setChecked(true);
                _showCircles = true;
            }
            showRangeCircles(_showCircles);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
