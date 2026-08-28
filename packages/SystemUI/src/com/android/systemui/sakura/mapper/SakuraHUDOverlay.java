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

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class SakuraHUDOverlay implements SakuraTouchNodeView.OnNodeActionListener {
    private static final String TAG = "SakuraHUDOverlay";

    private final Context mContext;
    private final WindowManager mWindowManager;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    private LinearLayout mToolbar;
    private TextView mPackageTitle;
    private WindowManager.LayoutParams mToolbarParams;

    private boolean mIsShowing = false;
    private String mCurrentPackage = "com.android.systemui";
    private final List<SakuraTouchNodeView> mActiveNodes = new ArrayList<>();

    public interface OnSaveListener {
        void onProfileSaved(String packageName, String profileJson);
        void onOverlayVisibilityChanged(boolean showing);
    }

    private OnSaveListener mSaveListener;

    public SakuraHUDOverlay(Context context, OnSaveListener listener) {
        this.mContext = context;
        this.mSaveListener = listener;
        this.mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        initToolbar();
    }

    private Rect getRealDisplayBounds() {
        return mWindowManager.getCurrentWindowMetrics().getBounds();
    }

    private void initToolbar() {
        DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
        float density = dm.density;
        Rect bounds = getRealDisplayBounds();
        int screenW = bounds.width();

        int toolbarWidth = Math.min((int) (540 * density), (int) (screenW * 0.94f));

        mToolbar = new LinearLayout(mContext);
        mToolbar.setOrientation(LinearLayout.VERTICAL);
        int padH = (int) (16 * density);
        int padV = (int) (12 * density);
        mToolbar.setPadding(padH, padV, padH, padV);

        GradientDrawable toolbarBg = new GradientDrawable();
        toolbarBg.setColor(Color.parseColor("#F210081E"));
        toolbarBg.setCornerRadius(16.0f * density);
        toolbarBg.setStroke((int) (1.5f * density), Color.parseColor("#7B2CBF"));
        mToolbar.setBackground(toolbarBg);

        LinearLayout row1 = new LinearLayout(mContext);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER_VERTICAL);
        row1.setPadding(0, 0, 0, (int) (8 * density));

        TextView brandTitle = new TextView(mContext);
        brandTitle.setText("SAKURA MAPPER");
        brandTitle.setTextColor(Color.parseColor("#E0AAFF"));
        brandTitle.setTextSize(12.5f);
        brandTitle.setTypeface(null, android.graphics.Typeface.BOLD);

        mPackageTitle = new TextView(mContext);
        mPackageTitle.setText(" • " + mCurrentPackage);
        mPackageTitle.setTextColor(Color.parseColor("#C77DFF"));
        mPackageTitle.setTextSize(10.5f);
        mPackageTitle.setSingleLine(true);

        View row1Spacer = new View(mContext);
        LinearLayout.LayoutParams spacerLp = new LinearLayout.LayoutParams(0, 0, 1.0f);
        row1Spacer.setLayoutParams(spacerLp);

        Button btnSave = createHeaderButton("SAVE", "#5A189A", "#9D4EDD", "#FFFFFF");
        btnSave.setOnClickListener(v -> saveAndApply());

        Button btnClose = createHeaderButton("CLOSE", "#20083B", "#7B2CBF", "#E0AAFF");
        btnClose.setOnClickListener(v -> hide());

        row1.addView(brandTitle);
        row1.addView(mPackageTitle);
        row1.addView(row1Spacer);
        row1.addView(btnSave);
        row1.addView(btnClose);

        mToolbar.addView(row1);

        LinearLayout row2 = new LinearLayout(mContext);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setGravity(Gravity.CENTER_VERTICAL);
        row2.setPadding(0, 0, 0, 0);

        Button btnAddTap = createGridButton("+ KEY", "#20083B", "#7B2CBF", "#F8F5FC");
        btnAddTap.setOnClickListener(v -> addNode(new SakuraKeyMapItem(KeyEvent.KEYCODE_SPACE, 0.5f, 0.5f)));

        Button btnAddWASD = createGridButton("+ WASD", "#20083B", "#7B2CBF", "#F8F5FC");
        btnAddWASD.setOnClickListener(v -> {
            addNode(new SakuraKeyMapItem(KeyEvent.KEYCODE_W, 0.20f, 0.58f));
            addNode(new SakuraKeyMapItem(KeyEvent.KEYCODE_A, 0.13f, 0.68f));
            addNode(new SakuraKeyMapItem(KeyEvent.KEYCODE_S, 0.20f, 0.78f));
            addNode(new SakuraKeyMapItem(KeyEvent.KEYCODE_D, 0.27f, 0.68f));
        });

        Button btnAddGyro = createGridButton("+ GYRO", "#20083B", "#7B2CBF", "#F8F5FC");
        btnAddGyro.setOnClickListener(v -> {
            addNode(SakuraKeyMapItem.createGyroLeft(0.18f, 0.65f));
            addNode(SakuraKeyMapItem.createGyroRight(0.82f, 0.65f));
        });

        Button btnExport = createGridButton("EXPORT", "#20083B", "#7B2CBF", "#F8F5FC");
        btnExport.setOnClickListener(v -> exportProfile());

        Button btnImport = createGridButton("IMPORT", "#20083B", "#7B2CBF", "#F8F5FC");
        btnImport.setOnClickListener(v -> importProfile());

        Button btnClear = createGridButton("CLEAR", "#2A0A16", "#FF4D6D", "#FF758F");
        btnClear.setOnClickListener(v -> clearAllNodes());

        row2.addView(btnAddTap);
        row2.addView(btnAddWASD);
        row2.addView(btnAddGyro);
        row2.addView(btnExport);
        row2.addView(btnImport);
        row2.addView(btnClear);

        mToolbar.addView(row2);

        setupToolbarDrag(mToolbar);

        mToolbarParams = new WindowManager.LayoutParams(
                toolbarWidth,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT);
        mToolbarParams.gravity = Gravity.TOP | Gravity.START;
        mToolbarParams.x = Math.max(0, (screenW - toolbarWidth) / 2);
        mToolbarParams.y = (int) (24 * density);
    }

    private Button createHeaderButton(String text, String bgColor, String strokeColor, String textColor) {
        DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
        float density = dm.density;

        Button btn = new Button(mContext);
        btn.setText(text);
        btn.setTextColor(Color.parseColor(textColor));
        btn.setTextSize(11.0f);
        btn.setTypeface(null, android.graphics.Typeface.BOLD);
        btn.setPadding((int) (14 * density), (int) (6 * density), (int) (14 * density), (int) (6 * density));
        btn.setAllCaps(true);
        btn.setMinHeight(0);
        btn.setMinimumHeight(0);
        btn.setMinWidth(0);
        btn.setMinimumWidth(0);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor(bgColor));
        bg.setCornerRadius(8.0f * density);
        bg.setStroke((int) (1.2f * density), Color.parseColor(strokeColor));
        btn.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins((int) (4 * density), 0, (int) (4 * density), 0);
        btn.setLayoutParams(lp);
        return btn;
    }

    private Button createGridButton(String text, String bgColor, String strokeColor, String textColor) {
        DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
        float density = dm.density;

        Button btn = new Button(mContext);
        btn.setText(text);
        btn.setTextColor(Color.parseColor(textColor));
        btn.setTextSize(10.5f);
        btn.setTypeface(null, android.graphics.Typeface.BOLD);
        btn.setPadding(0, (int) (8 * density), 0, (int) (8 * density));
        btn.setGravity(Gravity.CENTER);
        btn.setAllCaps(true);
        btn.setMinHeight(0);
        btn.setMinimumHeight(0);
        btn.setMinWidth(0);
        btn.setMinimumWidth(0);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor(bgColor));
        bg.setCornerRadius(8.0f * density);
        bg.setStroke((int) (1.2f * density), Color.parseColor(strokeColor));
        btn.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1.0f);
        lp.setMargins((int) (3 * density), 0, (int) (3 * density), 0);
        btn.setLayoutParams(lp);
        return btn;
    }

    private void setupToolbarDrag(View view) {
        view.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            private boolean isDrag = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = mToolbarParams.x;
                        initialY = mToolbarParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        isDrag = false;
                        return false;
                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - initialTouchX;
                        float dy = event.getRawY() - initialTouchY;
                        if (Math.hypot(dx, dy) > 8.0f) {
                            isDrag = true;
                            mToolbarParams.x = initialX + (int) dx;
                            mToolbarParams.y = initialY + (int) dy;
                            try {
                                mWindowManager.updateViewLayout(mToolbar, mToolbarParams);
                            } catch (Exception ignored) {}
                            return true;
                        }
                        return false;
                    case MotionEvent.ACTION_UP:
                        return isDrag;
                }
                return false;
            }
        });
    }

    public void show(String packageName, String existingProfileJson) {
        mMainHandler.post(() -> {
            mCurrentPackage = (packageName != null && !packageName.isEmpty()) ? packageName : "com.android.systemui";
            mPackageTitle.setText(" • " + mCurrentPackage);

            Rect bounds = getRealDisplayBounds();
            int screenW = bounds.width();
            DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
            float density = dm.density;

            if (!mIsShowing) {
                try {
                    int toolbarWidth = Math.min((int) (540 * density), (int) (screenW * 0.94f));
                    mToolbarParams.width = toolbarWidth;
                    mToolbarParams.x = Math.max(0, (screenW - toolbarWidth) / 2);
                    mToolbarParams.y = (int) (24 * density);

                    mWindowManager.addView(mToolbar, mToolbarParams);
                    mIsShowing = true;
                    if (mSaveListener != null) {
                        mSaveListener.onOverlayVisibilityChanged(true);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to add toolbar overlay", e);
                }
            }

            if (mActiveNodes.isEmpty() && existingProfileJson != null && !existingProfileJson.isEmpty()) {
                loadProfileJson(existingProfileJson);
            }

            for (SakuraTouchNodeView node : mActiveNodes) {
                node.setEditingMode(true);
            }
        });
    }

    public void hide() {
        mMainHandler.post(() -> {
            if (mIsShowing) {
                saveAndApplySilently();

                for (SakuraTouchNodeView node : mActiveNodes) {
                    node.setEditingMode(false);
                }

                try {
                    mWindowManager.removeView(mToolbar);
                    mIsShowing = false;
                    if (mSaveListener != null) {
                        mSaveListener.onOverlayVisibilityChanged(false);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to remove toolbar overlay", e);
                }
            }
        });
    }

    public void dismissAll() {
        mMainHandler.post(() -> {
            if (mIsShowing) {
                try {
                    mWindowManager.removeView(mToolbar);
                    mIsShowing = false;
                    if (mSaveListener != null) {
                        mSaveListener.onOverlayVisibilityChanged(false);
                    }
                } catch (Exception ignored) {}
            }
            removeNodeViewsFromScreen();
            mActiveNodes.clear();
        });
    }

    public void loadInGameHUD(String packageName, String profileJson) {
        mMainHandler.post(() -> {
            mCurrentPackage = (packageName != null && !packageName.isEmpty()) ? packageName : "com.android.systemui";
            mPackageTitle.setText(" • " + mCurrentPackage);

            removeNodeViewsFromScreen();
            mActiveNodes.clear();

            if (profileJson != null && !profileJson.isEmpty()) {
                loadProfileJson(profileJson);
                for (SakuraTouchNodeView node : mActiveNodes) {
                    node.setEditingMode(false);
                }
            }
        });
    }

    public boolean isShowing() {
        return mIsShowing;
    }

    private int getNodeSizePx(SakuraKeyMapItem item, float density) {
        if (SakuraKeyMapItem.TYPE_JOYSTICK_WASD.equals(item.type)) {
            return (int) (70 * density);
        } else if (SakuraKeyMapItem.TYPE_GYRO_LEFT.equals(item.type) || SakuraKeyMapItem.TYPE_GYRO_RIGHT.equals(item.type) || SakuraKeyMapItem.TYPE_GYRO.equals(item.type)) {
            return (int) (52 * density);
        }
        return (int) (44 * density);
    }

    private void addNode(SakuraKeyMapItem item) {
        DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
        float density = dm.density;
        Rect bounds = getRealDisplayBounds();
        int screenW = bounds.width();
        int screenH = bounds.height();

        int sizePx = getNodeSizePx(item, density);

        WindowManager.LayoutParams nodeParams = new WindowManager.LayoutParams(
                sizePx,
                sizePx,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT);
        nodeParams.gravity = Gravity.TOP | Gravity.START;
        nodeParams.x = (int) (item.normX * screenW - sizePx / 2.0f);
        nodeParams.y = (int) (item.normY * screenH - sizePx / 2.0f);

        SakuraTouchNodeView node = new SakuraTouchNodeView(mContext, mWindowManager, nodeParams, item, this);
        mActiveNodes.add(node);

        try {
            mWindowManager.addView(node, nodeParams);
            node.setListeningForKey(true);
        } catch (Exception e) {
            Log.e(TAG, "Failed to add node view", e);
        }

        saveAndApplySilently();
    }

    private void removeNodeViewsFromScreen() {
        for (SakuraTouchNodeView node : mActiveNodes) {
            try {
                mWindowManager.removeView(node);
            } catch (Exception ignored) {}
        }
    }

    private void clearAllNodes() {
        removeNodeViewsFromScreen();
        mActiveNodes.clear();
        saveAndApply();
    }

    private String buildProfileJson() {
        DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
        float density = dm.density;
        Rect bounds = getRealDisplayBounds();
        int screenW = bounds.width();
        int screenH = bounds.height();

        JSONObject root = new JSONObject();
        try {
            root.put("package", mCurrentPackage);
            root.put("version", 1);
            JSONArray array = new JSONArray();

            for (SakuraTouchNodeView node : mActiveNodes) {
                SakuraKeyMapItem item = node.getItem();
                WindowManager.LayoutParams lp = node.getLayoutParamsRef();
                int sizePx = getNodeSizePx(item, density);

                float cx = lp.x + (sizePx / 2.0f);
                float cy = lp.y + (sizePx / 2.0f);

                item.normX = Math.max(0.0f, Math.min(1.0f, cx / (float) screenW));
                item.normY = Math.max(0.0f, Math.min(1.0f, cy / (float) screenH));

                array.put(item.toJsonObject());
            }
            root.put("mappings", array);
        } catch (Exception e) {
            Log.e(TAG, "Error building profile json", e);
        }
        return root.toString();
    }

    private void loadProfileJson(String jsonStr) {
        try {
            JSONObject root = new JSONObject(jsonStr);
            JSONArray array = root.optJSONArray("mappings");
            if (array != null) {
                Rect bounds = getRealDisplayBounds();
                int screenW = bounds.width();
                int screenH = bounds.height();
                DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
                float density = dm.density;

                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    SakuraKeyMapItem item = SakuraKeyMapItem.fromJsonObject(obj);

                    int sizePx = getNodeSizePx(item, density);
                    WindowManager.LayoutParams nodeParams = new WindowManager.LayoutParams(
                            sizePx,
                            sizePx,
                            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                    | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                            PixelFormat.TRANSLUCENT);
                    nodeParams.gravity = Gravity.TOP | Gravity.START;
                    nodeParams.x = (int) (item.normX * screenW - sizePx / 2.0f);
                    nodeParams.y = (int) (item.normY * screenH - sizePx / 2.0f);

                    SakuraTouchNodeView node = new SakuraTouchNodeView(mContext, mWindowManager, nodeParams, item, this);
                    mActiveNodes.add(node);

                    try {
                        mWindowManager.addView(node, nodeParams);
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse profile json", e);
        }
    }

    private void saveAndApplySilently() {
        String json = buildProfileJson();
        if (mSaveListener != null) {
            mSaveListener.onProfileSaved(mCurrentPackage, json);
        }
    }

    private void saveAndApply() {
        saveAndApplySilently();
        Toast.makeText(mContext, "Sakura Keymap Saved", Toast.LENGTH_SHORT).show();
    }

    private void exportProfile() {
        String json = buildProfileJson();
        try {
            File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "SakuraKeymaps");
            if (!dir.exists()) dir.mkdirs();
            File exportFile = new File(dir, mCurrentPackage + ".sakura.json");
            Files.writeString(exportFile.toPath(), json);

            Intent shareIntent = new Intent(Intent.ACTION_SEND)
                    .setType("application/json")
                    .putExtra(Intent.EXTRA_TEXT, json)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(Intent.createChooser(shareIntent, "Share Keymap").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));

            Toast.makeText(mContext, "Exported to Downloads/SakuraKeymaps/", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Export failed", e);
            Toast.makeText(mContext, "Export Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void importProfile() {
        try {
            File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "SakuraKeymaps");
            File importFile = new File(dir, mCurrentPackage + ".sakura.json");
            if (importFile.exists()) {
                String content = Files.readString(importFile.toPath());
                removeNodeViewsFromScreen();
                mActiveNodes.clear();
                loadProfileJson(content);
                saveAndApply();
                Toast.makeText(mContext, "Imported " + importFile.getName(), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(mContext, "No profile found: " + importFile.getName(), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Import failed", e);
            Toast.makeText(mContext, "Import Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onNodeMoved(SakuraTouchNodeView node) {
        saveAndApplySilently();
    }

    @Override
    public void onNodeDeleted(SakuraTouchNodeView node) {
        try {
            mWindowManager.removeView(node);
        } catch (Exception ignored) {}
        mActiveNodes.remove(node);
        saveAndApplySilently();
        Toast.makeText(mContext, "Removed: " + node.getItem().keyName, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNodeKeyBound(SakuraTouchNodeView node, int keyCode, String keyName) {
        saveAndApplySilently();
        Toast.makeText(mContext, "Bound: " + keyName, Toast.LENGTH_SHORT).show();
    }
}
