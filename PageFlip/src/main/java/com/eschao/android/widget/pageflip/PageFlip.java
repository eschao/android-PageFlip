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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PointF;
import android.opengl.GLUtils;
import android.util.Log;
import android.view.animation.AccelerateInterpolator;
import android.widget.Scroller;

import com.eschao.android.widget.pageflip.Page.GPoint;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_TEST;
import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glClearDepthf;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glTexParameterf;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glViewport;

/**
 * 3D Style Page Flip
 *
 * @author escchao
 */
public class PageFlip {
	final static String TAG	= "PageFlip";

    // default pixels of mesh vertex
    private final static int DEFAULT_MESH_VERTEX_PIXELS = 10;

    // The min page curl angle (5 degree)
    private final static int MIN_PAGE_CURL_ANGLE = 5;
    private final static int MAX_PAGE_CURL_ANGLE = 65;
    private final static int PAGE_CURL_ANGEL_DIFF = MAX_PAGE_CURL_ANGLE -
                                                    MIN_PAGE_CURL_ANGLE;
    private final static float MIN_PAGE_CURL_RADIAN =
                                (float)(Math.PI * MIN_PAGE_CURL_ANGLE / 180);
    private final static float MAX_PAGE_CURL_RADIAN=
                                (float)(Math.PI * MAX_PAGE_CURL_ANGLE / 180);
    private final static float MIN_PAGE_CURL_TAN_OF_ANGLE =
                                (float)Math.tan(MIN_PAGE_CURL_RADIAN);
    private final static float MAX_PAGE_CURL_TAN_OF_ANGEL =
                                (float)Math.tan(MAX_PAGE_CURL_RADIAN);
    private final static float MAX_PAGE_CURL_ANGLE_RATIO =
                                MAX_PAGE_CURL_ANGLE / 90f;
    private final static float MAX_TAN_OF_FORWARD_FLIP =
                                (float)Math.tan(Math.PI / 6);
    private final static float MAX_TAN_OF_BACKWARD_FLIP =
                                (float)Math.tan(Math.PI / 20);

    // folder page shadow color buffer size
    private final static int FOLD_TOP_EDGE_SHADOW_VEX_COUNT = 22;

    // fold edge shadow color
    private final static float FOLD_EDGE_SHADOW_START_COLOR = 0.1f;
    private final static float FOLD_EDGE_SHADOW_START_ALPHA = 0.2f;
    private final static float FOLD_EDGE_SHADOW_END_COLOR = 0.3f;
    private final static float FOLD_EDGE_SHADOW_END_ALPHA = 0f;

    // fold base shadow color
    private final static float FOLD_BASE_SHADOW_START_COLOR = 0.05f;
    private final static float FOLD_BASE_SHADOW_START_ALPHA = 0.25f;
    private final static float FOLD_BASE_SHADOW_END_COLOR = 0.3f;
    private final static float FOLD_BASE_SHADOW_END_ALPHA = 0f;

    // first and second page
    private final static int FIRST_PAGE = 0;
    private final static int SECOND_PAGE = 1;
    private final static int PAGE_SIZE = 2;
    private final static int SINGLE_PAGE_MODE = 0;
    private final static int AUTO_PAGE_MODE = 1;

    // view size
    private GLViewRect mViewRect;

    // the pixel size for each mesh
    private int mPixelsOfMesh;

    // gradient shadow texture id
    private int mGradientShadowTextureID;

    // touch point and last touch point
    private PointF mTouchP;
    // the last touch point (could be deleted?)
    private PointF mLastTouchP;
    // the first touch point when finger down on the screen
    private PointF mStartTouchP;
    // the middle point between touch point and origin point
    private PointF mMiddleP;

    // from 2D perspective, the line will intersect Y axis and X axis that being
    // through middle point and perpendicular to the line which is from touch
    // point to origin point, The point on Y axis is mYFoldP, the mXFoldP is on
    // X axis. The mY{X}FoldP1 is up mY{X}FoldP, The mY{X}FoldP0 is under
    // mY{X}FoldP
    //
    //        <----- Flip
    //                          ^ Y
    //                          |
    //                          + mYFoldP1
    //                        / |
    //                       /  |
    //                      /   |
    //                     /    |
    //                    /     |
    //                   /      |
    //                  /       + mYFoldP
    //    mTouchP      /      / |
    //       .        /      /  |
    //               /      /   |
    //              /      /    |
    //             /      /     |
    //            /   .  /      + mYFoldP0
    //           /      /      /|
    //          /      /      / |
    //         /      /      /  |
    //X <-----+------+------+---+ originP
    //   mXFoldP1 mXFoldP mXFoldP0
    //
    private PointF mYFoldP;
    private PointF mYFoldP0;
    private PointF mYFoldP1;
    private PointF mXFoldP;
    private PointF mXFoldP0;
    private PointF mXFoldP1;

    //            ^ Y
    //   mTouchP  |
    //        +   |
    //         \  |
    //          \ |
    //       A ( \|
    // X <--------+ originP
    //
    // A is angle between X axis and line from mTouchP to originP
    // the max curling angle between line from touchP to originP and X axis
    private float mMaxT2OAngleTan;
    // another max curling angle when finger moving cause the originP change
    // from (x, y) to (x, -y) which means mirror based on Y axis.
    private float mMaxT2DAngleTan;
    // the tan value of current curling angle
    // mKValue = (touchP.y - originP.y) / (touchP.x - originP.x)
    private float mKValue;
    // the length of line from mTouchP to originP
    private float mLenOfTouchOrigin;
    // the cylinder radius
    private float mR;
    // the perimeter ratio of semi-cylinder based on mLenOfTouchOrigin;
    private float mSemiPerimeterRatio;
    // Mesh count
    private int mMeshCount;

    // edges shadow width of back of fold page
    private ShadowWidth mFoldBackEdgesShadowWidth;
    // base shadow width of front of fold page
    private ShadowWidth mFoldFrontBaseShadowWidth;

    // fold page and shadow vertexes
    private Vertexes mFoldFrontVertexes;
    private FoldBackVertexes mFoldBackVertexes;
    private ShadowVertexes mFoldBackEdgesShadow;
    private ShadowVertexes mFoldFrontBaseShadow;

    // Shader program for openGL drawing
    private VertexProgram mVertexProgram;
    private FoldBackVertexProgram mFoldBackVertexProgram;
    private ShadowVertexProgram mShadowVertexProgram;

    // is vertical page flip
    private boolean mIsVertical;
    private PageFlipState mFlipState;

    // use for flip animation
    private Scroller mScroller;
    private Context mContext;

    // pages and page mode
    // in single page mode, there is only one page in the index 0
    // in double pages mode, there are two pages, the first one is always active
    // page which is receiving finger events, for example: finger down/move/up
    private Page mPages[];
    private int mPageMode;

    // is clicking to flip page
    private boolean mIsClickToFlip;

    // listener for page flipping
    private OnPageFlipListener mListener;

