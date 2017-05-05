package com.hh.videoviewplayer.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;


/**
 * 网络可用的判断和配置
 *
 * @author oywf
 */
public class NetworkUtils {

    /**
     * 无网络
     */
    public final static int NONE = 0;
    /**
     * wifi网络
     */
    public final static int WIFI = 1;
    /**
     * 移动网络
     */
    public final static int MOBILE = 2;

    /**
     * 获取当前网络状态(是否可用)
     *
     * @param context
     * @return
     */
    public static boolean isNetworkAvailable(Context context) {
        boolean netWorkStatus = false;
        if (null != context) {
            ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connManager.getActiveNetworkInfo() != null) {
                netWorkStatus = connManager.getActiveNetworkInfo().isAvailable();
            }
        }
        return netWorkStatus;
    }

    /**
     * 获取3G或者WIFI网络
     *
     * @param context
     * @return
     */
    public static int getNetworkState(Context context) {
        if (null != context) {
            ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            State state;
            NetworkInfo networkInfo;
            if (null != connManager) {
                //Wifi网络判断
                networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                if (null != networkInfo) {
                    state = networkInfo.getState();
                    if (state == State.CONNECTED || state == State.CONNECTING) {
                        return WIFI;
                    }
                }

                //3G网络判断
                networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
                if (null != networkInfo) {
                    state = networkInfo.getState();
                    if (state == State.CONNECTED || state == State.CONNECTING) {
                        return MOBILE;
                    }
                }
            }

        }
        return NONE;
    }

    /**
     * 获取当前连接的wifi名字
     *
     * @param context
     * @return
     */
    public static String getWiFiName(Context context){
        WifiManager wifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifiMgr.getConnectionInfo();
        String wifiId = info != null ? info.getSSID() : null;
        return wifiId;
    }

}
