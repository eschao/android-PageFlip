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

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glVertexAttribPointer;

/**
 * Vertexes is used to manage vertex and texture data for openGL drawing
 *
 * @author eschao
 */

class Vertexes {

    private static final String TAG = "Vertexes";

    // how many vertexes in vertex float buffer will be drawn on screen
    int mVertexesSize;

    // how many float data is used for every vertex
    int mSizeOfPerVex;

    // vertex data array
    float[] mVertexes;

    // texture coordinates array
    float[] mTextureCoords;

    // float buffer for vertexes data and texture coordinates data
    FloatBuffer mVertexesBuf;
    FloatBuffer mTextureCoordsBuf;

    // next index when add vertex to float array
    int mNext;


    /**
     * Default constructor
     */
    public Vertexes() {
        mNext = 0;
        mVertexesSize = 0;
        mSizeOfPerVex = 0;
        mVertexes = null;
        mVertexesBuf = null;
        mTextureCoords = null;
        mTextureCoordsBuf = null;
    }

    /**
     * Constructor with given vertex amount
     *
     * @param capacity vertex max amount
     * @param sizeOfPerVex how many float data is used for a vertex
     */
    public Vertexes(int capacity, int sizeOfPerVex) {
        set(capacity, sizeOfPerVex, true);
    }

    /**
     * Constructor with given vertex max amount and texture
     *
     * @param capacity vertex amount
     * @param sizeOfPerVex how many float data is used for a vertex
     * @param hasTexture if need texture buffer for texture coordinates
     */
    public Vertexes(int capacity, int sizeOfPerVex, boolean hasTexture) {
        set(capacity, sizeOfPerVex, hasTexture);
    }

    /**
     * Set max vertex amount and create buffer for vertex and texture
     *
     * @param capacity vertex amount
     * @param sizeOfPerVex how many float data is used for a vertex
     * @param hasTexture True if need texture buffer for texture coordinates
     * @return self
     */
    public Vertexes set(int capacity, int sizeOfPerVex, boolean hasTexture) {
        if (sizeOfPerVex < 2) {
            Log.w(TAG, "sizeOfPerVex is invalid: " + sizeOfPerVex);
            throw new IllegalArgumentException("sizeOfPerVex:" + sizeOfPerVex +
                                               "is less than 2!");
        }

        // reset all
        mNext = 0;
        mVertexes = null;
        mVertexesBuf = null;
        mTextureCoords = null;
        mTextureCoordsBuf = null;

        // create vertexes buffer
        mSizeOfPerVex = sizeOfPerVex;
        mVertexes = new float[capacity * sizeOfPerVex];
        mVertexesBuf = ByteBuffer.allocateDirect(capacity * sizeOfPerVex * 4)
                                 .order(ByteOrder.nativeOrder())
                                 .asFloatBuffer();

        // if need, create texture buffer
        if (hasTexture) {
            mTextureCoords = new float[capacity << 1];
            mTextureCoordsBuf =  ByteBuffer.allocateDirect(capacity << 3)
                                           .order(ByteOrder.nativeOrder())
                                           .asFloatBuffer();
        }

        return this;
    }

    /**
     * Release all resources
     *
     * @return self
     */
    public Vertexes release() {
        mNext = 0;
        mVertexesSize = 0;
        mSizeOfPerVex = 0;
        mVertexes = null;
        mVertexesBuf = null;
        mTextureCoords = null;
        mTextureCoordsBuf = null;
        return this;
    }

    /**
     * Get max vertex amount
     *
     * @return max vertex amount
     */
    public int capacity() {
        return mVertexes == null ? 0 : mVertexes.length / mSizeOfPerVex;
    }

    /**
     * Reset index of float array before adding vertex to buffer
     */
    public void reset() {
        mNext = 0;
    }


    /**
     * Get float data with given index
     *
     * @param index float data position index
     * @return float data
     */
    public float getFloatAt(int index) {
        if (index >= 0 && index < mNext) {
            return mVertexes[index];
        }

        return 0;
    }

    /**
     * Set vertex coordinate(x, y, z) in given buffer position
     *
     * @param i where to start saving vertex data
     * @param x x value of vertex coordinate
     * @param y y value of vertex coordinate
     * @param z z value of vertex coordinate
     * @return self
     */
    public Vertexes setVertex(int i, float x, float y, float z) {
        assert(i+2 < mVertexes.length);

        mVertexes[i] = x;
        mVertexes[i + 1] = y;
        mVertexes[i + 2] = z;
        return this;
    }

    /**
     * Set vertex coordinate(x, y, z, width) in given buffer position
     *
     * @param i where to start saving vertex data
     * @param x x value of vertex coordinate
     * @param y y value of vertex coordinate
     * @param z z value of vertex coordinate
     * @param w width value which is normally used to pass other value to shader
     * @return self
     */
    public Vertexes setVertex(int i, float x, float y, float z, float w) {
        assert(i+3 < mVertexes.length);

        mVertexes[i] = x;
        mVertexes[i + 1] = y;
        mVertexes[i + 2] = z;
        mVertexes[i + 3] = w;
        return this;
    }

    /**
     * Set texture coordinate(x, y) in given buffer position
     *
     * @param i where to start saving texture coordinate
     * @param x x value of texture coordinate
     * @param y y value of texture coordinate
     * @return self
     */
    public Vertexes setTextureCoord(int i, float x, float y) {
        assert(i+1 < mTextureCoords.length);

        mTextureCoords[i] = x;
        mTextureCoords[i + 1] = y;
        return this;
    }

