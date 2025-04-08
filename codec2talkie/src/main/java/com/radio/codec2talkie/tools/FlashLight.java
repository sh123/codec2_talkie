package com.radio.codec2talkie.tools;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.util.Log;

public class FlashLight {
    private static final String TAG = FlashLight.class.getSimpleName();

    private final Context _context;

    public FlashLight(Context context) {
        _context = context;
    }

    public void turnOn() {
        try {
            CameraManager cameraManager = (CameraManager) _context.getSystemService(Context.CAMERA_SERVICE);
            String cameraId;
            if (cameraManager != null) {
                cameraId = cameraManager.getCameraIdList()[0];
                cameraManager.setTorchMode(cameraId, true);
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, e.toString());
        }
    }

    public void turnOff() {
        try {
            String cameraId;
            CameraManager cameraManager = (CameraManager) _context.getSystemService(Context.CAMERA_SERVICE);
            if (cameraManager != null) {
                cameraId = cameraManager.getCameraIdList()[0];
                cameraManager.setTorchMode(cameraId, false);
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, e.toString());
        }
    }
}