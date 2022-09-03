package com.radio.codec2talkie.storage.position;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.radio.codec2talkie.tools.DateTools;

public class PositionItemViewModel extends AndroidViewModel {

    private final PositionItemRepository _positionItemRepository;

    public PositionItemViewModel(@NonNull Application application) {
        super(application);
        _positionItemRepository = new PositionItemRepository(application);
    }

    public void deleteAllPositionItems() { _positionItemRepository.deleteAllPositionItems(); }

    public void deletePositionItems(String srcCallsign) {
        _positionItemRepository.deletePositionItemsFromCallsign(srcCallsign);
    }

    public void deletePositionItemsOlderThanHours(int hours) {
        _positionItemRepository.deletePositionItemsOlderThanTimestamp(DateTools.currentTimestampMinusHours(hours));
    }
}
