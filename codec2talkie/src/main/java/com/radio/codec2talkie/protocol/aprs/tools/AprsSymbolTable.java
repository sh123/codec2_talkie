package com.radio.codec2talkie.protocol.aprs.tools;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.util.DisplayMetrics;
import android.widget.ImageView;

import com.radio.codec2talkie.R;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class AprsSymbolTable {

    private final ArrayList<Bitmap> _primaryTableIcons;
    private final ArrayList<Bitmap> _secondaryTableIcons;
    private final ArrayList<Bitmap> _overlayTableIcons;

    private final ArrayList<Bitmap> _primaryTableIconsLarge;
    private final ArrayList<Bitmap> _secondaryTableIconsLarge;
    private final ArrayList<Bitmap> _overlayTableIconsLarge;

    private final int _cellWidth = 24;
    private final int _cellHeight = 24;

    private final int _cellWidthLarge = 64;
    private final int _cellHeightLarge = 64;

    private static final int _cntWidth = 16;
    private static final int _cntHeight = 6;

    private static final int _selectorIconDim = 64;

    private static final double _selectionIconScale = 2.0;

    private static AprsSymbolTable _symbolTable;

    private static final List<String> _symbolsToRotate = Arrays.asList("/'", "/(", "/*", "/<", "/=",
            "/C", "/F", "/P", "/U", "/X", "/Y", "/[", "/^", "/a", "/b", "/e", "/f", "/g", "/j",
            "/k", "/p", "/s", "/u", "/v", "/>", "\\k", "\\u", "\\v", "\\>");

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

        ImageView imageViewPrimaryLarge = new ImageView(context);
        imageViewPrimaryLarge.setImageResource(R.drawable.aprs_symbols_64_0);
        _primaryTableIconsLarge = Load(imageViewPrimaryLarge, _cellWidthLarge, _cellHeightLarge, _cntWidth, _cntHeight);

        ImageView imageViewSecondaryLarge = new ImageView(context);
        imageViewSecondaryLarge.setImageResource(R.drawable.aprs_symbols_64_1);
        _secondaryTableIconsLarge = Load(imageViewSecondaryLarge, _cellWidthLarge, _cellHeightLarge, _cntWidth, _cntHeight);

        ImageView imageViewOverlayLarge = new ImageView(context);
        imageViewOverlayLarge.setImageResource(R.drawable.aprs_symbols_64_2);
        _overlayTableIconsLarge = Load(imageViewOverlayLarge, _cellWidthLarge, _cellHeightLarge, _cntWidth, _cntHeight);
    }

    public Bitmap bitmapFromSymbol(String symbolCode, boolean useLarge) {
        if (symbolCode == null || symbolCode.length() != 2) return null;

        ArrayList<Bitmap> _primaryTable = useLarge ? _primaryTableIconsLarge : _primaryTableIcons;
        ArrayList<Bitmap> _secondaryTable = useLarge ? _secondaryTableIconsLarge : _secondaryTableIcons;
        ArrayList<Bitmap> _overlayTable = useLarge ? _overlayTableIconsLarge : _overlayTableIcons;

        char table = symbolCode.charAt(0);
        char symbol = symbolCode.charAt(1);

        int symbolIconIndex = (int)symbol - 33;
        int overlayIconIndex = (int)table - 33;

        if (symbolIconIndex < 0 || symbolIconIndex >= (_cntWidth * _cntHeight)) return null;

        if (table == '/') {
            return _primaryTable.get(symbolIconIndex);
        } else if (table == '\\') {
            return _secondaryTable.get(symbolIconIndex);
        }

        if (overlayIconIndex < 0 || overlayIconIndex >= (_cntWidth * _cntHeight)) return null;

        Bitmap icon = _secondaryTable.get(symbolIconIndex);
        Bitmap overlayIcon = _overlayTable.get(overlayIconIndex);
        Bitmap bmOverlay = Bitmap.createBitmap(icon.getWidth(), icon.getHeight(), null);
        bmOverlay.setDensity(DisplayMetrics.DENSITY_DEFAULT);

        Canvas canvas = new Canvas(bmOverlay);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);

        canvas.drawBitmap(icon, 0, 0, paint);
        canvas.drawBitmap(overlayIcon, 0, 0, paint);

        return bmOverlay;
    }

    private static ArrayList<Bitmap> Load(ImageView imageView, int cellWidth, int cellHeight, int cntWidth, int cntHeight) {
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

    public static String getSymbolFromCoordinate(float x, float y, int parentWidth, int parentHeight) {
        int cntX = (int) (parentWidth / (_selectorIconDim * _selectionIconScale));
        int cntY = 2 * _cntWidth * _cntHeight / cntX;

        int posX = (int) (x * cntX) / parentWidth;
        int posY = (int) (y * cntY) / parentHeight;
        int index = cntX * posY + posX;

        int count = _cntHeight * _cntWidth;

        char tbl = index < count ? '/' : '\\';
        char sym = index < count ? (char)(index + 33) : (char)(index + 33 - count);

        return String.format(Locale.US, "%c%c", tbl, sym);
    }

    public static Bitmap generateSelectionTable(Context context, int parentWidth) {
        double cntXDouble = parentWidth / (_selectorIconDim * _selectionIconScale);
        int cntX = (int)Math.floor(cntXDouble);
        int cntY = _cntWidth * _cntHeight / cntX;

        ImageView imageViewPrimary = new ImageView(context);
        imageViewPrimary.setImageResource(R.drawable.aprs_symbols_64_0);
        ArrayList<Bitmap> primaryTableIcons = Load(imageViewPrimary, _selectorIconDim, _selectorIconDim, _cntWidth, _cntHeight);
        ImageView imageViewSecondary = new ImageView(context);
        imageViewSecondary.setImageResource(R.drawable.aprs_symbols_64_1);
        ArrayList<Bitmap> secondaryTableIcons = Load(imageViewSecondary, _selectorIconDim, _selectorIconDim, _cntWidth, _cntHeight);
        primaryTableIcons.addAll(secondaryTableIcons);

        Bitmap bmOverlay = Bitmap.createBitmap(_selectorIconDim*cntX, _selectorIconDim*cntY*2, null);
        bmOverlay.setDensity(DisplayMetrics.DENSITY_DEFAULT);
        Canvas canvas = new Canvas(bmOverlay);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);

        for (int y = 0; y < 2*cntY; y++) {
            for (int x = 0; x < cntX; x++) {
                int index = cntX * y + x;
                if (index >= primaryTableIcons.size()) break;
                Bitmap icon = primaryTableIcons.get(index);
                canvas.drawBitmap(icon, x*_selectorIconDim, y*_selectorIconDim, paint);
            }
        }
        return bmOverlay;
    }

    public static boolean needsRotation(String symbol) {
        return _symbolsToRotate.contains(symbol);
    }
}
