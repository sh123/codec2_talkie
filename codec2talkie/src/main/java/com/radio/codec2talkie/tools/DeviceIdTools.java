package com.radio.codec2talkie.tools;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;

public class DeviceIdTools {
    private static final String TAG = DeviceIdTools.class.getSimpleName();
    private static final String _deviceIdsAssert = "tocalls.dense.json";
    private static final HashMap<String, String> _deviceIdMap = new HashMap<>();
    private static final HashMap<String, String> _deviceIdMiceMap = new HashMap<>();
    private static final HashMap<String, String> _deviceIdCache = new HashMap<>();

    public static String getDeviceDescription(String deviceId) {
        String description = _deviceIdCache.get(deviceId);
        if (description != null) return description;
        for (int i = deviceId.length(); i > 0; i--) {
            String key = deviceId.substring(0, i);
            description = _deviceIdMap.get(key);
            if (description != null) {
                _deviceIdCache.put(deviceId, description);
                return description;
            }
        }
        description = "";
        _deviceIdCache.put(deviceId, description);
        return description;
    }

    public static String getMiceDeviceDescription(String miceDeviceId) {
        return _deviceIdMiceMap.get(miceDeviceId);
    }

    public static void loadDeviceIdMap(Context context) {
        JSONObject jsonObject = loadJSONFromAsset(context);
        if (jsonObject == null) {
            Log.e(TAG, "Failed to load device ids");
            return;
        }
        try {
            // load tocalls
            JSONObject tocallsJsonObject = jsonObject.getJSONObject("tocalls");
            Iterator<String> deviceIds = tocallsJsonObject.keys();
            while (deviceIds.hasNext()) {
                String dbDeviceId = deviceIds.next();
                JSONObject jsonDeviceEntry = tocallsJsonObject.getJSONObject(dbDeviceId);
                String description = toDescription(jsonDeviceEntry);
                if (description == null) continue;
                String deviceId = dbDeviceId.replaceAll("[?*n]", "");
                _deviceIdMap.put(deviceId, description);
            }
            // load mice
            JSONObject miceJsonObject = jsonObject.getJSONObject("mice");
            Iterator<String> miceDeviceIds = miceJsonObject.keys();
            while (miceDeviceIds.hasNext()) {
                String miceDeviceId = miceDeviceIds.next();
                JSONObject jsonDeviceEntry = miceJsonObject.getJSONObject(miceDeviceId);
                String description = toDescription(jsonDeviceEntry);
                if (description == null) continue;
                _deviceIdMiceMap.put(miceDeviceId, description);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String toDescription(JSONObject jsonDeviceEntry) {
        try {
            String model = "Unknown";
            String vendor = "unknown";
            if (jsonDeviceEntry.has("model"))
                model = jsonDeviceEntry.getString("model");
            if (jsonDeviceEntry.has("vendor"))
                vendor = jsonDeviceEntry.getString("vendor");
            return model + " by " + vendor;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static JSONObject loadJSONFromAsset(Context context) {
        JSONObject jsonObject;
        try {
            InputStream is = context.getAssets().open(DeviceIdTools._deviceIdsAssert);
            int size = is.available();
            byte[] buffer = new byte[size];
            int bytesRead = is.read(buffer);
            if (bytesRead == 0) return null;
            is.close();
            jsonObject = new JSONObject(new String(buffer));
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
        return jsonObject;
    }
}
