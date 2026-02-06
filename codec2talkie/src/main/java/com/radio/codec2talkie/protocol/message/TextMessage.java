package com.radio.codec2talkie.protocol.message;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.radio.codec2talkie.settings.PreferenceKeys;
import com.radio.codec2talkie.storage.message.MessageItem;

import java.util.Locale;

public class TextMessage {
    public String src;
    public String dst;
    public String digipath;
    public String text;
    public String ackId;

    public MessageItem toMessageItem(boolean isTransmit) {
        MessageItem messageItem = new MessageItem();
        messageItem.setGroupId(buildGroupId());
        messageItem.setTimestampEpoch(System.currentTimeMillis());
        messageItem.setIsTransmit(isTransmit);
        messageItem.setSrcCallsign(this.src);
        messageItem.setDstCallsign(this.dst);
        messageItem.setMessage(this.text);
        messageItem.setAckId(this.ackId);
        messageItem.setIsAcknowledged(false);
        messageItem.setRetryCnt(0);
        return messageItem;
    }

    public String buildGroupId() {
        // null checks
        if (src == null && dst == null) return "";
        if (src == null) return dst;
        if (dst == null) return src;
        // find out who has user call sign
        String userRegex = "^[A-Za-z0-9]{1,2}[0-9]+[A-Za-z]{1,4}+(-[A-Za-z0-9]+)?$";
        boolean isSrcUser = src.matches(userRegex);
        boolean isDstUser = dst.matches(userRegex);
        // both are user call signs
        if (isSrcUser && isDstUser) {
            return src.compareTo(dst) < 0 ? src + "/" + dst : dst + "/" + src;
        }
        // one is group just user group callsign
        return isSrcUser ? dst : src;
    }

    public static boolean isMultiParty(String groupName) {
        String[] callSigns = groupName.split("/");
        return callSigns.length == 1;
    }

    public static String getTargetCallsign(Context context, String groupName) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String myCallsign = sharedPreferences.getString(PreferenceKeys.AX25_CALLSIGN, "N0CALL").toUpperCase(Locale.ROOT);
        String[] callSigns = groupName.split("/");
        if (callSigns.length == 1) return groupName;
        if (callSigns.length == 2) {
            if (callSigns[0].equals(myCallsign)) {
                return callSigns[1];
            } else if (callSigns[1].equals(myCallsign)) {
                return callSigns[0];
            }
            return null;
        }
        return null;
    }

    public boolean isAck() {
        return this.text != null &&
                this.text.toLowerCase(Locale.ROOT).equals("ack") &&
                this.ackId != null;
    }

    public boolean isRej() {
        return this.text != null &&
                this.text.toLowerCase(Locale.ROOT).equals("rej") &&
                this.ackId != null;
    }

    public boolean isAutoReply() {
        return isAck() || isRej();
    }
}
