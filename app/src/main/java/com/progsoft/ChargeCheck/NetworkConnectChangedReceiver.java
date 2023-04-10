package com.progsoft.ChargeCheck;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Parcelable;

/**
 * 网络改变监控广播
 * 监听网络的改变状态,只有在用户操作网络连接开关(wifi,mobile)的时候接受广播,
 */
public class NetworkConnectChangedReceiver extends BroadcastReceiver {
    public static long lastime = 0;
    public static ConnectivityManager manager;
    public static WifiManager wm;
    @Override
    public void onReceive(Context context, Intent intent) {
        // 这个监听wifi的打开与关闭，与wifi的连接无关
        Utils.FileWrite(Utils.LOG_FILENAME, "intent_action:" + intent.getAction(), true);

        if (lastime == 0) {
            manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            lastime = System.currentTimeMillis();
        }

        if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {
            int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
            Utils.FileWrite(Utils.LOG_FILENAME, "wifiState " + wifiState, true);
            switch (wifiState) {
                case WifiManager.WIFI_STATE_DISABLED:
                    break;
                case WifiManager.WIFI_STATE_DISABLING:
                    break;
                case WifiManager.WIFI_STATE_ENABLING:
                    break;
                case WifiManager.WIFI_STATE_ENABLED:
                    break;
                case WifiManager.WIFI_STATE_UNKNOWN:
                    break;
                default:
                    break;
            }
        }

        // 这个监听wifi的连接状态即是否连上了一个有效无线路由，当上边广播的状态是WifiManager
        // .WIFI_STATE_DISABLING 和 WIFI_STATE_DISABLED的时候，根本不会接到这个广播。
        // 在上边广播接到广播是WifiManager.WIFI_STATE_ENABLED状态的同时也会接到这个广播，
        // 当然刚打开wifi肯定还没有连接到有效的无线
        if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
            Parcelable parcelableExtra = intent
                    .getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if (null != parcelableExtra) {
                NetworkInfo networkInfo = (NetworkInfo) parcelableExtra;
                NetworkInfo.State state = networkInfo.getState();
                boolean isConnected = state == NetworkInfo.State.CONNECTED;// 当然，这边可以更精确的确定状态
                Utils.FileWrite(Utils.LOG_FILENAME, "isConnected  " + isConnected, true);
                if (isConnected) {
                    long now = System.currentTimeMillis();
                    Utils.FileWrite(Utils.LOG_FILENAME, "NETWORK_STATE_CHANGED_ACTION " + (now - lastime), true);
                    if (now - lastime > 1000L * 12 * 60) { //超过12分钟
                        Utils.FileWrite(Utils.LOG_FILENAME, "NETWORK_STATE_CHANGED_ACTION Connected", true);
                        lastime = now;
                        MainActivity.getChargeInfo();
                    }
                }
            }
        }
        NetworkInfo activeNetwork = manager.getActiveNetworkInfo();
        if (activeNetwork != null) {
            Utils.FileWrite(Utils.LOG_FILENAME, "getTypeName()=" + activeNetwork.getTypeName() +
                            " getSubtypeName()=" + activeNetwork.getSubtypeName() +
                            " getState()=" + activeNetwork.getState() +
                            " getDetailedState()=" + activeNetwork.getDetailedState().name() +
                            " getExtraInfo()=" + activeNetwork.getExtraInfo() +
                            " getType()=" + activeNetwork.getType()
                    , true);
            WifiInfo wi = wm.getConnectionInfo();
            Utils.FileWrite(Utils.LOG_FILENAME, "Wifi Info is " + wi.toString(), true);
        } else {
            Utils.FileWrite(Utils.LOG_FILENAME, "activeNetwork is null", true);
        }

        // 这个监听网络连接的设置，包括wifi和移动数据的打开和关闭。.
        // 最好用的还是这个监听。wifi如果打开，关闭，以及连接上可用的连接都会接到监听。见log
        // 这个广播的最大弊端是比上边两个广播的反应要慢，如果只是要监听wifi，还是用上边两个配合比较合适
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            if (activeNetwork != null) {
                if (activeNetwork.isConnected()) {
                    if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                        // connected to wifi
                        Utils.FileWrite(Utils.LOG_FILENAME, "当前WiFi连接可用 ", true);
                    } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                        // connected to the mobile provider's data plan
                        Utils.FileWrite(Utils.LOG_FILENAME, "当前移动网络连接可用 ", true);
                    }
                    long now = System.currentTimeMillis();
                    Utils.FileWrite(Utils.LOG_FILENAME, "CONNECTIVITY_ACTION " + (now - lastime), true);
                    if (now - lastime > 1000L * 12 * 60) { //超过12分钟
                        Utils.FileWrite(Utils.LOG_FILENAME, "CONNECTIVITY_ACTION " + activeNetwork.getType(), true);
                        lastime = now;
                        MainActivity.getChargeInfo();
                    }
                } else {
                    Utils.FileWrite(Utils.LOG_FILENAME, "activeNetwork not Connected 当前没有网络连接，请确保你已经打开网络 ", true);
                }
            }
        }
    }


}