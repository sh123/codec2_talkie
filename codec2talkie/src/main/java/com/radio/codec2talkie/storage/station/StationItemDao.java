package com.radio.codec2talkie.storage.station;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Update;

import com.radio.codec2talkie.storage.position.PositionItem;

@Dao
public interface StationItemDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertStation(StationItem logItem);

    @Update(onConflict = OnConflictStrategy.IGNORE)
    void updateStation(PositionItem logItem);
}