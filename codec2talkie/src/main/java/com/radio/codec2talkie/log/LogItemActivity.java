package com.radio.codec2talkie.log;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.ListFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.radio.codec2talkie.R;

public class LogItemActivity extends AppCompatActivity {

    private LogItemViewModel _logItemViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_view);
        RecyclerView recyclerView = findViewById(R.id.log_item_recyclerview);
        final LogItemAdapter adapter = new LogItemAdapter(new LogItemAdapter.LogItemDiff());
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }
}
