package com.android.systemui.sakura.mapper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

public class SakuraTouchNodeView extends View {
    private static final int COLOR_SURFACE = Color.parseColor("#CC120A21");
    private static final int COLOR_BORDER_DEFAULT = Color.parseColor("#9D4EDD");
    private static final int COLOR_BORDER_LISTENING = Color.parseColor("#E0AAFF");
    private static final int COLOR_TEXT = Color.parseColor("#F8F5FC");
    private static final int COLOR_SUBTEXT = Color.parseColor("#C77DFF");
    private static final int COLOR_JOYSTICK_CROSS = Color.parseColor("#66C77DFF");
    private static final int COLOR_DELETE_BG = Color.parseColor("#FF4D6D");

    private final WindowManager mWindowManager;
    private final WindowManager.LayoutParams mLayoutParams;
    private final SakuraKeyMapItem mItem;

    private final Paint mBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mSubtextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mCrosshairPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mDeletePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mBounds = new RectF();
    private final RectF mDeleteBounds = new RectF();

    private boolean mIsEditing = true;
    private boolean mIsListeningForKey = false;
    private int mBindingStep = 0;
    private float mTouchDownX, mTouchDownY;
    private int mInitialParamX, mInitialParamY;
    private boolean mIsDragging = false;

    public interface OnNodeActionListener {
        void onNodeMoved(SakuraTouchNodeView node);
        void onNodeDeleted(SakuraTouchNodeView node);
        void onNodeKeyBound(SakuraTouchNodeView node, int keyCode, String keyName);
    }

    private final OnNodeActionListener mListener;

