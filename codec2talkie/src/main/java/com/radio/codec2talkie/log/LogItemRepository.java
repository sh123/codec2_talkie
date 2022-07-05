package com.radio.codec2talkie.log;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;

public class LogItemRepository {

    private final LogItemDao _logItemDao;
    private final LiveData<List<LogItem>> _logItemLiveData;

    public LogItemRepository(Application application) {
        LogItemDatabase logItemDatabase = LogItemDatabase.getDatabase(application);
        this._logItemDao = logItemDatabase.dataDAO();
        this._logItemLiveData = _logItemDao.getAllData();
    }

    LiveData<List<LogItem>> getAllData() {
        return _logItemLiveData;
    }

    public void insert(LogItem logItem) {
        _logItemDao.insertItem(logItem);
    }
}
