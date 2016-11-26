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
import android.graphics.Color;
import android.graphics.PointF;
import android.opengl.GLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_TRIANGLE_FAN;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glDeleteTextures;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glTexParameterf;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glVertexAttribPointer;

/**
 * Page class
 * <p>
 * Page holds content textures and show them on screen. In single page mode, a
 * page represents the whole screen area. But in double pages mode, there are
 * two pages to depict the entire screen size, in the left part is called left
 * page and the right part is called right page.
 * Every page has the below properties:
 * </p>
 * <ul>
 *     <li>Page size: left/right/top/bottom and width/height</li>
 *     <li>Holding 3 content textures for drawing:
 *          <ul>
 *              <li>The first texture: which is showing on screen when page is
 *              stationary, we can relatively call it as the first 'Page' at
 *              some extend</li>
 *              <li>The second texture: normally it can be called the second
 *              'Page' against the first texture. It will be appeared when page
 *              is flipping or flip is over, in the later, the second texture
 *              will eventually become the first one</li>
 *              <li>The back texture: in single page mode, the back texture is
 *              always same with the first texture, thus, the caller shouldn't
 *              set it before drawing. But in double pages mode, it should be
 *              set with a different texture and can be called the second 'Page'
 *              , at this time, the second texture will be called the third
 *              'Page' as like we're reading a book</li>
 *              <li>Every texture should be set with a bitmap by outside caller
 *              </li>
 *          </ul>
 *     </li>
 * </ul>
 *
 * @author eschao
 */

public class Page {

    private final static int TEXTURE_SIZE = 3;
    private final static int FIRST_TEXTURE_ID = 0;
    private final static int SECOND_TEXTURE_ID = 1;
    private final static int BACK_TEXTURE_ID = 2;
    private final static int INVALID_TEXTURE_ID = -1;

    /**
     * <p>
     * 4 apexes of page has different permutation order according to original
     * point since original point will be changed when user click to curl page
     * from different direction. There are 4 kinds of order:
     * </p><pre>
     *   A           B           C           D
     * 2    1      3    0      0    3      1    2
     * +----+      +----+      +----+      +----+
     * |    |      |    |      |    |      |    |
     * +----+      +----+      +----+      +----+
     * 3    0      2    1      1    2      0    3
     *             From A      From A      From A
     *             0 <-> 1     0 <-> 2     0 <-> 3
     *             3 <-> 2     3 <-> 1     1 <-> 2
     * </pre>
     * <ul>
     *      <li>0 always represents the origin point, accordingly 2 is diagonal
     *      point</li>
     *      <li>Case A is default order: 0 -> 1 -> 2 -> 3</li>
     *      <li>Every apex data is stored in mApexes following the case A order
     *      and never changed</li>
     *      <li>This array is mapping apex order (case A - D) to real apex data
     *      stored in mApexes. For example:
     *      <ul>
     *          <li>Case A has same order with storing sequence of apex data in
     *          mApexes</li>
     *          <li>Case B: the 0 apex is stored in 1 position in mApexes</li>
     *      </ul></li>
     *  </ul>
     */
    private final static int[][] mPageApexOrders = new int[][] {
        new int[] {0, 1, 2, 3}, // for case A
        new int[] {1, 0, 3, 2}, // for case B
        new int[] {2, 3, 0, 1}, // for case C
        new int[] {3, 2, 1, 0}, // for case D
    };

