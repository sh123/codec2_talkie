package com.radio.codec2talkie.storage.position;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.radio.codec2talkie.storage.message.MessageItem;

import java.util.List;

@Dao
public interface PositionItemDao {

    @Insert
    void insertPositionItem(PositionItem logItem);

    @Update
    void updatePositionItem(PositionItem logItem);

    @Query("SELECT * FROM PositionItem WHERE srcCallsign = :srcCallsign ORDER BY timestampEpoch DESC LIMIT 1")
    PositionItem getLastPositionItem(String srcCallsign);

    @Query("SELECT srcCallsign from PositionItem GROUP BY srcCallsign")
    LiveData<List<String>> getStationNames();

    @Query("SELECT * FROM PositionItem ORDER by timestampEpoch DESC")
    LiveData<List<PositionItem>> getAllPositionItems();

    @Query("SELECT * FROM PositionItem WHERE srcCallsign = :srcCallsign ORDER BY timestampEpoch ASC")
    LiveData<List<PositionItem>> getPositionItems(String srcCallsign);

    @Query("DELETE FROM PositionItem WHERE srcCallsign = :srcCallsign")
    void deletePositionItems(String srcCallsign);

    @Query("DELETE FROM PositionItem WHERE timestampEpoch < :timestamp")
    void deletePositionItemsOlderThanTimestamp(long timestamp);

    @Query("DELETE FROM PositionItem")
    void deleteAllPositionItems();
}