    /**
     * Add vertex coordinate to buffer
     *
     * @param x x value of vertex coordinate
     * @param y y value of vertex coordinate
     * @param z z value of vertex coordinate
     * @return self
     */
    public Vertexes addVertex(float x, float y, float z) {
        mVertexes[mNext++] = x;
        mVertexes[mNext++] = y;
        mVertexes[mNext++] = z;
        return this;
    }

    /**
     * Add vertex and texture coordinates
     *
     * @param x x value of vertex coordinate
     * @param y y value of vertex coordinate
     * @param z z value of vertex coordinate
     * @param coordX x value of texture coordinate
     * @param coordY y value of texture coordinate
     * @return self
     */
    public Vertexes addVertex(float x, float y, float z,
                              float coordX, float coordY) {
        int j = mNext / mSizeOfPerVex * 2;
        mVertexes[mNext++] = x;
        mVertexes[mNext++] = y;
        mVertexes[mNext++] = z;

        mTextureCoords[j++] = coordX;
        mTextureCoords[j] = coordY;
        return this;
    }

    /**
     * Add vertex coordinate to buffer
     *
     * @param x x value of vertex coordinate
     * @param y y value of vertex coordinate
     * @param z z value of vertex coordinate
     * @param w width value which is normally used to pass other value to shader
     * @return self
     */
    public Vertexes addVertex(float x, float y, float z, float w) {
        mVertexes[mNext++] = x;
        mVertexes[mNext++] = y;
        mVertexes[mNext++] = z;
        mVertexes[mNext++] = w;
        return this;
    }

    /**
     * Add vertex and texture coordinates
     *
     * @param x x value of vertex coordinate
     * @param y y value of vertex coordinate
     * @param z z value of vertex coordinate
     * @param w width value which is normally used to pass other value to shader
     * @param coordX x value of texture coordinate
     * @param coordY y value of texture coordinate
     * @return self
     */
    public Vertexes addVertex(float x, float y, float z, float w,
                              float coordX, float coordY) {
        int j = mNext / mSizeOfPerVex * 2;
        mVertexes[mNext++] = x;
        mVertexes[mNext++] = y;
        mVertexes[mNext++] = z;
        mVertexes[mNext++] = w;

        mTextureCoords[j++] = coordX;
        mTextureCoords[j] = coordY;
        return this;
    }

    /**
     * Add GPoint to float buffer
     *
     * @param point GPoint object
     * @return self
     */
    public Vertexes addVertex(GPoint point) {
        int j = mNext / mSizeOfPerVex * 2;
        mVertexes[mNext++] = point.x;
        mVertexes[mNext++] = point.y;
        mVertexes[mNext++] = point.z;

        mTextureCoords[j++] = point.tX;
        mTextureCoords[j] = point.tY;
        return this;
    }

    /**
     * Put data from float array to float buffer
     *
     * @param offset data start offset in float array
     * @param length data length to be put
     * @return self
     */
    public Vertexes toFloatBuffer(int offset, int length) {
        mVertexesBuf.put(mVertexes, offset, length).position(0);
        mVertexesSize = length / mSizeOfPerVex;

        // has texture? put again
        if (mTextureCoords != null) {
            final int o = offset / mSizeOfPerVex * 2;
            final int l = mVertexesSize * 2;
            mTextureCoordsBuf.put(mTextureCoords, o, l).position(0);
        }
        return this;
    }

    /**
     * Put all data from float array to float buffer
     * <p>
     * The offset is 0 and the length is determined by mNext which is increased
     * after calling {@link #addVertex}
     * </p>
     *
     * @return self
     */
    public Vertexes toFloatBuffer() {
        mVertexesBuf.put(mVertexes, 0, mNext).position(0);
        mVertexesSize = mNext / mSizeOfPerVex;

        if (mTextureCoords != null) {
            mTextureCoordsBuf.put(mTextureCoords, 0, mVertexesSize << 1)
                             .position(0);
        }
        return this;
    }

    /**
     * Draw vertexes
     *
     * @param type openGL drawing type: TRIANGLE, STRIP, FAN
     * @param hVertexPos vertex handle in shader program
     * @param hTextureCoord texture handle in shader program
     */
    public void drawWith(int type, int hVertexPos, int hTextureCoord) {
        // pass vertex data
        glVertexAttribPointer(hVertexPos, mSizeOfPerVex, GL_FLOAT, false, 0,
                              mVertexesBuf);
        glEnableVertexAttribArray(hVertexPos);

        // pass texture data
        glVertexAttribPointer(hTextureCoord, 2, GL_FLOAT, false, 0,
                              mTextureCoordsBuf);
        glEnableVertexAttribArray(hTextureCoord);

        // draw triangles
        glDrawArrays(type, 0, mVertexesSize);
    }

    /**
     * Draw vertexes with given offset and length
     *
     * @param type openGL drawing type: TRIANGLE, STRIP, FAN
     * @param hVertexPos vertex handle in shader program
     * @param hTextureCoord texture handle in shader program
     * @param offset vertex start offset in buffer
     * @param length vertex length to be drawn
     */
    public void drawWith(int type, int hVertexPos, int hTextureCoord,
                         int offset, int length) {
        glVertexAttribPointer(hVertexPos, mSizeOfPerVex, GL_FLOAT, false, 0,
                              mVertexesBuf);
        glEnableVertexAttribArray(hVertexPos);

        glVertexAttribPointer(hTextureCoord, 2, GL_FLOAT, false, 0,
                              mTextureCoordsBuf);
        glEnableVertexAttribArray(hTextureCoord);

        glDrawArrays(type, offset, length);
    }
}
