package com.radio.codec2talkie.storage.log;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.radio.codec2talkie.storage.AppDatabase;

import java.util.List;

public class LogItemRepository {

    private final LogItemDao _logItemDao;
    private final LiveData<List<LogItem>> _logItemLiveData;

    public LogItemRepository(Application application) {
        AppDatabase appDatabase = AppDatabase.getDatabase(application);
        _logItemDao = appDatabase.logItemDao();
        _logItemLiveData = _logItemDao.getAllLogItems();
    }

    LiveData<List<LogItem>> getAllLogItems() {
        return _logItemLiveData;
    }

    public void insertLogItem(LogItem logItem) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            _logItemDao.insertLogItem(logItem);
        });
    }

    public void deleteAllLogItems() {
        AppDatabase.databaseWriteExecutor.execute(_logItemDao::deleteAllLogItems);
    }
}
