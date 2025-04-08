package com.radio.codec2talkie.storage.position;

import android.app.Application;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.radio.codec2talkie.storage.AppDatabase;
import com.radio.codec2talkie.tools.DateTools;


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

    public void deletePositionItems(String srcCallsign, int hours) {
        AppDatabase.getDatabaseExecutor().execute(() -> {
            AppDatabase.getDatabaseExecutor().execute(() -> {
                if (srcCallsign == null && hours == -1)
                    _positionItemDao.deleteAllPositionItems();
                else if (srcCallsign == null)
                    _positionItemDao.deletePositionItemsOlderThanTimestamp(DateTools.currentTimestampMinusHours(hours));
                else if (hours == -1)
                    _positionItemDao.deletePositionItemsFromCallsign(srcCallsign);
                else
                    _positionItemDao.deletePositionItems(srcCallsign, DateTools.currentTimestampMinusHours(hours));
            });
        });
    }
}
