/*
 * Copyright (C) 2020 The Pixel Experience Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.util.sakura;

import android.os.Build;
import android.util.Log;

import java.util.Arrays;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class PixelPropsUtils {

    private static final String TAG = PixelPropsUtils.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final Map<String, Object> propsToChange;
    private static final Map<String, Object> propsToChangePixel3;
    private static final Map<String, Object> propsToChangePixelXL;

    private static final String[] packagesToChange = {
        "com.breel.wallpapers20",
        "com.google.android.apps.customization.pixel",
        "com.google.android.apps.fitness",
        "com.google.android.apps.recorder",
        "com.google.android.apps.subscriptions.red",
        "com.google.android.apps.tachyon",
        "com.google.android.apps.wallpaper.pixel",
        "com.google.android.as",
        "com.google.android.dialer",
        "com.google.android.gms.location.history",
        "com.google.android.soundpicker",
        "com.google.pixel.dynamicwallpapers",
        "com.google.pixel.livewallpaper",
        "com.google.android.apps.safetyhub",
        "com.google.android.apps.turbo",
        "com.google.android.apps.turboadapter",
        "com.google.android.apps.wallpaper",
        "com.google.android.apps.maps",
        "com.google.android.gms",
        "com.google.android.apps.nexuslauncher"
    };

    private static final String[] packagesToChangePixel3 = {
        "com.google.android.googlequicksearchbox"
    };

    private static final String[] packagesToChangePixelXL = {
            "com.google.android.apps.photos",
            "com.samsung.android.app.watchmanager",
            "com.samsung.accessory.fridaymgr",
            "com.samsung.accessory.berrymgr",
            "com.samsung.accessory.neobeanmgr",
            "com.samsung.android.geargplugin",
            "com.samsung.android.gearnplugin",
            "com.samsung.android.gearpplugin",
            "com.samsung.android.gearrplugin",
            "com.samsung.android.modenplugin",
            "com.samsung.android.neatplugin"
    };

    static {
        propsToChange = new HashMap<>();
        propsToChange.put("BRAND", "google");
        propsToChange.put("MANUFACTURER", "Google");
        propsToChange.put("DEVICE", "redfin");
        propsToChange.put("PRODUCT", "redfin");
        propsToChange.put("MODEL", "Pixel 5");
        propsToChange.put("FINGERPRINT", "google/redfin/redfin:11/RQ3A.210805.001.A1/7474174:user/release-keys");
        propsToChangePixel3 = new HashMap<>();
        propsToChangePixel3.put("MODEL", "Pixel 3 XL");
        propsToChangePixelXL = new HashMap<>();
        propsToChangePixelXL.put("BRAND", "google");
        propsToChangePixelXL.put("MANUFACTURER", "Google");
        propsToChangePixelXL.put("DEVICE", "marlin");
        propsToChangePixelXL.put("PRODUCT", "marlin");
        propsToChangePixelXL.put("MODEL", "Pixel XL");
    }

    public static void setProps(String packageName) {
        if (packageName == null) {
            return;
        }
        if (Arrays.asList(packagesToChange).contains(packageName)) {
            if (DEBUG) {
                Log.d(TAG, "Defining props for: " + packageName);
            }
            for (Map.Entry<String, Object> prop : propsToChange.entrySet()) {
                String key = prop.getKey();
                Object value = prop.getValue();
                setPropValue(key, value);
            }
        }
        if (Arrays.asList(packagesToChangePixel3).contains(packageName)) {
            if (DEBUG) {
                Log.d(TAG, "Defining props for: " + packageName);
            }
            for (Map.Entry<String, Object> prop : propsToChangePixel3.entrySet()) {
                String key = prop.getKey();
                Object value = prop.getValue();
                setPropValue(key, value);
            }
        }
        // Set proper indexing fingerprint
        if (packageName.equals("com.google.android.settings.intelligence")) {
            setPropValue("FINGERPRINT", Build.DATE);
        }
        if (Arrays.asList(packagesToChangePixelXL).contains(packageName)){
            if (DEBUG){
                Log.d(TAG, "Defining props for: " + packageName);
            }
            for (Map.Entry<String, Object> prop : propsToChangePixelXL.entrySet()) {
                String key = prop.getKey();
                Object value = prop.getValue();
                setPropValue(key, value);
            }
        }
    }

    private static void setPropValue(String key, Object value) {
        try {
            if (DEBUG) {
                Log.d(TAG, "Defining prop " + key + " to " + value.toString());
            }
            Field field = Build.class.getDeclaredField(key);
            field.setAccessible(true);
            field.set(null, value);
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Failed to set prop " + key, e);
        }
    }
}
