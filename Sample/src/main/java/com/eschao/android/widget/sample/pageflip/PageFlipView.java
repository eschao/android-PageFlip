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
package com.eschao.android.widget.sample.pageflip;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.eschao.android.widget.pageflip.PageFlip;
import com.eschao.android.widget.pageflip.PageFlipException;

import java.util.concurrent.locks.ReentrantLock;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Page flip view
 *
 * @author eschao
 */

public class PageFlipView extends GLSurfaceView implements Renderer {

    private final static String TAG = "PageFlipView";

    final static int GREY_FABRIC_BG_TYPE = 0;
    final static int SURFACE_BG_TYPE = 1;

    int mPageNo;
    int mBackgroundType;
    Handler mHandler;
    PageFlip mPageFlip;
    PageRender mPageRender;
    ReentrantLock mDrawLock;

    public PageFlipView(Context context) {
        super(context);

        newHandler();
        mPageFlip = new PageFlip(context);
        mPageFlip.setSemiPerimeterRatio(0.8f)
                 .setShadowWidthOfFoldEdges(5, 60, 0.3f)
                 .setShadowWidthOfFoldBase(5, 80, 0.4f)
                 .setPixelsOfMesh(5)
                 .enableAutoPageMode(true);//.setFoldEdgesShadowColor(0, 1, 1, 0);
        setEGLContextClientVersion(2);
        //setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        //getHolder().setFormat(PixelFormat.RGBA_8888);
        //mRender = new PageFlipFlipRender();
        mPageNo = 1;
        mDrawLock = new ReentrantLock();
        mBackgroundType = /*SURFACE_BG_TYPE;*/GREY_FABRIC_BG_TYPE;
        mPageRender = new SinglePageRender(context, mPageFlip,
                                           mHandler, mPageNo);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    /**
     * Handle finger down event
     *
     * @param x finger x coordinate
     * @param y finger y coordinate
     */
    public void onFingerDown(float x, float y) {
        // if the animation is going, we should ignore this event to avoid
        // mess drawing on screen
        if (!mPageFlip.isAnimating()) {
            mPageFlip.onFingerDown(x, y);
        }
    }

    /**
     * Handle finger moving event
     *
     * @param x finger x coordinate
     * @param y finger y coordinate
     */
    public void onFingerMove(float x, float y) {
        if (mPageFlip.isAnimating()) {
            // nothing to do during animating
        }
        else if (mPageFlip.canAnimate(x, y)) {
            // if the point is out of current page, try to start animating
            onFingerUp(x, y);
        }
        // move page by finger
        else if (mPageFlip.onFingerMove(x, y)) {
            try {
                mDrawLock.lock();
                if (mPageRender != null &&
                    mPageRender.onFingerMove(x, y)) {
                    requestRender();
                }
            }
            finally {
                mDrawLock.unlock();
            }
        }
    }

    /**
     * Handle finger up event and start animating if need
     *
     * @param x finger x coordinate
     * @param y finger y coordinate
     */
    public void onFingerUp(float x, float y) {
        if (!mPageFlip.isAnimating()) {
            mPageFlip.onFingerUp(x, y, 1000);
            try {
                mDrawLock.lock();
                if (mPageRender != null &&
                    mPageRender.onFingerUp(x, y)) {
                    requestRender();
                }
            }
            finally {
                mDrawLock.unlock();
            }
        }
    }

    /**
     * Draw frame
     *
     * @param gl OpenGL handle
     */
    @Override
    public void onDrawFrame(GL10 gl) {
        try {
            mDrawLock.lock();
            if (mPageRender != null) {
                mPageRender.onDrawFrame();
            }
        }
        finally {
            mDrawLock.unlock();
        }
    }

    /**
     * Handle surface is changed
     *
     * @param gl OpenGL handle
     * @param width new width of surface
     * @param height new height of surface
     */
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        try {
            mPageFlip.onSurfaceChanged(width, height);

            // get selected background
            int resID = chooseBackground(width, height, mBackgroundType);
            if (resID == -1) {
               Log.d(TAG, "Can'top get a valid background resource id");
                return;
            }

            // decode background bitmap
            Resources res = getContext().getResources();
            Bitmap bitmap = BitmapFactory.decodeResource(res, resID);

            // if there is the second page, create double page render when need
            int pageNo = mPageRender.getPageNo();
            if (mPageFlip.getSecondPage() != null && width > height) {
                if (!(mPageRender instanceof DoublePagesRender)) {
                    mPageRender.release();
                    mPageRender = new DoublePagesRender(getContext(),
                                                        mPageFlip,
                                                        mHandler,
                                                        pageNo);
                }
            }
            // if there is only one page, create single page render when need
            else if(!(mPageRender instanceof SinglePageRender)) {
                mPageRender.release();
                mPageRender = new SinglePageRender(getContext(),
                                                   mPageFlip,
                                                   mHandler,
                                                   pageNo);
            }

            // let page render handle surface change
            mPageRender.onSurfaceChanged(bitmap);
        }
        catch (PageFlipException e) {
            Log.e(TAG, "Failed to run PageFlipFlipRender:onSurfaceChanged");
        }
    }

