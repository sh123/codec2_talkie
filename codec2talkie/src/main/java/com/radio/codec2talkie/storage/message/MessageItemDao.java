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

    @Query("SELECT * FROM MessageItem ORDER by timestampEpoch ASC")
    LiveData<List<MessageItem>> getAllMessageItems();

    @Query("DELETE FROM MessageItem")
    void deleteAllMessageItems();
}
