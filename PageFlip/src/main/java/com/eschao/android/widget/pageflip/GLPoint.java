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

/**
 * GLPoint includes (x,y,z) in OpenGL coordinate system and its texture
 * coordinates (texX, texY)
 *
 * @author eschao
 */
public final class GLPoint {
    // 3D coordinate
    float x;
    float y;
    float z;

    // texutre coordinate
    float texX;
    float texY;

    /**
     * Set GLPoint with given values
     *
     * @param x x coordinate
     * @param y y coordinate
     * @param z z coordinate
     * @param tX x coordinate of texture
     * @param tY y coordinate of texture
     */
    public void set(float x, float y, float z, float tX, float tY) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.texX = tX;
        this.texY = tY;
    }
}
