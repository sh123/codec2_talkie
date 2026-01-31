package com.radio.codec2talkie.ui;

import android.view.Menu;
import android.view.MenuItem;

public interface FragmentMenuHandler {
    boolean handleMenuItemClick(MenuItem item);
    void handleMenuCreation(Menu menu);
}
