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

import android.opengl.GLES20;

import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glUniform1f;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniform4f;
import static android.opengl.GLES20.glUniformMatrix4fv;

/**
 * Vertex buffer management for back of fold page
 *
 * @author eschao
 */
final class FoldBackVertexes extends Vertexes {

    private final static String TAG = "FoldBackVertexes";

    // mask alpha for back of fold page
    // mask color is in Page class since it follows the back of first bitmap
    float mMaskAlpha;

    public FoldBackVertexes() {
        super();

        mSizeOfPerVex = 4;
        mMaskAlpha = 0.6f;
    }

    /**
     * Set vertex buffer with given mesh count
     *
     * @param meshCount mesh count
     * @return self
     */
    public FoldBackVertexes set(int meshCount) {
        super.set(meshCount << 1, 4, true);
        mNext = 0;
        return this;
    }

    /**
     * Set mask alpha
     *
     * @param alpha mask alpha, value is [0 .. 255]
     * @return self
     */
    public FoldBackVertexes setMaskAlpha(int alpha) {
        if (alpha < 0 || alpha > 255) {
            throw new IllegalArgumentException("Alpha: " + alpha + "is out of " +
                                               "[0 .. 255]!");
        }

        mMaskAlpha = alpha / 255.0f;
        return this;
    }

    /**
     * set mask alpha
     *
     * @param alpha mask alpha, value is [0 .. 1]
     * @return self
     */
    public FoldBackVertexes setMaskAlpha(float alpha) {
        if (alpha < 0 || alpha > 1) {
            throw new IllegalArgumentException("Alpha: " + alpha + "is out of " +
                                               "[0 .. 1]!");
        }

        mMaskAlpha = alpha;
        return this;
    }

    /**
     * Put all data from float array to float buffer
     *
     * @return self
     */
    public FoldBackVertexes toFloatBuffer() {
        // fix coordinate x of texture for shadow of fold back
        super.toFloatBuffer();
        return this;
    }

    /**
     * Draw fold back and shadow
     *
     * @param program fold back vertex program
     * @param page the current operating page: First Page
     * @param hasSecondPage there has second page or not
     * @param gradientShadowId gradient shadow id
     */
    public void draw(FoldBackVertexProgram program,
                     Page page,
                     boolean hasSecondPage,
                     int gradientShadowId) {
        glUniformMatrix4fv(program.hMVPMatrix, 1, false,
                           VertexProgram.MVPMatrix, 0);

        // load fold back texture
        glBindTexture(GL_TEXTURE_2D, page.getBackTextureID());
        glUniform1i(program.hTexture, 0);

        // load gradient shadow texture
        glActiveTexture(GLES20.GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, gradientShadowId);
        glUniform1i(program.hShadow, 1);

        // set x offset of texture coordinate. In single page mode, the value is
        // set 0 to draw the back texture with x coordinate inversely against
        // the first texture since they are using the same texture, but in
        // double page mode, the back texture is different with the first one,
        // it is the next page content texture and should be drawn in the same
        // order with the first texture, so the value is set 1. Computing
        // details, please see the shader script.
        glUniform1f(program.hTexXOffset, hasSecondPage ? 1.0f : 0);

        // set mask color and alpha
        glUniform4f(program.hMaskColor,
                    page.maskColor[0],
                    page.maskColor[1],
                    page.maskColor[2],
                    hasSecondPage ? 0 : mMaskAlpha);

        // draw triangles
        drawWith(GL_TRIANGLE_STRIP,
                 program.hVertexPosition,
                 program.hTextureCoord);
    }
}
