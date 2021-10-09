package com.radio.codec2talkie.recorder;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.radio.codec2talkie.MainActivity;
import com.radio.codec2talkie.R;
import com.radio.codec2talkie.tools.StorageTools;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecorderActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private File _root;
    private File _currentDirectory;
    private ArrayAdapter<Object> _dirAdapter;
    private ListView _recordingList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recorder);

        _dirAdapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item);
        _recordingList = findViewById(R.id.listRecorder);
        _recordingList.setOnItemClickListener(onFileClickListener);
        _recordingList.setAdapter(_dirAdapter);

        _root = StorageTools.getStorage(getApplicationContext());
        loadFiles(_root);
    }

    private final AdapterView.OnItemClickListener onFileClickListener  = (parent, view, position, id) -> {
        Object selectedItem = parent.getAdapter().getItem(position);
        File selectedFile = new File(_root, selectedItem.toString());
        if (selectedFile.isDirectory()) {
            loadFiles(selectedFile);
        } else {
            // play file
        }
    };

    private void loadFiles(File directory) {
        _dirAdapter.clear();
        _currentDirectory = directory;

        String title = directory.getName();
        if (_root.getAbsolutePath().equals(directory.getAbsolutePath())) {
            title = getString(R.string.recorder_name);
        }
        setTitle(title);

        List<String> dirList = new ArrayList<>();
        File[] fileList = directory.listFiles();
        if (fileList != null) {
            for (File file : fileList) {
                dirList.add(file.getName());
            }
        }
        Collections.sort(dirList);

        for (Object dirElement: dirList) {
            _dirAdapter.add(dirElement);
        }
        _recordingList.setVisibility(View.VISIBLE);
    }

    private void deleteAll() {
        File[] fileList = _currentDirectory.listFiles();
        if (fileList != null) {
            for (File file : fileList) {
                if (!file.delete()) {
                    Log.e(TAG, file.getName() + " cannot be deleted");
                }
            }
        }
        loadFiles(_currentDirectory);
    }

    private void playAll() {
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK  && event.getRepeatCount() == 0) {
            if (!_root.getAbsolutePath().equals(_currentDirectory.getAbsolutePath())) {
                _currentDirectory = _currentDirectory.getParentFile();
                if (_currentDirectory != null) {
                    loadFiles(_currentDirectory);
                    return true;
                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void runConfirmation() {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setMessage(R.string.recorder_remove_all_confirmation_message)
                .setTitle(R.string.recorder_remove_all_confirmation_title)
                .setPositiveButton(R.string.ok, (dialog, id) -> deleteAll())
                .setNegativeButton(R.string.cancel, (dialog, id) -> {})
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.recorder_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int itemId = item.getItemId();

        if (itemId == R.id.recorder_play_all) {
            playAll();
            return true;
        }
        if (itemId == R.id.recorder_delete_all) {
            runConfirmation();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent Data) {
        super.onActivityResult(requestCode, resultCode, Data);
    }
}
