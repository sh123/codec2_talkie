package com.radio.codec2talkie.protocol.aprs.tools;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.widget.ImageView;

import com.radio.codec2talkie.R;

import java.util.ArrayList;

public class AprsSymbolTable {

    private final ArrayList<Bitmap> _primaryTableIcons;
    private final ArrayList<Bitmap> _secondaryTableIcons;
    private final ArrayList<Bitmap> _overlayTableIcons;

    private final int _cellWidth = 24;
    private final int _cellHeight = 24;
    private final int _cntWidth = 16;
    private final int _cntHeight = 6;

    private static AprsSymbolTable _symbolTable;

    public static AprsSymbolTable getInstance(Context context) {
        if (_symbolTable == null) {
            synchronized (AprsSymbolTable.class) {
                _symbolTable = new AprsSymbolTable(context);
            }
        }
        return _symbolTable;
    }

    private AprsSymbolTable(Context context) {
        ImageView imageViewPrimary = new ImageView(context);
        imageViewPrimary.setImageResource(R.drawable.aprs_symbols_24_0);
        _primaryTableIcons = Load(imageViewPrimary, _cellWidth, _cellHeight, _cntWidth, _cntHeight);

        ImageView imageViewSecondary = new ImageView(context);
        imageViewSecondary.setImageResource(R.drawable.aprs_symbols_24_1);
        _secondaryTableIcons = Load(imageViewSecondary, _cellWidth, _cellHeight, _cntWidth, _cntHeight);

        ImageView imageViewOverlay = new ImageView(context);
        imageViewOverlay.setImageResource(R.drawable.aprs_symbols_24_2);
        _overlayTableIcons = Load(imageViewOverlay, _cellWidth, _cellHeight, _cntWidth, _cntHeight);
    }

    public Bitmap bitmapFromSymbol(String symbolCode) {
        if (symbolCode.length() != 2) return null;
        char table = symbolCode.charAt(0);
        char symbol = symbolCode.charAt(1);
        int symbolIconIndex = (int)symbol - 33;
        int overlayIconIndex = (int)table - 33;
        if (symbolIconIndex < 0 || symbolIconIndex >= (_cntWidth * _cntHeight)) return null;
        if (table == '/') {
            return _primaryTableIcons.get(symbolIconIndex);
        } else if (table == '\\') {
            return _secondaryTableIcons.get(symbolIconIndex);
        }
        if (overlayIconIndex < 0 || overlayIconIndex >= (_cntWidth * _cntHeight)) return null;
        Bitmap icon = _secondaryTableIcons.get(symbolIconIndex);
        Bitmap overlayIcon = _overlayTableIcons.get(overlayIconIndex);
        Bitmap bmOverlay = Bitmap.createBitmap(icon.getWidth() * 2, icon.getHeight() * 2, icon.getConfig());
        Canvas canvas = new Canvas(bmOverlay);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(icon, 0, 0, paint);
        canvas.drawBitmap(overlayIcon, 0, 0, paint);
        return bmOverlay;
    }

    private ArrayList<Bitmap> Load(ImageView imageView, int cellWidth, int cellHeight, int cntWidth, int cntHeight) {
        ArrayList<Bitmap> bitmaps = new ArrayList<>(cntWidth * cntHeight);
        BitmapDrawable bitmapDrawable = (BitmapDrawable) imageView.getDrawable();
        Bitmap bitmap = bitmapDrawable.getBitmap();

        for (int y = 0; y < cntHeight; y++) {
            for (int x = 0; x < cntWidth; x++) {
                Bitmap cellBitmap = Bitmap.createBitmap(bitmap, x * cellWidth, y * cellHeight, cellWidth, cellHeight);
                bitmaps.add(cellBitmap);
            }
        }
        return bitmaps;
    }
}
