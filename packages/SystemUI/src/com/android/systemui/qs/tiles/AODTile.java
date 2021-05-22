/*
 * Copyright (C) 2018 The OmniROM Project
 *               2020-2021 The LineageOS Project
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

package com.android.systemui.qs.tiles;

import android.content.Intent;
import android.provider.Settings;
import android.service.quicksettings.Tile;

import com.android.systemui.R;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.SecureSetting;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.statusbar.policy.BatteryController;

import org.lineageos.internal.logging.LineageMetricsLogger;

import javax.inject.Inject;

public class AODTile extends QSTileImpl<BooleanState> implements
        BatteryController.BatteryStateChangeCallback {

    private final Icon mIcon = ResourceIcon.get(R.drawable.ic_qs_aod);
    private final BatteryController mBatteryController;

    private final SecureSetting mSetting;

    @Inject
    public AODTile(QSHost host, BatteryController batteryController) {
        super(host);

        mSetting = new SecureSetting(mContext, mHandler, Settings.Secure.DOZE_ALWAYS_ON) {
            @Override
            protected void handleValueChanged(int value, boolean observedChange) {
                handleRefreshState(value);
            }
        };

        mBatteryController = batteryController;
        batteryController.observe(getLifecycle(), this);
    }

    @Override
    public void onPowerSaveChanged(boolean isPowerSave) {
        refreshState();
    }

    @Override
    protected void handleDestroy() {
        super.handleDestroy();
        mSetting.setListening(false);
    }

    @Override
    public boolean isAvailable() {
        return mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_dozeAlwaysOnDisplayAvailable);
    }

    @Override
    public BooleanState newTileState() {
        BooleanState state = new BooleanState();
        state.handlesLongClick = false;
        return state;
    }

    @Override
    public void handleSetListening(boolean listening) {
        super.handleSetListening(listening);
        mSetting.setListening(listening);
    }

    @Override
    protected void handleUserSwitch(int newUserId) {
        mSetting.setUserId(newUserId);
        handleRefreshState(mSetting.getValue());
    }

    @Override
    public void handleClick() {
        mSetting.setValue(mState.value ? 0 : 1);
    }

    @Override
    public Intent getLongClickIntent() {
        return null;
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_always_on_display_label);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        final int value = arg instanceof Integer ? (Integer) arg : mSetting.getValue();
        final boolean enable = value != 0;
        state.icon = mIcon;
        state.value = enable;
        state.label = mContext.getString(R.string.quick_settings_always_on_display_label);
        state.secondaryLabel = null;
        if (mBatteryController.isAodPowerSave()) {
            state.state = Tile.STATE_UNAVAILABLE;
            state.secondaryLabel = mContext.getString(R.string.quick_settings_dark_mode_secondary_label_battery_saver);
        } else {
            state.state = enable ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE;
        }
    }

    @Override
    public int getMetricsCategory() {
        return LineageMetricsLogger.TILE_AOD;
    }
}
