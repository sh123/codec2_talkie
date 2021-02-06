package com.radio.codec2talkie.protocol;

public class ProtocolFactory {

    public enum ProtocolType {
        RAW("RAW"),
        KISS("KISS"),
        KISS_BUFFERED("KISS BUFFERED"),
        KISS_PARROT("KISS PARROT");

        private String _name;

        ProtocolType(String name) {
            _name = name;
        }

        @Override
        public String toString() {
            return _name;
        }
    };

    public static Protocol create(ProtocolType protocolType) {
        switch (protocolType) {
            case KISS:
                return new Kiss();
            case KISS_BUFFERED:
                return new KissBuffered();
            case KISS_PARROT:
                return new KissParrot();
            case RAW:
            default:
                return new Raw();
        }
    }
}
