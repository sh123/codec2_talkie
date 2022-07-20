package com.radio.codec2talkie.storage.log;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.radio.codec2talkie.R;

public class LogItemActivity extends AppCompatActivity {

    private LogItemViewModel _logItemViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_view);
        setTitle(R.string.aprs_log_view_title);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        RecyclerView recyclerView = findViewById(R.id.log_item_recyclerview);
        recyclerView.setHasFixedSize(true);
        final LogItemAdapter adapter = new LogItemAdapter(new LogItemAdapter.LogItemDiff());
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL));

        _logItemViewModel = new ViewModelProvider(this).get(LogItemViewModel.class);
        _logItemViewModel.getAllData().observe(this, adapter::submitList);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.log_view_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            finish();
            return true;
        }
        else if (itemId == R.id.log_view_menu_clear) {
            deleteAll();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteAll() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.log_item_activity_delete_all_title))
                .setPositiveButton(getString(R.string.yes), _deleteAllDialogClickListener)
                .setNegativeButton(getString(R.string.no), _deleteAllDialogClickListener).show();
    }

    private final DialogInterface.OnClickListener _deleteAllDialogClickListener = (dialog, which) -> {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            _logItemViewModel.deleteAllLogItems();
        }
    };
}