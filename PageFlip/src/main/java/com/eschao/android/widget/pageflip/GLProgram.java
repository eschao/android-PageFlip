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

import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_LINK_STATUS;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glAttachShader;
import static android.opengl.GLES20.glCreateProgram;
import static android.opengl.GLES20.glDeleteProgram;
import static android.opengl.GLES20.glGetProgramiv;
import static android.opengl.GLES20.glLinkProgram;
import static android.opengl.GLES20.glUseProgram;

/**
 * GLSL program class is used to load, compile and link shader scripts
 *
 * @author eschao
 */

public class GLProgram {

    // invalid GL getShaderRef including program reference and variable location
    protected final int INVALID_GL_HANDLE = -1;

    // GLSL program reference
    protected int mProgramRef;

    // Vertex shader
    protected GLShader mVertex;

    // Fragment shader
    protected GLShader mFragment;

    public GLProgram() {
        mProgramRef = INVALID_GL_HANDLE;
        mVertex = new GLShader();
        mFragment = new GLShader();
    }

    /**
     * Initiate with given vertex shader and fragment shader
     *
     * @param context android context
     * @param vertexResId vertex shader script id
     * @param fragmentResId fragment shader script id
     * @return self
     * @throws PageFlipException if fail to read or compile shader scripts
     */
    public GLProgram init(Context context, int vertexResId, int fragmentResId)
                                                    throws PageFlipException {
        // 1. init shader
        try {
            mVertex.compile(context, GL_VERTEX_SHADER, vertexResId);
            mFragment.compile(context, GL_FRAGMENT_SHADER, fragmentResId);
        }
        catch (PageFlipException e) {
            mVertex.delete();
            mFragment.delete();
            throw e;
        }

        // 2. create texture program and link shader
        mProgramRef = glCreateProgram();
        if (mProgramRef == 0) {
            mVertex.delete();
            mFragment.delete();
            throw new PageFlipException("Can't create texture program");
        }

        // 3. attach vertex and fragment shader
        glAttachShader(mProgramRef, mVertex.getShaderRef());
        glAttachShader(mProgramRef, mFragment.getShaderRef());
        glLinkProgram(mProgramRef);

        // 4. check shader link status
        int[] result = new int[1];
        glGetProgramiv(mProgramRef, GL_LINK_STATUS, result, 0);
        if (result[0] == 0) {
            delete();
            throw new PageFlipException("Can't link program");
        }

        // 5. get all variable handles defined in scripts
        // subclass should implement getVarsLocation to be responsible for its
        // own variables in script
        glUseProgram(mProgramRef);
        getVarsLocation();
        return this;
    }

    /**
     * Delete all handles
     */
    public void delete() {
        mVertex.delete();
        mFragment.delete();

        if (mProgramRef != INVALID_GL_HANDLE) {
            glDeleteProgram(mProgramRef);
            mProgramRef = INVALID_GL_HANDLE;
        }
    }

    /**
     * Get program GL reference
     *
     * @return program GL reference for program
     */
    public int getProgramRef() {
        return mProgramRef;
    }

    /**
     * Subclass should implement it to get its own variable handles which are
     * defined in its shader scripts
     */
    protected void getVarsLocation() {
    }
}