    public SakuraTouchNodeView(Context context, WindowManager wm, WindowManager.LayoutParams lp,
                               SakuraKeyMapItem item, OnNodeActionListener listener) {
        super(context);
        this.mWindowManager = wm;
        this.mLayoutParams = lp;
        this.mItem = item;
        this.mListener = listener;
        initPaints();
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    private void initPaints() {
        mBgPaint.setColor(COLOR_SURFACE);
        mBgPaint.setStyle(Paint.Style.FILL);

        mBorderPaint.setColor(COLOR_BORDER_DEFAULT);
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setStrokeWidth(4.5f);
        mBorderPaint.setShadowLayer(8.0f, 0, 0, Color.parseColor("#7B2CBF"));

        mTextPaint.setColor(COLOR_TEXT);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setFakeBoldText(true);
        mTextPaint.setTextSize(26.0f);

        mSubtextPaint.setColor(COLOR_SUBTEXT);
        mSubtextPaint.setTextAlign(Paint.Align.CENTER);
        mSubtextPaint.setFakeBoldText(true);
        mSubtextPaint.setTextSize(14.0f);

        mCrosshairPaint.setColor(COLOR_JOYSTICK_CROSS);
        mCrosshairPaint.setStrokeWidth(2.5f);
        mCrosshairPaint.setStyle(Paint.Style.STROKE);

        mDeletePaint.setColor(COLOR_DELETE_BG);
        mDeletePaint.setStyle(Paint.Style.FILL);
    }

    public SakuraKeyMapItem getItem() {
        return mItem;
    }

    public WindowManager.LayoutParams getLayoutParamsRef() {
        return mLayoutParams;
    }

    public boolean isListeningForKey() {
        return mIsListeningForKey;
    }

    public void setEditingMode(boolean editing) {
        this.mIsEditing = editing;
        if (editing) {
            mLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
            setAlpha(1.0f);
        } else {
            mLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
            setAlpha(0.65f);
        }
        try {
            mWindowManager.updateViewLayout(this, mLayoutParams);
        } catch (Exception ignored) {}
        invalidate();
    }

    public void setListeningForKey(boolean listening) {
        mIsListeningForKey = listening;
        mBindingStep = listening ? 1 : 0;
        mBorderPaint.setColor(mIsListeningForKey ? COLOR_BORDER_LISTENING : COLOR_BORDER_DEFAULT);
        mBorderPaint.setStrokeWidth(mIsListeningForKey ? 7.0f : 4.5f);

        if (mIsListeningForKey) {
            mLayoutParams.flags &= ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        } else {
            mLayoutParams.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        }

        try {
            mWindowManager.updateViewLayout(this, mLayoutParams);
        } catch (Exception ignored) {}

        if (mIsListeningForKey) {
            requestFocus();
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float cx = w / 2.0f;
        float cy = h / 2.0f;
        float radius = Math.min(w, h) / 2.0f - 8.0f;

        mBounds.set(cx - radius, cy - radius, cx + radius, cy + radius);
        canvas.drawCircle(cx, cy, radius, mBgPaint);

        boolean isGyroLeft = SakuraKeyMapItem.TYPE_GYRO_LEFT.equals(mItem.type);
        boolean isGyroRight = SakuraKeyMapItem.TYPE_GYRO_RIGHT.equals(mItem.type);
        boolean isGyroCombined = SakuraKeyMapItem.TYPE_GYRO.equals(mItem.type);
        boolean isJoystick = SakuraKeyMapItem.TYPE_JOYSTICK_WASD.equals(mItem.type);

        if (isJoystick) {
            canvas.drawLine(cx, cy - radius + 8, cx, cy + radius - 8, mCrosshairPaint);
            canvas.drawLine(cx - radius + 8, cy, cx + radius - 8, cy, mCrosshairPaint);
        } else if (isGyroLeft) {
            canvas.drawArc(mBounds, 120, 120, false, mCrosshairPaint);
        } else if (isGyroRight) {
            canvas.drawArc(mBounds, 300, 120, false, mCrosshairPaint);
        } else if (isGyroCombined) {
            canvas.drawArc(mBounds, 30, 120, false, mCrosshairPaint);
            canvas.drawArc(mBounds, 210, 120, false, mCrosshairPaint);
        }

        canvas.drawCircle(cx, cy, radius, mBorderPaint);

        String label;
        String subLabel = null;

        if (mIsListeningForKey) {
            if (isJoystick) {
                if (mBindingStep == 1) label = "UP";
                else if (mBindingStep == 2) label = "LEFT";
                else if (mBindingStep == 3) label = "DOWN";
                else label = "RIGHT";
            } else if (isGyroCombined) {
                if (mBindingStep == 1) label = "LEFT";
                else label = "RIGHT";
            } else {
                label = "PRESS";
            }
        } else {
            label = mItem.keyName;
            if (isGyroLeft) subLabel = "TILT L";
            else if (isGyroRight) subLabel = "TILT R";
        }

        if (subLabel != null) {
            mTextPaint.setTextSize(22.0f);
            float textY = cy - 4.0f;
            canvas.drawText(label, cx, textY, mTextPaint);
            canvas.drawText(subLabel, cx, cy + 18.0f, mSubtextPaint);
        } else {
            mTextPaint.setTextSize(mIsListeningForKey ? 18.0f : (label.length() > 3 ? 20.0f : 28.0f));
            float textY = cy - ((mTextPaint.descent() + mTextPaint.ascent()) / 2.0f);
            canvas.drawText(label, cx, textY, mTextPaint);
        }

        if (mIsEditing) {
            float badgeR = 12.0f;
            float badgeX = w - badgeR - 2.0f;
            float badgeY = badgeR + 2.0f;
            mDeleteBounds.set(badgeX - badgeR, badgeY - badgeR, badgeX + badgeR, badgeY + badgeR);

            mDeletePaint.setColor(COLOR_DELETE_BG);
            canvas.drawCircle(badgeX, badgeY, badgeR, mDeletePaint);

            mSubtextPaint.setColor(Color.WHITE);
            mSubtextPaint.setTextSize(14.0f);
            float dy = badgeY - ((mSubtextPaint.descent() + mSubtextPaint.ascent()) / 2.0f);
            canvas.drawText("✕", badgeX, dy, mSubtextPaint);
            mSubtextPaint.setColor(COLOR_SUBTEXT);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mIsEditing) return false;

        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (mDeleteBounds.contains(x, y)) {
                    if (mListener != null) {
                        mListener.onNodeDeleted(this);
                    }
                    return true;
                }

                mTouchDownX = event.getRawX();
                mTouchDownY = event.getRawY();
                mInitialParamX = mLayoutParams.x;
                mInitialParamY = mLayoutParams.y;
                mIsDragging = false;
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - mTouchDownX;
                float dy = event.getRawY() - mTouchDownY;
                if (Math.hypot(dx, dy) > 8.0f) {
                    mIsDragging = true;
                    mLayoutParams.x = mInitialParamX + (int) dx;
                    mLayoutParams.y = mInitialParamY + (int) dy;
                    try {
                        mWindowManager.updateViewLayout(this, mLayoutParams);
                    } catch (Exception ignored) {}
                    if (mListener != null) {
                        mListener.onNodeMoved(this);
                    }
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (!mIsDragging) {
                    if (mDeleteBounds.contains(x, y)) {
                        if (mListener != null) {
                            mListener.onNodeDeleted(this);
                        }
                    } else {
                        setListeningForKey(!mIsListeningForKey);
                    }
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mIsListeningForKey) {
            if (keyCode != KeyEvent.KEYCODE_BACK && keyCode != KeyEvent.KEYCODE_HOME) {
                if (SakuraKeyMapItem.TYPE_JOYSTICK_WASD.equals(mItem.type)) {
                    if (mBindingStep == 1) {
                        mItem.upKey = keyCode;
                        mBindingStep = 2;
                        invalidate();
                    } else if (mBindingStep == 2) {
                        mItem.leftKey = keyCode;
                        mBindingStep = 3;
                        invalidate();
                    } else if (mBindingStep == 3) {
                        mItem.downKey = keyCode;
                        mBindingStep = 4;
                        invalidate();
                    } else if (mBindingStep == 4) {
                        mItem.rightKey = keyCode;
                        mItem.keyName = SakuraKeyMapItem.getCleanKeyName(mItem.upKey)
                                + SakuraKeyMapItem.getCleanKeyName(mItem.leftKey)
                                + SakuraKeyMapItem.getCleanKeyName(mItem.downKey)
                                + SakuraKeyMapItem.getCleanKeyName(mItem.rightKey);
                        setListeningForKey(false);
                        if (mListener != null) {
                            mListener.onNodeKeyBound(this, keyCode, mItem.keyName);
                        }
                    }
                } else if (SakuraKeyMapItem.TYPE_GYRO.equals(mItem.type)) {
                    if (mBindingStep == 1) {
                        mItem.leftKey = keyCode;
                        mBindingStep = 2;
                        invalidate();
                    } else if (mBindingStep == 2) {
                        mItem.rightKey = keyCode;
                        mItem.keyName = SakuraKeyMapItem.getCleanKeyName(mItem.leftKey)
                                + "/" + SakuraKeyMapItem.getCleanKeyName(mItem.rightKey);
                        setListeningForKey(false);
                        if (mListener != null) {
                            mListener.onNodeKeyBound(this, keyCode, mItem.keyName);
                        }
                    }
                } else {
                    mItem.keyCode = keyCode;
                    mItem.keyName = SakuraKeyMapItem.getCleanKeyName(keyCode);
                    setListeningForKey(false);
                    if (mListener != null) {
                        mListener.onNodeKeyBound(this, keyCode, mItem.keyName);
                    }
                }
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
