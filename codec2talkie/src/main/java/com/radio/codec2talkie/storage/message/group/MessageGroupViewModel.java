package com.radio.codec2talkie.storage.message.group;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.radio.codec2talkie.storage.message.MessageItemRepository;

import java.util.List;

public class MessageGroupViewModel extends AndroidViewModel {

    private final MessageItemRepository _messageItemRepository;
    private final LiveData<List<String>> _messageGroups;

    public MessageGroupViewModel(@NonNull Application application) {
        super(application);
        _messageItemRepository = new MessageItemRepository(application);
        _messageGroups = _messageItemRepository.getGroups();
    }

    public LiveData<List<String>> getGroups() {
        return _messageGroups;
    }

    public void deleteAll() {
        _messageItemRepository.deleteAllMessageItems();
    }
}
