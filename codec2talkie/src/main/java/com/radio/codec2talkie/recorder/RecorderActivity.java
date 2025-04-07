package com.radio.codec2talkie.recorder;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.radio.codec2talkie.MainActivity;
import com.radio.codec2talkie.R;
import com.radio.codec2talkie.audio.AudioPlayer;
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
    private TextView _textPlaybackStatus;
    private AudioPlayer _audioPlayer;
    private Menu _menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recorder);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        _dirAdapter = new ArrayAdapter<>(this, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item);
        _recordingList = findViewById(R.id.listRecorder);
        _recordingList.setOnItemClickListener(onFileClickListener);
        _recordingList.setOnItemLongClickListener(onFileLongClickListener);
        _recordingList.setAdapter(_dirAdapter);

        _textPlaybackStatus = findViewById(R.id.textPlaybackStatus);
        _textPlaybackStatus.setText(R.string.player_status_stopped);

        _root = StorageTools.getStorage(getApplicationContext());
        loadFiles(_root);
    }

    private final Handler onPlayerStateChanged = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case AudioPlayer.PLAYER_STARTED:
                    _textPlaybackStatus.setText(R.string.player_status_started);
                    break;
                case AudioPlayer.PLAYER_STOPPED:
                    _textPlaybackStatus.setText(getString(R.string.player_status_stopped));
                    _audioPlayer = null;
                    break;
                case AudioPlayer.PLAYER_ERROR:
                    _textPlaybackStatus.setText(getString(R.string.player_status_error, msg.obj));
                    break;
                case AudioPlayer.PLAYER_PLAYING_FILE:
                    _textPlaybackStatus.setText(getString(R.string.player_status_playing_file, msg.obj));
                    break;
                case AudioPlayer.PLAYER_PLAYED_FILE:
                    _textPlaybackStatus.setText(getString(R.string.player_status_played_file, msg.obj));
                    break;
            }
        }
    };

    private final AdapterView.OnItemClickListener onFileClickListener  = (parent, view, position, id) -> {
        Object selectedItem = parent.getAdapter().getItem(position);
        File selectedFile = new File(_currentDirectory, selectedItem.toString());
        if (selectedFile.isDirectory()) {
            loadFiles(selectedFile);
        } else if (_audioPlayer == null) {
            File[] files = {selectedFile};
            _audioPlayer = new AudioPlayer(files, onPlayerStateChanged, this);
            _audioPlayer.start();
        }
    };

    private final AdapterView.OnItemLongClickListener onFileLongClickListener  = (parent, view, position, id) -> {
        Object selectedItem = parent.getAdapter().getItem(position);
        File selectedFile = new File(_currentDirectory, selectedItem.toString());
        if (selectedFile.isDirectory()) {
            runDeleteFromDirectoryConfirmation(selectedFile);
            if (selectedFile.delete()) {
                loadFiles(_currentDirectory);
            }
        } else {
            runDeleteFileConfirmation(selectedFile);
        }
        return true;
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
        updateMenu();
    }

    private void deleteAll(File directory) {
        File[] fileList = directory.listFiles();
        if (fileList != null) {
            for (File file : fileList) {
                if (file.isDirectory()) {
                    deleteAll(file);
                }
                if (!file.delete()) {
                    Log.e(TAG, file.getName() + " cannot be deleted");
                }
            }
        }
        loadFiles(_currentDirectory);
    }

    private void playAll() {
        if (_audioPlayer == null) {
            File[] files = _currentDirectory.listFiles();
            _audioPlayer = new AudioPlayer(files, onPlayerStateChanged, this);
            _audioPlayer.start();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK  && event.getRepeatCount() == 0) {
            if (loadPreviousDirectory()) return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private boolean loadPreviousDirectory() {
        if (_audioPlayer != null) {
            _audioPlayer.stopPlayback();
        }
        if (!isRootDirectory()) {
            _currentDirectory = _currentDirectory.getParentFile();
            if (_currentDirectory != null) {
                loadFiles(_currentDirectory);
                return true;
            }
        }
        return false;
    }

    private boolean isRootDirectory() {
        return _root.getAbsolutePath().equals(_currentDirectory.getAbsolutePath());
    }

    private void runDeleteFromDirectoryConfirmation(File directory) {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setMessage(getString(R.string.recorder_remove_all_confirmation_message, directory.getName()))
                .setTitle(R.string.recorder_remove_all_confirmation_title)
                .setPositiveButton(R.string.ok, (dialog, id) -> deleteAll(directory))
                .setNegativeButton(R.string.cancel, (dialog, id) -> {})
                .show();
    }

    private void runDeleteFileConfirmation(File file) {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setMessage(getString(R.string.recorder_remove_file_confirmation_message, file.getName()))
                .setTitle(R.string.recorder_remove_all_confirmation_title)
                .setPositiveButton(R.string.ok, (dialog, id) ->  {
                    if (file.isDirectory()) {
                        deleteAll(file);
                    }
                    else if (file.delete()) {
                        loadFiles(_currentDirectory);
                    }
                })
                .setNegativeButton(R.string.cancel, (dialog, id) -> {})
                .show();
    }

    private void updateMenu() {
        if (_menu == null) return;
        boolean isRoot = isRootDirectory();
        MenuItem playAllItem = _menu.findItem(R.id.recorder_play_all);
        playAllItem.setVisible(!isRoot);
        MenuItem stopItem = _menu.findItem(R.id.recorder_stop);
        stopItem.setVisible(!isRoot);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.recorder_menu, menu);
        _menu = menu;
        updateMenu();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            if (loadPreviousDirectory()) return true;
            onBackPressed();
            return true;
        }
        else if (itemId == R.id.recorder_play_all) {
            playAll();
            return true;
        }
        else if (itemId == R.id.recorder_delete_all) {
            runDeleteFromDirectoryConfirmation(_currentDirectory);
            return true;
        }
        else if (itemId == R.id.recorder_stop) {
            if (_audioPlayer != null) {
                _audioPlayer.stopPlayback();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent Data) {
        super.onActivityResult(requestCode, resultCode, Data);
    }
}