    /**
     * Constructor
     */
    public PageFlip(Context context) {
        mContext = context;
        mScroller = new Scroller(context, new AccelerateInterpolator(), true);
        mFlipState = PageFlipState.END_FLIP;
        mIsVertical = false;
        mViewRect = new GLViewRect();
        mPixelsOfMesh = DEFAULT_MESH_VERTEX_PIXELS;
        mSemiPerimeterRatio = 0.8f;
        mIsClickToFlip = true;
        mListener = null;

        // init pages
        mPages = new Page[PAGE_SIZE];
        mPageMode = SINGLE_PAGE_MODE;

        // key points
        mMiddleP = new PointF();
        mYFoldP = new PointF();
        mYFoldP0 = new PointF();
        mYFoldP1 = new PointF();
        mXFoldP = new PointF();
        mXFoldP0 = new PointF();
        mXFoldP1 = new PointF();
        mTouchP = new PointF();
        mLastTouchP = new PointF();
        mStartTouchP = new PointF();

        // init shadow width
        mFoldBackEdgesShadowWidth = new ShadowWidth(5, 30, 0.25f);
        mFoldFrontBaseShadowWidth = new ShadowWidth(2, 40, 0.4f);

        // init shader program
        mVertexProgram = new VertexProgram();
        mFoldBackVertexProgram = new FoldBackVertexProgram();
        mShadowVertexProgram = new ShadowVertexProgram();

        // init vertexes
        mFoldFrontVertexes = new Vertexes();
        mFoldBackVertexes = new FoldBackVertexes();
        mFoldBackEdgesShadow = new ShadowVertexes(FOLD_TOP_EDGE_SHADOW_VEX_COUNT,
                                                  FOLD_EDGE_SHADOW_START_COLOR,
                                                  FOLD_EDGE_SHADOW_START_ALPHA,
                                                  FOLD_EDGE_SHADOW_END_COLOR,
                                                  FOLD_EDGE_SHADOW_END_ALPHA);
        mFoldFrontBaseShadow = new ShadowVertexes(0,
                                                  FOLD_BASE_SHADOW_START_COLOR,
                                                  FOLD_BASE_SHADOW_START_ALPHA,
                                                  FOLD_BASE_SHADOW_END_COLOR,
                                                  FOLD_BASE_SHADOW_END_ALPHA);

        // init default scroller
        mScroller = new Scroller(context);
    }

    /**
     * Enable/disable page mode
     * <pre>
     * The default value is single page mode, which means there is only one page
     * no matter what the screen is portrait or landscape. If set mode with auto
     * page, it will automatically detect screen mode and choose single or
     * double pages to render the whole screen.
     * </pre>
     *
     * @param isAuto true if set mode with auto page
     * @return self
     */
    public PageFlip enableAutoPageMode(boolean isAuto) {
        mPageMode = isAuto ? AUTO_PAGE_MODE : SINGLE_PAGE_MODE;
        return this;
    }

    /**
     * Enable/disable clicking to flip page
     * <pre>
     *  By default, the page flipping will only be triggered by finger.
     *  Through this function to enable clicking, you can start flipping
     *  page with finger click.
     * </pre>
     *
     * @param enable true if enable it
     * @return self
     */
    public PageFlip enableClickToFlip(boolean enable) {
        mIsClickToFlip = enable;
        return this;
    }

    /**
     * Set listener for page flip
     * <pre>
     * Set a page flip listener to determine if page can flip forward or
     * backward
     * </pre>
     *
     * @param listener a listener for page flip
     * @return
     */
    public PageFlip setListener(OnPageFlipListener listener) {
        mListener = listener;
        return this;
    }

    /**
     * Sets pixels of mesh vertex
     * <p>The default value is 10 pixels for each mesh</p>
     *
     * @param pixelsOfMesh pixel amount of each mesh
     * @return self
     */
    public PageFlip setPixelsOfMesh(int pixelsOfMesh) {
        mPixelsOfMesh = pixelsOfMesh > 0 ? pixelsOfMesh :
                        DEFAULT_MESH_VERTEX_PIXELS;
        return this;
    }

    /**
     * Set ratio of semi-perimeter of fold cylinder
     * <pre>
     * When finger is clicking and moving on page, the page from touch point to
     * original point will be curled like as a cylinder, the radius of cylinder
     * is determined by line length from touch point to original point. You can
     * give a ratio of this line length to set cylinder radius, the default
     * value is 0.8
     * </pre>
     *
     * @param ratio ratio of line length from touch point to original point. Its
     *              value is (0..1]
     * @return self
     */
    public PageFlip setSemiPerimeterRatio(float ratio) {
        if (ratio <= 0 || ratio > 1) {
           throw new IllegalArgumentException("Invalid ratio value: " + ratio);
        }

        mSemiPerimeterRatio = ratio;
        return this;
    }

    /**
     * Set mask alpha for back of fold page
     * <pre>Mask alpha will be invalid in double pages</pre>
     *
     * @param alpha alpha value is in [0..255]
     * @return self
     */
    public PageFlip setMaskAlphaOfFold(int alpha) {
        mFoldBackVertexes.setMaskAlpha(alpha);
        return this;
    }

    /**
     * Sets edge shadow color of fold page
     *
     * @param startColor shadow start color: [0..1]
     * @param startAlpha shadow start alpha: [0..1]
     * @param endColor shadow end color: [0..1]
     * @param endAlpha shadow end alpha: [0..1]
     * @return self
     */
    public PageFlip setShadowColorOfFoldEdges(float startColor,
                                              float startAlpha,
                                              float endColor,
                                              float endAlpha) {
        mFoldBackEdgesShadow.mColor.set(startColor, startAlpha,
                                        endColor, endAlpha);
        return this;
    }

    /**
     * Sets base shadow color of fold page
     *
     * @param startColor shadow start color: [0..1]
     * @param startAlpha shadow start alpha: [0..1]
     * @param endColor shadow end color: [0..1]
     * @param endAlpha shadow end alpha: [0..1]
     * @return self
     */
    public PageFlip setShadowColorOfFoldBase(float startColor,
                                             float startAlpha,
                                             float endColor,
                                             float endAlpha) {
        mFoldFrontBaseShadow.mColor.set(startColor, startAlpha,
                                        endColor, endAlpha);
        return this;
    }

    /**
     * Set shadow width of fold edges
     *
     * @param min minimal width
     * @param max maximum width
     * @param ratio width ratio based on fold cylinder radius. It is in (0..1)
     * @return self
     */
    public PageFlip setShadowWidthOfFoldEdges(float min,
                                              float max,
                                              float ratio) {
        mFoldBackEdgesShadowWidth.set(min, max, ratio);
        return this;
    }

    /**
     * Set shadow width of fold base
     *
     * @param min minimal width
     * @param max maximum width
     * @param ratio width ratio based on fold cylinder radius. It is in (0..1)
     * @return self
     */
    public PageFlip setShadowWidthOfFoldBase(float min,
                                             float max,
                                             float ratio) {
        mFoldFrontBaseShadowWidth.set(min, max, ratio);
        return this;
    }

    /**
     * Get page flip state
     *
     * @return page flip state
     */
    public PageFlipState getFlipState() {
        return mFlipState;
    }

    /**
     * Handle surface creation event
     *
     * @throws PageFlipException if failed to compile and link OpenGL shader
     */
    public void onSurfaceCreated() throws PageFlipException {
        glClearColor(0, 0, 0, 1f);
        glClearDepthf(1.0f);
        glEnable(GL_DEPTH_TEST);

        try {
            // init shader programs
            mVertexProgram.init(mContext);
            mFoldBackVertexProgram.init(mContext);
            mShadowVertexProgram.init(mContext);

            // create gradient shadow texture
            createGradientShadowTexture();
        }
        catch (PageFlipException e) {
            mVertexProgram.delete();
            mFoldBackVertexProgram.delete();
            mShadowVertexProgram.delete();
            throw e;
        }
    }

