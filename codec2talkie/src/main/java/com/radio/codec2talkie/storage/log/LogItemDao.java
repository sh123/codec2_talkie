package com.radio.codec2talkie.storage.log;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface LogItemDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertLogItem(LogItem logItem);

    @Query("SELECT * FROM LogItem ORDER by timestampEpoch DESC")
    LiveData<List<LogItem>> getAllLogItems();

    @Query("DELETE FROM LogItem")
    void deleteAllLogItems();
}
