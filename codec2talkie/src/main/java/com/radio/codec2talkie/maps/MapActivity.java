package com.radio.codec2talkie.maps;

import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.radio.codec2talkie.R;
import com.radio.codec2talkie.storage.log.LogItemViewModel;
import com.radio.codec2talkie.storage.log.group.LogItemGroup;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

public class MapActivity extends AppCompatActivity {
    private static final String TAG = MapActivity.class.getSimpleName();

    private MapView _map;
    private IMapController _mapController;
    private CompassOverlay _compassOverlay;
    private MyLocationNewOverlay _myLocationNewOverlay;

    private LogItemViewModel _logItemViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.menu_aprs_map);
        setContentView(R.layout.activity_map_view);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        Context context = getApplicationContext();
        Configuration.getInstance().setUserAgentValue("C2T");

        _map = findViewById(R.id.map);
        _map.setTileSource(TileSourceFactory.MAPNIK);
        _map.setMultiTouchControls(true);

        _mapController = _map.getController();
        _mapController.zoomTo(5.0);

        // compass
        _compassOverlay = new CompassOverlay(context, new InternalCompassOrientationProvider(context), _map);
        _compassOverlay.enableCompass();
        _map.getOverlays().add(_compassOverlay);

        // my location
        _myLocationNewOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(context), _map);
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

            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
