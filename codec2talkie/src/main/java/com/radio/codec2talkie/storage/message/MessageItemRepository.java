package com.radio.codec2talkie.storage.message;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.radio.codec2talkie.protocol.message.TextMessage;
import com.radio.codec2talkie.storage.AppDatabase;

import java.util.List;

public class MessageItemRepository {

    private final static int BULLETIN_LIMIT = 16;

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
            _messages = TextMessage.isBulletin(groupName)
                ? _messageItemDao.getBulletinMessageItems(groupName, BULLETIN_LIMIT)
                : _messageItemDao.getMessageItems(groupName);
        return _messages;
    }

    public void ackMessageItem(MessageItem messageItem) {
        String src = messageItem.getSrcCallsign();
        String dst = messageItem.getDstCallsign();
        String ackId = messageItem.getAckId();
        AppDatabase.getDatabaseExecutor().execute(() -> _messageItemDao.ackMessageItem(dst, src, ackId));
    }

    public void upsertMessageItem(MessageItem messageItem) {
        AppDatabase.getDatabaseExecutor().execute(() -> _messageItemDao.upsertMessageItem(messageItem));
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
