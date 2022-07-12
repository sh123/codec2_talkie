package com.radio.codec2talkie.storage.message;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

public class MessageItemViewModel extends AndroidViewModel {

    private final MessageItemRepository _messageItemRepository;
    private final LiveData<List<MessageItem>> _messageItemLiveData;

    public MessageItemViewModel(@NonNull Application application) {
        super(application);
        _messageItemRepository = new MessageItemRepository(application);
        _messageItemLiveData = _messageItemRepository.getAllMessageItems();
    }

    public LiveData<List<MessageItem>> getAllData() {
        return _messageItemLiveData;
    }

    public void deleteAllMessageItems() { _messageItemRepository.deleteAllMessageItems(); }
}
