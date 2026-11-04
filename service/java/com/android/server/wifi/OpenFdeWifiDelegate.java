package com.android.server.wifi;

import android.net.DhcpInfo;
import android.net.InetAddresses;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiSsid;
import android.openfde.IWifi;
import android.openfde.Wifi;
import android.openfde.Wifi.CallBackEvents;
import android.text.TextUtils;
import android.util.Log;

import com.android.net.module.util.Inet4AddressUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class OpenFdeWifiDelegate {
    private static final String TAG = "OpenFdeWifiDelegate";

    private volatile Wifi mWifi;

    private Wifi wifi() {
        Wifi w = mWifi;
        if (w == null) {
            synchronized (this) {
                w = mWifi;
                if (w == null) {
                    try {
                        w = Wifi.getInstance(null);
                    } catch (Throwable t) {
                        Log.w(TAG, "Wifi.getInstance failed", t);
                        return null;
                    }
                    mWifi = w;
                }
            }
        }
        try {
            return Wifi.getService() != null ? w : null;
        } catch (Throwable t) {
            Log.w(TAG, "isAvailable check failed", t);
            return null;
        }
    }

    public boolean isAvailable() {
        return wifi() != null;
    }

    public interface EventSink {
        void onEvent(int what, String data);
    }

    public static final int EVENT_WIFI_STATE_CHANGED = CallBackEvents.WIFI_STATE_CHANGED.ordinal();
    public static final int EVENT_SCAN_RESULTS_AVAILABLE = CallBackEvents.SCAN_RESULTS_AVAILABLE.ordinal();
    public static final int EVENT_CONNECTION_CHANGED = CallBackEvents.CONNECTION_CHANGED.ordinal();
    public static final int EVENT_RSSI_CHANGED = CallBackEvents.RSSI_CHANGED.ordinal();

    private volatile EventSink mEventSink;
    private boolean mEventSinkRegistered = false;
    private volatile IWifi mEventSinkProxy;

    public boolean isEventSinkRegistered() {
        return mEventSinkRegistered;
    }

    public void ensureEventSink(EventSink sink) {
        if (mEventSinkRegistered && sameHostProxy()) {
            return;
        }
        mEventSink = sink;
        refreshEventSinkLocked();
    }

    public boolean init() {
        Wifi w = wifi();
        if (w == null) {
            return false;
        }
        try {
            return w.init();
        } catch (Throwable t) {
            Log.w(TAG, "init failed", t);
            return false;
        }
    }

    private boolean sameHostProxy() {
        try {
            return mEventSinkProxy == Wifi.getService();
        } catch (Throwable t) {
            return true;
        }
    }

    private void refreshEventSinkLocked() {
        Wifi w = wifi();
        if (w == null) {
            return;
        }
        try {
            w.unregisterCallback();
        } catch (Throwable t) {
            Log.w(TAG, "unregisterCallback failed", t);
        }
        try {
            if (w.registerCallback((what, data) -> {
                EventSink s = mEventSink;
                if (s != null) {
                    s.onEvent(what, data);
                }
            })) {
                mEventSinkRegistered = true;
                mEventSinkProxy = Wifi.getService();
            }
        } catch (Throwable t) {
            Log.w(TAG, "registerEventSink failed", t);
        }
    }

    public int getWifiEnabledState() {
        Wifi w = wifi();
        if (w == null) {
            return WifiManager.WIFI_STATE_UNKNOWN;
        }
        try {
            return w.isEnabled() ? WifiManager.WIFI_STATE_ENABLED : WifiManager.WIFI_STATE_DISABLED;
        } catch (Throwable t) {
            Log.w(TAG, "getWifiEnabledState failed", t);
            return WifiManager.WIFI_STATE_UNKNOWN;
        }
    }

    public WifiInfo getWifiInfo() {
        Wifi w = wifi();
        if (w == null) {
            return null;
        }
        String json;
        try {
            json = w.getConnectionInfo();
        } catch (Throwable t) {
            Log.w(TAG, "getConnectionInfo failed", t);
            return null;
        }
        if (json == null || !json.trim().startsWith("{")) {
            return null;
        }
        try {
            JSONObject o = new JSONObject(json);
            WifiInfo info = new WifiInfo();
            info.setNetworkId(-1);
            boolean connected = o.optBoolean("connected");
            info.setSupplicantState(connected ? SupplicantState.COMPLETED
                    : SupplicantState.DISCONNECTED);
            String ssid = o.optString("ssid", "");
            if (!TextUtils.isEmpty(ssid) && !"(hidden/empty)".equals(ssid)) {
                info.setSSID(WifiSsid.fromUtf8Text(ssid));
            }
            String bssid = o.optString("bssid", "");
            if (!TextUtils.isEmpty(bssid)) {
                info.setBSSID(bssid.toUpperCase());
            }
            int freq = o.optInt("freq", 0);
            if (freq > 0) {
                info.setFrequency(freq);
            }
            int rssi = o.optInt("rssiDbm",
                    (int) Math.round(o.optInt("strength", 0) * 0.7 - 100));
            if (rssi > -253) {
                info.setRssi(rssi);
            }
            double bitrateMbps = o.optDouble("bitrateMbps", -1);
            if (bitrateMbps >= 0) {
                info.setLinkSpeed((int) Math.round(bitrateMbps));
            }
            return info;
        } catch (JSONException e) {
            Log.w(TAG, "parse getConnectionInfo JSON failed", e);
            return null;
        }
    }

    public List<ScanResult> getScanResults() {
        Wifi w = wifi();
        if (w == null) {
            return Collections.emptyList();
        }
        String json;
        try {
            json = w.getScanResults();
        } catch (Throwable t) {
            Log.w(TAG, "getScanResults failed", t);
            return Collections.emptyList();
        }
        if (json == null || !json.trim().startsWith("{")) {
            return Collections.emptyList();
        }
        List<ScanResult> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONObject(json).optJSONArray("results");
            if (arr == null) {
                return out;
            }
            for (int i = 0; i < arr.length(); i++) {
                JSONObject a = arr.optJSONObject(i);
                if (a == null) {
                    continue;
                }
                ScanResult r = new ScanResult();
                String ssid = a.optString("ssid", "");
                String bssid = a.optString("bssid", "");
                if (TextUtils.isEmpty(ssid) && TextUtils.isEmpty(bssid)) {
                    continue;
                }
                r.SSID = ssid;
                r.BSSID = bssid.toUpperCase();
                r.frequency = a.optInt("freq", 0);
                r.level = a.optInt("rssiDbm",
                        (int) Math.round(a.optInt("strength", 0) * 0.7 - 100));
                String sec = a.optString("security", "OPEN");
                r.capabilities = securityToCapabilities(sec);
                out.add(r);
            }
        } catch (JSONException e) {
            Log.w(TAG, "parse getScanResults JSON failed", e);
        }
        return out;
    }

    public String getFactoryMacAddress() {
        Wifi w = wifi();
        if (w == null) {
            return null;
        }
        String json;
        try {
            json = w.getFactoryMacAddresses();
        } catch (Throwable t) {
            Log.w(TAG, "getFactoryMacAddresses failed", t);
            return null;
        }
        if (json == null) {
            return null;
        }
        try {
            JSONObject o = new JSONObject(json);
            Iterator<String> it = o.keys();
            if (!it.hasNext()) {
                return null;
            }
            return o.optString(it.next(), null);
        } catch (JSONException e) {
            Log.w(TAG, "parse getFactoryMacAddresses JSON failed", e);
            return null;
        }
    }

    public DhcpInfo getDhcpInfo() {
        Wifi w = wifi();
        if (w == null) {
            return null;
        }
        String json;
        try {
            json = w.getDhcpInfo();
        } catch (Throwable t) {
            Log.w(TAG, "getDhcpInfo failed", t);
            return null;
        }
        if (json == null) {
            return null;
        }
        try {
            JSONObject o = new JSONObject(json);
            if (o.length() == 0) {
                return null;
            }
            DhcpInfo info = new DhcpInfo();
            info.ipAddress = ipToInt(o.optString("ip", ""));
            info.gateway = ipToInt(o.optString("gateway", ""));
            info.netmask = ipToInt(o.optString("netmask", ""));
            info.dns1 = ipToInt(o.optString("dns1", ""));
            info.dns2 = ipToInt(o.optString("dns2", ""));
            info.serverAddress = ipToInt(o.optString("server", ""));
            info.leaseDuration = o.optInt("lease", 0);
            return info;
        } catch (JSONException e) {
            Log.w(TAG, "parse getDhcpInfo JSON failed", e);
            return null;
        }
    }

    public boolean setEnabled(boolean on) {
        Wifi w = wifi();
        if (w == null) {
            return false;
        }
        try {
            return w.setEnabled(on);
        } catch (Throwable t) {
            Log.w(TAG, "setEnabled failed", t);
            return false;
        }
    }

    public boolean startScan() {
        Wifi w = wifi();
        if (w == null) {
            return false;
        }
        try {
            return w.startScan();
        } catch (Throwable t) {
            Log.w(TAG, "startScan failed", t);
            return false;
        }
    }

    public boolean disconnect() {
        Wifi w = wifi();
        if (w == null) {
            return false;
        }
        try {
            return w.disconnect();
        } catch (Throwable t) {
            Log.w(TAG, "disconnect failed", t);
            return false;
        }
    }

    private static int ipToInt(String ip) {
        if (TextUtils.isEmpty(ip)) {
            return 0;
        }
        try {
            Inet4Address a = (Inet4Address) InetAddresses.parseNumericAddress(ip);
            return Inet4AddressUtils.inet4AddressToIntHTL(a);
        } catch (Exception e) {
            Log.w(TAG, "parse ip failed: " + ip, e);
            return 0;
        }
    }

    private static String securityToCapabilities(String sec) {
        if (TextUtils.isEmpty(sec) || "OPEN".equalsIgnoreCase(sec)) {
            return "";
        }
        switch (sec) {
            case "WPA2-PSK":
                return "WPA2-PSK";
            case "WPA3-SAE":
                return "WPA3-SAE";
            case "WPA3-WPA2":
                return "[WPA2-PSK+WPA3-SAE]";
            case "OWE":
                return "[OWE]";
            case "WPA-Enterprise":
                return "[WPA-EAP]";
            default:
                return sec;
        }
    }
}
