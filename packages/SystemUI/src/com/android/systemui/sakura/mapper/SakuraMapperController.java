package com.android.systemui.sakura.mapper;

import android.app.ActivityManager.RunningTaskInfo;
import android.app.ActivityTaskManager;
import android.app.TaskStackListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.input.IInputManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ServiceManager;
import android.util.DisplayMetrics;
import android.util.Log;

public class SakuraMapperController implements SakuraHUDOverlay.OnSaveListener {
    private static final String TAG = "SakuraMapperController";
    public static final String ACTION_TOGGLE_MAPPER = "com.android.systemui.sakura.ACTION_TOGGLE_MAPPER";

    private final Context mContext;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private SakuraHUDOverlay mOverlay;
    private IInputManager mInputManager;

    private String mForegroundPackage = "";

    private static SakuraMapperController sInstance;

    public static synchronized SakuraMapperController getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new SakuraMapperController(context.getApplicationContext());
        }
        return sInstance;
    }

    public SakuraMapperController(Context context) {
        this.mContext = context;
        init();
    }

    private void init() {
        mHandler.post(() -> {
            try {
                IntentFilter filter = new IntentFilter(ACTION_TOGGLE_MAPPER);
                mContext.registerReceiver(mToggleReceiver, filter, Context.RECEIVER_EXPORTED);
            } catch (Exception e) {
                Log.e(TAG, "Failed to register toggle receiver", e);
            }

            try {
                if (ActivityTaskManager.getService() != null) {
                    ActivityTaskManager.getService().registerTaskStackListener(mTaskStackListener);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to register TaskStackListener", e);
            }
        });
    }

    private synchronized SakuraHUDOverlay getOverlay() {
        if (mOverlay == null) {
            mOverlay = new SakuraHUDOverlay(mContext, this);
        }
        return mOverlay;
    }

    private synchronized IInputManager getInputManager() {
        if (mInputManager == null) {
            try {
                IBinder binder = ServiceManager.getService(Context.INPUT_SERVICE);
                if (binder != null) {
                    mInputManager = IInputManager.Stub.asInterface(binder);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to connect to InputManager", e);
            }
        }
        return mInputManager;
    }

    private final BroadcastReceiver mToggleReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_TOGGLE_MAPPER.equals(intent.getAction())) {
                toggleOverlay();
            }
        }
    };

    private final TaskStackListener mTaskStackListener = new TaskStackListener() {
        @Override
        public void onTaskMovedToFront(RunningTaskInfo taskInfo) {
            if (taskInfo != null && taskInfo.topActivity != null) {
                String pkg = taskInfo.topActivity.getPackageName();
                onForegroundAppChanged(pkg);
            }
        }
    };

    public void onForegroundAppChanged(String packageName) {
        if (packageName == null || packageName.equals(mForegroundPackage)) return;
        mForegroundPackage = packageName;

        mHandler.post(() -> {
            try {
                IInputManager im = getInputManager();
                if (im != null) {
                    String profileJson = im.getSakuraProfile(mForegroundPackage);
                    if (profileJson != null && !profileJson.isEmpty()) {
                        DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
                        im.setSakuraMapping(mForegroundPackage, profileJson, dm.widthPixels, dm.heightPixels);
                        im.setSakuraActive(true);
                        Log.i(TAG, "Auto-loaded Sakura keymap profile for " + mForegroundPackage);
                    } else {
                        im.setSakuraActive(false);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error applying profile on app change", e);
            }
        });
    }

    public void toggleOverlay() {
        mHandler.post(() -> {
            try {
                SakuraHUDOverlay overlay = getOverlay();
                if (overlay.isShowing()) {
                    overlay.hide();
                } else {
                    String existingJson = null;
                    try {
                        IInputManager im = getInputManager();
                        if (im != null) {
                            existingJson = im.getSakuraProfile(mForegroundPackage);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to get profile", e);
                    }
                    overlay.show(mForegroundPackage, existingJson);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to toggle overlay", e);
            }
        });
    }

    @Override
    public void onProfileSaved(String packageName, String profileJson) {
        try {
            IInputManager im = getInputManager();
            if (im != null) {
                im.saveSakuraProfile(packageName, profileJson);
                DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
                im.setSakuraMapping(packageName, profileJson, dm.widthPixels, dm.heightPixels);
                im.setSakuraActive(true);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to save profile via IInputManager", e);
        }
    }

    @Override
    public void onOverlayVisibilityChanged(boolean showing) {
        try {
            IInputManager im = getInputManager();
            if (im != null) {
                im.setSakuraOverlayShowing(showing);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to update overlay visibility in InputManager", e);
        }
    }
}
