package com.radio.codec2talkie.storage.station;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.radio.codec2talkie.maps.MapActivity;
import com.radio.codec2talkie.storage.AppDatabase;
import com.radio.codec2talkie.tools.DateTools;

import java.util.List;

public class StationItemRepository {
    private static final String TAG = StationItemRepository.class.getSimpleName();

    private final StationItemDao _stationItemDao;

    public StationItemRepository(Application application) {
        AppDatabase appDatabase = AppDatabase.getDatabase(application);
        _stationItemDao = appDatabase.stationitemDao();
    }

    public void insertStationItem(StationItem stationItem) {
        AppDatabase.getDatabaseExecutor().execute(() -> _stationItemDao.insertStationItem(stationItem));
    }

    public void upsertStationItem(StationItem stationItem) {
        AppDatabase.getDatabaseExecutor().execute(() -> {
        });
    }
}
