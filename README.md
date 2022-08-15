![CI](https://github.com/sh123/codec2_talkie/workflows/CI/badge.svg) 
![APK](https://img.shields.io/endpoint?url=https://apt.izzysoft.de/fdroid/api/v1/shield/com.radio.codec2talkie)

# Introduction
**Turn your Android phone into real Amateur Radio HF/VHF/UHF APRS enabled Codec2 DV (digital voice) and/or FreeDV handheld transceiver.**

**Requires additional hardware (e.g. AFSK/LoRa), software (e.g. Direwolf) radio modem or analog transceiver with USB audio + VOX/USB CAT PTT control, such as MCHF or ICOM**

For more information visit project [Wiki](https://github.com/sh123/codec2_talkie/wiki)

![alt text](images/diagram.png)

![alt text](images/screenshot.png)
![alt text](images/screenshot_settings.png)

# Short Description
What you can do with this app:
- Use it with your KISS Bluetooth/BLE/USB/TCPIP hardware modem, such as LoRa/FSK/AFSK/etc, control its parameters by using "set hardware" KISS command
- Use it with KISS software modem using TCPIP, such as Direwolf
- Use it with your HF/VHF/UHF transceiver as a sound modem
- Use your phone as a software sound modem by using external USB OTG audio adapter (voice + data) or built-in phone speaker and mic (only data)
- Send and reveive APRS position reports over FSK 300 (HF), AFSK1200 (VHF), FreeDV OFDM (HF)
- Send and receive APRS messages
- Send and receive raw Codec2 speech frames over KISS or inside APRS/AX.25 UI frames
- Use your phone for FreeDV protocol voice communication together with HF transceiver, which supports USB OTG audio
- Control your external transceiver PTT by using USB serial CAT (or VOX if CAT PTT is not supported)

# Requirements
- Android 7.0 (API 24) or higher
  - Application could also be used with your Android network radio, such as Inrico TM-7, apk just needs to be installed over USB, see [Discussion](https://github.com/sh123/codec2_talkie/issues/4)
- Android 5.0, 5.1, 6.0 (API 21, 22, 23)
  - Separate apk package is released with "legacy" suffix from legacy branch
- Modem, radio module or transceiver which supports [KISS protocol](https://en.wikipedia.org/wiki/KISS_(TNC)) or can process KISS or raw Codec2 audio frames over serial Bluetooth, BLE, USB or TCP/IP
- Analog transceiver with built-in or external USB audio adapter and VOX or USB CAT PTT control (such as MCHF or iCom IC-7x00 series)

# Dependencies
- Source code is integrated into this project for easier building and customization:
  - Codec2 codec: https://github.com/drowe67/codec2
  - Android Codec2 wrapper code: https://github.com/UstadMobile/Codec2-Android
- Fetched with gradle as dependency:
  - Android USB serial: https://github.com/mik3y/usb-serial-for-android

# Other similar or related projects
- ESP32 LoRa APRS modem (used with this application for testing): https://github.com/sh123/esp32_loraprs
- Version adopted for M17 protocol usage: https://github.com/mobilinkd/m17-kiss-ht
- iOS Codec2 wrapper: https://github.com/Beartooth/codec2-ios
- Minimal Arduino LoRa KISS modem: https://github.com/sh123/lora_arduino_kiss_modem
- Minimal Arduino NRF24 KISS modem: https://github.com/sh123/nrf24l01_arduino_kiss_modem
- Other interesting projects:
  - LoRa mesh text GPS communicator: https://github.com/meshtastic/Meshtastic-device
