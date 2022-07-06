package com.radio.codec2talkie.log;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {LogItem.class}, version = 1, exportSchema = false)
public abstract class LogItemDatabase extends RoomDatabase {

    private static final int NUMBER_OF_THREADS = 4;

    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
    public abstract LogItemDao logItemDao();

    private static LogItemDatabase _db;

    public static LogItemDatabase getDatabase(Context context) {
        if (_db == null) {
            synchronized (LogItemDatabase.class) {
                _db = Room.databaseBuilder(context.getApplicationContext(),
                        LogItemDatabase.class, LogItemDatabase.class.getName())
                        .build();
            }
        }
        return _db;
    }

    public static void destroyInstance() {
        _db = null;
    }
}
