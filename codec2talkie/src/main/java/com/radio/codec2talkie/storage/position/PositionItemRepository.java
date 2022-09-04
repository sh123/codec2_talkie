package com.radio.codec2talkie.storage.position;

import android.app.Application;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.radio.codec2talkie.storage.AppDatabase;


import java.util.List;

public class PositionItemRepository {
    private static final String TAG = PositionItemRepository.class.getSimpleName();

    private final PositionItemDao _positionItemDao;

    public PositionItemRepository(Application application) {
        AppDatabase appDatabase = AppDatabase.getDatabase(application);
        _positionItemDao = appDatabase.positionItemDao();
    }

    public void upsertPositionItem(PositionItem positionItem) {
        AppDatabase.getDatabaseExecutor().execute(() -> _positionItemDao.upsertPositionItem(positionItem));
    }

    public LiveData<List<PositionItem>> getPositionItems(String srcCallsign) {
        return Transformations.distinctUntilChanged(_positionItemDao.getPositionItems(srcCallsign));
    }

    public void deleteAllPositionItems() {
        AppDatabase.getDatabaseExecutor().execute(_positionItemDao::deleteAllPositionItems);
    }

    public void deletePositionItemsFromCallsign(String srcCallsign) {
        AppDatabase.getDatabaseExecutor().execute(() -> _positionItemDao.deletePositionItems(srcCallsign));
    }

    public void deletePositionItemsOlderThanTimestamp(long timestamp) {
        AppDatabase.getDatabaseExecutor().execute(() -> _positionItemDao.deletePositionItemsOlderThanTimestamp(timestamp));
    }
}
