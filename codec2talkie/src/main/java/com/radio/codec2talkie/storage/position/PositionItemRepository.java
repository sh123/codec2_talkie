package com.radio.codec2talkie.storage.position;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.radio.codec2talkie.maps.MapActivity;
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

    public void insertPositionItem(PositionItem positionItem) {
        AppDatabase.getDatabaseExecutor().execute(() -> _positionItemDao.insertPositionItem(positionItem));
    }

    public void upsertPositionItem(PositionItem positionItem) {
        AppDatabase.getDatabaseExecutor().execute(() -> {
            PositionItem oldPosition = _positionItemDao.getLastPositionItem(positionItem.getSrcCallsign());
            if (oldPosition != null && PositionItem.equalTo(positionItem, oldPosition)) {
                // update id and coordinates from existing position
                positionItem.setId(oldPosition.getId());
                positionItem.setLatitude(oldPosition.getLatitude());
                positionItem.setLongitude(oldPosition.getLongitude());
                Log.i(TAG, "UPDATE " + positionItem.getSrcCallsign());
                _positionItemDao.updatePositionItem(positionItem);
            } else {
                Log.i(TAG, "INSERT " + positionItem.getSrcCallsign());
                _positionItemDao.insertPositionItem(positionItem);
            }
        });
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
