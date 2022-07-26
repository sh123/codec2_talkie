package com.radio.codec2talkie.protocol.aprs.tools;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.widget.ImageView;

import com.radio.codec2talkie.R;

import java.util.ArrayList;

public class AprsSymbolTable {

    private final ArrayList<Bitmap> _primaryTableIcons;
    private final ArrayList<Bitmap> _secondaryTableIcons;

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
    }

    public Bitmap bitmapFromSymbol(String symbolCode) {
        if (symbolCode.length() != 2) return null;
        char table = symbolCode.charAt(0);
        char symbol = symbolCode.charAt(1);
        int index = (int)symbol - 33;
        if (index < 0 || index >= (_cntWidth * _cntHeight)) return null;
        if (table == '/') {
            return _primaryTableIcons.get(index);
        }
        return _secondaryTableIcons.get(index);
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
