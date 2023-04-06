package com.progsoft.ChargeCheck;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.util.Date;

@RequiresApi(api = Build.VERSION_CODES.O)
public class MyService extends Service {
    int mNotificationId = 1;
    int mGPSInfoId = 2;
    String channelId = "my_chn_01";

    NotificationCompat.Builder mBuilder;
    NotificationManager mNotificationManager;

    private static final String TAG = "PROG_Service";
    private LocationManager mLocationManager = null;
    private static final int LOCATION_INTERVAL = 10000;
    private static final float LOCATION_DISTANCE = 0;

    private static final int RUNNABLE_INTERVAL = 5000;
    private static final int DEFAULT_DELAY_TIME = 5;
    private long lastTime = 0;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Utils.FileWrite(Utils.LOG_FILENAME, "MyService onStartCommand", true);
        return super.onStartCommand(intent, flags, startId);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onCreate() {
        super.onCreate();
        Utils.FileWrite(Utils.LOG_FILENAME, "MyService onCreate", true);

        mNotificationManager =(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT >=Build.VERSION_CODES.O) {
            CharSequence name = "GPS服务通知";
            String description = "不知道在哪里显示的内容？";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(channelId, name, importance);

            mChannel.setDescription(description);
            mChannel.enableLights(true);

            mNotificationManager.createNotificationChannel(mChannel);
            mBuilder = new NotificationCompat.Builder(this, channelId);
        }

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Log.e(TAG,"huang: "+notificationIntent);
        mBuilder.setSmallIcon(R.mipmap.rc2)
                .setContentTitle("GPS服务 " + mNotificationId).setContentText("服务已经开启...")
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent);
        Notification notification = mBuilder.build();
        startForeground(mNotificationId, notification);
        mNotificationManager.notify(mNotificationId, notification);

        if (true) {
            Utils.FileWrite(Utils.LOG_FILENAME, "Start MainActivity by Service", true);
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

        if (mLocationManager == null) {
            Utils.FileWrite(Utils.LOG_FILENAME, "Start GPS Listen", true);
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
            if (mLocationManager != null) {
                Utils.FileWrite(Utils.LOG_FILENAME, mLocationListeners[0] + " " + mLocationListeners[1], true);
                if (false)
                    mLocationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE, mLocationListeners[0]);
                if (true)
                    mLocationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE, mLocationListeners[1]);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Utils.FileWrite(Utils.LOG_FILENAME, "MyService onDestroy", true);
    }

    private class LocationListener implements android.location.LocationListener {
        Location mLastLocation;

        public LocationListener(String provider) {
            //Log.e(TAG, "LocationListener " + provider);
            Utils.FileWrite(Utils.LOG_FILENAME, "MyService LocationListener " + provider, true);
            mLastLocation = new Location(provider);
        }

        @Override
        public void onLocationChanged(@NonNull Location location) {
            Utils.FileWrite(Utils.LOG_FILENAME, "MyService onLocationChanged Provider(" + location.getProvider() + "):" +
                    location.getLatitude() + "," + location.getLongitude() + "," + location.getTime(), true);
            long nowTime = location.getTime() / 1000;
            long diffTime;
            double lat = location.getLatitude() - Utils.CENTRE_LATITUDE;  //Four 中心
            double lon =  location.getLongitude() - Utils.CENTRE_LONGITUDE;
            double delta = Math.sqrt(lat * lat + lon * lon) * 105d; //1度约为105km，不准确 可以先这样用
            if (delta < 0.9) { //900米 刷新减慢
                diffTime = 3 * 60;
            } else if (delta < 7.0) { // 900-7000米，刷新增加
                diffTime = 60;
            } else {     // >7000米，刷新减慢
                diffTime = 12 * 60;
                int hour = new Date().getHours();
                Log.e(TAG, "Hours tobe delele: " + hour);
                if ((hour == 7) || (hour == 8)) { //早上如果还比较远，仍然高频率查询
                    diffTime = 2 * 60;
                }
            }
            MainActivity.sendHandler(1);
            if (nowTime - lastTime >= diffTime) {
                //new MainActivity().getChargeInfo();
                MainActivity.getChargeInfo();
                lastTime = nowTime;
            }
            mLastLocation.set(location);
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            //Log.e(TAG, "onProviderDisabled " + provider);
            Utils.FileWrite(Utils.LOG_FILENAME, "MyService onProviderDisabled " + provider, true);
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
            //Log.e(TAG, "onProviderEnabled " + provider);
            Utils.FileWrite(Utils.LOG_FILENAME, "MyService onProviderEnabled " + provider, true);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            //Log.e(TAG, "onStatusChanged " + provider);
            Utils.FileWrite(Utils.LOG_FILENAME, "MyService onStatusChanged " + provider, true);
        }
    }

    private final LocationListener[] mLocationListeners = new LocationListener[] {
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };

}
