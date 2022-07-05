package com.radio.codec2talkie.log;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.radio.codec2talkie.R;

public class LogItemActivity extends AppCompatActivity {

    private LogItemViewModel _logItemViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
    }
}
