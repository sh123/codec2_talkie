package com.radio.codec2talkie.settings;

public final class PreferenceKeys {
    public static String PORTS_TYPE="ports_type";

    public static String PORTS_USB_SERIAL_NUM = "ports_usb_serial_num";
    public static String PORTS_USB_SERIAL_SPEED = "ports_usb_serial_speed";
    public static String PORTS_USB_DATA_BITS = "ports_usb_data_bits";
    public static String PORTS_USB_STOP_BITS = "ports_usb_stop_bits";
    public static String PORTS_USB_PARITY = "ports_usb_parity";
    public static String PORTS_USB_DTR = "ports_usb_dtr";
    public static String PORTS_USB_RTS = "ports_usb_rts";
    public static String PORTS_BT_CLIENT_NAME = "ports_bt_client_name";

    public static String PORTS_TCP_IP_ADDRESS = "ports_tcp_ip_address";
    public static String PORTS_TCP_IP_PORT = "ports_tcp_ip_port";
    public static String PORTS_TCP_IP_RETRY_COUNT = "ports_tcp_ip_retry_count";
    public static String PORTS_TCP_IP_RETRY_DELAY = "ports_tcp_ip_retry_delay";

    public static String PORTS_SOUND_MODEM_TYPE = "ports_sound_modem_type";
    public static String PORTS_SOUND_MODEM_AUDIO_SOURCE = "ports_sound_modem_audio_source";
    public static String PORTS_SOUND_MODEM_AUDIO_DESTINATION = "ports_sound_modem_audio_destination";
    public static String PORTS_SOUND_MODEM_PREAMBLE = "ports_sound_modem_preamble";
    public static String PORTS_SOUND_MODEM_DISABLE_RX = "ports_sound_modem_disable_rx";
    public static String PORTS_SOUND_MODEM_RIG = "ports_sound_modem_rig";
    public static String PORTS_SOUND_MODEM_GAIN ="ports_sound_modem_gain";
    public static String PORTS_SOUND_MODEM_PTT_OFF_DELAY_MS = "ports_sound_modem_ptt_off_delay_ms";
    public static String PORTS_SOUND_MODEM_LOOPBACK="ports_sound_modem_loopback";
    public static String PORTS_SOUND_MODEM_FREEDV_ENABLE_SQUELCH="ports_sound_modem_freedv_enable_squelch";
    public static String PORTS_SOUND_MODEM_FREEDV_SQUELCH_SNR="ports_sound_modem_freedv_squelch_snr";
    public static String PORTS_SOUND_MODEM_FREEDV_DATA_MODE="ports_sound_modem_freedv_data_mode";

    public static String CUSTOM_PREFIX_ENABLED = "custom_prefix_enabled";
    public static String CUSTOM_PREFIX = "custom_prefix";

    public static String CODEC_TYPE = "codec_type";
    public static String CODEC2_RECORDING_ENABLED = "codec2_recording_enabled";

    public static String CODEC2_MODE = "codec2_mode";
    public static String CODEC2_TX_FRAME_MAX_SIZE = "codec2_tx_frame_max_size";

    public static String OPUS_BIT_RATE = "opus_bit_rate";
    public static String OPUS_FRAME_SIZE = "opus_frame_size";
    public static String OPUS_COMPLEXITY = "opus_complexity";

    public static String KISS_ENABLED = "kiss_enable";
    public static String KISS_BUFFERED_ENABLED = "kiss_buffered_enable";
    public static String KISS_PARROT = "kiss_parrot_enable";

    public static String KISS_BASIC_P = "kiss_basic_persistence";
    public static String KISS_BASIC_SLOT_TIME = "kiss_basic_slot_time";
    public static String KISS_BASIC_TX_DELAY = "kiss_basic_tx_delay";
    public static String KISS_BASIC_TX_TAIL = "kiss_basic_tx_tail";

    public static String KISS_CIPHER_TYPE="kiss_cipher_type";
    public static String KISS_CIPHER_KEY="kiss_cipher_key";

    public static String KISS_EXTENSIONS_ENABLED = "kiss_extensions_enable";
    public static String KISS_EXTENSIONS_RADIO_MOD = "kiss_extension_radio_mod";
    public static String KISS_EXTENSIONS_RADIO_SPLIT_FREQ = "kiss_extension_radio_split_freq";
    public static String KISS_EXTENSIONS_RADIO_FREQUENCY = "kiss_extension_radio_frequency";
    public static String KISS_EXTENSIONS_RADIO_FREQUENCY_TX = "kiss_extension_radio_frequency_tx";
    public static String KISS_EXTENSIONS_RADIO_BANDWIDTH = "kiss_extension_radio_bandwidth";
    public static String KISS_EXTENSIONS_RADIO_POWER = "kiss_extension_radio_power";
    public static String KISS_EXTENSIONS_RADIO_SF = "kiss_extension_radio_sf";
    public static String KISS_EXTENSIONS_RADIO_CR = "kiss_extension_radio_cr";
    public static String KISS_EXTENSIONS_RADIO_SYNC = "kiss_extension_radio_sync";
    public static String KISS_EXTENSIONS_RADIO_CRC = "kiss_extension_radio_crc";
    public static String KISS_EXTENSIONS_RADIO_FSK_BIT_RATE = "kiss_extension_radio_fsk_bit_rate";
    public static String KISS_EXTENSIONS_RADIO_FSK_FREQ_DEV = "kiss_extension_radio_fsk_freq_dev";
    public static String KISS_EXTENSIONS_RADIO_FSK_RX_BW = "kiss_extension_radio_fsk_rx_bw";

