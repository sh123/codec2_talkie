package com.radio.codec2talkie.storage.log;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.radio.codec2talkie.storage.log.group.LogItemGroup;

import java.util.List;

@Dao
public interface LogItemDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertLogItem(LogItem logItem);

    @Query("SELECT srcCallsign FROM LogItem GROUP BY srcCallsign")
    LiveData<List<LogItemGroup>> getGroups();

    @Query("SELECT * FROM LogItem ORDER by timestampEpoch DESC")
    LiveData<List<LogItem>> getAllLogItems();

    @Query("SELECT * FROM LogItem WHERE srcCallsign = :srcCallsign ORDER BY timestampEpoch DESC")
    LiveData<List<LogItem>> getLogItems(String srcCallsign);

    @Query("DELETE FROM LogItem WHERE srcCallsign = :srcCallsign")
    void deleteLogItems(String srcCallsign);

    @Query("DELETE FROM LogItem")
    void deleteAllLogItems();
}
