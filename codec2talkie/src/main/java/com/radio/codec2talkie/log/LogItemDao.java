package com.radio.codec2talkie.log;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface LogItemDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertItem(LogItem logItem);

    @Query("SELECT * FROM LogItem")
    LiveData<List<LogItem>> getAllData();

    @Query("DELETE FROM LogItem")
    void deleteAll();
}
