package com.radio.codec2talkie.storage.position;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.radio.codec2talkie.storage.message.MessageItem;

import java.util.List;

@Dao
public interface PositionItemDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertPositionItem(PositionItem logItem);

    @Query("SELECT srcCallsign from PositionItem GROUP BY srcCallsign")
    LiveData<List<String>> getGroups();

    @Query("SELECT * FROM PositionItem ORDER by timestampEpoch DESC")
    LiveData<List<PositionItem>> getAllPositionItems();

    @Query("SELECT * FROM PositionItem WHERE srcCallsign = :srcCallsign ORDER BY timestampEpoch DESC")
    LiveData<List<PositionItem>> getPositionItems(String srcCallsign);

    @Query("DELETE FROM PositionItem WHERE srcCallsign = :srcCallsign")
    void deletePositionItems(String srcCallsign);

    @Query("DELETE FROM PositionItem")
    void deleteAllPositionItems();
}
