package com.radio.codec2talkie.storage;

import android.content.Context;

import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.radio.codec2talkie.storage.log.LogItem;
import com.radio.codec2talkie.storage.log.LogItemDao;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@androidx.room.Database(entities = {LogItem.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final int NUMBER_OF_THREADS = 4;

    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
    public abstract LogItemDao logItemDao();

    private static AppDatabase _db;

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
