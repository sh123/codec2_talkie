package com.radio.codec2talkie.storage.log;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.radio.codec2talkie.storage.AppDatabase;

import java.util.List;

public class LogItemRepository {

    private final LogItemDao _logItemDao;
    private final LiveData<List<LogItem>> _logItemLiveData;
    private LiveData<List<LogItem>> _logItemGroupLiveData;
    private final LiveData<List<String>> _logItemGroups;

    public LogItemRepository(Application application) {
        AppDatabase appDatabase = AppDatabase.getDatabase(application);
        _logItemDao = appDatabase.logItemDao();
        _logItemLiveData = _logItemDao.getAllLogItems();
        _logItemGroups = _logItemDao.getGroups();
    }

    public LiveData<List<LogItem>> getAllLogItems() {
        return _logItemLiveData;
    }

    public LiveData<List<String>> getGroups() { return _logItemGroups; }

    public LiveData<List<LogItem>> getLogItems(String groupName) {
        return _logItemDao.getLogItems(groupName);
    }

    public void insertLogItem(LogItem logItem) {
        AppDatabase.getDatabaseExecutor().execute(() -> _logItemDao.insertLogItem(logItem));
    }

    public void deleteAllLogItems() {
        AppDatabase.getDatabaseExecutor().execute(_logItemDao::deleteAllLogItems);
    }

    public void deleteLogItems(String groupName) {
        AppDatabase.getDatabaseExecutor().execute(() -> _logItemDao.deleteLogItems(groupName));
    }
}