    /**
     * Handle surface is created
     *
     * @param gl OpenGL handle
     * @param config EGLConfig object
     */
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        try {
            mPageFlip.onSurfaceCreated();
        }
        catch (PageFlipException e) {
            Log.e(TAG, "Failed to run PageFlipFlipRender:onSurfaceCreated");
        }
    }

    private int chooseBackground(int w, int h, int type) {
        if ((w <= 800 && h <= 480) ||
            (w <= 480 && h <= 800)) {
            if (type == GREY_FABRIC_BG_TYPE) {
                return R.drawable.grey_fabric_480x800;
            }
            else if (type == SURFACE_BG_TYPE) {
                return R.drawable.surface_480x800;
            }
        }
        else if ((w <= 854 && h <= 480) ||
                 (w <= 480 && h <= 854)) {
            if (type == GREY_FABRIC_BG_TYPE) {
                return R.drawable.grey_fabric_480x854;
            }
            else if (type == SURFACE_BG_TYPE) {
                return R.drawable.surface_480x854;
            }
        }
        else if ((w <= 720 && h <= 1280) ||
                 (w <= 1280 && h <= 720)) {
            if (type == GREY_FABRIC_BG_TYPE) {
                return R.drawable.grey_fabric_720x1280;
            }
            else if (type == SURFACE_BG_TYPE) {
                return R.drawable.surface_720x1280;
            }
        }
        else if ((w <= 800 && h <= 1280) ||
                 (w <= 1280 && h <= 800)) {
            if (type == GREY_FABRIC_BG_TYPE) {
                return R.drawable.grey_fabric_800x1280;
            }
            else if (type == SURFACE_BG_TYPE) {
                return R.drawable.surface_800x1280;
            }
        }
        else if ((w <= 1080 && h <= 1920) ||
                 (w <= 1920 && h <= 1080)) {
            if (type == GREY_FABRIC_BG_TYPE) {
                return R.drawable.grey_fabric_1080x1920;
            }
            else if (type == SURFACE_BG_TYPE) {
                return R.drawable.surface_1080x1920;
            }
        }

        return -1;
    }

    /**
     * Create message handler to cope with messages from page render,
     * Page render will send message in GL thread, but we want to handle those
     * messages in main thread that why we need handler here
     */
    private void newHandler() {
        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case PageRender.MSG_ENDED_DRAWING_FRAME:
                        try {
                            mDrawLock.lock();
                            // notify page render to handle ended drawing
                            // message
                            if (mPageRender != null &&
                                mPageRender.onEndedDrawing(msg.arg1)) {
                                requestRender();
                            }
                        }
                        finally {
                            mDrawLock.unlock();
                        }
                        break;

                    default:
                        break;
                }
            }
        };
    }
}
