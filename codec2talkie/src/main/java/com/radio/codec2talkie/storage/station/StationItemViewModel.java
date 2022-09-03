package com.radio.codec2talkie.storage.station;

import android.app.Application;
import android.text.format.DateUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.radio.codec2talkie.storage.AppDatabase;
import com.radio.codec2talkie.storage.station.StationItemRepository;
import com.radio.codec2talkie.tools.DateTools;

import java.util.List;

public class StationItemViewModel extends AndroidViewModel {

    private final StationItemRepository _stationItemRepository;

    public StationItemViewModel(@NonNull Application application) {
        super(application);
        _stationItemRepository = new StationItemRepository(application);
    }

    public LiveData<List<StationItem>> getAllStationItems() { return _stationItemRepository.getAllStationItems(); }

    public void deleteAllStationItems() {
        _stationItemRepository.deleteAllStationItems();
    }

    public void deleteAllStationItemsOlderThanHours(int hours) {
        _stationItemRepository.deleteAllStationItemsOlderThanHours(hours);
    }

    public void deleteStationItem(String srcCallsign) {
        _stationItemRepository.deleteStationItemsFromCallsign(srcCallsign);
    }
}
