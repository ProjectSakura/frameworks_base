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
    private static final int COLOR_JOYSTICK_CROSS = Color.parseColor("#66C77DFF");

    private final WindowManager mWindowManager;
    private final WindowManager.LayoutParams mLayoutParams;
    private final SakuraKeyMapItem mItem;

    private final Paint mBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mCrosshairPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mBounds = new RectF();

    private boolean mIsListeningForKey = false;
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
        mBorderPaint.setStrokeWidth(5.0f);
        mBorderPaint.setShadowLayer(10.0f, 0, 0, Color.parseColor("#7B2CBF"));

        mTextPaint.setColor(COLOR_TEXT);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setFakeBoldText(true);
        mTextPaint.setTextSize(32.0f);

        mCrosshairPaint.setColor(COLOR_JOYSTICK_CROSS);
        mCrosshairPaint.setStrokeWidth(2.5f);
        mCrosshairPaint.setStyle(Paint.Style.STROKE);
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
    }

    public void setListeningForKey(boolean listening) {
        mIsListeningForKey = listening;
        mBorderPaint.setColor(mIsListeningForKey ? COLOR_BORDER_LISTENING : COLOR_BORDER_DEFAULT);
        mBorderPaint.setStrokeWidth(mIsListeningForKey ? 8.0f : 5.0f);

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
        float radius = Math.min(w, h) / 2.0f - 6.0f;

        mBounds.set(cx - radius, cy - radius, cx + radius, cy + radius);
        canvas.drawCircle(cx, cy, radius, mBgPaint);

        if (SakuraKeyMapItem.TYPE_JOYSTICK_WASD.equals(mItem.type)) {
            canvas.drawLine(cx, cy - radius + 8, cx, cy + radius - 8, mCrosshairPaint);
            canvas.drawLine(cx - radius + 8, cy, cx + radius - 8, cy, mCrosshairPaint);
        }

        canvas.drawCircle(cx, cy, radius, mBorderPaint);

        String label = mIsListeningForKey ? "PRESS" : mItem.keyName;
        mTextPaint.setTextSize(mIsListeningForKey ? 20.0f : (label.length() > 3 ? 24.0f : 32.0f));
        float textY = cy - ((mTextPaint.descent() + mTextPaint.ascent()) / 2.0f);
        canvas.drawText(label, cx, textY, mTextPaint);

        canvas.drawCircle(cx, cy, 3.0f, mBorderPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
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
                    setListeningForKey(!mIsListeningForKey);
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mIsListeningForKey) {
            if (keyCode != KeyEvent.KEYCODE_BACK && keyCode != KeyEvent.KEYCODE_HOME) {
                mItem.keyCode = keyCode;
                mItem.keyName = KeyEvent.keyCodeToString(keyCode).replace("KEYCODE_", "");
                setListeningForKey(false);
                if (mListener != null) {
                    mListener.onNodeKeyBound(this, keyCode, mItem.keyName);
                }
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