    /**
     * Handle surface changing event
     *
     * @param width surface width
     * @param height surface height
     * @throws PageFlipException if failed to compile and link OpenGL shader
     */
    public void onSurfaceChanged(int width, int height) throws
                                                        PageFlipException {
        mViewRect.set(width, height);
        glViewport(0, 0, width, height);
        mVertexProgram.initMatrix(-mViewRect.halfW, mViewRect.halfW,
                                  -mViewRect.halfH, mViewRect.halfH);
        computeMeshVertexesCount();

        // release textures hold in pages
        if (mPages[FIRST_PAGE] != null) {
            mPages[FIRST_PAGE].deleteAllTextures();
        }

        if (mPages[SECOND_PAGE] != null) {
            mPages[SECOND_PAGE].deleteAllTextures();
        }

        // landscape
        if (mPageMode == AUTO_PAGE_MODE && width > height) {
            mPages[FIRST_PAGE] = new Page(mViewRect.left, 0,
                                          mViewRect.top, mViewRect.bottom);
            mPages[SECOND_PAGE] = new Page(0, mViewRect.right,
                                           mViewRect.top, mViewRect.bottom);
        }
        else {
            mPages[FIRST_PAGE] = new Page(mViewRect.left, mViewRect.right,
                                          mViewRect.top, mViewRect.bottom);
            mPages[SECOND_PAGE] = null;
        }
    }

    /**
     * Handle finger down event
     *
     * @param touchX x of finger down point
     * @param touchY y of finger down point
     */
	public void onFingerDown(float touchX, float touchY) {
        // covert to OpenGL coordinate
        touchX = mViewRect.toOpenGLX(touchX);
        touchY = mViewRect.toOpenGLY(touchY);

        // check if touch point is contained in page?
        boolean isContained = false;
        if (mPages[FIRST_PAGE].contains(touchX, touchY)) {
            isContained = true;
        }
        else if (mPages[SECOND_PAGE].contains(touchX, touchY)) {
            // in double pages, the first page always is active page which touch
            // event is happening on
            isContained = true;
            Page p = mPages[SECOND_PAGE];
            mPages[SECOND_PAGE] = mPages[FIRST_PAGE];
            mPages[FIRST_PAGE] = p;
        }

        // point is contained, ready to flip
        if (isContained) {
            mMaxT2OAngleTan = 0f;
            mMaxT2DAngleTan = 0f;
            mLastTouchP.set(touchX, touchY);
            mStartTouchP.set(touchX, touchY);
            mTouchP.set(touchX, touchY);
            mFlipState = PageFlipState.BEGIN_FLIP;
        }
	}

    /**
     * Handle finger moving event
     *
     * @param touchX x of finger moving point
     * @param touchY y of finger moving point
     * @return true if moving will trigger to draw a new frame for page flip,
     *         False means the movement should be ignored.
     */
	public boolean onFingerMove(float touchX, float touchY) {
        touchX  = mViewRect.toOpenGLX(touchX);
        touchY  = mViewRect.toOpenGLY(touchY);

        // compute moving distance (dx, dy)
        float dy = (touchY - mStartTouchP.y);
        float dx = (touchX - mStartTouchP.x);

        final Page page = mPages[FIRST_PAGE];
        final GPoint originP = page.originP;
        final GPoint diagonalP = page.diagonalP;

        // begin to move
        if (mFlipState == PageFlipState.BEGIN_FLIP &&
            (Math.abs(dx) > mViewRect.width / 10)) {
            // set OriginP and DiagonalP points
            page.setOriginPoint(mPages[SECOND_PAGE] != null, dy);

            // compute max degree between X axis and line from TouchP to OriginP
            // and max degree between X axis and line from TouchP to
            // (OriginP.x, DiagonalP.Y)
            float y2o = Math.abs(mStartTouchP.y - originP.y);
            float y2d = Math.abs(mStartTouchP.y - diagonalP.y);
            mMaxT2OAngleTan = computeTanOfCurlAngle(y2o);
            mMaxT2DAngleTan = computeTanOfCurlAngle(y2d);

            // moving at the top and bottom screen have different tan value of
            // angle
            if ((originP.y < 0 && page.right > 0) ||
                (originP.y > 0 && page.right <= 0)) {
                mMaxT2OAngleTan = -mMaxT2OAngleTan;
            }
            else {
                mMaxT2DAngleTan = -mMaxT2DAngleTan;
            }

            // determine if it is moving backward or forward
            if (mPages[SECOND_PAGE] == null &&
                dx > 0 &&
                mListener != null &&
                mListener.canFlipBackward()) {
                mStartTouchP.x = originP.x;
                dx = (touchX - mStartTouchP.x);
                mFlipState = PageFlipState.BACKWARD_FLIP;
            }
            else if (mListener != null &&
                     mListener.canFlipForward() &&
                     (dx < 0 && originP.x > 0 || dx > 0 && originP.x < 0)) {
                mFlipState = PageFlipState.FORWARD_FLIP;
            }
        }

        // in moving, compute the TouchXY
        if (mFlipState == PageFlipState.FORWARD_FLIP ||
            mFlipState == PageFlipState.BACKWARD_FLIP ||
            mFlipState == PageFlipState.RESTORE_FLIP) {

            // check if page is flipping vertically
            mIsVertical = Math.abs(dy) <= 1f;

            // multiply a factor to make sure the touch point is always head of
            // finger point
            if (PageFlipState.FORWARD_FLIP == mFlipState) {
                dx *= 1.2f;
            }
            else {
                dx *= 1.1f;
            }

            // moving direction is changed
            if ((dy < 0 && originP.y < 0) || (dy > 0 && originP.y > 0)) {
                float t = mMaxT2DAngleTan;
                mMaxT2DAngleTan = mMaxT2OAngleTan;
                mMaxT2OAngleTan = t;

                t = originP.y;
                originP.y = diagonalP.y;
                diagonalP.y = t;

                t = originP.tY;
                originP.tY = diagonalP.tY;
                diagonalP.tY = t;
            }

            // compute new TouchP.y
            float maxY = dx * mMaxT2OAngleTan;
            if (Math.abs(dy) > Math.abs(maxY)) {
                dy = maxY;
            }

            // check if XFoldX1 is out of page width, if yes, recompute new
            // TouchP.y to assure the XFoldX1 is in page width
            float t2oK = dy / dx;
            float xTouchX = dx + dy * t2oK;
            float xRatio = (1 + mSemiPerimeterRatio) * 0.5f;
            float xFoldX1 = xRatio * xTouchX;
            if (Math.abs(xFoldX1) + 2 >= page.width) {
                float dy2 = ((diagonalP.x - originP.x) / xRatio - dx) * dx;
                // ignore current moving if we can't get a valid dy, for example
                // , in double pages mode, when finger is moving from the one
                // page to another page, the dy2 is negative and should be
                // ignored
                if (dy2 < 0) {
                    return false;
                }

                dy = (int)Math.sqrt(dy2);
                if (originP.y > 0) {
                    dy = -dy;
                }
            }

            // set touchP(x, y) and middleP(x, y)
            mLastTouchP.set(touchX, touchY);
            mTouchP.set(dx + originP.x, dy + originP.y);
            mMiddleP.x = (mTouchP.x + originP.x) * 0.5f;
            mMiddleP.y = (mTouchP.y + originP.y) * 0.5f;

            // continue to compute points to drawing flip
            computeVertexesAndBuildPage();
            return true;
        }

        return false;
    }

