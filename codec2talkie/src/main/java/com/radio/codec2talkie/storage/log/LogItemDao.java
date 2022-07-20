package com.radio.codec2talkie.storage.log;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.radio.codec2talkie.storage.message.MessageItem;

import java.util.List;

@Dao
public interface LogItemDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertLogItem(LogItem logItem);

    @Query("SELECT srcCallsign from LogItem GROUP BY srcCallsign")
    LiveData<List<String>> getGroups();

    @Query("SELECT * FROM LogItem ORDER by timestampEpoch ASC")
    LiveData<List<LogItem>> getAllLogItems();

    @Query("SELECT * FROM LogItem WHERE srcCallsign = :srcCallsign ORDER BY timestampEpoch ASC")
    LiveData<List<LogItem>> getLogItems(String srcCallsign);

    @Query("DELETE FROM LogItem WHERE srcCallsign = :srcCallsign")
    void deleteLogItems(String srcCallsign);

    @Query("DELETE FROM LogItem")
    void deleteAllLogItems();
}
