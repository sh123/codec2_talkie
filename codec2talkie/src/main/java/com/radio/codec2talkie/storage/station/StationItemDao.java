package com.radio.codec2talkie.storage.station;

import android.database.sqlite.SQLiteConstraintException;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

@Dao
public abstract class StationItemDao {

    @Insert
    public abstract void insertStationItem(StationItem stationItem);

    @Update
    public abstract void updateStationItem(StationItem stationItem);

    @Transaction
    public void upsertStationItem(StationItem stationItem) {
        StationItem oldStationItem  = getStationItem(stationItem.getSrcCallsign());
        if (oldStationItem == null) {
            try {
                insertStationItem(stationItem);
            } catch (SQLiteConstraintException ex) {
                oldStationItem  = getStationItem(stationItem.getSrcCallsign());
            }
        }
        if (oldStationItem != null) {
            oldStationItem.updateFrom(stationItem);
            updateStationItem(oldStationItem);
        }
    }

    @Query("SELECT * FROM StationItem WHERE srcCallsign = :srcCallsign")
    public abstract StationItem getStationItem(String srcCallsign);

    @Query("SELECT * FROM StationItem ORDER BY srcCallsign ASC")
    public abstract LiveData<List<StationItem>> getAllStationItems();

    @Query("SELECT *, (SELECT count(*) FROM PositionItem pos WHERE st.srcCallsign = pos.srcCallsign) AS positionCount " +
            "FROM StationItem st " +
            "WHERE positionCount > :minCount")
    public abstract LiveData<List<StationItem>> getMovingStationItems(int minCount);

    @Query("DELETE FROM StationItem WHERE srcCallsign = :srcCallsign")
    public abstract void deleteStationItemsFromCallsign(String srcCallsign);

    @Query("DELETE FROM StationItem WHERE timestampEpoch < :timestamp")
    public abstract void deleteStationItemsOlderThanTimestamp(long timestamp);

    @Query("DELETE FROM StationItem WHERE timestampEpoch < :timestamp AND srcCallsign = :srcCallsign")
    public abstract void deleteStationItems(String srcCallsign, long timestamp);

    @Query("DELETE FROM StationItem")
    public abstract void deleteAllStationItems();
}