    /**
     * Handles finger up event
     *
     * @param touchX x of finger moving point
     * @param touchY y of finger moving point
     * @param duration millisecond for page flip animation
     * @return true if animation is started or animation is not triggered
     */
    public boolean onFingerUp(float touchX, float touchY, int duration) {
        touchX  = mViewRect.toOpenGLX(touchX);
        touchY  = mViewRect.toOpenGLY(touchY);

        final Page page = mPages[FIRST_PAGE];
        final GPoint originP = page.originP;
        final GPoint diagonalP = page.diagonalP;
        final boolean hasSecondPage = mPages[SECOND_PAGE] != null;
        Point start = new Point((int)mTouchP.x, (int)mTouchP.y);
        Point end = new Point(0, 0);

        // forward flipping
        if (mFlipState == PageFlipState.FORWARD_FLIP) {
            // can't going forward, restore current page
            if (page.isXInRange(touchX, 0.4f)) {
                end.x = (int)originP.x;
                mFlipState = PageFlipState.RESTORE_FLIP;
            }
            else if (hasSecondPage && originP.x < 0) {
                end.x = (int)(diagonalP.x + page.width);
            }
            else {
                end.x = (int)(diagonalP.x - page.width);
            }
            end.y = (int)(originP.y);
        }
        // backward flipping
        else if (mFlipState == PageFlipState.BACKWARD_FLIP ||
                 mFlipState == PageFlipState.RESTORE_FLIP) {
            mMaxT2OAngleTan = (mTouchP.y - originP.y) / (mTouchP.x - originP.x);
            end.set((int)originP.x, (int)originP.y);
        }
        // ready to flip
        else if (mFlipState == PageFlipState.BEGIN_FLIP) {
            mIsVertical = false;
            mFlipState = PageFlipState.END_FLIP;
            page.setOriginPoint(hasSecondPage, -touchY);

            // if enable clicking to flip, compute scroller points for animation
            if (mIsClickToFlip) {
                computeScrollPointsForClickingFlip(touchX, start, end);
            }
        }

        // start scroller for animating
        if (mFlipState == PageFlipState.FORWARD_FLIP ||
            mFlipState == PageFlipState.BACKWARD_FLIP ||
            mFlipState == PageFlipState.RESTORE_FLIP) {
            mScroller.startScroll(start.x, start.y,
                                  end.x - start.x, end.y - start.y,
                                  duration);
            return true;
        }

        return false;
    }

    /**
     * Check finger point to see if it can trigger a flip animation
     *
     * @param touchX x of finger point
     * @param touchY y of finger point
     * @return true if the point can trigger a flip animation
     */
    public boolean canAnimate(float touchX, float touchY) {
        return (mFlipState == PageFlipState.FORWARD_FLIP &&
                !mPages[FIRST_PAGE].contains(mViewRect.toOpenGLX(touchX),
                                             mViewRect.toOpenGLY(touchY)));
    }

    /**
     * Compute scroller points for animating
     *
     * @param x x of clicking point
     * @param start start point of scroller will be set
     * @param end end point of scroller will be set
     */
    private void computeScrollPointsForClickingFlip(float x,
                                                    Point start,
                                                    Point end) {
        Page page = mPages[FIRST_PAGE];
        GPoint originP = page.originP;
        GPoint diagonalP = page.diagonalP;
        final boolean hasSecondPage = mPages[SECOND_PAGE] != null;

        // forward and backward flip have different degree
        float tanOfForwardAngle = MAX_TAN_OF_FORWARD_FLIP;
        float tanOfBackwardAngle = MAX_TAN_OF_BACKWARD_FLIP;
        if ((originP.y < 0 && originP.x > 0) ||
            (originP.y > 0 && originP.x < 0)) {
            tanOfForwardAngle = -tanOfForwardAngle;
            tanOfBackwardAngle = -tanOfBackwardAngle;
        }

        // backward flip
        if (!hasSecondPage &&
            x < diagonalP.x + page.width * 0.3f &&
            mListener != null &&
            mListener.canFlipBackward()) {
            mFlipState = PageFlipState.BACKWARD_FLIP;
            mKValue = tanOfBackwardAngle;
            start.set((int)diagonalP.x,
                      (int)(originP.y + (start.x - originP.x) * mKValue));
            end.set((int)originP.x - 5, (int)originP.y);
        }
        // forward flip
        else if (mListener != null &&
                 mListener.canFlipForward() &&
                 page.isXInRange(x, 0.3f)) {
            mFlipState = PageFlipState.FORWARD_FLIP;
            mKValue = tanOfForwardAngle;

            // compute start.x
            if (originP.x < 0) {
                start.x = (int)(originP.x + page.width * 0.25f);
            }
            else {
                start.x = (int)(originP.x - page.width * 0.25f);
            }

            // compute start.y
            start.y = (int)(originP.y + (start.x - originP.x) * mKValue);

            // compute end.x
            // left page in double page mode
            if (hasSecondPage && originP.x < 0) {
                end.x = (int)(diagonalP.x + page.width);
            }
            // right page in double page mode
            else {
                end.x = (int)(diagonalP.x - page.width);
            }
            end.y = (int)(originP.y);
        }
    }

    /**
     * Compute animating and check if it can continue
     *
     * @return true animating is continue or it is stopped
     */
    public boolean animating() {
        final Page page = mPages[FIRST_PAGE];
        final GPoint originP = page.originP;
        final GPoint diagonalP = page.diagonalP;

        // is to end animating?
        boolean isAnimating = !mScroller.isFinished();
        if (isAnimating) {
            // get new (x, y)
            mScroller.computeScrollOffset();
            mTouchP.set(mScroller.getCurrX(), mScroller.getCurrY());

            // for backward and restore flip, compute x to check if it can
            // continue to flip
            if (mFlipState == PageFlipState.BACKWARD_FLIP ||
                mFlipState == PageFlipState.RESTORE_FLIP) {
                mTouchP.y = (mTouchP.x - originP.x) * mKValue + originP.y;
                isAnimating = Math.abs(mTouchP.x - originP.x) > 10;
            }
            // check if flip is vertical
            else {
                mIsVertical = Math.abs(mTouchP.y - originP.y) < 1f;
            }

            // compute middle point
            mMiddleP.set((mTouchP.x + originP.x) * 0.5f,
                         (mTouchP.y + originP.y) * 0.5f);

            // compute key points
            if (mIsVertical) {
                computeKeyVertexesWhenVertical();
            }
            else {
                computeKeyVertexesWhenSlope();
            }

            // in double page mode
            if (mPages[SECOND_PAGE] != null) {
                // if the xFoldP1.x is out of page width, need to limit
                // xFoldP1.x is in page.width and recompute new key points so
                // that the page flip is still going forward
                if (page.isXOutOfPage(mXFoldP1.x)) {
                    mXFoldP1.x = diagonalP.x;
                    float cosA = (mTouchP.x - originP.x) / mLenOfTouchOrigin;
                    float ratio = 1 - page.width * Math.abs(cosA) /
                                      mLenOfTouchOrigin;
                    mR = (float)(mLenOfTouchOrigin * (1 - 2 * ratio) / Math.PI);
                    mXFoldP0.x = mLenOfTouchOrigin * ratio / cosA + originP.x;

                    if (mIsVertical) {
                        mYFoldP0.x = mXFoldP0.x;
                        mYFoldP1.x = mXFoldP1.x;
                    }
                    else {
                        mYFoldP1.y = originP.y + (mXFoldP1.x - originP.x)
                                                 / mKValue;
                        mYFoldP0.y = originP.y + (mXFoldP0.x - originP.x)
                                                 / mKValue;
                    }

                    // re-compute mesh count
                    float len = Math.abs(mMiddleP.x - mXFoldP0.x);
                    if (mMeshCount > len) {
                        mMeshCount = (int)len;
                    }
                    isAnimating = mMeshCount > 0 &&
                                  Math.abs(mXFoldP0.x - diagonalP.x) >= 2;
                }
            }
            // in single page mode, check if the whole fold page is out of
            // screen and animating should be stopped
            else if (mFlipState == PageFlipState.FORWARD_FLIP) {
                float r = (float)(mLenOfTouchOrigin * mSemiPerimeterRatio /
                                  Math.PI);
                float x = (mYFoldP1.y - diagonalP.y) * mKValue + r;
                isAnimating = x > (diagonalP.x - originP.x);
            }
        }

        // animation is stopped
        if (!isAnimating) {
            abortAnimating();
        }
        // continue animation and compute vertexes
        else if (mIsVertical) {
            computeVertexesWhenVertical();
        }
        else {
            computeVertexesWhenSlope();
        }

    	return isAnimating;
    }

