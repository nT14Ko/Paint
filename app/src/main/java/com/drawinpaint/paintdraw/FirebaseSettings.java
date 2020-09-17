package com.drawinpaint.paintdraw;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import static com.facebook.FacebookSdk.getApplicationContext;

public class FirebaseSettings {
    public void storeUpload(Context context, String deep){
        SharedPreferences sharedPreferences =
                getApplicationContext().getSharedPreferences("DATA", Context.MODE_PRIVATE);
        String installID = sharedPreferences.getString("installID", null);
        String check = sharedPreferences.getString("check", "true");
        assert check != null;

        //setParams2Firebase
        Map<String, Object> data = new HashMap<>();
        final String androidId = Settings.Secure.getString(getApplicationContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);
        Calendar cal = Calendar.getInstance();
        TimeZone tz = cal.getTimeZone();
        String timeZone = tz.getDisplayName();
        String ip = getDeviceIpAddress(context);
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String geo = tm.getNetworkCountryIso();
        String fullModelName = Build.MANUFACTURER
                + " " + Build.MODEL + " " + Build.VERSION.RELEASE
                + " " + Build.VERSION_CODES.class.getFields()
                [Build.VERSION.SDK_INT].getName();
        boolean emulator = Build.FINGERPRINT.contains("generic");
        String bundleId = context.getPackageName();
        Date currentTime = Calendar.getInstance().getTime();
        if (!deep.equals(""))
            data.put("deepLink", deep);
        data.put("version", 1);
        data.put("androidId", androidId);
        data.put("ip", ip);
        data.put("timeZone", timeZone);
        data.put("geo", geo);
        data.put("fullModelName", fullModelName);
        data.put("emulator", emulator);
        data.put("bundleId", bundleId);
        data.put("currentTime", currentTime);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        assert installID != null;
        db.collection("FirebaseTest").document(installID).set(data, SetOptions.merge());
        sharedPreferences.edit().putString("check", "false").apply();
    }

    @NonNull
    private String getDeviceIpAddress(Context context) {
        String actualConnectedToNetwork = null;
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connManager != null) {
            NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            assert mWifi != null;
            if (mWifi.isConnected()) {
                actualConnectedToNetwork = getWifiIp();
            }
        }
        if (TextUtils.isEmpty(actualConnectedToNetwork)) {
            actualConnectedToNetwork = getNetworkInterfaceIpAddress();
        }
        if (TextUtils.isEmpty(actualConnectedToNetwork)) {
            actualConnectedToNetwork = "127.0.0.1";
        }
        assert actualConnectedToNetwork != null;
        return actualConnectedToNetwork;
    }

    @Nullable
    private String getWifiIp() {
        final WifiManager mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (mWifiManager != null && mWifiManager.isWifiEnabled()) {
            int ip = mWifiManager.getConnectionInfo().getIpAddress();
            return (ip & 0xFF) + "." + ((ip >> 8) & 0xFF) + "." + ((ip >> 16) & 0xFF) + "."
                    + ((ip >> 24) & 0xFF);
        }
        return null;
    }


    @Nullable
    public String getNetworkInterfaceIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface networkInterface = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = networkInterface.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        String host = inetAddress.getHostAddress();
                        if (!TextUtils.isEmpty(host)) {
                            return host;
                        }
                    }
                }

            }
        } catch (Exception ex) {
            Log.e("IP Address", "getLocalIpAddress", ex);
        }
        return null;
    }
}
