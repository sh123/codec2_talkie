package com.radio.codec2talkie.storage.message;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MessageItemDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertMessageItem(MessageItem messageItem);

    @Query("SELECT groupId FROM MessageItem GROUP BY groupId")
    LiveData<List<String>> getGroups();

    @Query("SELECT * FROM MessageItem ORDER BY timestampEpoch ASC")
    LiveData<List<MessageItem>> getAllMessageItems();

    @Query("SELECT * FROM MessageItem WHERE groupId = :groupId ORDER BY timestampEpoch ASC")
    LiveData<List<MessageItem>> getMessageItems(String groupId);

    @Query("DELETE FROM MessageItem WHERE groupId = :groupId")
    void deleteMessageItems(String groupId);

    @Query("UPDATE MessageItem SET isAcknowledged = 1 " +
           "WHERE srcCallsign = :srcCallsign " +
           "AND dstCallsign = :dstCallsign " +
           "AND ackId = :ackId")
    void ackMessageItem(String srcCallsign, String dstCallsign, String ackId);

    @Query("DELETE FROM MessageItem")
    void deleteAllMessageItems();
}