    /**
     * <p>When page is curled, there are 4 kinds of vertexes orders for drawing
     * first texture and second texture with TRIANGLE_STRIP way</p><pre>
     *     A             B              C              D
     * 2       1     2     X 1      2 X     1      2       1
     * +-------+     +-----.-+      +-.-----+      +-------+
     * |       |     | F  /  |      |/      |      |   F   |
     * |   F   .Y    |   /   |     Y.   S   |     X.-------.Y
     * |      /|     |  /    |      |       |      |   S   |
     * +-----.-+     +-.-----+      +-------+      +-------+
     * 3    X  0     3 Y     0      3       0      3       0
     * </pre>
     * <ul>
     *      <li>All cases are based on the apex order case A(0 -> 1 -> 2 -> 3)
     *      </li>
     *      <li>X is xFoldX point, Y is yFoldY point</li>
     *      <li>Case A means: xFoldX and yFoldY are both in page</li>
     *      <li>Case B means: xFoldX is in page, but yFoldY is the intersecting
     *      point with line 1->2 since yFoldY is outside the page</li>
     *      <li>Case C means: xFoldX and yFoldY are both outside the page</li>
     *      <li>Case D means: xFoldX outside page but yFoldY is in the page</li>
     *      <li>Combining {@link #mPageApexOrders} with this array, we can get
     *      the right apex data from mApexes array which will help us quickly
     *      organizing triangle data for openGL drawing</li>
     *      <li>The last array in this array means: xFoldX and yFoldY are both
     *      outside the page and the whole page will be draw with second
     *      texture</li>
     * </ul>
     */
    private final static int[][] mFoldVexOrders = new int[][] {
        new int[] {4, 3, 1, 2, 0}, // Case A
        new int[] {3, 3, 2, 0, 1}, // Case B
        new int[] {3, 2, 1, 3, 0}, // Case C
        new int[] {2, 2, 3, 1, 0}, // Case D
        new int[] {1, 0, 1, 3, 2}, // Case E
    };

    // page size
    float left;
    float right;
    float top;
    float bottom;
    float width;
    float height;

    // texture size for rendering page, normally they are same with page width
    // and height
    float texWidth;
    float texHeight;

    /**
     * <p>origin point and diagonal point</p>
     * <pre>
     * 0-----+
     * |     |
     * |     |
     * +-----1
     * </pre>
     * <p>if origin(x, y) is 1, the diagonal(x, y) is 0</p>
     */
    GPoint originP;
    GPoint diagonalP;

    private GPoint mXFoldP;
    private GPoint mYFoldP;

    // vertexes and texture coordinates buffer for full page
    private FloatBuffer mFullPageVexBuf;
    private FloatBuffer mFullPageTexCoordsBuf;

    // storing 4 apexes data of page
    private float[] mApexes;
    // texture coordinates for page apex
    private float[] mApexTexCoords;
    // vertex size of front of fold page and unfold page
    private int mFrontVertexSize;
    // index of apex order array for current original point
    private int mApexOrderIndex;

    // mask color of back texture
    float[][] maskColor;

    // texture(front, back and second) ids allocated by OpenGL
    private int[] mTexIDs;
    // unused texture ids, will be deleted when next OpenGL drawing
    private int[] mUnusedTexIDs;
    // actual size of mUnusedTexIDs
    private int mUnusedTexSize;

    /**
     * Constructor
     */
    public Page() {
        init(0, 0, 0, 0);
    }

    /**
     * Constructor with page size
     */
    public Page(float l, float r, float t, float b) {
        init(l, r, t, b);
    }

    private void init(float l, float r, float t, float b) {
        top = t;
        left = l;
        right = r;
        bottom = b;
        width = right - left;
        height = top - bottom;
        texWidth = width;
        texHeight = height;
        mFrontVertexSize = 0;
        mApexOrderIndex = 0;

        mXFoldP = new GPoint();
        mYFoldP = new GPoint();
        originP = new GPoint();
        diagonalP = new GPoint();

        maskColor = new float[][] {new float[] {0, 0, 0},
                                   new float[] {0, 0, 0},
                                   new float[] {0, 0, 0}};

        mTexIDs = new int[] {INVALID_TEXTURE_ID,
                             INVALID_TEXTURE_ID,
                             INVALID_TEXTURE_ID};
        mUnusedTexSize = 0;
        mUnusedTexIDs = new int[] {INVALID_TEXTURE_ID,
                                   INVALID_TEXTURE_ID,
                                   INVALID_TEXTURE_ID};

        createVertexesBuffer();
        buildVertexesOfFullPage();
    }

