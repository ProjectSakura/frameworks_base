/*
 * Copyright (C) 2018-2020 crDroid Android Project
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

package com.android.internal.statusbar;

import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.os.RemoteException;

public class ThemeAccentUtils {

    public static final String TAG = "ThemeAccentUtils";

    public static void setCutoutOverlay(IOverlayManager om, int userId, boolean enable) {
        try {
            om.setEnabled("com.android.overlay.hidecutout", enable, userId);
        } catch (RemoteException e) {
        }
    }

    public static void setStatusBarStockOverlay(IOverlayManager om, int userId, boolean enable) {
        try {
            om.setEnabled("com.android.overlay.statusbarstock", enable, userId);
            om.setEnabled("com.android.overlay.statusbarstocksysui", enable, userId);
        } catch (RemoteException e) {
        }
    }
}

