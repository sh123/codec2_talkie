package com.radio.codec2talkie.protocol.aprs;

public class AprsDataType {

    public enum DataType {
        UNKNOWN,
        MIC_E,
        MESSAGE,
        POSITION_WITH_TIMESTAMP_MSG,
        POSITION_WITHOUT_TIMESTAMP_MSG,
        POSITION_WITH_TIMESTAMP_NO_MSG,
        POSITION_WITHOUT_TIMESTAMP_NO_MSG
    }

    private final DataType _dataType;
    private final char _ident;

    public AprsDataType(char ident) {
        _ident = ident;
        _dataType = getDataTypeFromIdent(_ident);
    }

    public AprsDataType(DataType dataType) {
        _dataType = dataType;
        _ident = getIdentFromDataType(dataType);
    }

    public char getIdent() {
        return _ident;
    }

    public DataType getDataType() {
        return _dataType;
    }

    private DataType getDataTypeFromIdent(char ident) {
        if (ident == '`') {
            return DataType.MIC_E;
        } else if (ident == ':') {
            return DataType.MESSAGE;
        } else if (ident == '=') {
            return DataType.POSITION_WITHOUT_TIMESTAMP_MSG;
        } else if (ident == '!') {
            return DataType.POSITION_WITHOUT_TIMESTAMP_NO_MSG;
        } else if (ident == '@') {
            return DataType.POSITION_WITH_TIMESTAMP_MSG;
        } else if (ident == '/') {
            return DataType.POSITION_WITH_TIMESTAMP_NO_MSG;
        } else {
            return DataType.UNKNOWN;
        }
    }

    private char getIdentFromDataType(DataType dataType) {
        if (dataType == DataType.MIC_E) {
            return '`';
        } else if (dataType == DataType.MESSAGE) {
            return ':';
        } else if (dataType == DataType.POSITION_WITHOUT_TIMESTAMP_MSG) {
            return '=';
        } else if (dataType == DataType.POSITION_WITHOUT_TIMESTAMP_NO_MSG) {
            return '!';
        } else if (dataType == DataType.POSITION_WITH_TIMESTAMP_MSG) {
            return '@';
        } else if (dataType == DataType.POSITION_WITH_TIMESTAMP_NO_MSG) {
            return '/';
        } else {
            return 0;
        }
    }
}
