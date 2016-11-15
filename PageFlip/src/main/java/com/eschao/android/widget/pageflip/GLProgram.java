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
 * GLSL program is used to load, compile and link shader scripts
 *
 * @author eschao
 */

public class GLProgram {

    protected final int INVALID_HANDLE = -1;

    // GLSL program handle
    protected int hProgram;

    // Vertex shader
    protected GLShader mVertex;

    // Fragment shader
    protected GLShader mFragment;

    public GLProgram() {
        hProgram = INVALID_HANDLE;
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
     * @throws PageFlipException If fail to read or compile GLSL scripts
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
        hProgram = glCreateProgram();
        if (hProgram == 0) {
            mVertex.delete();
            mFragment.delete();
            throw new PageFlipException("Can'top create texture program");
        }

        // 3. attach vertex and fragment shader
        glAttachShader(hProgram, mVertex.handle());
        glAttachShader(hProgram, mFragment.handle());
        glLinkProgram(hProgram);

        // 4. check shader link status
        int[] result = new int[1];
        glGetProgramiv(hProgram, GL_LINK_STATUS, result, 0);
        if (result[0] == 0) {
            delete();
            throw new PageFlipException("Can'top link program");
        }

        // 5. get all variable handles defined in scripts
        // subclass should implement getVarsLocation to be responsible for its
        // own variables in script
        glUseProgram(hProgram);
        getVarsLocation();
        return this;
    }

    /**
     * Delete all handles
     */
    public void delete() {
        mVertex.delete();
        mFragment.delete();

        if (hProgram != INVALID_HANDLE) {
            glDeleteProgram(hProgram);
            hProgram = INVALID_HANDLE;
        }
    }

    /**
     * Get program handle
     * @return
     */
    public int handle() {
        return hProgram;
    }

    /**
     * Subclass should implement it to get its own variable handles which are
     * defined in its GLSL scripts
     */
    protected void getVarsLocation() {
    }
}