    /**
     * Is the left page?
     * <p>Left page represents the left screen in double pages mode</p>
     *
     * @return true if current page is left page
     */
    public boolean isLeftPage() {
        return right <= 0;
    }

    /**
     * Is the right page?
     * <p>Right page represents the right screen in double pages mode</p>
     *
     * @return true if current page is right page
     */
    public boolean isRightPage() {
        return left >= 0;
    }

    /**
     * Get page width
     *
     * @return page width
     */
    public float width() {
        return width;
    }

    /**
     * Gets page height
     *
     * @return page height
     */
    public float height() {
        return height;
    }

    /**
     * Is the first texture set?
     *
     * @return true if the first texture is set
     */
    public boolean isFirstTextureSet() {
        return mTexIDs[FIRST_TEXTURE_ID] != INVALID_TEXTURE_ID;
    }

    /**
     * Is the second texture set ?
     *
     * @return true if the second texture is set
     */
    public boolean isSecondTextureSet() {
        return mTexIDs[SECOND_TEXTURE_ID] != INVALID_TEXTURE_ID;
    }

    /**
     * Is the back texture set ?
     *
     * @return true if the back texture is set
     */
    public boolean isBackTextureSet() {
        return mTexIDs[BACK_TEXTURE_ID] != INVALID_TEXTURE_ID;
    }

    /**
     * Deletes unused texture ids
     * <p>It should be called in OpenGL thread</p>
     */
    public void deleteUnusedTextures() {
        if (mUnusedTexSize > 0) {
            glDeleteTextures(mUnusedTexSize, mUnusedTexIDs, 0);
            mUnusedTexSize = 0;
        }
    }

    /**
     * Recycle the first texture id and set it with the second texture
     * <p>Manually call this function to set the first texture with the second
     * one after page forward flipped over in single page mode.</p>
     *
     * @return self
     */
    public Page setFirstTextureWithSecond() {
        if (mTexIDs[FIRST_TEXTURE_ID] > INVALID_TEXTURE_ID) {
            mUnusedTexIDs[mUnusedTexSize++] = mTexIDs[FIRST_TEXTURE_ID];
        }

        maskColor[FIRST_TEXTURE_ID][0] = maskColor[SECOND_TEXTURE_ID][0];
        maskColor[FIRST_TEXTURE_ID][1] = maskColor[SECOND_TEXTURE_ID][1];
        maskColor[FIRST_TEXTURE_ID][2] = maskColor[SECOND_TEXTURE_ID][2];
        mTexIDs[FIRST_TEXTURE_ID] = mTexIDs[SECOND_TEXTURE_ID];
        mTexIDs[SECOND_TEXTURE_ID] = INVALID_TEXTURE_ID;
        return this;
    }

    /**
     * Recycle the second texture id and set it with the first texture
     * <p>Manually call this function to set the second texture with the first
     * one when page is backward flipping in single page mode.</p>
     *
     * @return self
     */
    public Page setSecondTextureWithFirst() {
        if (mTexIDs[SECOND_TEXTURE_ID] > INVALID_TEXTURE_ID) {
            mUnusedTexIDs[mUnusedTexSize++] = mTexIDs[SECOND_TEXTURE_ID];
        }

        maskColor[SECOND_TEXTURE_ID][0] = maskColor[FIRST_TEXTURE_ID][0];
        maskColor[SECOND_TEXTURE_ID][1] = maskColor[FIRST_TEXTURE_ID][1];
        maskColor[SECOND_TEXTURE_ID][2] = maskColor[FIRST_TEXTURE_ID][2];
        mTexIDs[SECOND_TEXTURE_ID] = mTexIDs[FIRST_TEXTURE_ID];
        mTexIDs[FIRST_TEXTURE_ID] = INVALID_TEXTURE_ID;
        return this;

    }

