package com.radio.codec2talkie.maps;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.radio.codec2talkie.protocol.aprs.tools.AprsSymbolTable;
import com.radio.codec2talkie.protocol.position.Position;
import com.radio.codec2talkie.storage.position.PositionItemViewModel;
import com.radio.codec2talkie.storage.station.StationItem;
import com.radio.codec2talkie.storage.station.StationItemViewModel;
import com.radio.codec2talkie.tools.DateTools;
import com.radio.codec2talkie.tools.UnitTools;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MapStations {
    private static final String TAG = MapTrack.class.getSimpleName();

    private final Context _context;

    private final AprsSymbolTable _aprsSymbolTable;

    private final PositionItemViewModel _positionItemViewModel;
    private final ViewModelStoreOwner _owner;
    private final MapView _mapView;

    private final MarkerInfoWindow _infoWindow;

    private LiveData<List<StationItem>> _stationItemLiveData;
    private final StationItemViewModel _stationItemViewModel;

    private final HashMap<String, Marker> _objectOverlayItems = new HashMap<>();
    private final HashMap<String, Polygon> _objectOverlayRangeCircles = new HashMap<>();

    private boolean _showCircles = false;
    private boolean _showMoving = false;

    private final MapTrack _activeTrack;

    public MapStations(Context context, MapView mapView, ViewModelStoreOwner owner) {
        _context = context;
        _owner = owner;
        _mapView = mapView;
        _positionItemViewModel = new ViewModelProvider(_owner).get(PositionItemViewModel.class);

        _aprsSymbolTable = AprsSymbolTable.getInstance(context);
        _infoWindow = new MarkerInfoWindow(org.osmdroid.library.R.layout.bonuspack_bubble, _mapView);
        _activeTrack = new MapTrack(_context, _mapView, _owner);

        _stationItemViewModel = new ViewModelProvider(_owner).get(StationItemViewModel.class);
        loadStations(_showMoving);
    }

    private void loadStations(boolean movingOnly) {
        if (_stationItemLiveData != null)
            _stationItemLiveData.removeObservers((LifecycleOwner) _owner);
        removePositionMarkers();
        _stationItemLiveData = _stationItemViewModel.getAllStationItems(movingOnly);
        // FIXME, room livedata sends all list if one item changed event with distinctUntilChanged
        _stationItemLiveData.observe((LifecycleOwner) _owner, allStations -> {
            Log.i(TAG, "add stations " + allStations.size());
            for (StationItem station : allStations) {
                //Log.i(TAG, "new position " + station.getSrcCallsign() + ">" +
                //        station.getDstCallsign() + " " + station.getLatitude() + " " + station.getLongitude());
                // do not add items without coordinate
                if (station.getMaidenHead() == null) continue;
                try {
                    if (addStationPositionIcon(station)) {
                        addRangeCircle(station);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void removePositionMarkers() {
        for (Marker marker : _objectOverlayItems.values()) {
            marker.remove(_mapView);
        }
        _objectOverlayItems.clear();
        for (Polygon circle : _objectOverlayRangeCircles.values()) {
            _mapView.getOverlays().remove(circle);
        }
        _objectOverlayRangeCircles.clear();
    }

    public void showMovingStations(boolean isMoving) {
        _showMoving = isMoving;
        loadStations(_showMoving);
    }

    public void showRangeCircles(boolean isVisible) {
        _showCircles = isVisible;
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

            _mapView.getOverlayManager().add(0, polygon);
            _objectOverlayRangeCircles.put(callsign, polygon);
        }
        ArrayList<GeoPoint> circlePoints = new ArrayList<>();
        for (float f = 0; f < 360; f += 6) {
            circlePoints.add(new GeoPoint(group.getLatitude(), group.getLongitude()).destinationPoint(1000 * UnitTools.milesToKilometers(group.getRangeMiles()), f));
        }
        polygon.setPoints(circlePoints);
    }

    private boolean addStationPositionIcon(StationItem group) {
        String callsign = group.getSrcCallsign();
        Marker marker = null;

        String newTitle = callsign + " " + DateTools.epochToIso8601(group.getTimestampEpoch());
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
            Bitmap bitmapInfoIcon = _aprsSymbolTable.bitmapFromSymbol(group.getSymbolCode(), true);
            if (bitmapInfoIcon == null) return false;

            // add marker
            BitmapDrawable drawableText = group.drawLabelWithIcon(_context, 12);
            BitmapDrawable drawableInfoIcon = new BitmapDrawable(_context.getResources(), bitmapInfoIcon);
            marker = new Marker(_mapView);
            marker.setId(callsign);
            if (drawableText == null)
                Log.e(TAG, "Cannot load icon for " + callsign);
            else
                marker.setIcon(drawableText);
            marker.setImage(drawableInfoIcon);
            marker.setOnMarkerClickListener((monitoredStationMarker, mapView) -> {
                GeoPoint markerPoint = monitoredStationMarker.getPosition();
                _infoWindow.open(monitoredStationMarker, new GeoPoint(markerPoint.getLatitude(), markerPoint.getLongitude()), 0, -64);
                _activeTrack.drawForStationMarker(monitoredStationMarker);
                return false;
            });
            _mapView.getOverlays().add(marker);
            _objectOverlayItems.put(callsign, marker);
        }

        marker.setPosition(new GeoPoint(group.getLatitude(), group.getLongitude()));
        marker.setTitle(newTitle);
        marker.setSnippet(newSnippet);

        return true;
    }

    private String getStatus(StationItem station) {
        double range = UnitTools.milesToKilometers(station.getRangeMiles());

        // position, speed, altitude
        String data = String.format(Locale.US, "%s %f %f<br><b>%03d° %03dkm/h alt %04dm range %.2fkm</b>",
                station.getMaidenHead(), station.getLatitude(), station.getLongitude(),
                (int)station.getBearingDegrees(),
                UnitTools.metersPerSecondToKilometersPerHour((int)station.getSpeedMetersPerSecond()),
                (int)station.getAltitudeMeters(),
                range == 0 ? UnitTools.milesToKilometers(Position.DEFAULT_RANGE_MILES): range);

        // status + comment
        String status = station.getStatus();
        String comment = station.getComment();
        if (!status.isBlank() || !comment.isBlank())
            data += "<br><i><font color='#006400'>" + status + " " + comment + "</font></i></hr>";

        // device id description
        String deviceIdDescription = station.getDeviceIdDescription();
        if (deviceIdDescription != null && !deviceIdDescription.isEmpty())
            data += "<br><small>(" + deviceIdDescription + ")</small>";

        // raw target and digipath
        data += "<br><small>[" + station.getDstCallsign() + " " + station.getDigipath() + "]</small>";
        return data;
    }
}
