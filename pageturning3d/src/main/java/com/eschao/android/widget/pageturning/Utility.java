package com.eschao.android.widget.pageturning;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import android.graphics.Color;
import android.graphics.Bitmap;

public class Utility {

    public static final FloatBuffer toFloatBuffer(float[] data) {
        ByteBuffer byteBuf = ByteBuffer.allocateDirect(data.length*4);
        byteBuf.order(ByteOrder.nativeOrder());
        FloatBuffer floatBuf = byteBuf.asFloatBuffer();
        floatBuf.put(data);
        floatBuf.position(0);
        return floatBuf;
    }
    
    public static final IntBuffer toIntBuffer(int[] data) {
        ByteBuffer byteBuf = ByteBuffer.allocateDirect(data.length*4);
        byteBuf.order(ByteOrder.nativeOrder());
        IntBuffer intBuf = byteBuf.asIntBuffer();
        intBuf.put(data);
        intBuf.position(0);
        return intBuf;
    }
    
    public static final ByteBuffer toByteBuffer(byte[] data) {
        ByteBuffer byteBuf = ByteBuffer.allocateDirect(data.length);
        byteBuf.put(data);
        byteBuf.position(0);
        return byteBuf;
    }

    public static final int computeAverageColor(Bitmap bitmap, int pixels) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int red     = 0;
        int green   = 0;
        int blue    = 0;
        int alpha   = 0;
        int bottomColor = 0;
        int centerColor = 0;

        int maxWPixels  = width/3;
        int maxHPixels  = height/3;

        if (pixels > maxWPixels)
            pixels = maxWPixels;
        if (pixels > maxHPixels)
            pixels = maxHPixels;

        int right       = width-pixels;
        int bottom      = height-pixels;
        int centerLeft  = right/2;
        int centerTop   = bottom/2;
        for (int i=0; i<pixels; ++i) {
            // left-top
            int color = bitmap.getPixel(i, i);
            alpha   += Color.alpha(color);
            red     += Color.red(color);
            blue    += Color.blue(color);
            green   += Color.green(color);

            // center
            color = bitmap.getPixel(centerLeft+i, centerTop+i);
            alpha   += Color.alpha(color);
            red     += Color.red(color);
            blue    += Color.blue(color);
            green   += Color.green(color);

            // right-top
            color = bitmap.getPixel(right+i, i);
            alpha   += Color.alpha(color);
            red     += Color.red(color);
            blue    += Color.blue(color);
            green   += Color.green(color);
            
            // left-bottom
            color = bitmap.getPixel(i, bottom+i);
            alpha   += Color.alpha(color);
            red     += Color.red(color);
            blue    += Color.blue(color);
            green   += Color.green(color);

            // right-bottom
            color = bitmap.getPixel(right+i, bottom+i);
            alpha   += Color.alpha(color);
            red     += Color.red(color);
            blue    += Color.blue(color);
            green   += Color.green(color);
        }

        int count = pixels*5;
        red     = red/count;
        blue    = blue/count;
        green   = green/count;
        alpha   = alpha/count;
        return Color.argb(alpha, red, green, blue);
    }
}
