package com.radio.codec2talkie.storage.station;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Update;

@Dao
public interface StationItemDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertStation(StationItem stationItem);

    @Update(onConflict = OnConflictStrategy.IGNORE)
    void updateStation(StationItem stationItem);
}