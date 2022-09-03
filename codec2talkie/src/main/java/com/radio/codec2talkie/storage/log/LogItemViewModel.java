package com.radio.codec2talkie.storage.log;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.radio.codec2talkie.storage.station.Station;

import java.util.List;

public class LogItemViewModel extends AndroidViewModel {

    private final LogItemRepository _logItemRepository;
    private final LiveData<List<LogItem>> _logItemLiveData;
    private LiveData<List<LogItem>> _logItemGroupLiveData;
    private final LiveData<List<Station>> _lastPositions;

    public LogItemViewModel(@NonNull Application application) {
        super(application);
        _logItemRepository = new LogItemRepository(application);
        _logItemLiveData = _logItemRepository.getAllLogItems();
        _lastPositions = _logItemRepository.getLastPositions();
    }

    public LiveData<List<LogItem>> getAllData() {
        return _logItemLiveData;
    }

    public LiveData<List<LogItem>> getData(String groupName) {
        return _logItemRepository.getLogItems(groupName);
    }

    public LiveData<List<Station>> getLastPositions() { return _lastPositions; }

    public LiveData<List<Station>> getLastPositions(String callsign) { return _logItemRepository.getStationPositions(callsign); }

    public void deleteAllLogItems() { _logItemRepository.deleteAllLogItems(); }

    public void deleteLogItems(String groupName) {
        _logItemRepository.deleteLogItems(groupName);
    }

    public void deleteLogItemsOlderThanHours(int hours) { _logItemRepository.deleteLogItemsOlderThanHours(hours); }
}
