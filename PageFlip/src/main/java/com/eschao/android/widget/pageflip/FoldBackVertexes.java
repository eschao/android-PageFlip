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
 * Vertex buffer management for fold back part of page
 *
 * @author eschao
 */
final class FoldBackVertexes extends Vertexes {

    private final static String TAG = "FoldBackVertexes";

    // mask alpha for page fold back
    // mask color is in Page class since it is followed back of first bitmap
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
        mMaskAlpha = alpha / 255f;
        return this;
    }

    /**
     * set mask alpha
     *
     * @param alpha mask alpha, value is [0 .. 1]
     * @return self
     */
    public FoldBackVertexes setMaskAlpha(float alpha) {
        mMaskAlpha = alpha;
        return this;
    }

    public FoldBackVertexes toFloatBuffer() {
        // fix coordinate x of texture for shadow of fold back
        super.toFloatBuffer();
        return this;
    }

    /**
     * Draw fold back and shadow
     *
     * @param program Fold back vertex program
     * @param page The current page object
     * @param hasSecondPage There has second page or not
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

        // set x offset of texture coordinate, in single page mode, the value is
        // set 0 to make the back texture is drawing inversely against the first
        // texture since they are the same texture, but in double page mode,
        // the back texture is different with the first texture, it is the next
        // page and should be drawn in the same order with the first texture, so
        // the value is set 1
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
