package com.radio.codec2talkie.maps;

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

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class MapTrack {
    private static final String TAG = MapTrack.class.getSimpleName();

    private final PositionItemViewModel _positionItemViewModel;
    private final ViewModelStoreOwner _owner;
    private final MapView _mapView;

    private LiveData<List<PositionItem>> _activeTrackLiveData;
    private final HashSet<Long> _activeTrackTimestamps = new HashSet<>();
    private final List<GeoPoint> _activeTrackPoints = new ArrayList<>();
    private final Polyline _activeTrackLine = new Polyline();

    public MapTrack(MapView mapView, ViewModelStoreOwner owner) {
        _owner = owner;
        _mapView = mapView;
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
        GeoPoint markerPoint = marker.getPosition();
        if (_activeTrackLiveData != null)
            _activeTrackLiveData.removeObservers((LifecycleOwner) _owner);
        _mapView.getOverlays().remove(_activeTrackLine);
        _activeTrackPoints.clear();
        _activeTrackTimestamps.clear();
        _activeTrackLine.setPoints(_activeTrackPoints);
        _mapView.getOverlays().add(_activeTrackLine);
        // FIXME, room livedata sends all list if one item changed event with distinctUntilChanged
        _activeTrackLiveData = _positionItemViewModel.getPositionItems(marker.getId());
        _activeTrackLiveData.observe((LifecycleOwner) _owner, this::addTrack);
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
}
