package com.radio.codec2talkie.storage.log;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.radio.codec2talkie.storage.station.StationItem;

import java.util.List;

@Dao
public interface LogItemDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertLogItem(LogItem logItem);

    @Query("SELECT pos.timestampEpoch AS timestampEpoch, " +
            "pos.id AS id, " +
            "log.srcCallsign AS srcCallsign, " +
            "log.logLine AS logLine," +
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
            "pos.rangeMiles AS rangeMiles, " +
            "pos.directivityDeg AS directivityDeg, " +
            "MAX(pos.timestampEpoch)" +
            "FROM LogItem log " +
            "LEFT OUTER JOIN PositionItem pos ON (log.srcCallsign = pos.srcCallsign) " +
            "GROUP BY log.srcCallsign " +
            "UNION " +
            "SELECT pos.timestampEpoch AS timestampEpoch, " +
            "pos.id AS id, " +
            "pos.srcCallsign AS srcCallsign, " +
            "log.logLine AS logLine," +
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
            "pos.rangeMiles AS rangeMiles, " +
            "pos.directivityDeg AS directivityDeg, " +
            "MAX(pos.timestampEpoch)" +
            "FROM PositionItem pos " +
            "LEFT OUTER JOIN LogItem log ON (log.srcCallsign = pos.srcCallsign) " +
            "GROUP BY pos.srcCallsign " +
            "ORDER BY srcCallsign ASC")
    LiveData<List<StationItem>> getLastPositions();

    @Query("SELECT pos.timestampEpoch AS timestampEpoch, " +
            "pos.id AS id, " +
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
            "pos.rangeMiles AS rangeMiles, " +
            "pos.directivityDeg AS directivityDeg " +
            "FROM PositionItem AS pos WHERE pos.srcCallsign = :srcCallsign " +
            "ORDER BY pos.timestampEpoch DESC")
    LiveData<List<StationItem>> getStationPositions(String srcCallsign);

    @Query("SELECT * FROM LogItem ORDER by timestampEpoch ASC")
    LiveData<List<LogItem>> getAllLogItems();

    @Query("SELECT * FROM LogItem WHERE srcCallsign = :srcCallsign ORDER BY timestampEpoch ASC")
    LiveData<List<LogItem>> getLogItems(String srcCallsign);

    @Query("DELETE FROM LogItem WHERE srcCallsign = :srcCallsign")
    void deleteLogItems(String srcCallsign);

    @Query("DELETE FROM LogItem")
    void deleteAllLogItems();

    @Query("DELETE FROM LogItem WHERE timestampEpoch < :timestampEpoch")
    void deleteLogItemsOlderThanTimestamp(long timestampEpoch);
}
