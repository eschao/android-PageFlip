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
 * Animation state of page flip
 *
 * @author eschao
 */
public enum PageFlipState {
    BEGIN_FLIP,
    FORWARD_FLIP,
    BACKWARD_FLIP,
    RESTORE_FLIP,
    END_FLIP,
    END_WITH_FORWARD,
    END_WITH_BACKWARD,
    END_WITH_RESTORE,
}
