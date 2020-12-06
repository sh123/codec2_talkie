# Android Codec2 Walkie-Talkie
Android KISS Bluetooth modem client for Amateur Radio digital voice communication by using open source Codec2.

![alt text](images/screenshot.jpg)

# Introduction
This Android application is a digital voice frontend for your radio. It connects to your radio KISS Bluetooth modem, sends and receives Codec2 audio frames, which are encapsulated inside KISS frames. It does not deal with radio management, modulation, etc, it is up to your modem and radio, it could be just AFSK1200, GMSK 9600, LoRa, FSK or any other radio. Radio just needs to expose KISS Bluetooth interface.

# Requirements
- Android 6.0 (API 23) or higher.

# Features
- PTT button, push and talk, encoded speech will be transmitted to the modem
- Bluetooth connectivity on startup, lists paired devices, so you can choose your modem and connect
- Voice codec2 mode selection, which allows you to select various codec2 modes from 450 up to 3200 bps.
- Codec2 loopback mode, which records and plays your recorded voice back to test and evaluate different modes

# Suitable radios
- AFSK1200 MicroModem: https://unsigned.io/micromodem
- LoRa modem: https://github.com/sh123/esp32_loraprs

# Related Projects
- Codec2 codec: https://github.com/drowe67/codec2
- Android Codec2 wrapper code: https://github.com/UstadMobile/Codec2-Android
