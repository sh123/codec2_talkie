package com.radio.codec2talkie.transport;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.radio.codec2talkie.connect.BleHandler;
import com.radio.codec2talkie.connect.BluetoothSocketHandler;
import com.radio.codec2talkie.connect.TcpIpSocketHandler;
import com.radio.codec2talkie.connect.UsbPortHandler;
import com.radio.codec2talkie.settings.SettingsWrapper;

import java.io.IOException;

public class TransportFactory {

    public enum TransportType {
        USB("USB"),
        BLUETOOTH("BLUETOOTH"),
        LOOPBACK("LOOPBACK"),
        TCP_IP("TCP_IP"),
        BLE("BLE"),
        SOUND_MODEM("SOUND_MODEM");

        private final String _name;

        TransportType(String name) {
            _name = name;
        }

        @NonNull
        @Override
        public String toString() {
            return _name;
        }
    }

    public static Transport create(TransportType transportType, Context context) throws IOException {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        switch (transportType) {
            case USB:
                return new UsbSerial(UsbPortHandler.getPort(), UsbPortHandler.getName());
            case BLUETOOTH:
                return new Bluetooth(BluetoothSocketHandler.getSocket(), BluetoothSocketHandler.getName());
            case TCP_IP:
                return new TcpIp(TcpIpSocketHandler.getSocket(), TcpIpSocketHandler.getName());
            case BLE:
                return new Ble(BleHandler.getGatt(), BleHandler.getName());
            case SOUND_MODEM:
                return SettingsWrapper.isFreeDvSoundModemModulation(sharedPreferences) ? new SoundModemRaw(context) : new SoundModemFsk(context);
            case LOOPBACK:
            default:
                return new Loopback();
        }
    }
}
