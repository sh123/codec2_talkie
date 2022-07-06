package com.radio.codec2talkie.log;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;

public class LogItemRepository {

    private final LogItemDao _logItemDao;
    private final LiveData<List<LogItem>> _logItemLiveData;

    public LogItemRepository(Application application) {
        LogItemDatabase logItemDatabase = LogItemDatabase.getDatabase(application);
        _logItemDao = logItemDatabase.logItemDao();
        _logItemLiveData = _logItemDao.getAllLogItems();
    }

    LiveData<List<LogItem>> getAllLogItems() {
        return _logItemLiveData;
    }

    public void insertLogItem(LogItem logItem) {
        LogItemDatabase.databaseWriteExecutor.execute(() -> {
            _logItemDao.insertLogItem(logItem);
        });
    }

    public void deleteAllLogItems() {
        LogItemDatabase.databaseWriteExecutor.execute(_logItemDao::deleteAllLogItems);
    }
}
