package com.radio.codec2talkie.storage.log;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.radio.codec2talkie.storage.AppDatabase;
import com.radio.codec2talkie.tools.DateTools;

import java.util.List;

public class LogItemRepository {

    private final LogItemDao _logItemDao;
    private final LiveData<List<LogItem>> _logItemLiveData;

    public LogItemRepository(Application application) {
        AppDatabase appDatabase = AppDatabase.getDatabase(application);
        _logItemDao = appDatabase.logItemDao();
        _logItemLiveData = _logItemDao.getAllLogItems();
    }

    public LiveData<List<LogItem>> getAllLogItems() {
        return _logItemLiveData;
    }

    public LiveData<List<LogItem>> getLogItems(String groupName) {
        return _logItemDao.getLogItems(groupName);
    }

    public void insertLogItem(LogItem logItem) {
        AppDatabase.getDatabaseExecutor().execute(() -> _logItemDao.insertLogItem(logItem));
    }

    public void deleteLogItems(String srcCallsign, int hours) {
        AppDatabase.getDatabaseExecutor().execute(() -> {
            if (srcCallsign == null && hours == -1)
                _logItemDao.deleteAllLogItems();
            else if (srcCallsign == null)
                _logItemDao.deleteLogItemsOlderThanTimestamp(DateTools.currentTimestampMinusHours(hours));
            else if (hours == -1)
                _logItemDao.deleteLogItemsFromCallsign(srcCallsign);
            else
                _logItemDao.deleteLogItems(srcCallsign, DateTools.currentTimestampMinusHours(hours));
        });
    }
}
