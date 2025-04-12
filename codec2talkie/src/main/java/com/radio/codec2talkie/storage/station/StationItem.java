package com.radio.codec2talkie.storage.station;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.radio.codec2talkie.protocol.aprs.tools.AprsSymbolTable;

import java.util.Objects;

@Entity(indices = {@Index(value = {"srcCallsign"}, unique = true)})
public class StationItem {
    @NonNull
    @PrimaryKey
    private String srcCallsign;
    private long timestampEpoch;
    public String dstCallsign;
    public String digipath;
    private String maidenHead;
    public double latitude;
    public double longitude;
    public double altitudeMeters;
    public double bearingDegrees;
    public double speedMetersPerSecond;
    public String status;
    public String comment;
    public String deviceIdDescription;
    public String symbolCode;
    public String logLine;
    public int privacyLevel;
    public double rangeMiles;
    public int directivityDeg;

    public StationItem(@NonNull String srcCallsign) {
        this.srcCallsign = srcCallsign;
    }

    public long getTimestampEpoch() { return timestampEpoch; }

    @NonNull
    public String getSrcCallsign() { return srcCallsign; }

    public String getDstCallsign() { return dstCallsign; }

    public String getDigipath() { return digipath; }

    public double getLatitude() { return latitude; }

    public double getLongitude() { return longitude; }

    public String getMaidenHead() { return maidenHead; }

    public double getAltitudeMeters() { return altitudeMeters; }

    public double getBearingDegrees() { return bearingDegrees; }

    public double getSpeedMetersPerSecond() { return speedMetersPerSecond; }

    public String getStatus() { return status; }

    public String getComment() { return comment; }

    public String getDeviceIdDescription() { return deviceIdDescription; }

    public String getSymbolCode() { return symbolCode; }

    public String getLogLine() { return logLine; }

    public int getPrivacyLevel() { return privacyLevel; }

    public double getRangeMiles() { return rangeMiles; }

    public int getDirectivityDeg() { return directivityDeg; }

    public void setTimestampEpoch(long timestampEpoch) { this.timestampEpoch = timestampEpoch; }

    public void setSrcCallsign(@NonNull String srcCallsign) { this.srcCallsign = srcCallsign; }

    public void setDigipath(String digipath) { this.digipath = digipath; }

    public void setMaidenHead(String maidenHead) { this.maidenHead = maidenHead; }

    public void setDstCallsign(String dstCallsign) { this.dstCallsign = dstCallsign; }

    public void setLatitude(double latitude) { this.latitude = latitude; }

    public void setLongitude(double longitude) { this.longitude = longitude; }

    public void setAltitudeMeters(double altitudeMeters) { this.altitudeMeters = altitudeMeters; }

    public void setBearingDegrees(double bearingDegrees) { this.bearingDegrees = bearingDegrees; }

    public void setSpeedMetersPerSecond(double speedMetersPerSecond) { this.speedMetersPerSecond = speedMetersPerSecond; }

    public void setStatus(String status) { this.status = status; }

    public void setComment(String comment) { this.comment = comment; }

    public void setDeviceIdDescription(String deviceIdDescription) { this.deviceIdDescription = deviceIdDescription; }

    public void setSymbolCode(String symbolCode) { this.symbolCode = symbolCode; }

    public void setPrivacyLevel(int privacyLevel) { this.privacyLevel = privacyLevel; }

    public void setLogLine(String logLine) { this.logLine = logLine; }

    public void setRangeMiles(double rangeMiles) { this.rangeMiles = rangeMiles; }

    public void setDirectivityDeg(int directivityDeg) { this.directivityDeg = directivityDeg; }

