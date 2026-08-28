/*
 * Copyright (C) 2026 Project Sakura Authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.android.systemui.sakura.mapper;

import android.view.KeyEvent;
import org.json.JSONException;
import org.json.JSONObject;

public class SakuraKeyMapItem {
    public static final String TYPE_TAP = "tap";
    public static final String TYPE_JOYSTICK_WASD = "joystick_wasd";
    public static final String TYPE_GYRO = "gyro";
    public static final String TYPE_GYRO_LEFT = "gyro_left";
    public static final String TYPE_GYRO_RIGHT = "gyro_right";

    public String type = TYPE_TAP;
    public int keyCode = KeyEvent.KEYCODE_SPACE;
    public String keyName = "SPACE";
    public float normX = 0.5f;
    public float normY = 0.5f;
    public float radius = 0.08f;
    public float sensitivity = 1.0f;

    public int upKey = KeyEvent.KEYCODE_W;
    public int downKey = KeyEvent.KEYCODE_S;
    public int leftKey = KeyEvent.KEYCODE_A;
    public int rightKey = KeyEvent.KEYCODE_D;

    public SakuraKeyMapItem() {}

    public static String getCleanKeyName(int keyCode) {
        String name = KeyEvent.keyCodeToString(keyCode).replace("KEYCODE_", "");
        if ("DPAD_UP".equals(name)) return "↑";
        if ("DPAD_DOWN".equals(name)) return "↓";
        if ("DPAD_LEFT".equals(name)) return "←";
        if ("DPAD_RIGHT".equals(name)) return "→";
        return name;
    }

    public SakuraKeyMapItem(int keyCode, float normX, float normY) {
        this.type = TYPE_TAP;
        this.keyCode = keyCode;
        this.keyName = getCleanKeyName(keyCode);
        this.normX = normX;
        this.normY = normY;
    }

    public static SakuraKeyMapItem createJoystick(float normX, float normY, float radius) {
        SakuraKeyMapItem item = new SakuraKeyMapItem();
        item.type = TYPE_JOYSTICK_WASD;
        item.keyName = "WASD";
        item.normX = normX;
        item.normY = normY;
        item.radius = radius;
        item.upKey = KeyEvent.KEYCODE_W;
        item.leftKey = KeyEvent.KEYCODE_A;
        item.downKey = KeyEvent.KEYCODE_S;
        item.rightKey = KeyEvent.KEYCODE_D;
        return item;
    }

    public static SakuraKeyMapItem createGyroLeft(float normX, float normY) {
        SakuraKeyMapItem item = new SakuraKeyMapItem();
        item.type = TYPE_GYRO_LEFT;
        item.keyCode = KeyEvent.KEYCODE_G;
        item.keyName = "G";
        item.normX = normX;
        item.normY = normY;
        return item;
    }

    public static SakuraKeyMapItem createGyroRight(float normX, float normY) {
        SakuraKeyMapItem item = new SakuraKeyMapItem();
        item.type = TYPE_GYRO_RIGHT;
        item.keyCode = KeyEvent.KEYCODE_H;
        item.keyName = "H";
        item.normX = normX;
        item.normY = normY;
        return item;
    }

    public static SakuraKeyMapItem createGyro(float normX, float normY) {
        SakuraKeyMapItem item = new SakuraKeyMapItem();
        item.type = TYPE_GYRO;
        item.keyName = "G/H";
        item.normX = normX;
        item.normY = normY;
        item.leftKey = KeyEvent.KEYCODE_G;
        item.rightKey = KeyEvent.KEYCODE_H;
        item.upKey = KeyEvent.KEYCODE_T;
        item.downKey = KeyEvent.KEYCODE_B;
        return item;
    }

    public JSONObject toJsonObject() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("type", type);
            obj.put("x", (double) normX);
            obj.put("y", (double) normY);
            if (TYPE_TAP.equals(type) || TYPE_GYRO_LEFT.equals(type) || TYPE_GYRO_RIGHT.equals(type)) {
                obj.put("key", keyCode);
                obj.put("key_name", keyName);
                obj.put("sensitivity", (double) sensitivity);
            } else if (TYPE_JOYSTICK_WASD.equals(type)) {
                obj.put("radius", (double) radius);
                obj.put("key_name", keyName);
                obj.put("up", upKey);
                obj.put("down", downKey);
                obj.put("left", leftKey);
                obj.put("right", rightKey);
            } else if (TYPE_GYRO.equals(type)) {
                obj.put("sensitivity", (double) sensitivity);
                obj.put("key_name", keyName);
                obj.put("up", upKey);
                obj.put("down", downKey);
                obj.put("left", leftKey);
                obj.put("right", rightKey);
            }
        } catch (JSONException ignored) {}
        return obj;
    }

    public static SakuraKeyMapItem fromJsonObject(JSONObject obj) {
        SakuraKeyMapItem item = new SakuraKeyMapItem();
        item.type = obj.optString("type", TYPE_TAP);
        item.normX = (float) obj.optDouble("x", 0.5);
        item.normY = (float) obj.optDouble("y", 0.5);

        if (TYPE_TAP.equals(item.type)) {
            item.keyCode = obj.optInt("key", KeyEvent.KEYCODE_SPACE);
            item.keyName = obj.optString("key_name", getCleanKeyName(item.keyCode));
        } else if (TYPE_GYRO_LEFT.equals(item.type)) {
            item.keyCode = obj.optInt("key", KeyEvent.KEYCODE_G);
            item.keyName = obj.optString("key_name", getCleanKeyName(item.keyCode));
            item.sensitivity = (float) obj.optDouble("sensitivity", 1.0);
        } else if (TYPE_GYRO_RIGHT.equals(item.type)) {
            item.keyCode = obj.optInt("key", KeyEvent.KEYCODE_H);
            item.keyName = obj.optString("key_name", getCleanKeyName(item.keyCode));
            item.sensitivity = (float) obj.optDouble("sensitivity", 1.0);
        } else if (TYPE_JOYSTICK_WASD.equals(item.type)) {
            item.radius = (float) obj.optDouble("radius", 0.08);
            item.upKey = obj.optInt("up", KeyEvent.KEYCODE_W);
            item.downKey = obj.optInt("down", KeyEvent.KEYCODE_S);
            item.leftKey = obj.optInt("left", KeyEvent.KEYCODE_A);
            item.rightKey = obj.optInt("right", KeyEvent.KEYCODE_D);
            item.keyName = obj.optString("key_name", "WASD");
        } else if (TYPE_GYRO.equals(item.type)) {
            item.sensitivity = (float) obj.optDouble("sensitivity", 1.0);
            item.upKey = obj.optInt("up", KeyEvent.KEYCODE_T);
            item.downKey = obj.optInt("down", KeyEvent.KEYCODE_B);
            item.leftKey = obj.optInt("left", KeyEvent.KEYCODE_G);
            item.rightKey = obj.optInt("right", KeyEvent.KEYCODE_H);
            item.keyName = obj.optString("key_name", "G/H");
        }
        return item;
    }
}