    /**
     * Is animating ?
     *
     * @return true if page is flipping
     */
    public boolean isAnimating() {
        return !mScroller.isFinished();
    }

    /**
     * Abort animating
     */
    public void abortAnimating() {
    	mScroller.abortAnimation();
        if (mFlipState == PageFlipState.FORWARD_FLIP) {
            mFlipState = PageFlipState.END_WITH_FORWARD;
        }
        else if (mFlipState == PageFlipState.BACKWARD_FLIP) {
            mFlipState = PageFlipState.END_WITH_BACKWARD;
        }
        else if (mFlipState == PageFlipState.RESTORE_FLIP) {
            mFlipState = PageFlipState.END_WITH_RESTORE;
        }
    }

    /**
     * Is animation stated?
     *
     * @return true if flip is started
     */
    public boolean isStartedFlip() {
        return mFlipState == PageFlipState.BACKWARD_FLIP ||
               mFlipState == PageFlipState.FORWARD_FLIP ||
               mFlipState == PageFlipState.RESTORE_FLIP;
    }

    /**
     * The moving is ended?
     *
     * @return true if flip is ended
     */
    public boolean isEndedFlip() {
    	return mFlipState == PageFlipState.END_FLIP ||
               mFlipState == PageFlipState.END_WITH_RESTORE ||
               mFlipState == PageFlipState.END_WITH_BACKWARD ||
               mFlipState == PageFlipState.END_WITH_FORWARD;
    }

    /**
     * Get the first page
     * First page is currently operating page which means it is the page user
     * finger is clicking or moving
     *
     * @return flip state, See {@link PageFlipState}
     */
    public Page getFirstPage() {
        return mPages[FIRST_PAGE];
    }

    /**
     * Get the second page
     * <pre>
     * Second page is only valid in double page mode, if it is null, that means
     * there is only one page for whole screen whatever the screen is portrait
     * or landscape
     * </pre>
     *
     * @return the second page, null if no second page
     */
    public Page getSecondPage() {
        return mPages[SECOND_PAGE];
    }

    /**
     * Delete unused textures
     */
    public void deleteUnusedTextures() {
        mPages[FIRST_PAGE].deleteUnusedTextures();
        if (mPages[SECOND_PAGE] != null) {
            mPages[SECOND_PAGE].deleteUnusedTextures();
        }
    }

    /**
     * Draw flipping frame
     */
    public void drawFlipFrame() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        final boolean hasSecondPage = mPages[SECOND_PAGE] != null;

        // 1. draw fold back part
        glUseProgram(mFoldBackVertexProgram.hProgram);
        glActiveTexture(GL_TEXTURE0);
        mFoldBackVertexes.draw(mFoldBackVertexProgram,
                               mPages[FIRST_PAGE],
                               hasSecondPage,
                               mGradientShadowTextureID);


        // 2. draw unfold page and fold front part
        glUseProgram(mVertexProgram.hProgram);
        glActiveTexture(GL_TEXTURE0);
        glUniformMatrix4fv(mVertexProgram.hMVPMatrix, 1, false,
                           VertexProgram.MVPMatrix, 0);
        mPages[FIRST_PAGE].drawFrontPage(mVertexProgram,
                                         mFoldFrontVertexes);
        if (hasSecondPage) {
            mPages[SECOND_PAGE].drawFullPage(mVertexProgram, true);
        }

