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

import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;

/**
 * Shadow vertex shader program which is used to load:
 * <ul>
 *     <li>shadow_vertex_shader.glsl</li>
 *     <li>shadow_fragment_shader.glsl</li>
 * </ul>
 *
 * @author eschao
 */
public class ShadowVertexProgram extends GLProgram {

    // variable names defined in shader scripts
    final static String VAR_MVP_MATRIX  = "u_MVPMatrix";
    final static String VAR_VERTEX_Z    = "u_vexZ";
    final static String VAR_VERTEX_POS  = "a_vexPosition";

    int hMVPMatrix;
    int hVertexZ;
    int hVertexPosition;

    /**
     * Constructor
     */
    public ShadowVertexProgram() {
        super();

        hMVPMatrix = INVALID_HANDLE;
        hVertexZ = INVALID_HANDLE;
        hVertexPosition = INVALID_HANDLE;
    }

    /**
     * Initiate shader program
     *
     * @param context android context
     * @return self
     * @throws PageFlipException raise exception if fail to compile & link
     *                           program
     */
    public ShadowVertexProgram init(Context context) throws
                                                     PageFlipException {
        super.init(context,
                   R.raw.shadow_vertex_shader,
                   R.raw.shadow_fragment_shader);
        return this;
    }

    /**
     * Get variable handles from linked shader program
     */
    protected void getVarsLocation() {
        if (hProgram != 0) {
            hVertexZ = glGetUniformLocation(hProgram, VAR_VERTEX_Z);
            hVertexPosition = glGetAttribLocation(hProgram, VAR_VERTEX_POS);
            hMVPMatrix = glGetUniformLocation(hProgram, VAR_MVP_MATRIX);
        }
    }

    /**
     * Delete shader resources
     */
    public void delete() {
        super.delete();

        hMVPMatrix = INVALID_HANDLE;
        hVertexZ = INVALID_HANDLE;
        hVertexPosition = INVALID_HANDLE;
    }
}
