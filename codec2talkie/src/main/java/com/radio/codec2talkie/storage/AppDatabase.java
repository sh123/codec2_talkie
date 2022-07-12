package com.radio.codec2talkie.storage;

import android.content.Context;

import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.radio.codec2talkie.storage.log.LogItem;
import com.radio.codec2talkie.storage.log.LogItemDao;
import com.radio.codec2talkie.storage.message.MessageItem;
import com.radio.codec2talkie.storage.message.MessageItemDao;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@androidx.room.Database(entities = {LogItem.class, MessageItem.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final int NUMBER_OF_THREADS = 4;

    public abstract LogItemDao logItemDao();
    public abstract MessageItemDao messageItemDao();

    private static AppDatabase _db;
    private static ExecutorService _executor;

    public static ExecutorService getDatabaseExecutor() {
        if (_executor == null) {
            synchronized (AppDatabase.class) {
                 _executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
            }
        }
        return _executor;
    }

    public static AppDatabase getDatabase(Context context) {
        if (_db == null) {
            synchronized (AppDatabase.class) {
                _db = Room.databaseBuilder(context.getApplicationContext(),
                        AppDatabase.class, AppDatabase.class.getName())
                        .build();
            }
        }
        return _db;
    }

    public static void destroyInstance() {
        _db = null;
    }
}