    public static String KISS_EXTENSIONS_ACTION_REBOOT_REQUESTED = "com.radio.codec2talkie.MODEM_REBOOT";

    public static String APP_VOLUME_PTT = "app_volume_ptt";
    public static String APP_KEEP_SCREEN_ON = "app_keep_screen_on";
    public static String APP_NO_LOCK = "app_no_lock";
    public static String APP_TURN_SCREEN_ON = "app_turn_screen_on";
    public static String APP_NOTIFICATIONS_VOICE = "app_notifications_voice";
    public static String APP_NO_CPU_SLEEP = "app_no_cpu_sleep";
    public static String APP_AUDIO_SOURCE = "app_audio_source";
    public static String APP_AUDIO_DESTINATION = "app_audio_destination";

    public static String AX25_VOAX25_ENABLE = "aprs_voax25_enable";
    public static String AX25_TEXT_PACKETS_ENABLE = "aprs_text_packets_enable";
    public static String AX25_CALLSIGN = "aprs_callsign";
    public static String AX25_SSID = "aprs_ssid";
    public static String AX25_DIGIPATH = "aprs_digipath";
    public static String AX25_DIGIREPEATER_ENABLED = "aprs_digirepeater_enable";

    public static String APRS_ENABLED = "aprs_enable";
    public static String APRS_SYMBOL = "aprs_symbol";
    public static String APRS_COMMENT = "aprs_comment";
    public static String APRS_LOCATION_PACKET_FORMAT = "aprs_location_packet_format";
    public static String APRS_LOCATION_SOURCE = "aprs_location_source";
    public static String APRS_LOCATION_SOURCE_GPS_UPDATE_TIME = "aprs_location_source_gps_update_time";
    public static String APRS_LOCATION_MIC_E_MESSAGE_TYPE = "aprs_location_mice_message_type";
    public static String APRS_LOCATION_MIC_E_DIGIPATH = "aprs_location_mice_digipath";
    public static String APRS_LOCATION_SOURCE_GPS_UPDATE_DISTANCE = "aprs_location_source_gps_update_distance";
    public static String APRS_LOCATION_SOURCE_MANUAL_LAT = "aprs_location_source_manual_lat";
    public static String APRS_LOCATION_SOURCE_MANUAL_LON = "aprs_location_source_manual_lon";
    public static String APRS_LOCATION_SOURCE_MANUAL_UPDATE_INTERVAL_MINUTES = "aprs_location_source_manual_update_time";
    public static String APRS_LOCATION_SOURCE_MANUAL_AUTO_SEND = "aprs_location_source_manual_auto_send";
    public static String APRS_PRIVACY_POSITION_AMBIGUITY = "aprs_privacy_position_ambiguity";
    public static String APRS_PRIVACY_SPEED_ENABLED = "aprs_privacy_speed_enable";
    public static String APRS_PRIVACY_ALTITUDE_ENABLED = "aprs_privacy_altitude_enable";

    public static String APRS_LOCATION_SOURCE_SMART_FAST_SPEED = "aprs_location_source_smart_fast_speed";
    public static String APRS_LOCATION_SOURCE_SMART_FAST_RATE = "aprs_location_source_smart_fast_rate";
    public static String APRS_LOCATION_SOURCE_SMART_SLOW_SPEED = "aprs_location_source_smart_slow_speed";
    public static String APRS_LOCATION_SOURCE_SMART_SLOW_RATE = "aprs_location_source_smart_slow_rate";
    public static String APRS_LOCATION_SOURCE_SMART_MIN_TURN_TIME = "aprs_location_source_smart_min_turn_time";
    public static String APRS_LOCATION_SOURCE_SMART_MIN_TURN_ANGLE = "aprs_location_source_smart_min_turn_angle";
    public static String APRS_LOCATION_SOURCE_SMART_TURN_SLOPE = "aprs_location_source_smart_turn_slope";

    public static String APRS_IS_ENABLE="aprs_is_enable";
    public static String APRS_IS_CODE = "aprs_is_code";
    public static String APRS_IS_TCPIP_SERVER = "aprs_is_tcpip_server";
    public static String APRS_IS_TCPIP_SERVER_PORT = "aprs_is_tcpip_server_port";
    public static String APRS_IS_ENABLE_RX_GATE = "aprs_is_enable_rx_gate";
    public static String APRS_IS_ENABLE_TX_GATE = "aprs_is_enable_tx_gate";
    public static String APRS_IS_ENABLE_SELF = "aprs_is_enable_self";
    public static String APRS_IS_RADIUS = "aprs_is_radius";
    public static String APRS_IS_FILTER = "aprs_is_filter";
}