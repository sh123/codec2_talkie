package com.radio.codec2talkie.storage.position;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

public class PositionItemViewModel extends AndroidViewModel {

    private final PositionItemRepository _positionItemRepository;

    public PositionItemViewModel(@NonNull Application application) {
        super(application);
        _positionItemRepository = new PositionItemRepository(application);
    }

    public LiveData<List<PositionItem>> getPositionItems(String srcCallsign) {
        return _positionItemRepository.getPositionItems(srcCallsign);
    }

    public void deletePositionItems(String srcCallsign, int hours) {
        _positionItemRepository.deletePositionItems(srcCallsign, hours);
    }
}
