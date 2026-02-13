package com.radio.codec2talkie.protocol.message;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.radio.codec2talkie.protocol.ax25.AX25Callsign;
import com.radio.codec2talkie.settings.SettingsWrapper;
import com.radio.codec2talkie.storage.message.MessageItem;

import java.util.Locale;

public class TextMessage {
    public String src;
    public String dst;
    public String digipath;
    public String text;
    public String ackId;
    public boolean needsRetry;

    public MessageItem toMessageItem(boolean isTransmit) {
        MessageItem messageItem = new MessageItem();
        messageItem.setGroupId(generateGroupId(this.src, this.dst));
        messageItem.setTimestampEpoch(System.currentTimeMillis());
        messageItem.setIsTransmit(isTransmit);
        messageItem.setSrcCallsign(this.src);
        messageItem.setDstCallsign(this.dst);
        messageItem.setMessage(this.text);
        messageItem.setAckId(this.ackId);
        messageItem.setIsAcknowledged(false);
        messageItem.setRetryCnt(0);
        messageItem.setNeedsRetry(needsRetry);
        // bulletin messages do not require ack
        if (isBulletin(this.dst))
            messageItem.setAckId(null);
        return messageItem;
    }

    public TextMessage getAckMessage() {
        TextMessage textMessage = new TextMessage();
        textMessage.src = dst;
        textMessage.dst = src;
        textMessage.text = "ack";
        textMessage.ackId = ackId;
        textMessage.needsRetry = false;
        return textMessage;
    }

    public static String generateGroupId(String src, String dst) {
        // null checks
        if (src == null && dst == null) return "";
        if (src == null) return dst;
        if (dst == null) return src;
        // find out who has user call sign
        boolean isSrcUser = AX25Callsign.isValidUserCallsign(src);
        boolean isDstUser = AX25Callsign.isValidUserCallsign(dst);
        // both are user call signs, create 1-1 chat
        if (isSrcUser && isDstUser) {
            return src.compareTo(dst) < 0 ? src + "/" + dst : dst + "/" + src;
        }
        // one is group and another is user, use group
        if (isSrcUser) return dst;
        if (isDstUser) return src;

        // bulletins have priority, use bulletin group
        boolean isSrcBln = src.toLowerCase().startsWith("bln");
        boolean isDstBln = dst.toLowerCase().startsWith("bln");
        if (isSrcBln) return src;
        if (isDstBln) return dst;

        // both are groups
        return src.compareTo(dst) < 0 ? src : dst;
    }

    public boolean shouldAcknowledge(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String myCallsign = SettingsWrapper.getMyCallsignWithSsid(sharedPreferences);
        boolean isAckEnabled = SettingsWrapper.isMessageAckEnabled(sharedPreferences);
        return myCallsign.equals(dst) && isAckEnabled;
    }

    public static String getTargetCallsign(Context context, String groupName) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String[] callSigns = groupName.split("/");
        if (callSigns.length == 1) return groupName;
        if (callSigns.length == 2) {
            if (SettingsWrapper.isMyCallsign(sharedPreferences, callSigns[0])) {
                return callSigns[1];
            } else if (SettingsWrapper.isMyCallsign(sharedPreferences, callSigns[1])) {
                return callSigns[0];
            }
            return null;
        }
        return null;
    }

    public static boolean isMultiParty(String groupName) {
        String[] callSigns = groupName.split("/");
        return callSigns.length == 1;
    }

    public static boolean isBulletin(String groupName) {
        return groupName.toLowerCase().startsWith("bln") ||
                groupName.toLowerCase().startsWith("bom") ||
                groupName.toLowerCase().startsWith("nws");
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