    /**
     * Swap textures of two pages and recycle unused texture ids
     * <p>Call this function when page is flipped over in double pages mode</p>
     *
     * @param page another page
     * @return self
     */
    public Page swapTexturesWithPage(Page page) {
        // [second page]: second -> first
        mUnusedTexIDs[mUnusedTexSize++] = mTexIDs[SECOND_TEXTURE_ID];
        mTexIDs[SECOND_TEXTURE_ID] = mTexIDs[FIRST_TEXTURE_ID];

        // [first page] first -> [second page] back of first
        mUnusedTexIDs[mUnusedTexSize++] = mTexIDs[BACK_TEXTURE_ID];
        mTexIDs[BACK_TEXTURE_ID] = page.mTexIDs[FIRST_TEXTURE_ID];

        // [first page] back of first -> [second page] first
        mTexIDs[FIRST_TEXTURE_ID] = page.mTexIDs[BACK_TEXTURE_ID];
        page.mTexIDs[BACK_TEXTURE_ID] = INVALID_TEXTURE_ID;

        // [first page] second -> [first page] first
        page.mTexIDs[FIRST_TEXTURE_ID] = page.mTexIDs[SECOND_TEXTURE_ID];
        page.mTexIDs[SECOND_TEXTURE_ID] = INVALID_TEXTURE_ID;
        return this;
    }

    /**
     * Get back texture ID
     *
     * @return back texture id, If it is not set, return the first texture id
     */
    int getBackTextureID() {
        // In single page mode, the back texture is same with the first texture
        if (mTexIDs[BACK_TEXTURE_ID] == INVALID_TEXTURE_ID) {
            return mTexIDs[FIRST_TEXTURE_ID];
        }
        else {
            return mTexIDs[BACK_TEXTURE_ID];
        }
    }

    /**
     * Is given point(x, y) in page?
     *
     * @param x x coordinate
     * @param y y coordinate
     * @return true if the point is in page
     */
    boolean contains(float x, float y) {
        return left < right && bottom < top &&
               left <= x && x < right &&
               bottom <= y && y < top;
    }

    /**
     * Is given x coordinate in specified page range?
     *
     * @param x x coordinate
     * @param ratio range ratio based on page width, start from OriginP.x
     * @return True if x is in specified range
     */
    boolean isXInRange(float x, float ratio) {
        final float w = width * ratio;
        return originP.x < 0 ? x < (originP.x + w) : x > (originP.x - w);
    }

    /**
     * Is given x coordinate out of page width?
     *
     * @param x x coordinate
     * @return true if given x is not in page
     */
    boolean isXOutOfPage(float x) {
        return originP.x < 0 ? x > diagonalP.x : x < diagonalP.x;
    }

    /**
     * Compute index of page apexes order for current original point
     */
    private void computeIndexOfApexOrder() {
        mApexOrderIndex = 0;
        if (originP.x < right && originP.y < 0) {
            mApexOrderIndex = 3;
        }
        else {
            if (originP.y > 0) {
                mApexOrderIndex++;
            }
            if (originP.x < right) {
                mApexOrderIndex++;
            }
        }
    }

    /**
     * Set original point and diagonal point
     *
     * @param hasSecondPage has the second page in double pages mode?
     * @param dy relative finger movement on Y axis
     * @return self
     */
    Page setOriginAndDiagonalPoints(boolean hasSecondPage, float dy) {
        if (hasSecondPage && left < 0) {
            originP.x = left;
            diagonalP.x = right;
        }
        else {
            originP.x = right;
            diagonalP.x = left;
        }

        if (dy > 0) {
            originP.y = bottom;
            diagonalP.y = top;
        }
        else {
            originP.y = top;
            diagonalP.y = bottom;
        }

        computeIndexOfApexOrder();

        // set texture coordinates
        originP.tX = (originP.x - left) / texWidth;
        originP.tY = (top - originP.y) / texHeight;
        diagonalP.tX = (diagonalP.x - left) / texWidth;
        diagonalP.tY = (top - diagonalP.y) / texHeight;
        return this;
    }

