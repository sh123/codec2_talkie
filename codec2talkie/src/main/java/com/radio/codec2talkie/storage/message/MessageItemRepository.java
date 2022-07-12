package com.radio.codec2talkie.storage.message;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.radio.codec2talkie.storage.AppDatabase;

import java.util.List;

public class MessageItemRepository {

    private final MessageItemDao _messageItemDao;
    private final LiveData<List<MessageItem>> _messageItemLiveData;

    public MessageItemRepository(Application application) {
        AppDatabase appDatabase = AppDatabase.getDatabase(application);
        _messageItemDao = appDatabase.messageItemDao();
        _messageItemLiveData = _messageItemDao.getAllMessageItems();
    }

    LiveData<List<MessageItem>> getAllMessageItems() {
        return _messageItemLiveData;
    }

    public void insertMessageItem(MessageItem messageItem) {
        AppDatabase.getDatabaseExecutor().execute(() -> {
            _messageItemDao.insertMessageItem(messageItem);
        });
    }

    public void deleteAllMessageItems() {
        AppDatabase.getDatabaseExecutor().execute(_messageItemDao::deleteAllMessageItems);
    }
}
