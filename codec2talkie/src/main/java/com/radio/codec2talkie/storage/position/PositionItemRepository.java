package com.radio.codec2talkie.storage.position;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.radio.codec2talkie.storage.AppDatabase;
import com.radio.codec2talkie.tools.DateTools;

import java.util.List;

public class PositionItemRepository {

    private final PositionItemDao _positionItemDao;

    public PositionItemRepository(Application application) {
        AppDatabase appDatabase = AppDatabase.getDatabase(application);
        _positionItemDao = appDatabase.positionItemDao();
    }

    public void insertPositionItem(PositionItem positionItem) {
        AppDatabase.getDatabaseExecutor().execute(() -> _positionItemDao.insertPositionItem(positionItem));
    }

    public void deleteAllPositionItems() {
        AppDatabase.getDatabaseExecutor().execute(_positionItemDao::deleteAllPositionItems);
    }

    public void deletePositionItems(String srcCallsign) {
        AppDatabase.getDatabaseExecutor().execute(() -> _positionItemDao.deletePositionItems(srcCallsign));
    }

    public void deletePositionItemsOlderThanTimestamp(long timestamp) {
        AppDatabase.getDatabaseExecutor().execute(() -> _positionItemDao.deletePositionItemsOlderThanTimestamp(timestamp));
    }
}
