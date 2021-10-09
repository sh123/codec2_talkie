package com.radio.codec2talkie;

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

import androidx.appcompat.app.AppCompatActivity;

import com.radio.codec2talkie.settings.SettingsActivity;
import com.radio.codec2talkie.tools.StorageTools;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class VoicemailActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private File _root;
    private File _currentDirectory;
    private ArrayAdapter<Object> _dirAdapter;
    private ListView _listVoicemail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voicemail);

        _dirAdapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item);
        _listVoicemail = findViewById(R.id.listVoicemail);
        _listVoicemail.setOnItemClickListener(onFileClickListener);
        _listVoicemail.setAdapter(_dirAdapter);

        _root = StorageTools.getStorage(getApplicationContext());
        loadFiles(_root);
    }

    private final AdapterView.OnItemClickListener onFileClickListener  = (parent, view, position, id) -> {
        Object selectedItem = (String)parent.getAdapter().getItem(position);
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
            title = "Voicemails";
        }
        setTitle(title);

        List<String> dirList = new ArrayList<String>();
        String[] fileList = directory.list();
        if (fileList != null) {
            dirList.addAll(Arrays.asList(fileList));
        }
        Collections.sort(dirList);

        for (Object dirElement: dirList) {
            _dirAdapter.add(dirElement);
        }
        _listVoicemail.setVisibility(View.VISIBLE);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.voicemail_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int itemId = item.getItemId();

        if (itemId == R.id.voicemail_play_all) {
            playAll();
            return true;
        }
        if (itemId == R.id.voicemail_delete_all) {
            deleteAll();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent Data) {
        super.onActivityResult(requestCode, resultCode, Data);
    }
}