    public void updateFrom(StationItem stationItem) {
        setTimestampEpoch(stationItem.getTimestampEpoch());
        // update position if known
        if (stationItem.getMaidenHead() != null) {
            setMaidenHead(stationItem.getMaidenHead());
            setLatitude(stationItem.getLatitude());
            setLongitude(stationItem.getLongitude());
            setAltitudeMeters(stationItem.getAltitudeMeters());
            setBearingDegrees(stationItem.getBearingDegrees());
            setSpeedMetersPerSecond(stationItem.getSpeedMetersPerSecond());
            setPrivacyLevel(stationItem.getPrivacyLevel());
            setRangeMiles(stationItem.getRangeMiles());
            setDirectivityDeg(stationItem.getDirectivityDeg());
        }
        if (stationItem.getStatus() != null)
            setStatus(stationItem.getStatus());
        if (stationItem.getComment() != null)
            setComment(stationItem.getComment());
        if (stationItem.getDeviceIdDescription() != null)
            setDeviceIdDescription(stationItem.getDeviceIdDescription());
        if (stationItem.getSymbolCode() != null)
            setSymbolCode(stationItem.getSymbolCode());
        if (stationItem.getLogLine() != null)
            setLogLine(stationItem.getLogLine());
        if (stationItem.getDigipath() != null)
            setDigipath(stationItem.getDigipath());
        if (stationItem.getDstCallsign() != null)
            setDstCallsign(stationItem.getDstCallsign());
    }

    @Override
    public boolean equals(Object o) {
        StationItem stationItem = (StationItem)o;
        return srcCallsign.equals(stationItem.getSrcCallsign()) &&
                timestampEpoch == stationItem.getTimestampEpoch() &&
                Objects.equals(comment, stationItem.getComment()) &&
                Objects.equals(dstCallsign, stationItem.getDstCallsign()) &&
                latitude == stationItem.getLatitude() &&
                longitude == stationItem.getLongitude();
    }

    public BitmapDrawable drawLabelWithIcon(Context context, float textSize) {
        String callsign = getSrcCallsign();

        Bitmap bitmapIcon = AprsSymbolTable.getInstance(context).bitmapFromSymbol(getSymbolCode(), false);
        if (bitmapIcon == null) return null;

        // construct and calculate bounds
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(textSize);
        Rect bounds = new Rect();
        paint.getTextBounds(callsign, 0, callsign.length(), bounds);
        int width = Math.max(bitmapIcon.getWidth(), bounds.width());
        int height = bitmapIcon.getHeight() + bounds.height();

        // create overlay bitmap
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);

        // draw APRS icon
        Canvas canvas = new Canvas(bitmap);
        float bitmapLeft = width > bitmapIcon.getWidth() ? width / 2.0f - bitmapIcon.getWidth() / 2.0f : 0;
        // do not rotate
        if (getBearingDegrees() == 0 || !AprsSymbolTable.needsRotation(getSymbolCode())) {
            canvas.drawBitmap(bitmapIcon, bitmapLeft, 0, null);
            // rotate
        } else {
            float rotationDeg = (float) (getBearingDegrees() - 90.0f);
            Matrix m = new Matrix();
            // flip/rotate
            if (getBearingDegrees() > 180) {
                m.postScale(-1, 1);
                m.postTranslate(bitmapIcon.getWidth(), 0);
                m.postRotate(rotationDeg - 180, bitmapIcon.getWidth() / 2.0f, bitmapIcon.getHeight() / 2.0f);
                // rotate
            } else {
                m.postRotate(rotationDeg, bitmapIcon.getWidth() / 2.0f, bitmapIcon.getHeight() / 2.0f);
            }
            m.postTranslate(bitmapLeft, 0);
            canvas.drawBitmap(bitmapIcon, m, null);
        }

        // draw background
        paint.setColor(Color.WHITE);
        paint.setAlpha(120);
        bounds.set(0, bitmapIcon.getHeight(), width, height);
        canvas.drawRect(bounds, paint);

        // draw text
        paint.setColor(Color.BLACK);
        paint.setAlpha(255);
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        canvas.drawText(callsign, 0, height, paint);

        // add marker
        return new BitmapDrawable(context.getResources(), bitmap);
    }
}
