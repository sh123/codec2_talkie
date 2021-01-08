![CI](https://github.com/sh123/codec2_talkie/workflows/CI/badge.svg)

# Android Codec2 Walkie-Talkie
Minimalistic Android KISS Bluetooth/USB modem client for Amateur Radio DV (digital voice) communication by using open source [Codec2](https://github.com/drowe67/codec2).

![alt text](images/screenshot.png)

# Introduction
This minimalistic Android application is a digital voice frontend for your radio. It connects to your radio KISS Bluetooth/USB modem, sends and receives Codec2 audio frames, which are encapsulated inside KISS frames. It does not deal with radio management, modulation, etc, it is up to your modem and radio, it could be just AFSK1200, GMSK 9600, LoRa, FSK, FreeDV or any other modulation scheme. Radio just needs to expose KISS Bluetooth interface for speech frames.

# Requirements
- Android 6.0 (API 23) or higher
  - Application could also be used with your Android network radio, such as Inrico TM-7, apk just needs to be installed over USB
- Modem, radio module or transceiver which supports [KISS protocol](https://en.wikipedia.org/wiki/KISS_(TNC)) over Bluetooth or USB 
  - KISS support is not 100% necesssary for experiments, usually radio module needs to decide on protocol, but KISS frames could be sent over aether, there are plans to add support for other frame structures or raw audio frames

# Features
- **PTT UI button**, push and talk, Codec2 speech frames will be transmitted to the modem
- **PTT hardware button**, `KEYCODE_TV_DATA SERVICE` (230 key code) hardware button is used for PTT (used on some Android network radios)
- **USB serial connectivity** (115200 bps, 8 data bits, 1 stop bit, no parity), just select this app after connecting to USB and it will use given connection
- **Bluetooth connectivity** on startup, lists paired devices, so you can choose your modem and connect, you need to pair with your Bluetooth device first from Android Bluetooth Settings
- **Voice codec2 mode selection**, which allows you to select various codec2 modes from 450 up to 3200 bps on the fly, sender and receiver should agree on the codec mode and use the same codec mode on both ends
- **Codec2 loopback mode**, which records and plays your recorded voice back to test and evaluate different Codec2 modes and speech quality

# Suitable radios and modems
- Tested, works:
  - (BT) LoRa modem 450/700 bps codec2 modes tested at 1300 bps and 900 bps LoRa speeds: https://github.com/sh123/esp32_loraprs
  - (BT) custom AFSK1200 LibAPRS based modem with increased TXTail parameter and Baofeng handheld transceiver: 450 works fine, 700 works with small gaps, probably LibAPRS needs some tweaks: https://github.com/markqvist/LibAPRS
  - (USB) HC-12 modules: works, but application needs to use lower USB serial bit rate (separate builds for now), because module RF bit rate is hardwired to its serial bit rate, also module needs to be preconfigured with AT commands first
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
  - iOS Codec2 wrapper: https://github.com/Beartooth/codec2-ios

# FAQ
- Gaps in received audio, indicator often changes between RX/IDLE on the receiver side when sender and recipient are close by then 
  - most likely you do not have enough bandwidth, use higher throughput modem mode, for LoRa modems you need approximately 170% of data rate for the given codec2 bitrate, AFSK1200 modems should work at 450 bps codec2 mode, 700 bps might work, but not on all modems, higher speed codec2 modes won't work on AFSK1200
  - modem goes into RX state too fast after sending first packet, increase TxTail parameter for your modem if it does not handle corresponding KISS command, so it will stay longer in TX before switching to RX
    - if you are using LibAPRS based modem, set TxTail and TxPreamble programatically by using `APRS_setPreamble` and `APRS_setTail` methods as it does not seem to set these values from `CMD_TXDELAY` and `CMD_TXTAIL` KISS commands
  
# TODO
- Parrot mode, so speech coming from aether will be transmitted back (testing or digirepeating)
- QSO log, voicemail style recording of incoming speech so that incoming transmissions are not missed
- Separate settings to avoid repeated operations
  - Override default USB serial parameters
  - Default Bluetooth device name to connect upon startup
  - Default codec2 bitrate on startup
  - Settings for parrot mode
  - Settings for QSO log
- Modem profiles, so different modems could be controlled from the UI with KISS command extensions, so that user can change frequency, modulation scheme or other modem parameters on the fly from the user interface
- Investigate support for other non-KISS frame formats
