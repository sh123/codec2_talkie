package com.radio.codec2talkie.settings;

public final class PreferenceKeys {
    public static String PORTS_USB_SERIAL_SPEED = "ports_usb_serial_speed";
    public static String PORTS_USB_DATA_BITS = "ports_usb_data_bits";
    public static String PORTS_USB_STOP_BITS = "ports_usb_stop_bits";
    public static String PORTS_USB_PARITY = "ports_usb_parity";
    public static String PORTS_USB_DTR = "ports_usb_dtr";
    public static String PORTS_USB_RTS = "ports_usb_rts";
    public static String PORTS_BT_CLIENT_NAME = "ports_bt_client_name";
    public static String PORTS_BT_BLE_ENABLED = "ports_bt_ble_enable";
    public static String PORTS_TCP_IP_ENABLED = "ports_tcp_ip_enable";
    public static String PORTS_TCP_IP_ADDRESS = "ports_tcp_ip_address";
    public static String PORTS_TCP_IP_PORT = "ports_tcp_ip_port";
    public static String PORTS_TCP_IP_RETRY_COUNT = "ports_tcp_ip_retry_count";
    public static String PORTS_TCP_IP_RETRY_DELAY = "ports_tcp_ip_retry_delay";

    public static String CODEC2_MODE = "codec2_mode";
    public static String CODEC2_TEST_MODE = "codec2_test_mode";
    public static String CODEC2_RECORDING_ENABLED = "codec2_recording_enabled";
    public static String CODEC2_TX_FRAME_MAX_SIZE = "codec2_tx_frame_max_size";

    public static String KISS_ENABLED = "kiss_enable";
    public static String KISS_BUFFERED_ENABLED = "kiss_buffered_enable";
    public static String KISS_PARROT = "kiss_parrot_enable";

    public static String KISS_BASIC_P = "kiss_basic_persistence";
    public static String KISS_BASIC_SLOT_TIME = "kiss_basic_slot_time";
    public static String KISS_BASIC_TX_DELAY = "kiss_basic_tx_delay";
    public static String KISS_BASIC_TX_TAIL = "kiss_basic_tx_tail";
    public static String KISS_SCRAMBLING_ENABLED = "kiss_enable_scrambler";
    public static String KISS_SCRAMBLER_KEY = "kiss_scrambler_key";
    public static String KISS_SCRAMBLER_ITERATIONS = "kiss_scrambler_iterations";

    public static String KISS_EXTENSIONS_ENABLED = "kiss_extensions_enable";
    public static String KISS_EXTENSIONS_RADIO_FREQUENCY = "kiss_extension_radio_frequency";
    public static String KISS_EXTENSIONS_RADIO_BANDWIDTH = "kiss_extension_radio_bandwidth";
    public static String KISS_EXTENSIONS_RADIO_POWER = "kiss_extension_radio_power";
    public static String KISS_EXTENSIONS_RADIO_SF = "kiss_extension_radio_sf";
    public static String KISS_EXTENSIONS_RADIO_CR = "kiss_extension_radio_cr";
    public static String KISS_EXTENSIONS_RADIO_SYNC = "kiss_extension_radio_sync";
    public static String KISS_EXTENSIONS_RADIO_CRC = "kiss_extension_radio_crc";

    public static String KISS_EXTENSIONS_ACTION_REBOOT_REQUESTED = "com.radio.codec2talkie.MODEM_REBOOT";

    public static String APP_VOLUME_PTT = "app_volume_ptt";
    public static String APP_KEEP_SCREEN_ON = "app_keep_screen_on";
    public static String APP_AUDIO_OUTPUT_SPEAKER = "app_audio_output_speaker";
    public static String APP_AUDIO_INPUT_VOICE_COMMUNICATION = "app_audio_input_voice_communication";

    public static String APRS_ENABLED = "aprs_enable";
    public static String APRS_CALLSIGN = "aprs_callsign";
    public static String APRS_SSID = "aprs_ssid";
    public static String APRS_DIGIPATH = "aprs_digipath";
    public static String APRS_SYMBOL = "aprs_symbol";
    public static String APRS_COMMENT = "aprs_comment";
    public static String APRS_LOCATION_COMPRESSED = "aprs_location_compressed";
    public static String APRS_LOCATION_SOURCE = "aprs_location_source";
    public static String APRS_LOCATION_SOURCE_GPS_UPDATE_TIME = "aprs_location_source_gps_update_time";
    public static String APRS_LOCATION_SOURCE_GPS_UPDATE_DISTANCE = "aprs_location_source_gps_update_distance";
    public static String APRS_LOCATION_SOURCE_MANUAL_LATE = "aprs_location_source_manual_lat";
    public static String APRS_LOCATION_SOURCE_MANUAL_LON = "aprs_location_source_manual_lon";
    public static String APRS_LOCATION_SOURCE_MANUAL_AUTO_SEND = "aprs_location_source_manual_auto_send";
    public static String APRS_LOCATION_SOURCE_MANUAL_UPDATE_TIME = "aprs_location_source_manual_update_time";
    public static String APRS_PRIVACY_POSITION_AMBIGUITY = "aprs_privacy_position_ambiguity";
    public static String APRS_PRIVACY_SPEED_ENABLED = "aprs_privacy_speed_enable";
    public static String APRS_PRIVACY_ALTITUDE_ENABLED = "aprs_privacy_altitude_enable";
}