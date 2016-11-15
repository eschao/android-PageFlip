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
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static android.opengl.GLES20.GL_COMPILE_STATUS;
import static android.opengl.GLES20.glCompileShader;
import static android.opengl.GLES20.glCreateShader;
import static android.opengl.GLES20.glDeleteShader;
import static android.opengl.GLES20.glGetError;
import static android.opengl.GLES20.glGetShaderInfoLog;
import static android.opengl.GLES20.glGetShaderiv;
import static android.opengl.GLES20.glShaderSource;

/**
 * GLSL shader which is used to load shader script from resources
 *
 * @author eschao
 */

public class GLShader {

    private final static String TAG = "GLShader";

    // shader handle
    protected int hShader;

    /**
     * Default cosntrcutor
     */
    public GLShader() {
        hShader = 0;
    }

    /**
     * Read shader script from resources and compile
     *
     * @param context android context
     * @param type  GL_VERTEX_SHADER or GL_FRAGMENT_SHADER
     * @param resId script resource id
     * @return self
     * @throws PageFlipException if fail to compile GLSL script
     */
    public GLShader compile(Context context, int type, int resId)
    throws PageFlipException {
        // read shader scripts from resource
        String codes = readGLSLFromResource(context, resId);
        if (codes.length() < 1) {
            throw new PageFlipException("Empty GLSL shader for resource id:"
                                        + resId);
        }

        // create a shader
        hShader = glCreateShader(type);
        if (hShader != 0) {
            // upload shader scripts to GL
            glShaderSource(hShader, codes);

            // compile shader scripts
            glCompileShader(hShader);

            // get compile results to check if it is successful
            final int[] result = new int[1];
            glGetShaderiv(hShader, GL_COMPILE_STATUS, result, 0);
            if (result[0] == 0) {
                // delete shader if compile is failed
                Log.e(TAG, "Can'top compile shader for type: " + type +
                           "Error: " + glGetError());
                Log.e(TAG, "Compile shader error: " +
                           glGetShaderInfoLog(hShader));
                glDeleteShader(hShader);
                throw new PageFlipException("Can'top compile shader for" +
                                            "type: " + type);
            }
        } else {
            throw new PageFlipException("Can'top create shader. Error: " +
                                        glGetError());
        }

        return this;
    }

    /**
     * Delete shader
     */
    public void delete() {
        if (hShader != 0) {
            glDeleteShader(hShader);
            hShader = 0;
        }
    }

    /**
     * Get shader handle
     *
     * @return shader shandle
     */
    public int handle() {
        return hShader;
    }

    /**
     * Read GLSL script from resources
     *
     * @param context android context
     * @param resId script resource id
     * @return GLSL script contents
     * @throws PageFlipException If fail to read script from resources
     */
    String readGLSLFromResource(Context context, int resId)
    throws PageFlipException {
        StringBuilder s = new StringBuilder();
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new InputStreamReader(
            context.getResources().openRawResource(resId)));
            String line;

            while ((line = reader.readLine()) != null) {
                s.append(line);
                s.append("\n");
            }
        }
        catch (IOException e) {
            throw new PageFlipException("Could not open resource: "
                                        + resId , e);
        }
        finally {
            // close
            try {
                if (reader != null) {
                    reader.close();
                }
            }
            catch (IOException e) {
            }
        }

        return s.toString();
    }
}
