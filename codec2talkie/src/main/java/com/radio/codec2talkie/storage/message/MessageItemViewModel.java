package com.radio.codec2talkie.storage.message;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

public class MessageItemViewModel extends AndroidViewModel {

    private final MessageItemRepository _messageItemRepository;
    private LiveData<List<MessageItem>> _messages;

    public MessageItemViewModel(@NonNull Application application) {
        super(application);
        _messageItemRepository = new MessageItemRepository(application);
    }

    public LiveData<List<MessageItem>> getMessages(String groupName) {
        if (_messages == null)
            _messages = _messageItemRepository.getMessages(groupName);
        return _messages;
    }
}
