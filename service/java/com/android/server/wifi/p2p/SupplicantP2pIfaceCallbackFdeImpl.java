/*
 * Copyright (C) 2026 The OpenFDE Project
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

import android.hardware.wifi.supplicant.P2pDeviceFoundEventParams;
import android.hardware.wifi.supplicant.P2pGoNegotiationReqEventParams;
import android.hardware.wifi.supplicant.P2pGroupStartedEventParams;
import android.hardware.wifi.supplicant.P2pInvitationEventParams;
import android.hardware.wifi.supplicant.P2pPeerClientDisconnectedEventParams;
import android.hardware.wifi.supplicant.P2pPeerClientJoinedEventParams;
import android.hardware.wifi.supplicant.P2pProvisionDiscoveryCompletedEventParams;
import android.hardware.wifi.supplicant.P2pUsdBasedServiceDiscoveryResultParams;
import android.openfde.P2p;

/**
 * P2P event callback registered to the openfde p2p binder service ("openfdep2p") instead of
 * the vendor supplicant HAL. The fde interface widens byte/char to int and flattens the
 * *WithParams parcelables; each such event is adapted here and forwarded to the parent
 * implementation, which translates it into {@link WifiP2pMonitor} broadcasts. Events whose
 * signatures already match the parent implementation are inherited as-is.
 */
