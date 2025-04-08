package com.radio.codec2talkie.storage.position;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

@Dao
public abstract class PositionItemDao {

    @Insert
    public abstract void insertPositionItem(PositionItem positionItem);

    @Update
    public abstract void updatePositionItem(PositionItem positionItem);

    @Transaction
    public void upsertPositionItem(PositionItem positionItem) {
        PositionItem oldPosition = getLastPositionItem(positionItem.getSrcCallsign());
        if (oldPosition != null && oldPosition.equals(positionItem)) {
            // update id and coordinates from existing position
            positionItem.setId(oldPosition.getId());
            positionItem.setLatitude(oldPosition.getLatitude());
            positionItem.setLongitude(oldPosition.getLongitude());
            //Log.i(TAG, "UPDATE " + positionItem.getSrcCallsign());
            updatePositionItem(positionItem);
        } else {
            //Log.i(TAG, "INSERT " + positionItem.getSrcCallsign());
            insertPositionItem(positionItem);
        }
    }

    @Query("SELECT * FROM PositionItem WHERE srcCallsign = :srcCallsign ORDER BY timestampEpoch DESC LIMIT 1")
    public abstract PositionItem getLastPositionItem(String srcCallsign);

    @Query("SELECT srcCallsign from PositionItem GROUP BY srcCallsign")
    public abstract LiveData<List<String>> getStationNames();

    @Query("SELECT * FROM PositionItem ORDER by timestampEpoch DESC")
    public abstract LiveData<List<PositionItem>> getAllPositionItems();

    @Query("SELECT * FROM PositionItem WHERE srcCallsign = :srcCallsign ORDER BY timestampEpoch ASC")
    public abstract LiveData<List<PositionItem>> getPositionItems(String srcCallsign);

    @Query("DELETE FROM PositionItem WHERE srcCallsign = :srcCallsign")
    public abstract void deletePositionItemsFromCallsign(String srcCallsign);

    @Query("DELETE FROM PositionItem WHERE timestampEpoch < :timestamp")
    public abstract void deletePositionItemsOlderThanTimestamp(long timestamp);

    @Query("DELETE FROM PositionItem WHERE timestampEpoch < :timestamp AND srcCallsign = :srcCallsign")
    public abstract void deletePositionItems(String srcCallsign, long timestamp);

    @Query("DELETE FROM PositionItem")
    public abstract void deleteAllPositionItems();
}
