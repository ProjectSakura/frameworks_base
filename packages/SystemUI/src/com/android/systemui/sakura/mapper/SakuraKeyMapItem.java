package com.android.systemui.sakura.mapper;

import android.view.KeyEvent;
import org.json.JSONException;
import org.json.JSONObject;

public class SakuraKeyMapItem {
    public static final String TYPE_TAP = "tap";
    public static final String TYPE_JOYSTICK_WASD = "joystick_wasd";

    public String type = TYPE_TAP;
    public int keyCode = KeyEvent.KEYCODE_SPACE;
    public String keyName = "SPACE";
    public float normX = 0.5f;
    public float normY = 0.5f;
    public float radius = 0.08f;

    public int upKey = KeyEvent.KEYCODE_W;
    public int downKey = KeyEvent.KEYCODE_S;
    public int leftKey = KeyEvent.KEYCODE_A;
    public int rightKey = KeyEvent.KEYCODE_D;

    public SakuraKeyMapItem() {}

    public SakuraKeyMapItem(int keyCode, float normX, float normY) {
        this.type = TYPE_TAP;
        this.keyCode = keyCode;
        this.keyName = KeyEvent.keyCodeToString(keyCode).replace("KEYCODE_", "");
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
        return item;
    }

    public JSONObject toJsonObject() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("type", type);
            obj.put("x", (double) normX);
            obj.put("y", (double) normY);
            if (TYPE_TAP.equals(type)) {
                obj.put("key", keyCode);
                obj.put("key_name", keyName);
            } else if (TYPE_JOYSTICK_WASD.equals(type)) {
                obj.put("radius", (double) radius);
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
            item.keyName = obj.optString("key_name", KeyEvent.keyCodeToString(item.keyCode).replace("KEYCODE_", ""));
        } else if (TYPE_JOYSTICK_WASD.equals(item.type)) {
            item.keyName = "WASD";
            item.radius = (float) obj.optDouble("radius", 0.08);
            item.upKey = obj.optInt("up", KeyEvent.KEYCODE_W);
            item.downKey = obj.optInt("down", KeyEvent.KEYCODE_S);
            item.leftKey = obj.optInt("left", KeyEvent.KEYCODE_A);
            item.rightKey = obj.optInt("right", KeyEvent.KEYCODE_D);
        }
        return item;
    }
}