        // 3. draw edge and base shadow of fold parts
        glUseProgram(mShadowVertexProgram.hProgram);
        mFoldFrontBaseShadow.draw(mShadowVertexProgram);
        mFoldBackEdgesShadow.draw(mShadowVertexProgram);
    }

    /**
     * Draw frame with full page
     */
	public void drawPageFrame() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glUseProgram(mVertexProgram.hProgram);
        glUniformMatrix4fv(mVertexProgram.hMVPMatrix, 1, false,
                           VertexProgram.MVPMatrix, 0);
        glActiveTexture(GL_TEXTURE0);

        // 1. draw first page
        mPages[FIRST_PAGE].drawFullPage(mVertexProgram, true);

        // 2. draw second page if have
        if (mPages[SECOND_PAGE] != null) {
            mPages[SECOND_PAGE].drawFullPage(mVertexProgram, true);
        }
	}

    /**
     * Compute max mesh count and allocate vertexes buffer
     */
    private void computeMeshVertexesCount() {
        // compute max mesh count
        int maxMeshCount = (int)mViewRect.minOfWH() / mPixelsOfMesh;

        // make sure the vertex count is even number
        if (maxMeshCount % 2 != 0) {
            maxMeshCount++;
        }

        // init vertexes buffers
        mFoldBackVertexes.set(maxMeshCount + 2);
        mFoldFrontVertexes.set(maxMeshCount + 8, 3, true);
        mFoldBackEdgesShadow.set(maxMeshCount + 2);
        mFoldFrontBaseShadow.set(maxMeshCount + 2);
    }

    /**
     * Create gradient shadow texture for lighting effect
     */
    private void createGradientShadowTexture() {
        int textureIDs[] = new int[1];
        glGenTextures(1, textureIDs, 0);
        glActiveTexture(GL_TEXTURE0);
        mGradientShadowTextureID = textureIDs[0];

        // gradient shadow texture
        Bitmap shadow = PageFlipUtils.createGradientBitmap();
        glBindTexture(GL_TEXTURE_2D, mGradientShadowTextureID);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        GLUtils.texImage2D(GL_TEXTURE_2D, 0, shadow, 0);
        shadow.recycle();
	}

    /**
     * Compute vertexes of page
     */
    private void computeVertexesAndBuildPage() {
        if (mIsVertical) {
            computeKeyVertexesWhenVertical();
            computeVertexesWhenVertical();
        }
        else {
            computeKeyVertexesWhenSlope();
            computeVertexesWhenSlope();
        }
    }

    /**
     * Compute key vertexes when page turning is vertical
     */
    private void computeKeyVertexesWhenVertical() {
        final float oX = mPages[FIRST_PAGE].originP.x ;
        final float oY = mPages[FIRST_PAGE].originP.y;
        final float dY = mPages[FIRST_PAGE].diagonalP.y;

        mTouchP.y = oY;
        mMiddleP.y = oY;

        // set key point on X axis
        float r0 = 1 - mSemiPerimeterRatio;
        float r1 = 1 + mSemiPerimeterRatio;
        mXFoldP.set(mMiddleP.x, oY);
        mXFoldP0.set(oX + (mXFoldP.x - oX) * r0, mXFoldP.y);
        mXFoldP1.set(oX + r1 * (mXFoldP.x - oX), mXFoldP.y);

        // set key point on Y axis
        mYFoldP.set(mMiddleP.x, dY);
        mYFoldP0.set(mXFoldP0.x, mYFoldP.y);
        mYFoldP1.set(mXFoldP1.x, mYFoldP.y);

        // line length from mTouchP to originP
        mLenOfTouchOrigin = Math.abs(mTouchP.x - oX);
        mR = (float)(mLenOfTouchOrigin * mSemiPerimeterRatio / Math.PI);

        // compute mesh count
        computeMeshCount();
    }

    /**
     * Compute all vertexes when page flip is vertical
     */
    private void computeVertexesWhenVertical() {
        int count = mMeshCount;// >> 1;
        float x = mMiddleP.x;
        float stepX = (mMiddleP.x - mXFoldP0.x) / count;

        final Page page = mPages[FIRST_PAGE];
        final float oY = page.originP.y;
        final float dY = page.diagonalP.y;
        final float cDY = page.diagonalP.tY;
        final float cOY = page.originP.tY;
        final float cOX = page.originP.tX;

        // compute the point on back page half cylinder
        mFoldBackVertexes.reset();

        for (int i = 0; i <= count; ++i, x -= stepX) {
            // compute radian of x point
            float x2t = x - mXFoldP1.x;
            float radius = x2t / mR;
            float sinR = (float)Math.sin(radius);
            float coordX = page.textureX(x);
            float fx = mXFoldP1.x + mR * sinR;
            float fz = (float) (mR * (1 - Math.cos(radius)));

            // compute vertex when it is curled
            mFoldBackVertexes.addVertex(fx, dY, fz, sinR, coordX, cDY)
                             .addVertex(fx, oY, fz, sinR, coordX, cOY);
        }

        float tx0 = mTouchP.x;
        mFoldBackVertexes.addVertex(tx0, dY, 1, 0, cOX, cDY)
                         .addVertex(tx0, oY, 1, 0, cOX, cOY)
                         .toFloatBuffer();

        // compute shadow width
        float sw = -mFoldBackEdgesShadowWidth.width(mR);
        float bw = mFoldFrontBaseShadowWidth.width(mR);
        if (page.originP.x < 0) {
            sw = -sw;
            bw = -bw;
        }

        // fold base shadow
        float bx0 = mFoldBackVertexes.mVertexes[0];
        mFoldFrontBaseShadow.setVertexes(0, bx0, oY, bx0 + bw, oY)
                            .setVertexes(8, bx0, dY, bx0 + bw, dY)
                            .toFloatBuffer(16);

        // fold edge shadow
        mFoldBackEdgesShadow.setVertexes(0, tx0, oY, tx0 + sw, oY)
                            .setVertexes(8, tx0, dY, tx0 + sw, dY)
                            .toFloatBuffer(16);

        // fold front
        mFoldFrontVertexes.reset();
        page.buildVertexesOfPageWhenVertical(mFoldFrontVertexes, mXFoldP1);
        mFoldFrontVertexes.toFloatBuffer();
    }

    /**
     * Compute key vertexes when page flip is slope
     */
	private void computeKeyVertexesWhenSlope() {
        final float oX = mPages[FIRST_PAGE].originP.x;
        final float oY = mPages[FIRST_PAGE].originP.y;

		float dX = mMiddleP.x - oX;
		float dY = mMiddleP.y - oY;
		
        // compute key points on X axis
        float r0 = 1 - mSemiPerimeterRatio;
        float r1 = 1 + mSemiPerimeterRatio;
		mXFoldP.set(mMiddleP.x + dY * dY / dX, oY);
        mXFoldP0.set(oX + (mXFoldP.x - oX) * r0, mXFoldP.y);
        mXFoldP1.set(oX + r1 * (mXFoldP.x - oX), mXFoldP.y);

        // compute key points on Y axis
		mYFoldP.set(oX, mMiddleP.y + dX * dX / dY);
        mYFoldP0.set(mYFoldP.x, oY + (mYFoldP.y - oY) * r0);
        mYFoldP1.set(mYFoldP.x, oY + r1 * (mYFoldP.y - oY));

        // line length from TouchXY to OriginalXY
        mLenOfTouchOrigin = (float)Math.hypot((mTouchP.x - oX),
                                              (mTouchP.y - oY));

        // cylinder radius
        mR = (float)(mLenOfTouchOrigin * mSemiPerimeterRatio / Math.PI);

        // compute line slope
        mKValue = (mTouchP.y - oY) / (mTouchP.x - oX);

        // compute mesh count
        computeMeshCount();
	}

    /**
     * Compute back vertex and edge shadow vertex of fold page
     * <pre>
     * In 2D coordinate system, for every vertex on fold page, we will follow
     * the below steps to compute its 3D point (x,y,z) on curled page(cylinder):
     * 1. Deem originP as (0, 0) to simplify the next computing steps
     * 2. translate point(x, y) to new coordinate system (originP is (0, 0))
     * 3. rotate point(x, y) with curling angle A in clockwise
     * 4. compute 3d point (x, y, z) for 2d point(x, y), at this time, the
     *    cylinder is vertical in new coordinate system which will help us
     *    compute point
     * 5. rotate 3d point (x, y, z) with -A to restore
     * 6. translate 3d point (x, y, z) to original coordinate system
     *
     * For point of edge shadow, the most computing steps are same but:
     * 1. shadow point is following the page point except different x coordinate
     * 2. shadow point has same z coordinate with the page point
     * </pre>
     *
     * @param isY is vertex for x point on x axis or y point on y axis?
     * @param x0 x of point on axis
     * @param y0 y of point on axis
     * @param sx0 x of edge shadow point
     * @param sy0 y of edge shadow point
     * @param tX x of xFoldP1 point in rotated coordinate system
     * @param sinA sin value of page curling angle
     * @param cosA cos value of page curling angel
     * @param coordX x of texture coordinate
     * @param coordY y of texture coordinate
     * @param oX x of originate point
     * @param oY y of originate point
     */
    private void computeBackVertex(boolean isY, float x0, float y0, float sx0,
                                   float sy0, float tX, float sinA, float cosA,
                                   float coordX, float coordY, float oX,
                                   float oY) {
        // rotate degree A
        float x = x0 * cosA - y0 * sinA;
        float y = x0 * sinA + y0 * cosA;

        // rotate degree A for vertexes of fold edge shadow
        float sx = sx0 * cosA - sy0 * sinA;
        float sy = sx0 * sinA + sy0 * cosA;

        // compute mapping point on cylinder
        float rad = (x - tX) / mR;
        double sinR = Math.sin(rad);
        x = (float) (tX + mR * sinR);
        float cz = (float) (mR * (1 - Math.cos(rad)));

        // rotate degree -A, sin(-A) = -sin(A), cos(-A) = cos(A)
        float cx = x * cosA + y * sinA + oX;
        float cy = y * cosA - x * sinA + oY;
        mFoldBackVertexes.addVertex(cx, cy, cz, (float)sinR, coordX, coordY);

        // compute coordinates of fold shadow edge
        float sRadian = (sx - tX) / mR;
        sx = (float)(tX + mR * Math.sin(sRadian));
        mFoldBackEdgesShadow.addVertexes(isY, cx, cy,
                                         sx * cosA + sy * sinA + oX,
                                         sy * cosA - sx * sinA + oY);
    }

    /**
     * Compute back vertex of fold page
     * <pre>
     * Almost same with another computeBackVertex function except expunging the
     * shadow point part
     * </pre>
     *
     * @param x0 x of point on axis
     * @param y0 y of point on axis
     * @param tX x of xFoldP1 point in rotated coordinate system
     * @param sinA sin value of page curling angle
     * @param cosA cos value of page curling angel
     * @param coordX x of texture coordinate
     * @param coordY y of texture coordinate
     * @param oX x of originate point
     * @param oY y of originate point
     */
    private void computeBackVertex(float x0, float y0, float tX,
                                   float sinA, float cosA, float coordX,
                                   float coordY, float oX, float oY) {
        // rotate degree A
        float x = x0 * cosA - y0 * sinA;
        float y = x0 * sinA + y0 * cosA;

        // compute mapping point on cylinder
        float rad = (x - tX) / mR;
        double sinR = Math.sin(rad);
        x = (float) (tX + mR * sinR);
        float cz = (float) (mR * (1 - Math.cos(rad)));

        // rotate degree -A, sin(-A) = -sin(A), cos(-A) = cos(A)
        float cx = x * cosA + y * sinA + oX;
        float cy = y * cosA - x * sinA + oY;
        mFoldBackVertexes.addVertex(cx, cy, cz, (float)sinR, coordX, coordY);
    }

    /**
     * Compute front vertex and base shadow vertex of fold page
     * <p>Its computing principle is almost same with
     * {@link #computeBackVertex(boolean, float, float, float, float, float,
     * float, float, float, float, float, float)}</p>
     *
     * @param isX is vertex for x point on x axis or y point on y axis?
     * @param x0 x of point on axis
     * @param y0 y of point on axis
     * @param tX x of xFoldP1 point in rotated coordinate system
     * @param sinA sin value of page curling angle
     * @param cosA cos value of page curling angel
     * @param baseWcosA base shadow width * cosA
     * @param baseWsinA base shadow width * sinA
     * @param coordX x of texture coordinate
     * @param coordY y of texture coordinate
     * @param oX x of originate point
     * @param oY y of originate point
     */
    private void computeFrontVertex(boolean isX, float x0, float y0, float tX,
                                    float sinA, float cosA,
                                    float baseWcosA, float baseWsinA,
                                    float coordX, float coordY,
                                    float oX, float oY) {
        // rotate degree A
        float x = x0 * cosA - y0 * sinA;
        float y = x0 * sinA + y0 * cosA;

        // compute mapping point on cylinder
        float rad = (x - tX)/ mR;
        x = (float)(tX + mR * Math.sin(rad));
        float cz = (float)(mR * (1 - Math.cos(rad)));

        // rotate degree -A, sin(-A) = -sin(A), cos(-A) = cos(A)
        float cx = x * cosA + y * sinA + oX;
        float cy = y * cosA - x * sinA + oY;
        mFoldFrontVertexes.addVertex(cx, cy, cz, coordX, coordY);
        mFoldFrontBaseShadow.addVertexes(isX, cx, cy,
                                         cx + baseWcosA, cy - baseWsinA);
    }

    /**
     * Compute vertexes when page flip is slope
     */
    private void computeVertexesWhenSlope() {
        final Page page = mPages[FIRST_PAGE];
        final float oX = page.originP.x;
        final float oY = page.originP.y;
        final float dY = page.diagonalP.y;
        final float cOX = page.originP.tX;
        final float cOY = page.originP.tY;
        final float cDY = page.diagonalP.tY;
        final float height = page.height;
        final float d2oY = dY - oY;

        // compute radius and sin/cos of angle
        float sinA = (mTouchP.y - oY) / mLenOfTouchOrigin;
        float cosA = (oX - mTouchP.x) / mLenOfTouchOrigin;

        // need to translate before rotate, and then translate back
        int count = mMeshCount;
        float xFoldP1 = (mXFoldP1.x - oX) * cosA;
        float edgeW = mFoldBackEdgesShadowWidth.width(mR);
        float baseW = mFoldFrontBaseShadowWidth.width(mR);
        float baseWcosA = baseW * cosA;
        float baseWsinA = baseW * sinA;
        float edgeY = oY > 0 ? edgeW : -edgeW;
        float edgeX = oX > 0 ? edgeW : -edgeW;
        float stepSY = edgeY / count;
        float stepSX = edgeX / count;

        // reset vertexes buffer counter
        mFoldBackEdgesShadow.reset();
        mFoldFrontBaseShadow.reset();
        mFoldFrontVertexes.reset();
        mFoldBackVertexes.reset();

        // add the first 3 float numbers is fold triangle
        mFoldBackVertexes.addVertex(mTouchP.x, mTouchP.y, 1, 0, cOX, cOY);

        // compute vertexes for fold back part
        float stepX = (mXFoldP0.x - mXFoldP.x) / count;
        float stepY = (mYFoldP0.y - mYFoldP.y) / count;
        float x = mXFoldP0.x - oX;
        float y = mYFoldP0.y - oY;
        float sx = edgeX;
        float sy = edgeY;

        int i = 0;
        for (;i <= count && Math.abs(y) < height;
             ++i, x -= stepX, y -= stepY, sy -= stepSY, sx -= stepSX) {
            computeBackVertex(false, x, 0, x, sy, xFoldP1, sinA, cosA,
                              page.textureX(x + oX), cOY, oX, oY);
            computeBackVertex(true, 0, y, sx, y, xFoldP1, sinA, cosA, cOX,
                              page.textureY(y + oY), oX, oY);
        }

        if (i <= count) {
            if (y != height) {
                if (Math.abs(mYFoldP0.y - oY) > height) {
                    float tx = oX + 2 * mKValue * (mYFoldP.y - dY);
                    float ty = dY + mKValue * (tx - oX);
                    mFoldBackVertexes.addVertex(tx, ty, 1, 0, cOX, cDY);
                    float tsx = tx - sx;
                    float tsy = dY + mKValue * (tsx - oX);
                    mFoldBackEdgesShadow.addVertexes(true, tx, ty, tsx, tsy);
                }
                else {
                    float x1 = mKValue * d2oY;
                    computeBackVertex(false, x1, 0, x1, sy, xFoldP1, sinA, cosA,
                                      page.textureX(x1 + oX), cOY, oX, oY);
                    computeBackVertex(true, 0, d2oY, sx, d2oY, xFoldP1, sinA,
                                      cosA, cOX, cDY, oX, oY) ;
                }
            }

            for (; i <= count;
                 ++i, x -= stepX, y -= stepY, sy -= stepSY, sx -= stepSX) {
                computeBackVertex(false, x, 0, x, sy, xFoldP1, sinA, cosA,
                                  page.textureX(x + oX), cOY, oX, oY);
                float x1 = mKValue * (y + oY - dY);
                computeBackVertex(x1, d2oY, xFoldP1, sinA, cosA,
                                  page.textureX(x1 + oX), cDY, oX, oY);
            }
        }

        mFoldBackVertexes.toFloatBuffer();

        // compute vertexes for front fold
        stepX = (mXFoldP.x - mXFoldP1.x) / count;
        stepY = (mYFoldP.y - mYFoldP1.y) / count;
        x = mXFoldP.x - oX - stepX;
        y = mYFoldP.y - oY - stepY;
        int j = 1;
        for (; j <= count && Math.abs(y) < height; ++j, x -= stepX, y -= stepY) {
            computeFrontVertex(true, x, 0, xFoldP1, sinA, cosA,
                               baseWcosA, baseWsinA,
                               page.textureX(x + oX), cOY, oX, oY);
            computeFrontVertex(false, 0, y, xFoldP1, sinA, cosA,
                               baseWcosA, baseWsinA,
                               cOX, page.textureY(y + oY), oX, oY);
        }

        for (; j <= count; ++j, x -= stepX, y -= stepY) {
            computeFrontVertex(true, x, 0, xFoldP1, sinA, cosA,
                               baseWcosA, baseWsinA,
                               page.textureX(x + oX), cOY, oX, oY);
            float x1 = mKValue * (y + oY - dY);
            computeFrontVertex(false, x1, d2oY, xFoldP1, sinA, cosA,
                               baseWcosA, baseWsinA,
                               page.textureX(x1 + oX), cDY, oX, oY);
        }

        // set uniform Z value for shadow vertexes
        mFoldBackEdgesShadow.vertexZ = mFoldFrontVertexes.getFloatAt(2);
        mFoldFrontBaseShadow.vertexZ = -0.5f;

        // add two vertexes to connect with the unfold front page
        page.buildVertexesOfPageWhenSlope(mFoldFrontVertexes, mXFoldP1, mYFoldP1,
                                          mKValue);
        mFoldFrontVertexes.toFloatBuffer();

        // compute vertexes of fold edge shadow
        mFoldFrontBaseShadow.toFloatBuffer();
        computeVertexesOfFoldTopEdgeShadow(mTouchP.x, mTouchP.y, sinA, cosA,
                                           -edgeX, edgeY);
        mFoldBackEdgesShadow.toFloatBuffer();
    }

    /**
     * Compute vertexes of fold top edge shadow
     * <p>>Top edge shadow of fold page is a quarter circle</p>
     *
     * @param x0 X of touch point
     * @param y0 Y of touch point
     * @param sinA Sin value of page curling angle
     * @param cosA Cos value of page curling angle
     * @param sx Shadow width on X axis
     * @param sy Shadow width on Y axis
     */
    private void computeVertexesOfFoldTopEdgeShadow(float x0, float y0,
                                                    float sinA, float cosA,
                                                    float sx, float sy) {
        float sin2A = 2 * sinA * cosA;
        float cos2A = (float)(1 - 2 * Math.pow(sinA, 2));
        float r = 0;
        float dr = (float)(Math.PI / (FOLD_TOP_EDGE_SHADOW_VEX_COUNT - 2));
        int size = FOLD_TOP_EDGE_SHADOW_VEX_COUNT / 2;
        int j = mFoldBackEdgesShadow.mMaxBackward;

        //                 ^ Y                             __ |
        //      TouchP+    |                             /    |
        //             \   |                            |     |
        //              \  |                             \    |
        //               \ |              X <--------------+--+- OriginP
        //                \|                                 /|
        // X <----------+--+- OriginP                       / |
        //             /   |                               /  |
        //             |   |                              /   |
        //              \__+ Top edge              TouchP+    |
        //                 |                                  v Y
        // 1. compute quarter circle at origin point
        // 2. rotate quarter circle to touch point direction
        // 3. move quarter circle to touch point as top edge shadow
        for (int i = 0; i < size; ++i, r += dr, j += 8) {
            float x = (float)(sx * Math.cos(r));
            float y = (float)(sy * Math.sin(r));

            // rotate -2A and then translate to touchP
            mFoldBackEdgesShadow.setVertexes(j, x0, y0,
                                             x * cos2A + y * sin2A + x0,
                                             y * cos2A - x * sin2A + y0);
        }
    }

    /**
     * Compute mesh count for page flip
     */
    private void computeMeshCount() {
        float dx = Math.abs(mXFoldP0.x - mXFoldP1.x);
        float dy = Math.abs(mYFoldP0.y - mYFoldP1.y);
        if (mIsVertical) {
            mMeshCount = (int)dx / mPixelsOfMesh;
        }
        else {
            mMeshCount = (int)Math.min(dx, dy) / mPixelsOfMesh;
        }

        // keep count is even
        if (mMeshCount % 2 != 0) {
            mMeshCount++;
        }

        // half count for fold page
        mMeshCount >>= 1;
    }

    /**
     * Compute tan value of curling angle
     *
     * @param dy the diff value between touchP.y and originP.y
     * @return tan value of max curl angle
     */
    private float computeTanOfCurlAngle(float dy) {
        float ratio = dy / mViewRect.halfH;
        if (ratio <= 1 - MAX_PAGE_CURL_ANGLE_RATIO) {
            return MAX_PAGE_CURL_TAN_OF_ANGEL;
        }

        float degree = MAX_PAGE_CURL_ANGLE - PAGE_CURL_ANGEL_DIFF * ratio;
        if (degree < MIN_PAGE_CURL_ANGLE) {
            return MIN_PAGE_CURL_TAN_OF_ANGLE;
        }
        else {
            return (float)Math.tan(Math.PI * degree / 180);
        }
    }


    /**
     * Debug information
     */
    private void debugInfo() {
        final GPoint originP = mPages[FIRST_PAGE].originP;
        final GPoint diagonalP = mPages[FIRST_PAGE].diagonalP;

        Log.d(TAG, "************************************");
        Log.d(TAG, " Mesh Count:    " + mMeshCount);
        Log.d(TAG, " Mesh Pixels:   " + mPixelsOfMesh);
        Log.d(TAG, " Origin:        " + originP.x + ", " + originP.y);
        Log.d(TAG, " Diagonal:      " + diagonalP.x + ", " + diagonalP.y);
        Log.d(TAG, " OriginTouchP:  " + mStartTouchP.x + ", "
                   + mStartTouchP.y);
        Log.d(TAG, " TouchP:        " + mTouchP.x + ", " + mTouchP.y);
        Log.d(TAG, " MiddleP:       " + mMiddleP.x + ", " + mMiddleP.y);
        Log.d(TAG, " XFoldP:        " + mXFoldP.x + ", " + mXFoldP.y);
        Log.d(TAG, " XFoldP0:       " + mXFoldP0.x + ", " + mXFoldP0.y);
        Log.d(TAG, " XFoldP1:       " + mXFoldP1.x + ", " + mXFoldP1.y);
        Log.d(TAG, " YFoldP:        " + mYFoldP.x + ", " + mYFoldP.y);
        Log.d(TAG, " YFoldP0:       " + mYFoldP0.x + ", " + mYFoldP0.y);
        Log.d(TAG, " YFoldP1:       " + mYFoldP1.x + ", " + mYFoldP1.y);
        Log.d(TAG, " LengthT->O:    " + mLenOfTouchOrigin);
    }
}
