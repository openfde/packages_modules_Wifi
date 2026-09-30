/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.server.wifi.p2p;

import android.os.IBinder;
import android.util.Log;

import com.android.server.wifi.WifiInjector;

/**
 * Implementation of Supplicant P2P Iface HAL using the vendor AIDL service.
 */
public class SupplicantP2pIfaceHalAidlVendorImpl extends SupplicantP2pIfaceHalAidlBase {
    private static final String TAG = "SupplicantP2pIfaceHalAidlVendorImpl";

    public SupplicantP2pIfaceHalAidlVendorImpl(WifiP2pMonitor monitor, WifiInjector wifiInjector) {
        super(monitor, wifiInjector, false);
    }

    /**
     * Retrieve the ISupplicant service and link to service death.
     * @return true if successful, false otherwise
     */
    @Override
    public boolean initialize() {
        synchronized (mLock) {
            if (mInitializationStarted) {
                Log.i(TAG, "Service is already initialized.");
                return true;
            }
            mInitializationStarted = true;
            mP2p = null;
            return true;
        }
    }

    @Override
    protected IBinder getCurrentServiceBinderMockable() {
        return null;
    }

    /**
     * Signals whether initialization started successfully.
     */
    @Override
    public boolean isInitializationStarted() {
        synchronized (mLock) {
            return mInitializationStarted;
        }
    }

    /**
     * Signals whether initialization completed successfully.
     */
    @Override
    public boolean isInitializationComplete() {
        synchronized (mLock) {
            return mInitializationStarted;
        }
    }

    @Override
    public boolean setLogLevel(boolean turnOnVerbose, boolean globalShowKeys) {
        return true;
    }

    @Override
    protected boolean setCurrentUserIdentity(int userId) {
        return true;
    }

    @Override
    public void terminate() {
    }
}
