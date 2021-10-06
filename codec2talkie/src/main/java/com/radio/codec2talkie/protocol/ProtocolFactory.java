package com.radio.codec2talkie.protocol;

public class ProtocolFactory {

    public enum ProtocolType {
        RAW("RAW"),
        KISS("KISS"),
        KISS_BUFFERED("KISS BUFFERED"),
        KISS_PARROT("KISS PARROT");

        private final String _name;

        ProtocolType(String name) {
            _name = name;
        }

        @Override
        public String toString() {
            return _name;
        }
    };

    public static Protocol create(ProtocolType protocolType, boolean voicemailEnabled) {
        Protocol proto;
        switch (protocolType) {
            case KISS:
                proto = new Kiss();
                break;
            case KISS_BUFFERED:
                proto = new KissBuffered();
                break;
            case KISS_PARROT:
                proto = new KissParrot();
                break;
            case RAW:
            default:
                proto = new Raw();
                break;
        }

        if (voicemailEnabled) {
            proto = new VoicemailProxy(proto);
        }

        return proto;
    }
}
