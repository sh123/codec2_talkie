package com.radio.codec2talkie.tools;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.LinkedList;
import java.util.List;

public class PermissionsManager {
    public final static int REQUEST_PERMISSIONS = 1;

    private static List<String> getRequiredPermissions() {
        List<String> versionRequiredPermissions = new LinkedList<>();
        versionRequiredPermissions.add(Manifest.permission.BLUETOOTH);
        versionRequiredPermissions.add(Manifest.permission.BLUETOOTH_ADMIN);
        versionRequiredPermissions.add(Manifest.permission.RECORD_AUDIO);
        versionRequiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        versionRequiredPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            versionRequiredPermissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            versionRequiredPermissions.add(Manifest.permission.BLUETOOTH_SCAN);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            versionRequiredPermissions.add(Manifest.permission.FOREGROUND_SERVICE);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            versionRequiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS);
            versionRequiredPermissions.add(Manifest.permission.NEARBY_WIFI_DEVICES);
            versionRequiredPermissions.add(Manifest.permission.CAMERA);
        }
        return versionRequiredPermissions;
    }

    public static boolean allGranted(@NonNull int[] grantResults) {
        boolean allGranted = true;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        return allGranted;
    }

    public static boolean requestPermissions(Activity activity) {
        List<String> permissionsToRequest = new LinkedList<>();
        List<String> versionRequiredPermissions = getRequiredPermissions();
        if (!versionRequiredPermissions.isEmpty()) {
            for (String permission : versionRequiredPermissions) {
                if (ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_DENIED) {
                    permissionsToRequest.add(permission);
                }
            }
        }
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                    activity,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS);
            return false;
        }
        return true;
    }
}
