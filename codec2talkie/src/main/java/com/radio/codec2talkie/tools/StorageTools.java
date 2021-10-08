package com.radio.codec2talkie.tools;

import android.content.Context;
import android.os.Environment;

import androidx.core.content.ContextCompat;

import java.io.File;

public class StorageTools {

    public static boolean isExternalStorageAvailable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    public static File getExternalStorage(Context context) {
        File[] externalStorageVolumes =
                ContextCompat.getExternalFilesDirs(context, null);
        return externalStorageVolumes[0];
    }

    public static File getStorage(Context context) {
        if (isExternalStorageAvailable()) {
            return getExternalStorage(context);
        } else {
            return context.getFilesDir();
        }
    }
}
