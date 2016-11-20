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
import android.text.method.BaseKeyListener;

import java.io.FileReader;
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
import static android.opengl.GLES20.glVertexAttribPointer;

/**
 * Page class
 * <pre>
 * Page holds content textures and show them on screen. In single page mode, a
 * page represents the whole screen area. But in double pages mode, there are
 * two pages to depict the entire screen size, in the left part is called left
 * page and the right part is called right page.
 * Every page has the below properties:
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
 *              <li>Every texture should be set with a bitmap by outer called
 *              </li>
 *          </ul>
 *     </li>
 * </ul>
 * </pre>
 *
 * @author eschao
 */

public class Page {

    public final static int TEXTURE_SIZE = 3;
    public final static int FIRST_TEXTURE_ID = 0;
    public final static int SECOND_TEXTURE_ID = 1;
    public final static int BACK_TEXTURE_ID = 2;
    public final static int INVALID_TEXTURE_ID = -1;

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

    // origin point and diagonal point
    //
    // 0-----+
    // |     |
    // |     |
    // +-----1
    //
    // if origin(x, y) is 1, the diagonal(x, y) is 0
    GPoint originP;
    GPoint diagonalP;

    // vertexes and texture coordinates buffer for full page
    FloatBuffer mFullPageVexBuf;
    FloatBuffer mFullPageTexCoordsBuf;

    // vertexes for curling page
    Vertexes mVertexes;

    // mask color of back texture
    float[][] maskColor;

    // texture(front, back and second) ids allocated by OpenGL
    int[] mTexIDs;
    // unused texture ids, will be deleted when next OpenGL drawing
    int[] mUnusedTexIDs;
    // actual size of mUnusedTexIDs
    int mUnusedTexSize;

    /**
     * Constructor
     */
    public Page() {
        left = 0;
        right = 0;
        top = 0;
        bottom = 0;
        width = 0;
        height = 0;
        texWidth = width;
        texHeight = height;

        originP = new GPoint();
        diagonalP = new GPoint();
        maskColor = new float[][] {
                        new float[] {0, 0, 0},
                        new float[] {0, 0, 0},
                        new float[] {0, 0, 0}};

        mTexIDs = new int[] {INVALID_TEXTURE_ID,
                             INVALID_TEXTURE_ID,
                             INVALID_TEXTURE_ID};
        mUnusedTexSize = 0;
        mUnusedTexIDs = new int[] {INVALID_TEXTURE_ID,
                                   INVALID_TEXTURE_ID,
                                   INVALID_TEXTURE_ID};
        // create vertexes buffer
        createVertexesBuffer();
    }

