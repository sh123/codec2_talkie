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

    @Query("SELECT dstCallsign FROM MessageItem GROUP BY dstCallsign")
    LiveData<List<String>> getGroups();

    @Query("SELECT * FROM MessageItem ORDER BY timestampEpoch ASC")
    LiveData<List<MessageItem>> getAllMessageItems();

    @Query("SELECT * FROM MessageItem WHERE dstCallsign = :dstCallsign ORDER BY timestampEpoch ASC")
    LiveData<List<MessageItem>> getMessageItems(String dstCallsign);

    @Query("DELETE FROM MessageItem WHERE dstCallsign = :dstCallsign")
    void deleteMessageItems(String dstCallsign);

    @Query("DELETE FROM MessageItem")
    void deleteAllMessageItems();
}
