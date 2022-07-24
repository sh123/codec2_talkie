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

    @Query("SELECT pos.timestampEpoch AS timestampEpoch, " +
            "pos.srcCallsign AS srcCallsign, " +
            "pos.dstCallsign AS dstCallsign, " +
            "pos.latitude AS latitude, " +
            "pos.longitude AS longitude, " +
            "pos.maidenHead AS maidenHead, " +
            "pos.altitudeMeters AS altitudeMeters, " +
            "pos.bearingDegrees AS bearingDegrees, " +
            "pos.speedMetersPerSecond AS speedMetersPerSecond, " +
            "pos.status AS status, " +
            "pos.comment AS comment, " +
            "pos.symbolCode AS symbolCode, " +
            "pos.privacyLevel AS privacyLevel, " +
            "MAX(pos.timestampEpoch)" +
            "FROM LogItem log " +
            "LEFT OUTER JOIN PositionItem pos ON (log.srcCallsign = pos.srcCallsign)" +
            "GROUP BY log.srcCallsign")
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
