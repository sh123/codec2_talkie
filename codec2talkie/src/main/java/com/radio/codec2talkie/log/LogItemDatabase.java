package com.radio.codec2talkie.log;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {LogItem.class}, version = 1, exportSchema = false)
public abstract class LogItemDatabase extends RoomDatabase {

    private static LogItemDatabase _db;

    public abstract LogItemDao dataDAO();

    public static LogItemDatabase getDatabase(Context context) {
        if (_db == null) {
            _db = Room.databaseBuilder(context.getApplicationContext(),
                    LogItemDatabase.class, LogItemDatabase.class.getName())
                    .build();
        }
        return _db;
    }

    public static void destroyInstance() {
        _db = null;
    }
}
