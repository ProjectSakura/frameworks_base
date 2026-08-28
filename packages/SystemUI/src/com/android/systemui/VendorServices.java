package com.android.systemui;

import android.app.ActivityThread;
import android.content.Context;
import com.android.systemui.sakura.mapper.SakuraMapperController;

public class VendorServices implements CoreStartable {
    public VendorServices() {
    }

    @Override
    public void start() {
        Context context = ActivityThread.currentApplication();
        if (context != null) {
            SakuraMapperController.getInstance(context);
        }
    }
}
