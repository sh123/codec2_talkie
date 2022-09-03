package com.radio.codec2talkie.storage.log;

import android.app.Application;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.radio.codec2talkie.storage.AppDatabase;
import com.radio.codec2talkie.storage.log.group.LogItemGroup;
import com.radio.codec2talkie.tools.DateTools;

import java.util.List;

public class LogItemRepository {

    private final LogItemDao _logItemDao;
    private final LiveData<List<LogItem>> _logItemLiveData;
    private LiveData<List<LogItem>> _logItemGroupLiveData;
    private final LiveData<List<LogItemGroup>> _lastPositions;

    public LogItemRepository(Application application) {
        AppDatabase appDatabase = AppDatabase.getDatabase(application);
        _logItemDao = appDatabase.logItemDao();
        _logItemLiveData = _logItemDao.getAllLogItems();
        _lastPositions = _logItemDao.getLastPositions();
    }

    public LiveData<List<LogItem>> getAllLogItems() {
        return _logItemLiveData;
    }

    public LiveData<List<LogItemGroup>> getLastPositions() { return _lastPositions; }

    public LiveData<List<LogItemGroup>> getStationPositions(String srcCallsign) {
        return _logItemDao.getStationPositions(srcCallsign);
    }

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

    public void deleteLogItemsOlderThanHours(int hours) {
        AppDatabase.getDatabaseExecutor().execute(() -> _logItemDao.deleteLogItemsOlderThanTimestamp(DateTools.currentTimestampMinusHours(hours)));
    }
}
