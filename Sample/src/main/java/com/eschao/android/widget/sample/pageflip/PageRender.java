package com.eschao.android.widget.sample.pageflip;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Handler;

import com.eschao.android.widget.pageflip.OnPageFlipListener;
import com.eschao.android.widget.pageflip.PageFlip;

/**
 * Created by chao on 08/11/2016.
 */

public abstract class PageRender implements OnPageFlipListener {
    public final static int MSG_ENDED_DRAWING_FRAME = 1;

    private final static String TAG = "PageRender";
    final static int DRAW_MOVING_FRAME = 0;
    final static int DRAW_ANIMATING_FRAME = 1;
    final static int DRAW_FULL_PAGE = 2;

    final static int MAX_PAGES = 25;

    int mPageNo;
    int mDrawCommand;
    Bitmap mBitmap;
    Canvas mCanvas;
    Bitmap mBackgroundBitmap;
    Context mContext;
    Handler mHandler;
    PageFlip mPageFlip;

    public PageRender(Context context, PageFlip pageFlip,
                      Handler handler, int pageNo) {
        mContext = context;
        mPageFlip = pageFlip;
        mPageNo = pageNo;
        mDrawCommand = DRAW_FULL_PAGE;
        mCanvas = new Canvas();
        mPageFlip.setListener(this);
        mHandler = handler;
    }

    public int getPageNo() {
        return mPageNo;
    }

    public void release() {
        if (mBitmap != null) {
            mBitmap.recycle();
            mBitmap = null;
        }

        mPageFlip.setListener(null);
        mCanvas = null;
        mBackgroundBitmap = null;
    }

    public boolean onFingerMove(float x, float y) {
        mDrawCommand = DRAW_MOVING_FRAME;
        return true;
    }

    public boolean onFingerUp(float x, float y) {
        if (mPageFlip.animating()) {
            mDrawCommand = DRAW_ANIMATING_FRAME;
            return true;
        }

        return false;
    }


    abstract void onDrawFrame();

    abstract void onSurfaceChanged(Bitmap background);

    abstract boolean onEndedDrawing(int what);
}
