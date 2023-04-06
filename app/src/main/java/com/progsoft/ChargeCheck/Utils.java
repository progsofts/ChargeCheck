package com.progsoft.ChargeCheck;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Date;

public class Utils {
    private static final String TAG = "Utils";
    public static final String LOG_FILENAME = "progsoft/charge/log/log.txt";
    public static final String INFO_FILENAME = "progsoft/charge/info3.txt";
    public static final String GET_CHARGE_URL = "https://www.baidu.com/";
    public static final double CENTRE_LATITUDE = 12.6569;
    public static final double CENTRE_LONGITUDE = 14.0537;
    public static final int MAX_DISPLAY_RECORD = 30;
    public static final int STATUS_GUN_FREE = 1;
    public static final int STATUS_GUN_PLUGED = 2;
    public static final int STATUS_GUN_CHARGING = 3;
    public static final int STATUS_GUN_CHARGED  = 4;
    public static final int STATUS_GUN_ERROR = 255;

    public static void FileWrite(String filename, String context, boolean tFlag) {
        try {
            String newTitle = "";
            File file = new File(Environment.getExternalStorageDirectory(), filename);
            if (!file.exists()) {
                newTitle = "开启新的一天，加油吧\n";
            }
            BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
            Date now = new Date();
            if (tFlag) {
                bw.write(newTitle + now + " , " + context);
            } else {
                bw.write(context);
            }
            bw.newLine();
            bw.flush();
            bw.close();
            Log.e(TAG, context);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