public class SupplicantP2pIfaceCallbackFdeImpl extends SupplicantP2pIfaceCallbackAidlImpl
        implements P2p.EventListener {

    public SupplicantP2pIfaceCallbackFdeImpl(
            String iface, WifiP2pMonitor monitor, int serviceVersion) {
        super(iface, monitor, serviceVersion);
    }

    @Override
    public void onDeviceFound(byte[] srcAddress, byte[] p2pDeviceAddress, byte[] primaryDeviceType,
            String deviceName, int configMethods, int deviceCapabilities, int groupCapabilities,
            byte[] wfdDeviceInfo) {
        super.onDeviceFound(srcAddress, p2pDeviceAddress, primaryDeviceType, deviceName,
                configMethods, (byte) deviceCapabilities, groupCapabilities, wfdDeviceInfo);
    }

    @Override
    public void onProvisionDiscoveryCompleted(byte[] p2pDeviceAddress, boolean isRequest,
            int status, int configMethods, String generatedPin) {
        super.onProvisionDiscoveryCompleted(p2pDeviceAddress, isRequest, (byte) status,
                configMethods, generatedPin);
    }

    @Override
    public void onR2DeviceFound(byte[] srcAddress, byte[] p2pDeviceAddress,
            byte[] primaryDeviceType, String deviceName, int configMethods,
            int deviceCapabilities, int groupCapabilities, byte[] wfdDeviceInfo,
            byte[] wfdR2DeviceInfo) {
        super.onR2DeviceFound(srcAddress, p2pDeviceAddress, primaryDeviceType, deviceName,
                configMethods, (byte) deviceCapabilities, groupCapabilities, wfdDeviceInfo,
                wfdR2DeviceInfo);
    }

    @Override
    public void onServiceDiscoveryResponse(byte[] srcAddress, int updateIndicator, byte[] tlvs) {
        super.onServiceDiscoveryResponse(srcAddress, (char) updateIndicator, tlvs);
    }

    @Override
    public void onDeviceFoundWithVendorElements(byte[] srcAddress, byte[] p2pDeviceAddress,
            byte[] primaryDeviceType, String deviceName, int configMethods,
            int deviceCapabilities, int groupCapabilities, byte[] wfdDeviceInfo,
            byte[] wfdR2DeviceInfo, byte[] vendorElemBytes) {
        super.onDeviceFoundWithVendorElements(srcAddress, p2pDeviceAddress, primaryDeviceType,
                deviceName, configMethods, (byte) deviceCapabilities, groupCapabilities,
                wfdDeviceInfo, wfdR2DeviceInfo, vendorElemBytes);
    }

    @Override
    public void onGroupStartedWithParams(String groupInterfaceName, boolean isGroupOwner,
            byte[] ssid, int frequencyMHz, byte[] psk, String passphrase, byte[] goDeviceAddress,
            boolean isPersistent) {
        P2pGroupStartedEventParams params = new P2pGroupStartedEventParams();
        params.groupInterfaceName = groupInterfaceName;
        params.isGroupOwner = isGroupOwner;
        params.ssid = ssid;
        params.frequencyMHz = frequencyMHz;
        params.psk = psk;
        params.passphrase = passphrase;
        params.goDeviceAddress = goDeviceAddress;
        params.isPersistent = isPersistent;
        super.onGroupStartedWithParams(params);
    }

    @Override
    public void onPeerClientJoined(byte[] srcAddress, byte[] p2pDeviceAddress,
            boolean isVpSupported) {
        P2pPeerClientJoinedEventParams params = new P2pPeerClientJoinedEventParams();
        params.clientInterfaceAddress = srcAddress;
        params.clientDeviceAddress = p2pDeviceAddress;
        super.onPeerClientJoined(params);
    }

    @Override
    public void onPeerClientDisconnected(byte[] srcAddress, byte[] p2pDeviceAddress) {
        P2pPeerClientDisconnectedEventParams params = new P2pPeerClientDisconnectedEventParams();
        params.clientInterfaceAddress = srcAddress;
        params.clientDeviceAddress = p2pDeviceAddress;
        super.onPeerClientDisconnected(params);
    }

    @Override
    public void onProvisionDiscoveryCompletedEvent(byte[] p2pDeviceAddress, int status,
            int configMethods, String generatedPin) {
        P2pProvisionDiscoveryCompletedEventParams params =
                new P2pProvisionDiscoveryCompletedEventParams();
        params.p2pDeviceAddress = p2pDeviceAddress;
        // The fde interface does not carry isRequest; treat as a request so peer-initiated
        // flows (PBC-REQ, ENTER-PIN, SHOW-PIN) map to the expected framework events.
        params.isRequest = true;
        params.status = (byte) status;
        params.configMethods = configMethods;
        params.generatedPin = generatedPin;
        super.onProvisionDiscoveryCompletedEvent(params);
    }

    @Override
    public void onDeviceFoundWithParams(byte[] srcAddress, byte[] p2pDeviceAddress,
            byte[] primaryDeviceType, String deviceName, int configMethods,
            int deviceCapabilities, int groupCapabilities, byte[] wfdDeviceInfo,
            byte[] wfdR2DeviceInfo, byte[] vendorElem) {
        P2pDeviceFoundEventParams params = new P2pDeviceFoundEventParams();
        params.srcAddress = srcAddress;
        params.p2pDeviceAddress = p2pDeviceAddress;
        params.primaryDeviceType = primaryDeviceType;
        params.deviceName = deviceName;
        params.configMethods = configMethods;
        params.deviceCapabilities = (byte) deviceCapabilities;
        params.groupCapabilities = groupCapabilities;
        params.wfdDeviceInfo = wfdDeviceInfo;
        params.wfdR2DeviceInfo = wfdR2DeviceInfo;
        params.vendorElemBytes = vendorElem;
        super.onDeviceFoundWithParams(params);
    }

    @Override
    public void onGoNegotiationRequestWithParams(byte[] srcAddress, int passwordId, int goIntent) {
        P2pGoNegotiationReqEventParams params = new P2pGoNegotiationReqEventParams();
        params.srcAddress = srcAddress;
        params.passwordId = passwordId;
        super.onGoNegotiationRequestWithParams(params);
    }

    @Override
    public void onInvitationReceivedWithParams(byte[] srcAddress, byte[] goDeviceAddress,
            byte[] bssid, int persistentNetworkId, int operatingFrequencyMHz) {
        P2pInvitationEventParams params = new P2pInvitationEventParams();
        params.srcAddress = srcAddress;
        params.goDeviceAddress = goDeviceAddress;
        params.bssid = bssid;
        params.persistentNetworkId = persistentNetworkId;
        params.operatingFrequencyMHz = operatingFrequencyMHz;
        super.onInvitationReceivedWithParams(params);
    }

    @Override
    public void onUsdBasedServiceDiscoveryResult(int sessionId, byte[] srcAddress,
            int updateIndicator, byte[] tlvs) {
        P2pUsdBasedServiceDiscoveryResultParams params =
                new P2pUsdBasedServiceDiscoveryResultParams();
        params.sessionId = sessionId;
        params.peerMacAddress = srcAddress;
        params.serviceSpecificInfo = tlvs;
        super.onUsdBasedServiceDiscoveryResult(params);
    }
}
