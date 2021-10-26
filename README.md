![CI](https://github.com/sh123/codec2_talkie/workflows/CI/badge.svg)

# Introduction
**Turn your Android phone into real VHF/UHF Walkie-Talkie (requires additional digital radio modem).**

For more information visit project [Wiki](https://github.com/sh123/codec2_talkie/wiki)

![alt text](images/diagram.png)

![alt text](images/screenshot.png)
![alt text](images/screenshot_settings.png)

This minimalistic Android application is a Walkie-Talkie style digital voice frontend for your radio modem, which uses open source [Codec2](https://github.com/drowe67/codec2) for speech audio frame encoding/decoding. 

It is mainly intended for DV experimentation with ultra low cost 3-8$ radio modems, such as LoRa and 15-25$ esp32 board flavours with built-in LoRa module: T-Beam,
LoPy, TTGO, Heltec and others, but could also be used with custom hardware of software (Direwolf) modems + external transceivers or as a test harness for Codec2 frames generation and their playback.

![alt text](images/tracker.jpg)

Application connects to your radio KISS Bluetooth/USB/TCPIP modem, records speech from the phone microphone on transmit, encodes audio into Codec2 format, encapsulates into KISS frames and sends to your modem. 
On receive, modem sends KISS packets to the phone with Codec2 speech, application decodes Codec2 frames and plays them through phone speaker.

It does not deal with radio management, modulation, etc, it is up to your modem and radio, it could be just AFSK1200, GMSK 9600, LoRa, FSK, FreeDV or any other modulation scheme. Radio just needs to expose KISS Bluetooth/USB/TCPIP interface for speech frames.

# Requirements
- Android 7.0 (API 24) or higher
  - Application could also be used with your Android network radio, such as Inrico TM-7, apk just needs to be installed over USB, see [Discussion](https://github.com/sh123/codec2_talkie/issues/4)
- Android 5.0, 5.1, 6.0 (API 21, 22, 23)
  - Separate apk package is released with "legacy" suffix from legacy branch
- Modem, radio module or transceiver which supports [KISS protocol](https://en.wikipedia.org/wiki/KISS_(TNC)) or can process KISS or raw Codec2 audio frames over serial Bluetooth, USB or TCP/IP
