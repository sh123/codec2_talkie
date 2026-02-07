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

    @Query("SELECT groupId FROM MessageItem GROUP BY groupId")
    LiveData<List<String>> getGroups();

    @Query("SELECT * FROM MessageItem ORDER BY timestampEpoch ASC")
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

    @Query("SELECT * FROM MessageItem WHERE groupId = :groupId ORDER BY timestampEpoch ASC")
    LiveData<List<MessageItem>> getMessageItems(String groupId);

    @Transaction
    default void upsertMessageItem(MessageItem messageItem) {
        // handle same message retries
        String ackId = messageItem.getAckId();
        if (ackId != null) {
            MessageItem existingItem = getMessageItem(messageItem.getGroupId(), messageItem.getAckId());
            if (existingItem != null) {
                existingItem.setRetryCnt(existingItem.getRetryCnt() + 1);
                //existingItem.setTimestampEpoch(messageItem.getTimestampEpoch());
                updateMessageItem(existingItem);
                return;
            }
        }
        insertMessageItem(messageItem);
    }

    @Update
    void updateMessageItem(MessageItem messageItem);

    @Query("SELECT * FROM MessageItem WHERE groupId = :groupId AND ackId = :ackId LIMIT 1")
    MessageItem getMessageItem(String groupId, String ackId);

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
