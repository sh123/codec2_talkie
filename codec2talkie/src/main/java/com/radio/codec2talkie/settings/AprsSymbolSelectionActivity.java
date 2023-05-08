package com.radio.codec2talkie.settings;


import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.radio.codec2talkie.R;
import com.radio.codec2talkie.protocol.AudioFrameAggregator;
import com.radio.codec2talkie.protocol.aprs.tools.AprsSymbolTable;

public class AprsSymbolSelectionActivity extends AppCompatActivity {

    private static final String TAG = AprsSymbolSelectionActivity.class.getSimpleName();

    private SharedPreferences _sharedPreferences;

    private ImageView _currentSelectionView;
    private TextView _currentSelectionText;

    private String _currentSymbolCode;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.aprs_symbol_title);
        setContentView(R.layout.activity_aprs_symbol_selection);

        _sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        ImageView selectionView = findViewById(R.id.settings_aprs_symbol_image_view);
        selectionView.getViewTreeObserver().addOnGlobalLayoutListener(() -> selectionView.setImageBitmap(
                AprsSymbolTable.generateSelectionTable(selectionView.getContext(), selectionView.getWidth())));
        selectionView.setOnTouchListener(_onTouchListener);

        _currentSelectionView = findViewById(R.id.settings_aprs_symbol_image_selected);
        _currentSymbolCode = _sharedPreferences.getString(PreferenceKeys.APRS_SYMBOL, "/$");
        Bitmap currentSymbolBitmap = AprsSymbolTable.getInstance(this).bitmapFromSymbol(_currentSymbolCode, true);
        _currentSelectionView.setImageBitmap(currentSymbolBitmap);

        _currentSelectionText = findViewById(R.id.settings_aprs_symbol_text);
        _currentSelectionText.setText(_currentSymbolCode);

        Button okBtn = findViewById(R.id.settings_aprs_symbol_button_ok);
        okBtn.setOnClickListener(v -> {
            SharedPreferences.Editor editor = _sharedPreferences.edit();
            editor.putString(PreferenceKeys.APRS_SYMBOL, _currentSymbolCode);
            editor.apply();
            finish();
        });

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);
    }

    private final View.OnTouchListener _onTouchListener  = (v, event) -> {
        if(event.getAction() == MotionEvent.ACTION_UP) {
            float x = event.getX();
            float y = event.getY();
            _currentSymbolCode = AprsSymbolTable.getSymbolFromCoordinate(x, y, v.getWidth(), v.getHeight());
            _currentSelectionText.setText(_currentSymbolCode);
            Log.i(TAG, "Selected symbol: " + _currentSymbolCode);
            Bitmap currentSymbolBitmap = AprsSymbolTable.getInstance(this).bitmapFromSymbol(_currentSymbolCode, true);
            if (currentSymbolBitmap == null)
                Log.e(TAG, "Cannot select symbol");
            else
                _currentSelectionView.setImageBitmap(currentSymbolBitmap);
            v.performClick();
        }
        return true;
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
