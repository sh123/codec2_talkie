package com.radio.codec2talkie.storage.station;

import android.app.Application;
import android.database.sqlite.SQLiteConstraintException;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.radio.codec2talkie.storage.AppDatabase;
import com.radio.codec2talkie.tools.DateTools;

import java.util.List;

public class StationItemRepository {
    private static final String TAG = StationItemRepository.class.getSimpleName();

    private final StationItemDao _stationItemDao;
    private final LiveData<List<StationItem>> _stationItems;

    public StationItemRepository(Application application) {
        AppDatabase appDatabase = AppDatabase.getDatabase(application);
        _stationItemDao = appDatabase.stationitemDao();
        _stationItems = Transformations.distinctUntilChanged(_stationItemDao.getAllStationItems());
    }

    public LiveData<List<StationItem>> getAllStationItems() { return _stationItems; }

    public void upsertStationItem(StationItem stationItem) {
        AppDatabase.getDatabaseExecutor().execute(() -> {
            _stationItemDao.upsertStationItem(stationItem);
        });
    }

    public void deleteStationItems(String srcCallsign, int hours) {
        AppDatabase.getDatabaseExecutor().execute(() -> {
            AppDatabase.getDatabaseExecutor().execute(() -> {
                if (srcCallsign == null && hours == -1)
                    _stationItemDao.deleteAllStationItems();
                else if (srcCallsign == null)
                    _stationItemDao.deleteStationItemsOlderThanTimestamp(DateTools.currentTimestampMinusHours(hours));
                else if (hours == -1)
                    _stationItemDao.deleteStationItemsFromCallsign(srcCallsign);
                else
                    _stationItemDao.deleteStationItems(srcCallsign, DateTools.currentTimestampMinusHours(hours));
            });
        });
    }
}
