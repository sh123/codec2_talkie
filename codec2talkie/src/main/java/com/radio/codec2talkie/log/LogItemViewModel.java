package com.radio.codec2talkie.log;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

public class LogItemViewModel extends AndroidViewModel {

    private final LogItemRepository _logItemRepository;
    private final LiveData<List<LogItem>> _logItemLiveData;

    public LogItemViewModel(@NonNull Application application) {
        super(application);
        _logItemRepository = new LogItemRepository(application);
        _logItemLiveData = _logItemRepository.getAllLogItems();
    }

    public LiveData<List<LogItem>> getAllData() {
        return _logItemLiveData;
    }

    public void deleteAllLogItems() { _logItemRepository.deleteAllLogItems(); }
}