    /**
     * Invert Y coordinate of original point and diagonal point
     */
    void invertYOfOriginalPoint() {
        float t = originP.y;
        originP.y = diagonalP.y;
        diagonalP.y = t;

        t = originP.tY;
        originP.tY = diagonalP.tY;
        diagonalP.tY = t;

        // re-compute index for apex order since original point is changed
        computeIndexOfApexOrder();
    }

    /**
     * Compute X coordinate of texture
     *
     * @param x x coordinate
     * @return x coordinate of texture, value is in [0 .. 1]
     */
    public float textureX(float x) {
        return (x - left) / texWidth;
    }

    /**
     * Compute Y coordinate of texture
     *
     * @param y y coordinate
     * @return y coordinate of texture, value is in [0 .. 1]
     */
    public float textureY(float y) {
        return (top - y) / texHeight;
    }

    /**
     * Delete all textures
     */
    public void deleteAllTextures() {
        glDeleteTextures(TEXTURE_SIZE, mTexIDs, 0);
        mTexIDs[FIRST_TEXTURE_ID] = INVALID_TEXTURE_ID;
        mTexIDs[SECOND_TEXTURE_ID] = INVALID_TEXTURE_ID;
        mTexIDs[BACK_TEXTURE_ID] = INVALID_TEXTURE_ID;
    }

