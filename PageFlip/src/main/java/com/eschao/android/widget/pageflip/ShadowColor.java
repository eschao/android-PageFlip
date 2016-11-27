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
 * Shadow color
 *
 * @author eschao
 */

public final class ShadowColor {

    float startColor;
    float startAlpha;
    float endColor;
    float endAlpha;

    /**
     * Default constructor
     */
    public ShadowColor() {
        startColor = 0;
        startAlpha = 0;
        endColor = 0;
        endAlpha = 0;
    }

    /**
     * Constructor
     *
     * @param startColor start color, range is [0 .. 1]
     * @param startAlpha start alpha, range is [0 .. 1]
     * @param endColor end color, range is [0 .. 1]
     * @param endAlpha end alpha, range is [0 .. 1]
     */
    public ShadowColor(float startColor, float startAlpha,
                       float endColor, float endAlpha) {
        set(startColor, startAlpha, endColor, endAlpha);
    }

    /**
     * Set color and alpha
     *
     * @param startColor start color, range is [0 .. 1]
     * @param startAlpha start alpha, range is [0 .. 1]
     * @param endColor end color, range is [0 .. 1]
     * @param endAlpha end alpha, range is [0 .. 1]
     */
    public void set(float startColor, float startAlpha,
                    float endColor, float endAlpha) {
        if (startColor < 0 || startColor > 1 ||
            startAlpha < 0 || startAlpha > 1 ||
            endColor < 0 || endColor > 1 ||
            endAlpha < 0 || endAlpha > 1) {
            throw new IllegalArgumentException("Illegal color or alpha value!");
        }

        this.startColor = startColor;
        this.startAlpha = startAlpha;
        this.endColor = endColor;
        this.endAlpha = endAlpha;
    }
}
