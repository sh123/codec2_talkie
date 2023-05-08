package com.radio.codec2talkie.tools;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.DisplayMetrics;

public class BitmapTools {
    public static BitmapDrawable drawLabel(Context context, String text, float textSize) {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(textSize);

        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);

            Bitmap bitmap = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888);
        bitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);

        Canvas canvas = new Canvas(bitmap);

        paint.setColor(Color.WHITE);
        //paint.setAlpha(200);
        canvas.drawRect(0, 0, bounds.width(), bounds.height(), paint);

        paint.setColor(Color.BLACK);
        paint.setAlpha(255);
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        canvas.drawText(text, -bounds.left, bounds.height(), paint);

        return new BitmapDrawable(context.getResources(), bitmap);
    }
}
