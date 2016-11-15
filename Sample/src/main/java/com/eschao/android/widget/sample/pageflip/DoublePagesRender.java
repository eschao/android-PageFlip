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
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import com.eschao.android.widget.pageflip.Page;
import com.eschao.android.widget.pageflip.PageFlip;
import com.eschao.android.widget.pageflip.PageFlipState;

/**
 * Double pages render
 * <p>
 * Some key points here:
 * <ul>
 *     <li>First page is which page user is clicking on or moving by finger
 *          Sometimes it is left page on screen, sometimes it is right page.
 *          Second page is leftover page against the first page
 *     </li>
 *     <li>mPageNo is always the number of left page instead of first page</li>
 * </ul>
 * </p>
 * <p>
 * Every screen 'Page' contains 3 page contents, so it need 3 textures:
 * <ul>
 *     <li>First texture: first page content of this 'Page'</li>
 *     <li>Back texture: the second page content of this 'Page'</li>
 *     <li>Second texture: the third page content of this 'Page'</li>
 * </ul>
 * </p>
 *
 * @author eschao
 */

public class DoublePagesRender extends PageRender {

    /**
     * Constructor
     * @see {@link #PageRender(Context, PageFlip, Handler, int)}
     */
    public DoublePagesRender(Context context, PageFlip pageFlip,
                             Handler handler, int pageNo) {
        super(context, pageFlip, handler, pageNo);
    }

    /**
     * Draw page frame
     */
    public void onDrawFrame() {
        // 1. delete unused textures to save memory
        mPageFlip.deleteUnusedTextures();

        // 2. there are two pages for representing the whole screen, we need to
        // draw them one by one
        final Page first = mPageFlip.getFirstPage();
        final Page second = mPageFlip.getSecondPage();

        // 3. check if the first texture is valid for first page, if not,
        // create it with relative content
        if (!first.isFirstTextureSet()) {
            drawPage(first.isLeftPage() ? mPageNo : mPageNo + 1);
            first.setFirstTexture(mBitmap);
        }

        // 4. check if the first texture is valid for second page
        if (!second.isFirstTextureSet()) {
            drawPage(second.isLeftPage() ? mPageNo : mPageNo + 1);
            second.setFirstTexture(mBitmap);
        }

        // 5. handle drawing command triggered from finger moving and animating
        if (mDrawCommand == DRAW_MOVING_FRAME ||
            mDrawCommand == DRAW_ANIMATING_FRAME) {
            // before drawing, check if back texture of first page is valid
            // Remember: the first page is always the fold page
            if (!first.isBackTextureSet()) {
                drawPage(first.isLeftPage() ? mPageNo - 1 : mPageNo + 2);
                first.setBackTexture(mBitmap);
            }

            // check the second texture of first page is valid.
            if (!first.isSecondTextureSet()) {
                drawPage(first.isLeftPage() ? mPageNo - 2 : mPageNo + 3);
                first.setSecondTexture(mBitmap);
            }

            // draw frame for page flip
            mPageFlip.drawFlipFrame();
        }
        // draw stationary page without flipping
        else if (mDrawCommand == DRAW_FULL_PAGE){
            mPageFlip.drawPageFrame();
        }

        // 6. send message to main thread to notify drawing is ended so that
        // we can continue to calculate next animation frame if need.
        // Remember: the drawing operation is always in GL thread instead of
        // main thread
        Message msg = Message.obtain();
        msg.what = MSG_ENDED_DRAWING_FRAME;
        msg.arg1 = mDrawCommand;
        mHandler.sendMessage(msg);
    }

    /**
     * Handle GL surface is changed
     *
     * @param background background bitmap
     */
    public void onSurfaceChanged(Bitmap background) {
        // recycle bitmap resources if need
        if (mBackgroundBitmap != null) {
            mBackgroundBitmap.recycle();
        }

        if (mBitmap != null) {
            mBitmap.recycle();
        }

        // create bitmap and canvas for page
        mBackgroundBitmap = background;
        Page page = mPageFlip.getFirstPage();
        mBitmap = Bitmap.createBitmap((int)page.width(), (int)page.height(),
                                      Bitmap.Config.ARGB_8888);
        mCanvas.setBitmap(mBitmap);
    }

    /**
     * Handle ended drawing event
     * In here, we only tackle the animation drawing event, If we need to
     * continue requesting render, please return true. Remember this function
     * will be called in main thread
     *
     * @param what event type
     * @return ture if need render again
     */
    public boolean onEndedDrawing(int what) {
        if (what == DRAW_ANIMATING_FRAME) {
            boolean isAnimating = mPageFlip.animating();
            // continue animating
            if (isAnimating) {
                mDrawCommand = DRAW_ANIMATING_FRAME;
                return true;
            }
            // animation is finished
            else {
                // should handle forward flip to update page number and exchange
                // textures between first and second pages. Don'top have to handle
                // backward flip since there is no such state happened in double
                // page mode
                if (mPageFlip.getFlipState() == PageFlipState.END_WITH_FORWARD)
                {
                    final Page first = mPageFlip.getFirstPage();
                    final Page second = mPageFlip.getSecondPage();
                    second.swapTexturesWithPage(first);

                    // update page number for left page
                    if (first.isLeftPage()) {
                        mPageNo -= 2;
                    }
                    else {
                        mPageNo += 2;
                    }
                }

                mDrawCommand = DRAW_FULL_PAGE;
                return true;
            }
        }
        return false;
    }

    /**
     * Draw page content
     *
     * @param number page number
     */
    private void drawPage(int number) {
        final int width = mCanvas.getWidth();
        final int height = mCanvas.getHeight();
        Paint p = new Paint();
        p.setFilterBitmap(true);

        // 1. draw background bitmap
        Rect rect = new Rect(0, 0, width, height);
        if (width > height) {
            mCanvas.rotate(90);
            mCanvas.drawBitmap(mBackgroundBitmap, null, rect, p);
            mCanvas.rotate(-90);
        }
        else {
            mCanvas.drawBitmap(mBackgroundBitmap, null, rect, p);
        }

        // 2. draw page number
        p.setColor(Color.WHITE);
        p.setStrokeWidth(1);
        p.setAntiAlias(true);
        p.setTextSize(100);

        String text = String.valueOf(number);
        if (number < 1) {
            text = "Preface";
        }
        else if (number > MAX_PAGES) {
            text = "End";
        }
        float textWidth = p.measureText(text);
        mCanvas.drawText(text, (width - textWidth) / 2, height / 2, p);
    }

    /**
     * If page can flip forward
     *
     * @return true if it can flip forward
     */
    public boolean canForwardFlip() {
        final Page page = mPageFlip.getFirstPage();
        // current page is left page
        if (page.isLeftPage()) {
            if (mPageNo > 1) {
                return true;
            }
            else {
                Toast.makeText(mContext,
                               "This is the first page!",
                               Toast.LENGTH_SHORT)
                     .show();
            }
        }
        // current page is right page
        else {
            if (mPageNo + 2 <= MAX_PAGES) {
                return true;
            }
            else {
                Toast.makeText(mContext,
                               "This is the last page!",
                               Toast.LENGTH_SHORT)
                     .show();
            }
        }

        return false;
    }

    /**
     * Don'top need to handle backward flip
     *
     * @return always false
     */
    public boolean canBackwardFlip() {
        return false;
    }
}
