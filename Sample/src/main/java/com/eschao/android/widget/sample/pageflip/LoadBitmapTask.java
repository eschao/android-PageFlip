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
import android.graphics.Matrix;
import android.util.Log;

import java.util.LinkedList;
import java.util.Random;

/**
 * A singleton thread task to load bitmap
 * <p>Attempt to load bitmap in separate thread to get better performance.</p>
 *
 * @author eschao
 */

public final class LoadBitmapTask implements Runnable {

    private final static String TAG = "LoadBitmapTask";
    private static LoadBitmapTask __object;

    final static int SMALL_BG = 0;
    final static int MEDIUM_BG = 1;
    final static int LARGE_BG = 2;
    final static int BG_COUNT = 10;

    int mBGSizeIndex;
    int mQueueMaxSize;
    int mPreRandomNo;
    boolean mIsLandscape;
    boolean mStop;
    Random mBGRandom;
    Resources mResources;
    Thread mThread;
    LinkedList<Bitmap> mQueue;
    int[][] mPortraitBGs;

    /**
     * Get an unique task object
     *
     * @param context Android context
     * @return unique task object
     */
    public static LoadBitmapTask get(Context context) {
        if (__object == null) {
           __object = new LoadBitmapTask(context);
        }
        return __object;
    }

    /**
     * Constructor
     *
     * @param context Android context
     */
    private LoadBitmapTask(Context context) {
        mResources = context.getResources();
        mBGRandom = new Random();
        mBGSizeIndex = SMALL_BG;
        mStop = false;
        mThread = null;
        mPreRandomNo = 0;
        mIsLandscape = false;
        mQueueMaxSize = 1;
        mQueue = new LinkedList<Bitmap>();

        // init all available bitmaps
        mPortraitBGs = new int[][] {
            new int[] {R.drawable.p1_480, R.drawable.p2_480, R.drawable.p3_480,
                       R.drawable.p4_480, R.drawable.p5_480, R.drawable.p6_480,
                       R.drawable.p7_480, R.drawable.p8_480, R.drawable.p9_480,
                       R.drawable.p10_480},
            new int[] {R.drawable.p1_720, R.drawable.p2_720, R.drawable.p3_720,
                       R.drawable.p4_720, R.drawable.p5_720, R.drawable.p6_720,
                       R.drawable.p7_720, R.drawable.p8_720, R.drawable.p9_720,
                       R.drawable.p10_720},
            new int[] {R.drawable.p1_1080, R.drawable.p2_1080,
                       R.drawable.p3_1080, R.drawable.p4_1080,
                       R.drawable.p5_1080, R.drawable.p6_1080,
                       R.drawable.p7_1080, R.drawable.p8_1080,
                       R.drawable.p9_1080, R.drawable.p10_1080}
        };
    }

    /**
     * Acquire a bitmap to show
     * <p>If there is no cached bitmap, it will load one immediately</p>
     *
     * @return bitmap
     */
    public Bitmap getBitmap() {
        Bitmap b = null;
        synchronized (this) {
            if (mQueue.size() > 0) {
                b = mQueue.pop();
            }

            notify();
        }

        if (b == null) {
            Log.d(TAG, "Load bitmap instantly!");
            b = getRandomBitmap();
        }

        return b;
    }

    /**
     * Is task running?
     *
     * @return true if task is running
     */
    public boolean isRunning() {
        return mThread != null && mThread.isAlive();
    }

    /**
     * Start task
     */
    public synchronized void start() {
        if (mThread == null || !mThread.isAlive()) {
            mStop = false;
            mThread = new Thread(this);
            mThread.start();
        }
    }

    /**
     * Stop task
     * <p>Set mStop flag with true and notify task thread, at last, it will
     * check if task is alive every 500ms with 3 times to make sure the thread
     * stop</p>
     */
    public void stop() {
        synchronized (this) {
            mStop = true;
            notify();
        }

        // wait for thread stopping
        for (int i = 0; i < 3 && mThread.isAlive(); ++i) {
            Log.d(TAG, "Waiting thread to stop ...");
            try {
                Thread.sleep(500);
            }
            catch (InterruptedException e) {

            }
        }

        if (mThread.isAlive()) {
            Log.d(TAG, "Thread is still alive after waited 1.5s!");
        }
    }

    /**
     * Set bitmap width , height and maximum size of cache queue
     *
     * @param w width of bitmap
     * @param h height of bitmap
     * @param maxCached maximum size of cache queue
     */
    public void set(int w, int h, int maxCached) {
        int newIndex = LARGE_BG;
        if ((w <= 480 && h <= 854) ||
            (w <= 854 && h <= 480)) {
            newIndex = SMALL_BG;
        }
        else if ((w <= 800 && h <= 1280) ||
                 (h <= 800 && w <= 1280)) {
            newIndex = MEDIUM_BG;
        }

        mIsLandscape = w > h;

        if (maxCached != mQueueMaxSize) {
            mQueueMaxSize = maxCached;
        }

        if (newIndex != mBGSizeIndex) {
            mBGSizeIndex = newIndex;
            synchronized (this) {
                cleanQueue();
                notify();
            }
        }
    }

    /**
     * Load bitmap from resources randomly
     *
     * @return bitmap object
     */
    private Bitmap getRandomBitmap() {
        int newNo = mPreRandomNo;
        while (newNo == mPreRandomNo) {
            newNo = mBGRandom.nextInt(BG_COUNT);
        }

        mPreRandomNo = newNo;
        int resId = mPortraitBGs[mBGSizeIndex][newNo];
        Bitmap b = BitmapFactory.decodeResource(mResources, resId);
        // if (mIsLandscape) {
        //     Matrix matrix = new Matrix();
        //     matrix.postRotate(90);
        //     Bitmap lb = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(),
        //                             matrix, true);
        //     b.recycle();
        //     return lb;
        // }

        return b;
    }

    /**
     * Clear cache queue
     */
    private void cleanQueue() {
        for (int i = 0; i < mQueue.size(); ++i) {
            mQueue.get(i).recycle();
        }
        mQueue.clear();
    }

    public void run() {
        while (true) {
            synchronized (this) {
                // check if ask thread stopping
                if (mStop) {
                    cleanQueue();
                    break;
                }

                // load bitmap only when no cached bitmap in queue
                int size = mQueue.size();
                if (size < 1) {
                    for (int i = 0; i < mQueueMaxSize; ++i) {
                        Log.d(TAG, "Load Queue:" + i + " in background!");
                        mQueue.push(getRandomBitmap());
                    }
                }

                // wait to be awaken
                try {
                    wait();
                }
                catch (InterruptedException e) {
                }
            }
        }
    }
}
