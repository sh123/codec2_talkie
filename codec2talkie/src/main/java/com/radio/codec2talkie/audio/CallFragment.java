package com.radio.codec2talkie.audio;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Message;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.radio.codec2talkie.R;
import com.radio.codec2talkie.app.AppMessage;
import com.radio.codec2talkie.settings.PreferenceKeys;
import com.radio.codec2talkie.ui.FragmentWithServiceConnection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

public class CallFragment extends FragmentWithServiceConnection {
    private SharedPreferences _sharedPreferences;

    private ImageButton _btnPtt;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        _sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_call_view, container, false);

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                return handleKeyUp(keyCode);
            } else if (event.getAction() == KeyEvent.ACTION_DOWN) {
                return handleKeyDown(keyCode);
            }
            return false;
        });
        return view;
    }

    public void setEnabled(boolean enabled) {
        if (_btnPtt != null)
            _btnPtt.setEnabled(enabled);
    }

    public void onServiceMessage(Message msg) {
        switch (AppMessage.values()[msg.what]) {
            case EV_CONNECTED:
                setStrokeColor(R.color.dark_blue);
                _btnPtt.setEnabled(true);
                break;
            case EV_DISCONNECTED:
                setStrokeColor(R.color.black);
                _btnPtt.setImageResource(R.drawable.btn_ptt_stop);
                _btnPtt.setEnabled(false);
                break;
            case EV_LISTENING:
                setStrokeColor(R.color.dark_gray);
                _btnPtt.setImageResource(R.drawable.btn_ptt_touch);
                break;
            case EV_TRANSMITTED_VOICE:
                setStrokeColor(R.color.red);
                _btnPtt.setImageResource(R.drawable.btn_ptt_mic);
                break;
            case EV_RECEIVING:
                setStrokeColor(R.color.green);
                _btnPtt.setImageResource(R.drawable.btn_ptt_listen);
                break;
            case EV_TEXT_MESSAGE_RECEIVED:
            case EV_DATA_RECEIVED:
            case EV_POSITION_RECEIVED:
                setStrokeColor(R.color.green);
                _btnPtt.setImageResource(R.drawable.btn_ptt_letter);
                break;
            case EV_VOICE_RECEIVED:
                setStrokeColor(R.color.green);
                _btnPtt.setImageResource(R.drawable.btn_ptt_listen);
                break;
            case EV_RX_RADIO_LEVEL:
            case EV_TELEMETRY:
            case EV_RX_LEVEL:
            case EV_TX_LEVEL:
            case EV_STARTED_TRACKING:
            case EV_STOPPED_TRACKING:
                break;
            case EV_RX_ERROR:
                _btnPtt.setImageResource(R.drawable.btn_ptt_err_rx);
                break;
            case EV_TX_ERROR:
                _btnPtt.setImageResource(R.drawable.btn_ptt_err_tx);
                break;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        _btnPtt = view.findViewById(R.id.btnPtt);
        _btnPtt.setEnabled(true);
        _btnPtt.setOnTouchListener(onBtnPttTouchListener);
    }

    public boolean handleKeyUp(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (_sharedPreferences.getBoolean(PreferenceKeys.APP_VOLUME_PTT, false)) {
                    _btnPtt.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(),
                            SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 0, 0, 0));
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_TV_DATA_SERVICE:
                _btnPtt.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(),
                        SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 0, 0, 0));
                return true;
        }
        return false;
    }

    public boolean handleKeyDown(int keyCode) {
        switch (keyCode) {
            // headset hardware ptt cannot be used for long press
            case KeyEvent.KEYCODE_HEADSETHOOK:
                if (_btnPtt.isPressed()) {
                    _btnPtt.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(),
                            SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 0, 0, 0));
                } else {
                    _btnPtt.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(),
                            SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0, 0, 0));
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (_sharedPreferences.getBoolean(PreferenceKeys.APP_VOLUME_PTT, false)) {
                    _btnPtt.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(),
                            SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0, 0, 0));
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_TV_DATA_SERVICE:
                _btnPtt.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(),
                        SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0, 0, 0));
                return true;
        }
        return false;
    }

    private void setStrokeColor(int color) {
        GradientDrawable gradientDrawable = (GradientDrawable) _btnPtt.getBackground();
        int strokeWidthPx = (int) getResources().getDimension(R.dimen.button_stroke);
        gradientDrawable.setStroke(strokeWidthPx, ContextCompat.getColor(requireContext(), color));
        _btnPtt.invalidate();
    }

    private final View.OnTouchListener onBtnPttTouchListener = (v, event) -> {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                setStrokeColor(R.color.red);
                if (getService() != null)
                    getService().startTransmit();
                break;
            case MotionEvent.ACTION_UP:
                setStrokeColor(R.color.dark_gray);
                v.performClick();
                if (getService() != null)
                    getService().startReceive();
                break;
        }
        return false;
    };
}
