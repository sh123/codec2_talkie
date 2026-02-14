package com.radio.codec2talkie.storage.message;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MessageItemDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertMessageItem(MessageItem messageItem);

    @Query("SELECT DISTINCT groupId FROM MessageItem " +
            "ORDER BY groupId ASC")
    LiveData<List<String>> getGroups();

    @Query("SELECT DISTINCT groupId FROM MessageItem " +
           "WHERE groupId LIKE :query " +
           "ORDER BY groupId ASC")
    LiveData<List<String>> getFilteredGroups(String query);

    @Query("SELECT * FROM MessageItem " +
           "ORDER BY timestampEpoch ASC")
    LiveData<List<MessageItem>> getAllMessageItems();

    @Query("SELECT * FROM MessageItem " +
           "WHERE groupId = :groupId " +
           "AND timestampEpoch IN (" +
           "SELECT MAX(timestampEpoch) FROM MessageItem " +
           "WHERE groupId = :groupId " +
           "GROUP BY message, srcCallsign) " +
           "ORDER BY timestampEpoch ASC " +
           "LIMIT :limit")
    LiveData<List<MessageItem>> getBulletinMessageItems(String groupId, int limit);

    @Query("SELECT * FROM MessageItem " +
           "WHERE groupId = :groupId " +
           "ORDER BY timestampEpoch ASC")
    LiveData<List<MessageItem>> getMessageItems(String groupId);

    @Transaction
    default void upsertMessageItem(MessageItem messageItem) {
        String ackId = messageItem.getAckId();
        if (ackId != null) {
            MessageItem existingItem = getMessageItem(messageItem.getGroupId(),
                    messageItem.getSrcCallsign(), messageItem.getDstCallsign(), messageItem.getAckId());
            if (existingItem != null) {
                existingItem.setRetryCnt(existingItem.getRetryCnt() + 1);
                updateMessageItem(existingItem);
                return;
            }
        }
        insertMessageItem(messageItem);
    }

    @Update
    void updateMessageItem(MessageItem messageItem);

    @Query("SELECT * FROM MessageItem " +
            "WHERE groupId = :groupId " +
            "AND srcCallsign = :srcCallsign " +
            "AND dstCallsign = :dstCallsign " +
            "AND ackId = :ackId LIMIT 1")
    MessageItem getMessageItem(String groupId, String srcCallsign, String dstCallsign, String ackId);

    @Query("DELETE FROM MessageItem WHERE groupId = :groupId")
    void deleteMessageItems(String groupId);

    @Query("UPDATE MessageItem SET isAcknowledged = 1, needsRetry = 0 " +
           "WHERE srcCallsign = :srcCallsign " +
           "AND dstCallsign = :dstCallsign " +
           "AND ackId = :ackId")
    void ackMessageItem(String srcCallsign, String dstCallsign, String ackId);

    @Query("DELETE FROM MessageItem")
    void deleteAllMessageItems();

    @Query("DELETE FROM MessageItem WHERE timestampEpoch < :timestampEpoch")
    void deleteLogItemsOlderThanTimestamp(long timestampEpoch);
}