    /**
     * Set the first texture with given bitmap
     *
     * @param b Bitmap object for creating texture
     */
    public void setFirstTexture(Bitmap b) {
        // compute mask color
        int color = PageFlipUtils.computeAverageColor(b, 30);
        maskColor[FIRST_TEXTURE_ID][0] = Color.red(color) / 255.0f;
        maskColor[FIRST_TEXTURE_ID][1] = Color.green(color) / 255.0f;
        maskColor[FIRST_TEXTURE_ID][2] = Color.blue(color) / 255.0f;

        glGenTextures(1, mTexIDs, FIRST_TEXTURE_ID);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, mTexIDs[FIRST_TEXTURE_ID]);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        GLUtils.texImage2D(GL_TEXTURE_2D, 0, b, 0);
    }

    /**
     * Set the second texture with given bitmap
     *
     * @param b Bitmap object for creating texture
     */
    public void setSecondTexture(Bitmap b) {
        // compute mask color
        int color = PageFlipUtils.computeAverageColor(b, 30);
        maskColor[SECOND_TEXTURE_ID][0] = Color.red(color) / 255.0f;
        maskColor[SECOND_TEXTURE_ID][1] = Color.green(color) / 255.0f;
        maskColor[SECOND_TEXTURE_ID][2] = Color.blue(color) / 255.0f;

        glGenTextures(1, mTexIDs, SECOND_TEXTURE_ID);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, mTexIDs[SECOND_TEXTURE_ID]);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        GLUtils.texImage2D(GL_TEXTURE_2D, 0, b, 0);
    }

    /**
     * Set the back texture with given bitmap
     * <p>If given bitmap is null, the back texture will be same with the first
     * texture</p>
     *
     * @param b Bitmap object for creating back texture
     */
    public void setBackTexture(Bitmap b) {
        if (b == null) {
            // back texture is same with the first texture
            if (mTexIDs[BACK_TEXTURE_ID] != INVALID_TEXTURE_ID) {
                mUnusedTexIDs[mUnusedTexSize++] = mTexIDs[BACK_TEXTURE_ID];
            }
            mTexIDs[BACK_TEXTURE_ID] = INVALID_TEXTURE_ID;
        }
        else {
            // compute mask color
            int color = PageFlipUtils.computeAverageColor(b, 50);
            maskColor[BACK_TEXTURE_ID][0] = Color.red(color) / 255.0f;
            maskColor[BACK_TEXTURE_ID][1] = Color.green(color) / 255.0f;
            maskColor[BACK_TEXTURE_ID][2] = Color.blue(color) / 255.0f;

            glGenTextures(1, mTexIDs, BACK_TEXTURE_ID);
            glBindTexture(GL_TEXTURE_2D, mTexIDs[BACK_TEXTURE_ID]);
            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            GLUtils.texImage2D(GL_TEXTURE_2D, 0, b, 0);
        }
    }

    /**
     * Draw front page when page is flipping
     *
     * @param program GL shader program
     * @param vertexes Vertexes of the curled front page
     */
    public void drawFrontPage(VertexProgram program,
                              Vertexes vertexes) {
        // 1. draw unfold part and curled part with the first texture
        glUniformMatrix4fv(program.hMVPMatrix, 1, false,
                           VertexProgram.MVPMatrix, 0);
        glBindTexture(GL_TEXTURE_2D, mTexIDs[FIRST_TEXTURE_ID]);
        glUniform1i(program.hTexture, 0);
        vertexes.drawWith(GL_TRIANGLE_STRIP,
                          program.hVertexPosition,
                          program.hTextureCoord,
                          0, mFrontVertexSize);

        // 2. draw the second texture
        glBindTexture(GL_TEXTURE_2D, mTexIDs[SECOND_TEXTURE_ID]);
        glUniform1i(program.hTexture, 0);
        glDrawArrays(GL_TRIANGLE_STRIP,
                     mFrontVertexSize,
                     vertexes.mVertexesSize - mFrontVertexSize);
    }

    /**
     * Draw full page
     *
     * @param program GL shader program
     * @param isFirst use the first or second texture to draw
     */
    public void drawFullPage(VertexProgram program, boolean isFirst) {
        if (isFirst) {
            drawFullPage(program, mTexIDs[FIRST_TEXTURE_ID]);
        }
        else {
            drawFullPage(program, mTexIDs[SECOND_TEXTURE_ID]);
        }
    }

    /**
     * Draw full page with given texture id
     */
    private void drawFullPage(VertexProgram program, int textureID) {
        glBindTexture(GL_TEXTURE_2D, textureID);
        glUniform1i(program.hTexture, 0);

        glVertexAttribPointer(program.hVertexPosition, 3, GL_FLOAT, false, 0,
                              mFullPageVexBuf);
        glEnableVertexAttribArray(program.hVertexPosition);

        glVertexAttribPointer(program.hTextureCoord, 2, GL_FLOAT, false, 0,
                              mFullPageTexCoordsBuf);
        glEnableVertexAttribArray(program.hTextureCoord);

        glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
    }

    /**
     * Create vertexes buffer
     */
    private void createVertexesBuffer() {
        // 4 vertexes for full page
        mFullPageVexBuf = ByteBuffer.allocateDirect(48)
                                    .order(ByteOrder.nativeOrder())
                                    .asFloatBuffer();

        mFullPageTexCoordsBuf = ByteBuffer.allocateDirect(32)
                                          .order(ByteOrder.nativeOrder())
                                          .asFloatBuffer();

        mApexes = new float[12];
        mApexTexCoords = new float[8];
    }

    /**
     * Build vertexes of page when page is flipping vertically
     * <pre>
     *        <---- flip
     *     1        fY    2
     *     +--------#-----+
     *     |        |     |
     *     |        |     |
     *     |        |     |
     *     +--------#-----+
     *     4        fX    3
     * </pre>
     * <p>
     * There is only one case to draw when page is flipping vertically
     * </p>
     * <ul>
     *      <li>Page is flipping from right -> left</li>
     *      <li>Origin point: 3</li>
     *      <li>Diagonal point: 1</li>
     *      <li>xFoldP1.y: fY, xFoldP2.x: fX</li>
     *      <li>Drawing front part with the first texture(GL_TRIANGLE_STRIP):
     *      fX -> fY -> 4 -> 1</li>
     *      <li>Drawing back part with the second texture(GL_TRIANGLE_STRIP):
     *      3 -> 2 -> fX -> fY</li>
     * </ul>
     *
     * @param frontVertexes vertexes for drawing font part of page
     * @param xFoldP1 fold point on X axis
     */
    public void buildVertexesOfPageWhenVertical(Vertexes frontVertexes,
                                                PointF xFoldP1) {
        // if xFoldX and yFoldY are both outside the page, use the last vertex
        // order to draw page
        int index = 4;

        // compute xFoldX and yFoldY points
        if (!isXOutOfPage(xFoldP1.x)) {
            // use the case B of vertex order to draw page
            index = 1;
            float cx = textureX(xFoldP1.x);
            mXFoldP.set(xFoldP1.x, originP.y, 0, cx, originP.tY);
            mYFoldP.set(xFoldP1.x, diagonalP.y, 0, cx, diagonalP.tY);
        }

        // get apex order and fold vertex order
        final int[] apexOrder = mPageApexOrders[mApexOrderIndex];
        final int[] vexOrder = mFoldVexOrders[index];

        // need to draw first texture, add xFoldX and yFoldY first. Remember
        // the adding order of vertex in float buffer is X point prior to Y
        // point
        if (vexOrder[0] > 1) {
            frontVertexes.addVertex(mXFoldP).addVertex(mYFoldP);
        }

        // add the leftover vertexes for the first texture
        for (int i = 1; i < vexOrder[0]; ++i) {
            int k = apexOrder[vexOrder[i]];
            int m = k * 3;
            int n = k << 1;
            frontVertexes.addVertex(mApexes[m], mApexes[m + 1], 0,
                                    mApexTexCoords[n], mApexTexCoords[n + 1]);
        }

        // the vertex size for drawing front of fold page and first texture
        mFrontVertexSize = frontVertexes.mNext / 3;

        // if xFoldX and yFoldY are in the page, need add them for drawing the
        // second texture
        if (vexOrder[0] > 1) {
            mXFoldP.z = mYFoldP.z = -1;
            frontVertexes.addVertex(mXFoldP).addVertex(mYFoldP);
        }

        // add the remaining vertexes for the second texture
        for (int i = vexOrder[0]; i < vexOrder.length; ++i) {
            int k = apexOrder[vexOrder[i]];
            int m = k * 3;
            int n = k << 1;
            frontVertexes.addVertex(mApexes[m], mApexes[m + 1], -1,
                                    mApexTexCoords[n], mApexTexCoords[n + 1]);
        }
    }

    /**
     * Build vertexes of page when page flip is slope
     * <p>See {@link #mApexOrderIndex} and {@link #mFoldVexOrders} to get more
     * details</p>
     *
     * @param frontVertexes vertexes for drawing front part of page
     * @param xFoldP1 fold point on X axis
     * @param yFoldP1 fold point on Y axis
     * @param kValue tan value of page curling angle
     */
    public void buildVertexesOfPageWhenSlope(Vertexes frontVertexes,
                                             PointF xFoldP1,
                                             PointF yFoldP1,
                                             float kValue) {
        // compute xFoldX point
        float halfH = height * 0.5f;
        int index = 0;
        mXFoldP.set(xFoldP1.x, originP.y, 0, textureX(xFoldP1.x), originP.tY);
        if (isXOutOfPage(xFoldP1.x)) {
            index = 2;
            mXFoldP.x = diagonalP.x;
            mXFoldP.y = originP.y + (xFoldP1.x - diagonalP.x) / kValue;
            mXFoldP.tX = diagonalP.tX;
            mXFoldP.tY = textureY(mXFoldP.y);
        }

        // compute yFoldY point
        mYFoldP.set(originP.x, yFoldP1.y, 0, originP.tX, textureY(yFoldP1.y));
        if (Math.abs(yFoldP1.y) > halfH)  {
            index++;
            mYFoldP.x = originP.x + kValue * (yFoldP1.y - diagonalP.y);
            if (isXOutOfPage(mYFoldP.x)) {
                index++;
            }
            else {
                mYFoldP.y = diagonalP.y;
                mYFoldP.tX = textureX(mYFoldP.x);
                mYFoldP.tY = diagonalP.tY;
            }
        }

        // get apex order and fold vertex order
        final int[] apexOrder = mPageApexOrders[mApexOrderIndex];
        final int[] vexOrder = mFoldVexOrders[index];

        // need to draw first texture, add xFoldX and yFoldY first. Remember
        // the adding order of vertex in float buffer is X point prior to Y
        // point
        if (vexOrder[0] > 1) {
            frontVertexes.addVertex(mXFoldP).addVertex(mYFoldP);
        }

        // add the leftover vertexes for the first texture
        for (int i = 1; i < vexOrder[0]; ++i) {
            int k = apexOrder[vexOrder[i]];
            int m = k * 3;
            int n = k << 1;
            frontVertexes.addVertex(mApexes[m], mApexes[m + 1], 0,
                                    mApexTexCoords[n], mApexTexCoords[n + 1]);
        }

        // the vertex size for drawing front of fold page and first texture
        mFrontVertexSize = frontVertexes.mNext / 3;

        // if xFoldX and yFoldY are in the page, need add them for drawing the
        // second texture
        if (vexOrder[0] > 1) {
            mXFoldP.z = mYFoldP.z = -1;
            frontVertexes.addVertex(mXFoldP).addVertex(mYFoldP);
        }

        // add the remaining vertexes for the second texture
        for (int i = vexOrder[0]; i < vexOrder.length; ++i) {
            int k = apexOrder[vexOrder[i]];
            int m = k * 3;
            int n = k << 1;
            frontVertexes.addVertex(mApexes[m], mApexes[m + 1], -1,
                                    mApexTexCoords[n], mApexTexCoords[n + 1]);
        }
    }

    /**
     * Build vertexes of full page
     * <pre>
     *        <---- flip
     *     3              2
     *     +--------------+
     *     |              |
     *     |              |
     *     |              |
     *     |              |
     *     +--------------+
     *     4              1
     * </pre>
     * <ul>
     *      <li>Page is flipping from right -> left</li>
     *      <li>Origin point: 3</li>
     *      <li>Diagonal point: 1</li>
     *      <li>xFoldP1.y: fY, xFoldP2.x: fX</li>
     *      <li>Drawing order: 3 -> 2 -> 4 -> 1</li>
     * </ul>
     */
    private void buildVertexesOfFullPage() {
        int i = 0;
        int j = 0;

        mApexes[i++] = right;
        mApexes[i++] = bottom;
        mApexes[i++] = 0;
        mApexTexCoords[j++] = textureX(right);
        mApexTexCoords[j++] = textureY(bottom);

        mApexes[i++] = right;
        mApexes[i++] = top;
        mApexes[i++] = 0;
        mApexTexCoords[j++] = textureX(right);
        mApexTexCoords[j++] = textureY(top);

        mApexes[i++] = left;
        mApexes[i++] = top;
        mApexes[i++] = 0;
        mApexTexCoords[j++] = textureX(left);
        mApexTexCoords[j++] = textureY(top);

        mApexes[i++] = left;
        mApexes[i++] = bottom;
        mApexes[i] = 0;
        mApexTexCoords[j++] = textureX(left);
        mApexTexCoords[j] = textureY(bottom);

        mFullPageVexBuf.put(mApexes, 0, 12).position(0);
        mFullPageTexCoordsBuf.put(mApexTexCoords, 0, 8).position(0);
    }
}
