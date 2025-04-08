package com.radio.codec2talkie.storage.message;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.radio.codec2talkie.storage.AppDatabase;

import java.util.List;

public class MessageItemRepository {

    private final MessageItemDao _messageItemDao;
    private LiveData<List<MessageItem>> _messages;
    private final LiveData<List<String>> _messageGroups;

    public MessageItemRepository(Application application) {
        AppDatabase appDatabase = AppDatabase.getDatabase(application);
        _messageItemDao = appDatabase.messageItemDao();
        _messageGroups = _messageItemDao.getGroups();
    }

    public LiveData<List<String>> getGroups() {
        return _messageGroups;
    }

    public LiveData<List<MessageItem>> getMessages(String groupName) {
        if (_messages == null)
            _messages = _messageItemDao.getMessageItems(groupName);
        return _messages;
    }

    public void insertMessageItem(MessageItem messageItem) {
        AppDatabase.getDatabaseExecutor().execute(() -> _messageItemDao.insertMessageItem(messageItem));
    }

    public void deleteAllMessageItems() {
        AppDatabase.getDatabaseExecutor().execute(_messageItemDao::deleteAllMessageItems);
    }

    public void deleteGroup(String groupName) {
        AppDatabase.getDatabaseExecutor().execute(() -> _messageItemDao.deleteMessageItems(groupName));
    }
}
