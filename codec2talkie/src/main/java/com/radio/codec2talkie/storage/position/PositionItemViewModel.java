package com.radio.codec2talkie.storage.position;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.radio.codec2talkie.tools.DateTools;

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

    public void deleteAllPositionItems() { _positionItemRepository.deleteAllPositionItems(); }

    public void deletePositionItems(String srcCallsign) {
        _positionItemRepository.deletePositionItemsFromCallsign(srcCallsign);
    }

    public void deletePositionItemsOlderThanHours(int hours) {
        _positionItemRepository.deletePositionItemsOlderThanTimestamp(DateTools.currentTimestampMinusHours(hours));
    }
}
