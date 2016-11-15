/*
 * Copyright (C) 2016 eschao <esc.chao@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.eschao.android.widget.pageflip;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;

/**
 * Utilities of page flip
 *
 * @author eschao
 */
public class PageFlipUtils {

    /**
     * Compute average color for given bitmap
     *
     * @param bitmap Bitmap object
     * @param pixels How many sample pixels is used to compute
     * @return Average color
     */
    public static int computeAverageColor(Bitmap bitmap, int pixels) {
        int red = 0;
        int green = 0;
        int blue = 0;
        int alpha = 0;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int maxWPixels = width / 3;
        int maxHPixels = height / 3;

        if (pixels > maxWPixels) {
            pixels = maxWPixels;
        }

        if (pixels > maxHPixels) {
            pixels = maxHPixels;
        }

        int right = width - pixels;
        int bottom = height - pixels;
        int centerLeft = right / 2;
        int centerTop = bottom / 2;

        for (int i = 0; i < pixels; ++i) {
            // left-top
            int color = bitmap.getPixel(i, i);
            red += Color.red(color);
            blue += Color.blue(color);
            green += Color.green(color);
            alpha += Color.alpha(color);

            // center
            color = bitmap.getPixel(centerLeft + i, centerTop + i);
            red += Color.red(color);
            blue += Color.blue(color);
            green += Color.green(color);
            alpha += Color.alpha(color);

            // right-top
            color = bitmap.getPixel(right + i, i);
            red += Color.red(color);
            blue += Color.blue(color);
            green += Color.green(color);
            alpha += Color.alpha(color);

            // left-bottom
            color = bitmap.getPixel(i, bottom + i);
            red += Color.red(color);
            blue += Color.blue(color);
            green += Color.green(color);
            alpha += Color.alpha(color);

            // right-bottom
            color = bitmap.getPixel(right + i, bottom + i);
            red += Color.red(color);
            blue += Color.blue(color);
            green += Color.green(color);
            alpha += Color.alpha(color);
        }

        int count = pixels * 5;
        red /= count;
        blue /= count;
        green /= count;
        alpha /= count;
        return Color.argb(alpha, red, green, blue);
    }

    /**
     * Create gradient bitmap for drawing fold back part of page
     *
     * @return Gradient bitmap object
     */
    public static Bitmap createGradientBitmap() {
        Canvas c = new Canvas();
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Bitmap bitmap = Bitmap.createBitmap(256, 1, Bitmap.Config.ARGB_8888);

        c.setBitmap(bitmap);
        int[] colors = new int[]{0x00FFFFFF,
                                 0x24000000,
                                 0x24101010,
                                 0x48000000};
        float[] positions = new float[]{0.5f, 0.9f, 0.94f, 1.0f};
        LinearGradient shader = new LinearGradient(0, 0, 256, 0, colors,
                                                   positions,
                                                   Shader.TileMode.CLAMP);
        paint.setShader(shader);
        c.drawRect(0, 0, 256, 1, paint);
        return bitmap;
    }
}
