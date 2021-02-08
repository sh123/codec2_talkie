![CI](https://github.com/sh123/codec2_talkie/workflows/CI/badge.svg)

# Table of contents
- [Introduction](#introduction)
- [Requirements](#requirements)
- [Features](#features)
- [Suitable radios and modems](#suitable-radios-and-modems)
- [Related projects](#related-projects)
- [FAQ](#faq)
- [KISS command extensions](#kiss-command-extensions)
- [Planned features](#todo)

# Introduction
![alt text](images/diagram.png)

![alt text](images/screenshot.png)
![alt text](images/screenshot_settings.png)

This minimalistic Android application is a Walkie-Talkie style digital voice frontend for your radio, which uses open source [Codec2](https://github.com/drowe67/codec2) for speech audio frame encoding/decoding. 

It is mainly intended for DV experimentation with ultra low cost (under 10$) radio modems (such as LoRa), but could also be used with custom modems + external transceivers or as a test harness for Codec2 frames generation and their playback.

It connects to your radio KISS Bluetooth/USB modem, records speech from the phone microphone on transmit, encodes audio into codec2 format, encapsulates into KISS frames and sends to your modem. 

On receive, modem sends KISS packets to the phone with codec2 speech, application decodes codec2 frames and plays them through phone speaker.

It does not deal with radio management, modulation, etc, it is up to your modem and radio, it could be just AFSK1200, GMSK 9600, LoRa, FSK, FreeDV or any other modulation scheme. Radio just needs to expose KISS Bluetooth interface for speech frames. 

# Requirements
- Android 6.0 (API 23) or higher
  - Application could also be used with your Android network radio, such as Inrico TM-7, apk just needs to be installed over USB, see [Discussion](https://github.com/sh123/codec2_talkie/issues/4)
- Modem, radio module or transceiver which supports [KISS protocol](https://en.wikipedia.org/wiki/KISS_(TNC)) or can process raw Codec2 audio frames over serial Bluetooth or USB

# Features
- **PTT UI button**, push and talk, Codec2 speech frames will be transmitted to the modem
- **PTT hardware button**, `KEYCODE_TV_DATA SERVICE` (230 key code) hardware button is used for PTT (used on some Android network radios)
- **USB serial connectivity** (default 115200 bps, 8 data bits, 1 stop bit, no parity), just select this app after connecting to USB and it will use given connection, baud rate could be changed from Preferences
- **Bluetooth connectivity** on startup, lists paired devices, so you can choose your modem and connect, you need to pair with your Bluetooth device first from Android Bluetooth Settings, default Bluetooth device could be set from Preferences
- **Voice codec2 mode selection**, which allows you to select various codec2 modes from 450 up to 3200 bps on the fly, sender and receiver should agree on the codec mode and use the same codec mode on both ends as codec2 mode negotiation between clients is not implemented at the moment
- **Codec2 loopback mode**, which records and plays your recorded voice back to test and evaluate different Codec2 modes and speech quality, could be enabled or disabled from Preferences, this mode is activated if no USB or Bluetooth connection were made
- **Voice level VU indicator**, display auido level on transmit or receive
- **S-meter**, displayed only when KISS extensions are enabled and modem is able to send signal level information
- **Parrot mode**, received voice will be digirepated in addition to playback through the speaker
- **KISS buffered mode**, non-real time, playback will start only after all speech is received, use when modem bit rate is lower than codec2 bit rate to avoid gaps during playback at the cost of longer receiving delay before playback
- **Preferences**, allow to modify default parameters
  - **Codec2**
    - Set Codec2 mode/speed from 450 up to 3200 bps
    - Enable/disable loopback test mode
  - **KISS**
     - Enable/Disable KISS, when disabled raw codec2 audio frames will be transmitted
     - Enable/Disable parrot (digirepeater) mode
     - Enable/Disable KISS non-real time buffered playback mode
     - Enable/Disable KISS extensions for radio module control and signal levels (modem must support them to work correctly!)
       - Set radio parameters (frequency, bandwidth, spreading factor, coding rate, power, sync word, crc checksum enable/disable)
  - **TNC parameters**
    - Change default baud rate for USB port
    - Set default Bluetooth device for automatic connectivity on startup

# Suitable radios and modems
- Tested, works:
  - (BT) LoRa modem 450/700 bps codec2 modes tested at 1300 bps and 900 bps LoRa speeds: https://github.com/sh123/esp32_loraprs
  - (BT) custom AFSK1200 LibAPRS based modem with increased TXTail parameter and Baofeng handheld transceiver: 450 works fine, 700 works with small gaps, probably LibAPRS needs some tweaks: https://github.com/markqvist/LibAPRS
  - (USB) HC-12 modules: works, but application needs to use lower USB serial bit rate (change from Preferences), because module RF bit rate is hardwired to its serial bit rate, also module needs to be preconfigured with AT commands first
- Tested, works, but not too stable, probably needs TXTail tuning:
  - (USB) AFSK1200 PicoAPRS: http://www.db1nto.de/index_en.html
  - (BT) AFSK1200/GMSK9600 Kenwood TH-D74A: https://dl1gkk.com/kenwood-th-d74-bluetooth-packet-radio-setup/
- Could work, needs testing:
  - (USB) AFSK1200 MicroModem: https://unsigned.io/micromodem
  - (BT/USB) AFSK1200/GMSK9600 Mobilinkd TNC3: https://store.mobilinkd.com/products/mobilinkd-tnc3
  - (USB) nRF2401L01 2.4 GHz: https://github.com/sh123/nrf24l01_arduino_kiss_modem

# Related Projects
- Source code is integrated into this project for easier building and customization:
  - Codec2 codec: https://github.com/drowe67/codec2
  - Android Codec2 wrapper code: https://github.com/UstadMobile/Codec2-Android
- Fetched with gradle as dependency:
  - Android USB serial: https://github.com/mik3y/usb-serial-for-android
- Other interesting projects:
  - ESP32 LoRa APRS modem (used with this application for testing): https://github.com/sh123/esp32_loraprs
  - iOS Codec2 wrapper: https://github.com/Beartooth/codec2-ios

# FAQ
- **Gaps in received audio**, indicator often changes between RX/IDLE on the receiver side when sender and recipient are close by then
  - most likely you do not have enough bandwidth, use higher throughput modem mode, for LoRa modems you need approximately 100-150% of data rate for the given codec2 bitrate, AFSK1200 modems should work at 450 bps codec2 mode, 700 bps might work, but not on all modems, higher speed codec2 modes won't work on AFSK1200 for real time, need to enable KISS non-realtime buffering mode
  - modem goes into RX state too fast after sending first packet, increase TxTail parameter for your modem if it does not handle corresponding KISS command, so it will stay longer in TX before switching to RX
  - if you are using LibAPRS based modem, set TxTail and TxPreamble programatically by using `APRS_setPreamble` and `APRS_setTail` methods as it does not seem to set these values from `CMD_TXDELAY` and `CMD_TXTAIL` KISS commands
  - enable KISS buffering mode in Preferences, this will help with gaps at the cost of larger delay on receive before playback
- **Receiving audio on PC/Raspberry**
  - For raw audio frames `sudo cat /dev/ttyUSB0 | c2dec 700 - - | play -t raw -r 8000 -e signed-integer -b 16 -c 1 -`
  - For KISS encapsulated audio frames command above could be used, but instead of `cat` use https://pypi.org/project/kiss/

# KISS command extensions
KISS command extensions are used for radio module control and signal report events on port 0, command for radio control is defined as 6 (KISS SetHardware) and signal report command as 7. Radio modules/modems can implement these commands, so they will be controllable from the application and application will be able to show signal levels on S-meter.

Payloads for commands are sent and expected as big endian and defined as:
```
  // KISS SetHardware 6
  struct ControlCommand {
    uint32_t freq;
    uint32_t bw;
    uint16_t sf;
    uint16_t cr;
    uint16_t pwr;
    uint16_t sync;
    uint8_t crc;
  } __attribute__((packed));
  
  // KISS command 7
  struct SignalLevelEvent {
    int16_t rssi;
    int16_t snr;  // snr * 100
  } __attribute__((packed));
```

# TODO
- QSO log and non real time voicemail style communcation, where incoming transmissions are recorded, stored and could be played back later if recipient cannot reply immediately in real time
- Investigate support for other non-KISS frame formats and protocols, switcheable from the UI
  - AX.25 packets over existing KISS, voice over AX.25 (VoAX.25).
  - [M17 Project](https://m17project.org) protocol support
- Support for non-KISS modems control if needed
  - HC-12 module control by using AT commands
