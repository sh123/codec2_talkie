package com.radio.codec2talkie.protocol;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.radio.codec2talkie.app.AppWorker;
import com.radio.codec2talkie.protocol.message.TextMessage;
import com.radio.codec2talkie.protocol.position.Position;
import com.radio.codec2talkie.settings.PreferenceKeys;
import com.radio.codec2talkie.settings.SettingsWrapper;
import com.radio.codec2talkie.tools.AudioTools;
import com.radio.codec2talkie.transport.Transport;
import com.ustadmobile.codec2.Codec2;

import java.io.IOException;
import java.util.Arrays;

public class Freedv implements Protocol {
    private static final String TAG = Freedv.class.getSimpleName();

    private ProtocolCallback _parentProtocolCallback;
    private Transport _transport;

    private long _freedv;

    private short[] _modemTxBuffer;
    private short[] _speechRxBuffer;

    public Freedv() {
    }

    @Override
    public void initialize(Transport transport, Context context, ProtocolCallback parentProtocolCallback) throws IOException {
        _transport = transport;
        _parentProtocolCallback = parentProtocolCallback;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int mode = SettingsWrapper.getFreeDvSoundModemModulation(sharedPreferences);
        int gain = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.PORTS_SOUND_MODEM_GAIN, "10000"));

        Log.i(TAG, "Using freedv mode " + AudioTools.getFreedvModeAsText(sharedPreferences) + " gain " + gain);

        _freedv = Codec2.freedvCreate(mode, gain);
        _modemTxBuffer = new short[Codec2.freedvGetNomModemSamples(_freedv)];
        _speechRxBuffer = new short[Codec2.freedvGetMaxSpeechSamples(_freedv)];
    }

    @Override
    public int getPcmAudioBufferSize() {
        return Codec2.freedvGetNSpeechSamples(_freedv);
    }

    @Override
    public void sendPcmAudio(String src, String dst, int codec, short[] pcmFrame) throws IOException {
        Codec2.freedvTx(_freedv, _modemTxBuffer, pcmFrame);
        //Log.i(TAG, "send pcm " + _modemTxBuffer.length);
        _transport.write(_modemTxBuffer);
        _parentProtocolCallback.onTransmitPcmAudio(src, dst, codec, pcmFrame);
    }

    @Override
    public void sendCompressedAudio(String src, String dst, int codec, byte[] frame) throws IOException {
    }

    @Override
    public void sendTextMessage(TextMessage textMessage) throws IOException {
    }

    @Override
    public void sendData(String src, String dst, byte[] dataPacket) throws IOException {
    }

    @Override
    public boolean receive() throws IOException {
        int nin = Codec2.freedvNin(_freedv);
        short[] buf = new short[nin];
        int bytesRead = _transport.read(buf);
        if (bytesRead == nin) {
            //Log.i(TAG, "read " + bytesRead);
            long cntRead = Codec2.freedvRx(_freedv, _speechRxBuffer, buf);
            if (cntRead > 0) {
                Log.i(TAG, "receive " + cntRead);
                _parentProtocolCallback.onReceivePcmAudio(null, null, -1, Arrays.copyOf(_speechRxBuffer, (int) cntRead));
                return true;
            }
        } else {
            //Log.w(TAG, bytesRead + "!=" + nin);
        }
        return false;
    }

    @Override
    public void sendPosition(Position position) throws IOException {
    }

    @Override
    public void flush() throws IOException {
    }

    @Override
    public void close() {
        Codec2.freedvDestroy(_freedv);
    }
}
