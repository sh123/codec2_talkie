package com.radio.codec2talkie.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import androidx.fragment.app.Fragment;

import com.radio.codec2talkie.app.AppService;

public class FragmentWithServiceConnection extends Fragment implements ServiceConnection {

    private static final String TAG = FragmentWithServiceConnection.class.getSimpleName();

    private AppService _appService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindAppService();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindAppService();
    }

    private void bindAppService() {
        if (getActivity() != null && !getActivity().bindService(new Intent(getActivity(), AppService.class), this, Context.BIND_AUTO_CREATE)) {
            Log.e(TAG, "Service does not exists or no access");
        }
    }

    private void unbindAppService() {
        if (getActivity() != null) {
            getActivity().unbindService(this);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.i(TAG, "Connected to app service");
        _appService = ((AppService.AppServiceBinder)service).getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.i(TAG, "Disconnected from app service");
        _appService = null;
    }

    protected AppService getService() {
        return _appService;
    }
}
