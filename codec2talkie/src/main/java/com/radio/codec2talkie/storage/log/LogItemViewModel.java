package com.radio.codec2talkie.storage.log;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.radio.codec2talkie.storage.log.group.LogItemGroup;

import java.util.List;

public class LogItemViewModel extends AndroidViewModel {

    private final LogItemRepository _logItemRepository;
    private final LiveData<List<LogItem>> _logItemLiveData;
    private LiveData<List<LogItem>> _logItemGroupLiveData;
    private final LiveData<List<LogItemGroup>> _logItemGroups;

    public LogItemViewModel(@NonNull Application application) {
        super(application);
        _logItemRepository = new LogItemRepository(application);
        _logItemLiveData = _logItemRepository.getAllLogItems();
        _logItemGroups = _logItemRepository.getGroups();
    }

    public LiveData<List<LogItem>> getAllData() {
        return _logItemLiveData;
    }

    public LiveData<List<LogItem>> getData(String groupName) {
        return _logItemRepository.getLogItems(groupName);
    }

    public LiveData<List<LogItemGroup>> getGroups() { return _logItemGroups; }

    public void deleteAllLogItems() { _logItemRepository.deleteAllLogItems(); }

    public void deleteLogItems(String groupName) {
        _logItemRepository.deleteLogItems(groupName);
    }
}