    /**
     * Constructor with page size
     */
    public Page(float l, float r, float t, float b) {
        left = l;
        right = r;
        top = t;
        bottom = b;
        width = right - left;
        height = top - bottom;
        texWidth = width;
        texHeight = height;

        originP = new GPoint();
        diagonalP = new GPoint();
        maskColor = new float[][] {
                        new float[] {0, 0, 0},
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
        return originP.x < 0 ? x >= diagonalP.x : x <= diagonalP.x;
    }

    /**
     * Set original point and diagonal point
     *
     * @param hasSecondPage has the second page in double pages mode?
     * @param dy relative finger movement on Y axis
     * @return self
     */
    Page setOriginPoint(boolean hasSecondPage, float dy) {
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

        // set texture coordinates
        originP.tX = (originP.x - left) / texWidth;
        originP.tY = (top - originP.y) / texHeight;
        diagonalP.tX = (diagonalP.x - left) / texWidth;
        diagonalP.tY = (top - diagonalP.y) / texHeight;
        return this;
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
     * @param vertexesOfFrontPage Vertexes of the curled front page
     */
    public void drawFrontPage(VertexProgram program,
                              Vertexes vertexesOfFrontPage) {
        // 1. draw unfold part and curled part with the first texture
        glBindTexture(GL_TEXTURE_2D, mTexIDs[FIRST_TEXTURE_ID]);
        glUniform1i(program.hTexture, 0);
        vertexesOfFrontPage.drawWith(GL_TRIANGLE_STRIP,
                                     program.hVertexPosition,
                                     program.hTextureCoord);

        // 2. draw the back part with the second texture
        glVertexAttribPointer(program.hVertexPosition, 3, GL_FLOAT, false, 0,
                              mVertexes.mVertexesBuf);
        glEnableVertexAttribArray(program.hVertexPosition);
        glVertexAttribPointer(program.hTextureCoord, 2, GL_FLOAT, false, 0,
                              mVertexes.mTextureCoordsBuf);
        glEnableVertexAttribArray(program.hTextureCoord);

        glBindTexture(GL_TEXTURE_2D, mTexIDs[SECOND_TEXTURE_ID]);
        glUniform1i(program.hTexture, 0);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, mVertexes.mVertexesSize);
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

        // vertexes buffer for unfold page when page is flipping, it includes
        // two parts: one is drawing with the first texture, another is drawing
        // with the second texture
        mVertexes = new Vertexes(6, 3);
    }

    /**
     * Build vertexes of page when page is flipping vertically
     * <pre>
     *           <---- flip
     *     1        fY    2
     *     +--------#-----+
     *     |        |     |
     *     |        |     |
     *     |        |     |
     *     +--------#-----+
     *     4        fX    3
     *
     *  1) Page is flipping from right -> left
     *  2) Origin point: 3
     *  3) Diagonal point: 1
     *  4) xFoldP1.y: fY, xFoldP2.x: fX
     *  5) Drawing front part with the first texture(GL_TRIANGLE_STRIP):
     *      fX -> fY -> 4 -> 1
     *  6) Drawing back part with the second texture(GL_TRIANGLE_STRIP):
     *      3 -> 2 -> fX -> fY
     * </pre>
     * @param frontVertexes vertexes for drawing font part of page
     * @param xFoldP1 fold point on X axis
     */
    public void buildVertexesOfPageWhenVertical(Vertexes frontVertexes,
                                                PointF xFoldP1) {
        mVertexes.mVertexesSize = 0;
        final float[] vertexes = mVertexes.mVertexes;
        final float[] texCoords = mVertexes.mTextureCoords;

        int i = 0;
        int j = 0;
        int start = -1;
        vertexes[i++] = originP.x;
        vertexes[i++] = originP.y;
        vertexes[i++] = -1;
        texCoords[j++] = originP.tX;
        texCoords[j++] = originP.tY;

        vertexes[i++] = originP.x;
        vertexes[i++] = diagonalP.y;
        vertexes[i++] = -1;
        texCoords[j++] = originP.tX;
        texCoords[j++] = diagonalP.tY;

        if (!isXOutOfPage(xFoldP1.x)) {
            float cx = textureX(xFoldP1.x);
            start = 6;
            vertexes[i++] = xFoldP1.x;
            vertexes[i++] = originP.y;
            vertexes[i++] = -1;
            texCoords[j++] = cx;
            texCoords[j++] = originP.tY;

            vertexes[i++] = xFoldP1.x;
            vertexes[i++] = diagonalP.y;
            vertexes[i++] = -1;
            texCoords[j++] = cx;
            texCoords[j++] = diagonalP.tY;
        }

        vertexes[i++] = diagonalP.x;
        vertexes[i++] = originP.y;
        vertexes[i++] = -1;
        texCoords[j++] = diagonalP.tX;
        texCoords[j++] = originP.tY;

        vertexes[i++] = diagonalP.x;
        vertexes[i++] = diagonalP.y;
        vertexes[i++] = -1;
        texCoords[j++] = diagonalP.tX;
        texCoords[j] = diagonalP.tY;

        // copy vertexes of unfold front part into front vertexes buffer so that
        // the front part can be draw at a time
        if (start > -1) {
            for (int k = 6, m = 4; k < 18; k += 3, m += 2) {
                frontVertexes.addVertex(vertexes[k], vertexes[k+1], 0,
                                        texCoords[m], texCoords[m+1]);
            }
            i = 12;
        }

        mVertexes.toFloatBuffer(0, i);
    }

    /**
     * Build vertexes of page when page flip is slope
     * <pre>
     *   There are 3 cases need to be considered
     *           <---- flip
     *         case A               case B               case C
     *     1            2      1         yFy  2      1    yFy     2
     *     +------------+      +----------+---+      +-----+------+
     *     |            |      |         /    |      |    /       |
     *     |            +yFy   |        /     |      |   /        |
     *     |           /|      |       /      |      |  /         |
     *     |          / |      |      /       |      | /          |
     *     |         /  |      |     /        |   xFx+            |
     *     |        /   |      |    /         |      |            |
     *     +-------+----+      +---+----------+      +------------+
     *     4        xFx   3    4  xFx         3      4            3
     *
     *  1) Page is flipping from right -> left
     *  2) Origin point: 3
     *  3) Diagonal point: 1
     *  4) xFoldP1.x: xFx, yFoldP1.y: yFy
     *  6) Drawing case A (TRIANGLE_STRIP):
     *      Second: 3 -> yFy -> xFx
     *      First : yFy -> xFx -> 2 -> 4 -> 1
     *  7) Drawing case B (TRIANGLE_STRIP):
     *      Second: 3 -> 2 -> xFx -> yFy
     *      First : xFx -> yFy -> 4 -> 1
     *  8) Drawing case C (TRIANGLE_STRIP):
     *      Second: 3 -> 2 -> 4 -> yFy -> xFx
     *      First : yFy -> xFx -> 1
     *  9) If yFy is out of page, that means xFx is also out and it will
     *  degenerate to a normal full page drawing: 3 -> 2 -> 4 -> 1
     * </pre>
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
        mVertexes.mVertexesSize = 0;
        final float[] vertexes = mVertexes.mVertexes;
        final float[] texCoords = mVertexes.mTextureCoords;

        int i = 0;
        int j = 0;
        int z = -1;
        int iy = -1;
        int ix = -1;
        int idy = -1;
        int iox = -1;
        int start = -1;
        float halfH = height * 0.5f;
        float yX = originP.x;
        float yY = yFoldP1.y;
        float xX = xFoldP1.x;
        float xY = originP.y;

        // compute FoldY point position in vertexes array
        if (yFoldP1.y <= halfH && yFoldP1.y >= -halfH) {
            iy = 3;
            idy = 9;
        }
        else {
            yX = originP.x + kValue * (yFoldP1.y - diagonalP.y);
            yY = diagonalP.y;
            if (!isXOutOfPage(yX)) {
                iy = 9;
            }
            idy = 3;
        }

        // compute XFold point position in vertexes array
        if (isXOutOfPage(xFoldP1.x)) {
            if (iy > -1) {
                xY = originP.y - (xFoldP1.x - diagonalP.x) / kValue;
                xX = diagonalP.x;
                ix = 12;
            }
            iox = 6;
        }
        else {
            ix = 6;
            iox = 12;
        }

        // add origin point in the first index
        vertexes[0] = originP.x;
        vertexes[1] = originP.y;
        vertexes[2] = z;
        texCoords[0] = originP.tX;
        texCoords[1] = originP.tY;

        j = idy / 3 * 2;
        vertexes[idy++] = originP.x;
        vertexes[idy++] = diagonalP.y;
        vertexes[idy] = z;
        texCoords[j++] = originP.tX;
        texCoords[j] = diagonalP.tY;

        j = iox / 3 * 2;
        vertexes[iox++] = diagonalP.x;
        vertexes[iox++] = originP.y;
        vertexes[iox] = z;
        texCoords[j++] = diagonalP.tX;
        texCoords[j] = originP.tY;

        i = 9;
        j = 6;
        if (iy > -1) {
            start = iy;
            int n = iy / 3 * 2;
            vertexes[iy++] = yX;
            vertexes[iy++] = yY;
            vertexes[iy] = z;
            texCoords[n++] = textureX(yX);
            texCoords[n] = textureY(yY);
            i += 3;
            j += 2;
        }

        if (ix > -1) {
            if (start > ix) {
                start = ix;
            }

            int n = ix / 3 * 2;
            vertexes[ix++] = xX;
            vertexes[ix++] = xY;
            vertexes[ix] = z;
            texCoords[n++] = textureX(xX);
            texCoords[n] = textureY(xY);
            i += 3;
            j += 2;
        }

        // add the diagonal point in the tail
        vertexes[i++] = diagonalP.x;
        vertexes[i++] = diagonalP.y;
        vertexes[i++] = z;
        texCoords[j++] = diagonalP.tX;
        texCoords[j] = diagonalP.tY;

        // copy into front vertexes so that they can be draw at a time
        if (start > -1) {
            int end = i;
            int m = start / 3 * 2;
            i = start + 6;
            for (; start < end; start += 3, m += 2) {
                frontVertexes.addVertex(vertexes[start], vertexes[start+1], 0,
                                        texCoords[m], texCoords[m+1]);
            }
        }

        // for second part of page
        mVertexes.toFloatBuffer(0, i);
    }

    /**
     * Build vertexes of full page
     * <pre>
     *           <---- flip
     *     1              2
     *     +--------------+
     *     |              |
     *     |              |
     *     |              |
     *     +--------------+
     *     4              3
     *
     *  1) Page is flipping from right -> left
     *  2) Origin point: 3
     *  3) Diagonal point: 1
     *  4) xFoldP1.y: fY, xFoldP2.x: fX
     *  5) Drawing order: 3 -> 2 -> 4 -> 1
     */
    private void buildVertexesOfFullPage() {
        float vertexes[] = new float[12];
        float texCoords[] = new float[8];
        int i = 0;
        int j = 0;

        vertexes[i++] = left;
        vertexes[i++] = bottom;
        vertexes[i++] = 0;
        texCoords[j++] = textureX(left);
        texCoords[j++] = textureY(bottom);

        vertexes[i++] = left;
        vertexes[i++] = top;
        vertexes[i++] = 0;
        texCoords[j++] = textureX(left);
        texCoords[j++] = textureY(top);

        vertexes[i++] = right;
        vertexes[i++] = top;
        vertexes[i++] = 0;
        texCoords[j++] = textureX(right);
        texCoords[j++] = textureY(top);

        vertexes[i++] = right;
        vertexes[i++] = bottom;
        vertexes[i] = 0;
        texCoords[j++] = textureX(right);
        texCoords[j] = textureY(bottom);

        mFullPageVexBuf.put(vertexes, 0, 12).position(0);
        mFullPageTexCoordsBuf.put(texCoords, 0, 8).position(0);
    }

    /**
     * GPoint includes (x,y) in OpenGL coordinate system and its texture
     * coordinates (tX, tY)
     */
    static class GPoint {
        float x;
        float y;
        float tX;
        float tY;
    }
}
