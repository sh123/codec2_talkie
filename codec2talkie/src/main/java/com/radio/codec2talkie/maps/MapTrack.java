package com.radio.codec2talkie.maps;

import android.content.Context;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.util.Log;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.radio.codec2talkie.storage.position.PositionItem;
import com.radio.codec2talkie.storage.position.PositionItemViewModel;
import com.radio.codec2talkie.tools.BitmapTools;
import com.radio.codec2talkie.tools.DateTools;
import com.radio.codec2talkie.tools.UnitTools;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class MapTrack {
    private static final String TAG = MapTrack.class.getSimpleName();

    private final PositionItemViewModel _positionItemViewModel;
    private final ViewModelStoreOwner _owner;
    private final MapView _mapView;
    private final Context _context;

    // track db data
    private LiveData<List<PositionItem>> _activeTrackLiveData;

    // track data
    private final HashSet<Long> _activeTrackTimestamps = new HashSet<>();
    private final List<GeoPoint> _activeTrackPoints = new ArrayList<>();
    private final Polyline _activeTrackLine = new Polyline();

    // track points
    private final HashSet<Marker> _trackMarkers = new HashSet<>();

    public MapTrack(Context context, MapView mapView, ViewModelStoreOwner owner) {
        _owner = owner;
        _mapView = mapView;
        _context = context;
        _positionItemViewModel = new ViewModelProvider(_owner).get(PositionItemViewModel.class);

        // initialize track
        Paint p = _activeTrackLine.getOutlinePaint();
        p.setStrokeWidth(8);
        p.setColor(Color.RED);
        p.setStyle(Paint.Style.STROKE);
        p.setPathEffect(new DashPathEffect(new float[] {10f, 10f}, 0f));
        _mapView.getOverlayManager().add(_activeTrackLine);
    }

    public void drawForStationMarker(Marker marker) {
        if (_activeTrackLiveData != null)
            _activeTrackLiveData.removeObservers((LifecycleOwner) _owner);

        for (Marker trackMarker : _trackMarkers) {
            _mapView.getOverlays().remove(trackMarker);
        }
        _mapView.getOverlays().remove(_activeTrackLine);

        _activeTrackPoints.clear();
        _activeTrackTimestamps.clear();
        _trackMarkers.clear();

        _activeTrackLine.setPoints(_activeTrackPoints);
        _activeTrackLine.setVisible(false);
        _mapView.getOverlays().add(_activeTrackLine);

        // FIXME, room livedata sends all list if one item changed event with distinctUntilChanged
        _activeTrackLiveData = _positionItemViewModel.getPositionItems(marker.getId());
        _activeTrackLiveData.observe((LifecycleOwner) _owner, this::addTrack);
    }

    private void addTrack(List<PositionItem> positions) {
        boolean shouldSet = false;
        for (PositionItem trackPoint : positions) {
            if (!_activeTrackTimestamps.contains(trackPoint.getTimestampEpoch())) {
                long pointTimestamp = trackPoint.getTimestampEpoch();
                Log.i(TAG, "addPoint " + trackPoint.getTimestampEpoch() + " " + trackPoint.getLatitude() + " " + trackPoint.getLongitude());

                // add point into the line
                GeoPoint point = new GeoPoint(trackPoint.getLatitude(), trackPoint.getLongitude());
                _activeTrackPoints.add(point);
                _activeTrackTimestamps.add(pointTimestamp);
                if (_activeTrackPoints.size() > 1)
                    _activeTrackLine.setVisible(true);

                // draw point marker with time
                Marker marker = new Marker(_mapView);
                marker.setIcon(BitmapTools.drawLabel(_context, DateTools.epochToIso8601Time(pointTimestamp), 11));
                marker.setTitle(DateTools.epochToIso8601(pointTimestamp) + " " + trackPoint.getSrcCallsign());
                marker.setSnippet(getStatus(trackPoint));
                marker.setPosition(point);
                _trackMarkers.add(marker);
                _mapView.getOverlays().add(marker);

                shouldSet = true;
            }
        }
        if (shouldSet)
            _activeTrackLine.setPoints(_activeTrackPoints);
    }

    private String getStatus(PositionItem position) {
        return String.format(Locale.US, "%s<br>%s %f %f<br>%03d° %03dkm/h %04dm<br>%s",
                position.getDigipath(),
                position.getMaidenHead(), position.getLatitude(), position.getLongitude(),
                (int)position.getBearingDegrees(),
                UnitTools.metersPerSecondToKilometersPerHour((int)position.getSpeedMetersPerSecond()),
                (int)position.getAltitudeMeters(),
                position.getComment());
    }
}
