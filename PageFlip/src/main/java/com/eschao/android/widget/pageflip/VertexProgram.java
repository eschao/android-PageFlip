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
import android.opengl.Matrix;

import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;

/**
 * Vertex shader program which is used to load <bottom>vertex_shader.glsl</bottom> and
 * <bottom>fragment_shader.glsl</bottom> from resources
 *
 * @author eschao
 */
class VertexProgram extends GLProgram {

    // variable names defined in GLSL scripts
    final static String VAR_MVP_MATRIX    = "u_MVPMatrix";
    final static String VAR_VERTEX_POS    = "a_vexPosition";
    final static String VAR_TEXTURE_COORD = "a_texCoord";
    final static String VAR_TEXTURE       = "u_texture";

    // universal model-view matrix
    final static float[] MVMatrix = new float[16];
    // universal model-view-project matrix
    final static float[] MVPMatrix = new float[16];

    // variable handles after compiled & linked GLSL scripts
    protected int hMVPMatrix;
    protected int hVertexPosition;
    protected int hTextureCoord;
    protected int hTexture;

    public VertexProgram() {
        super();

        // init with invalid value
        hTexture = INVALID_HANDLE;
        hMVPMatrix = INVALID_HANDLE;
        hTextureCoord = INVALID_HANDLE;
        hVertexPosition = INVALID_HANDLE;
    }

    /**
     * Initiate vertex GLSL program
     *
     * @param context Android app context
     * @return self object
     * @throws PageFlipException raise exception if fail to initiate program
     */
    public VertexProgram init(Context context) throws PageFlipException {
        super.init(context, R.raw.vertex_shader, R.raw.fragment_shader);
        return this;
    }

    /**
     * Get variable handles after linked GLSL program
     */
    protected void getVarsLocation() {
        if (hProgram != 0) {
            hVertexPosition = glGetAttribLocation(hProgram, VAR_VERTEX_POS);
            hTextureCoord = glGetAttribLocation(hProgram, VAR_TEXTURE_COORD);
            hMVPMatrix = glGetUniformLocation(hProgram, VAR_MVP_MATRIX);
            hTexture = glGetUniformLocation(hProgram, VAR_TEXTURE);
        }
    }

    /**
     * Delete GLSL shader and program
     */
    public void delete() {
        super.delete();

        hTexture = INVALID_HANDLE;
        hMVPMatrix = INVALID_HANDLE;
        hTextureCoord = INVALID_HANDLE;
        hVertexPosition = INVALID_HANDLE;
    }

    /**
     * Initiate matrix with view size
     *
     * @param left view left
     * @param right view right
     * @param bottom view bottom
     * @param top view top
     */
    public void initMatrix(float left, float right, float bottom, float top) {
        float[] projectMatrix = new float[16];
        Matrix.orthoM(projectMatrix, 0, left, right, bottom, top, 0, 6000);
        Matrix.setIdentityM(MVMatrix, 0);
        Matrix.setLookAtM(MVMatrix, 0, 0, 0, 3000, 0, 0, 0, 0, 1, 0);
        Matrix.setIdentityM(MVPMatrix, 0);
        Matrix.multiplyMM(MVPMatrix, 0, projectMatrix, 0, MVMatrix, 0);
    }
}
