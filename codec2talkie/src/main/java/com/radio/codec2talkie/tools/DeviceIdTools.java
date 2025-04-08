package com.radio.codec2talkie.tools;

import android.content.Context;

import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class DeviceIdTools {
    private static final String TAG = DeviceIdTools.class.getSimpleName();

    private static final String _deviceIdsAssert = "tocalls.dense.json";

    private final Context _context;

    public DeviceIdTools(Context context) {
        _context = context;
    }

    public String getDescriptionByDeviceId(String deviceId) {
        String jsonString = loadJSONFromAsset(_context, _deviceIdsAssert);
        if (jsonString == null) return "Unknown device";
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            JSONObject tocallsJsonObject = jsonObject.getJSONObject("tocalls");
            Iterator<String> deviceIds = tocallsJsonObject.keys();
            while (deviceIds.hasNext()) {
                String dbDeviceId = deviceIds.next();
                if (deviceId.startsWith(dbDeviceId.replaceAll("\\?+$", ""))) {
                    try {
                        JSONObject jsonDeviceEntry = tocallsJsonObject.getJSONObject(dbDeviceId);
                        String model = jsonDeviceEntry.getString("model");
                        String vendor = jsonDeviceEntry.getString("vendor");
                        return model + " by " + vendor;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Unknown device";
    }

    private String loadJSONFromAsset(Context context, String fileName) {
        String json;
        try {
            InputStream is = context.getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }
}
