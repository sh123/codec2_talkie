package com.radio.codec2talkie.protocol;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

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
    private long _freedvData;

    private short[] _modemTxBuffer;
    private short[] _speechRxBuffer;

    private short[] _dataSamplesBuffer;
    private byte[] _dataTxBuffer;

    public Freedv() {
    }

    @Override
    public void initialize(Transport transport, Context context, ProtocolCallback parentProtocolCallback) throws IOException {
        _transport = transport;
        _parentProtocolCallback = parentProtocolCallback;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int mode = SettingsWrapper.getFreeDvSoundModemModulation(sharedPreferences);
        int dataMode = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.PORTS_SOUND_MODEM_FREEDV_DATA_MODE, "12"));
        boolean isSquelchEnabled = sharedPreferences.getBoolean(PreferenceKeys.PORTS_SOUND_MODEM_FREEDV_ENABLE_SQUELCH, true);
        float squelchSnr = Float.parseFloat( sharedPreferences.getString(PreferenceKeys.PORTS_SOUND_MODEM_FREEDV_SQUELCH_SNR, "0.0"));

        Log.i(TAG, "Using freedv mode " + AudioTools.getFreedvModeAsText(sharedPreferences));

        _freedv = Codec2.freedvCreate(mode, isSquelchEnabled, squelchSnr);
        _modemTxBuffer = new short[Codec2.freedvGetNomModemSamples(_freedv)];
        _speechRxBuffer = new short[Codec2.freedvGetMaxSpeechSamples(_freedv)];

        _freedvData = Codec2.freedvCreate(dataMode, isSquelchEnabled, squelchSnr);
        _dataTxBuffer = new byte[Codec2.freedvGetBitsPerModemFrame(_freedvData) / 8];
        _dataSamplesBuffer = new short[Codec2.freedvGetNTxSamples(_freedvData)];
    }

    @Override
    public int getPcmAudioRecordBufferSize() {
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
        Log.w(TAG, "sendCompressedAudio() is not supported");
    }

    @Override
    public void sendTextMessage(TextMessage textMessage) throws IOException {
        Log.w(TAG, "sendTextMessage() is not supported");
    }

    @Override
    public void sendData(String src, String dst, byte[] dataPacket) throws IOException {
        Log.v(TAG, "sendData() " + dataPacket.length);
        if (dataPacket.length > _dataTxBuffer.length - 2) {
            Log.e(TAG, "Too large packet " + dataPacket.length + " > " + _dataTxBuffer.length);
            return;
        }
        long cnt = Codec2.freedvRawDataPreambleTx(_freedvData, _dataSamplesBuffer);
        Log.v(TAG, "sendData() write preamble " + cnt);
        _transport.write(Arrays.copyOf(_dataSamplesBuffer, (int) cnt));

        Arrays.fill(_dataTxBuffer, (byte) 0);
        System.arraycopy(dataPacket, 0, _dataTxBuffer, 0, dataPacket.length);
        Codec2.freedvRawDataTx(_freedvData, _dataSamplesBuffer, _dataTxBuffer);
        Log.v(TAG, "sendData() write data " + _dataSamplesBuffer.length);
        _transport.write(_dataSamplesBuffer);

        cnt = Codec2.freedvRawDataPostambleTx(_freedvData, _dataSamplesBuffer);
        Log.v(TAG, "sendData() write postamble " + cnt);
        _transport.write(Arrays.copyOf(_dataSamplesBuffer, (int) cnt));
    }

    @Override
    public boolean receive() throws IOException {
        // TODO, distinguish between audio and data and call onReceiveData on parent callback
        int nin = Codec2.freedvNin(_freedv);
        short[] buf = new short[nin];
        int bytesRead = _transport.read(buf);
        if (bytesRead == nin) {
            //Log.i(TAG, "read " + bytesRead);
            long cntRead = Codec2.freedvRx(_freedv, _speechRxBuffer, buf);
            if (cntRead > 0) {
                //Log.i(TAG, "receive " + cntRead);
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
        Log.w(TAG, "sendPosition() is not supported");
    }

    @Override
    public void flush() throws IOException {
    }

    @Override
    public void close() {
        Codec2.freedvDestroy(_freedvData);
        Codec2.freedvDestroy(_freedv);
    }
}
