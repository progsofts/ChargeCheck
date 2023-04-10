package com.progsoft.ChargeCheck;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

@SuppressLint("SetTextI18n")
public class MainActivity extends AppCompatActivity {
    // 声明变量
    TableLayout tableLayout;
    private static final ArrayList<DataItem> dataItems = new ArrayList<>();
    private static final String TAG = "MainActivity";
//    Thread mThread;
    public static MyHandler myHandler = null;
    public static NetworkConnectChangedReceiver mNetworkChangeListener = new NetworkConnectChangedReceiver();

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.e(TAG, "onCreate");

        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        filter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        filter.addAction("android.net.wifi.STATE_CHANGE");
        registerReceiver(mNetworkChangeListener,filter);

        File path = new File(Environment.getExternalStorageDirectory()+"/progsoft/charge/log/");
        if(!path.exists()){
            Log.e(TAG, "mkdir " + path + " " + path.mkdirs());
        }

        TextView tv = findViewById(R.id.version);
        tv.setText("PC-VER:" + BuildConfig.VERSION_NAME + " " + BuildConfig.BUILD_TYPE);
        // 添加要显示的数据
        dataItems.add(new DataItem("新星花园2号楼1001室", "(21, 104)", "90"));
        for (int i = 0; i < 2; i++)
            dataItems.add(new DataItem("地址" + i, "(22, 115)", "89"));
        // 绘制表格
//        initTable();

        Intent service = new Intent(getApplicationContext(), MyService.class);
        getApplicationContext().startForegroundService(service);
        if (myHandler == null) {
            myHandler = new MyHandler(new WeakReference<>(this));
            Log.e(TAG, "new Handler");
        } else {
            Log.e(TAG, "old Handler");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        fileRead(Utils.INFO_FILENAME);
        initTable();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy");
        myHandler = null;
    }

    class MyHandler extends Handler {
        private final WeakReference<MainActivity> activity;
        MyHandler(WeakReference<MainActivity> activity) {
            this.activity = activity;
        }
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                fileRead(Utils.INFO_FILENAME);
                initTable();
            } else {
                TextView tv = findViewById(R.id.textView);
                tv.setText(new Date().toLocaleString());
            }
        }
    }

    public static void sendHandler(int what) {
        Message check = myHandler.obtainMessage(what, "");
        myHandler.sendMessage(check);
    }

    static class MyRunnable implements Runnable {
        @Override
        public void run() {
            //等待2s，可能网络还没连接上
            try {
                Thread.sleep(2 * 1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            String result = HttpClient.doGet(Utils.GET_CHARGE_URL);
            if (result.length() < 1000) {
                Utils.FileWrite(Utils.LOG_FILENAME, "GET URL ERROR responses:" + result, true);
                return;
            }
            int index = 0;
            int k = 0;
            StringBuilder newRecord = new StringBuilder();
            do {
                index = result.indexOf("status", index + 1);
                if (index < 0) break;
                int var = result.indexOf(",", index + 1);
                k++;
                newRecord.append(result.substring(index + 8, var)).append(",");
            } while (k < 40); //最多支持充电桩数量 防止死循环
            long now = System.currentTimeMillis() / 1000;
            @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("HHmm");
            dataItems.add(new DataItem(sdf.format(new Date()) + "," + newRecord + now));
            Utils.FileWrite(Utils.LOG_FILENAME, sdf.format(new Date()) + "," + newRecord + now, true);
            Utils.FileWrite(Utils.INFO_FILENAME, sdf.format(new Date()) + "," + newRecord + now, false);
            sendHandler(0);
        }
    }

    class MyLoop implements Runnable {
        @Override
        public void run() {
            int loop = 0;
            while (true) {
                try {
                    if (loop % 2 == 0) {
                        MyRunnable myRunnable = new MyRunnable();
                        Thread mThread = new Thread(myRunnable, "Timer");
                        mThread.start();
                    }
                    loop++;
                    Thread.sleep(30 * 1000L);
                    Utils.FileWrite(Utils.LOG_FILENAME, "My loop:" + loop, true);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }

    public static void getChargeInfo() {
        MyRunnable myRunnable = new MyRunnable();
        Thread mThread = new Thread(myRunnable, "Timer");
        mThread.start();
    }


    private void initTable() {
        tableLayout = findViewById(R.id.tableLayout);
        tableLayout.removeAllViews();

        int padding = dip2px(getApplicationContext(), 5);

        // 遍历dataItems, 每一条数据都加进TableLayout中
        for(int i = 0; i<dataItems.size(); i++){
            // 获取一条数据
            DataItem dataItem = dataItems.get(i);

            // 新建一个TableRow并设置样式
            TableRow newRow = new TableRow(getApplicationContext());
            TableRow.LayoutParams layoutParams = new TableRow.LayoutParams();
            newRow.setLayoutParams(layoutParams);

            //新建一个LinearLayout
            LinearLayout linearLayout = new LinearLayout(getApplicationContext());
            linearLayout.setOrientation(LinearLayout.HORIZONTAL);

            // 底部边框的宽度
            int bottomLine = dip2px(getApplicationContext(), 1);
            if(i == dataItems.size() - 1) {
                // 如果当前行是最后一行, 则底部边框加粗
                bottomLine = dip2px(getApplicationContext(), 2);
            }

            String g = dataItem.getRecord();
            if (g.equals("")) {
                continue;
            }

            int index = 0;
            do {
                int var = g.indexOf(",", index + 1);
                if (var < 0) break;
                // 将所有新的组件加入到对应的视图中
                TextView tvDistance = new TextView(getApplicationContext());
                // 设置文字居中
                tvDistance.setGravity(Gravity.CENTER);
                // 设置表格中的数据不自动换行
                //tvDistance.setSingleLine();
                // 设置边框和weight
                LinearLayout.LayoutParams lpDistance = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 2f);
                lpDistance.setMargins(dip2px(getApplicationContext(), 1),
                        dip2px(getApplicationContext(), 1),
                        dip2px(getApplicationContext(), 1),
                        bottomLine);
                tvDistance.setLayoutParams(lpDistance);
                // 设置padding和背景颜色
                tvDistance.setPadding(padding, padding, padding, padding);
                tvDistance.setBackgroundColor(Color.parseColor("#FFFFFF"));
                // 填充文字数据
                String status = g.substring(index, var);
                tvDistance.setText(status);
                switch (status) {
                    case "1":
                        tvDistance.setBackgroundColor(Color.parseColor("#00FF00"));
                        break;
                    case "2":
                        tvDistance.setBackgroundColor(Color.parseColor("#808080"));
                        break;
                    case "3":
                        tvDistance.setBackgroundColor(Color.parseColor("#FF0000"));
                        break;
                    default:
                        tvDistance.setBackgroundColor(Color.parseColor("#707070"));
                        break;
                }

                linearLayout.addView(tvDistance);
                index = var + 1;
            } while (true);
            newRow.addView(linearLayout);
            tableLayout.addView(newRow);
        }
    }

    private void initTableMap(int[] status, long[] update) {
        tableLayout = findViewById(R.id.mapTableLayout);
        tableLayout.removeAllViews();

        int[][] map = {{105,45,45,60,0,3,2,1},
                {75,6,2,18,4,0,0,0},
                {75,4,0,24,5,0,0,0},
                {195,4,1,30,6,0,0,7},
                {105,4,1,150,9,0,0,8},
                {75,4,1,60,10,0,0,16},
                {75,4,1,30,11,0,0,15},
                {75,4,1,30,12,0,0,14},
                {195,4,1,150,13,0,0,0},
                {105,4,1,60,26,0,0,0},
                {75,4,1,30,25,0,0,27},
                {75,4,1,30,24,0,0,28},
                {75,4,1,30,23,0,0,29},
                {75,4,1,150,22,0,0,0},
                {195,4,1,60,30,0,0,21},
                {105,4,1,30,19,0,0,20},
                {75,4,0,18,18,0,0,0},
                {195,132,128,144,17,0,0,0},
        };

        int padding = dip2px(getApplicationContext(), 5);

        // 遍历dataItems, 每一条数据都加进TableLayout中
        for (int[] ints : map) {
            // 新建一个TableRow并设置样式
            TableRow newRow = new TableRow(getApplicationContext());
            TableRow.LayoutParams layoutParams = new TableRow.LayoutParams();
            newRow.setLayoutParams(layoutParams);

            //新建一个LinearLayout
            LinearLayout linearLayout = new LinearLayout(getApplicationContext());
            linearLayout.setOrientation(LinearLayout.HORIZONTAL);
            //一半是格式，一半是值
            for (int j = 0; j < ints.length / 2; j++) {
                // 将所有新的组件加入到对应的视图中
                TextView tvDistance = new TextView(getApplicationContext());

                // 设置文字居中
                tvDistance.setGravity(Gravity.CENTER);
                // 设置表格中的数据不自动换行
                //tvDistance.setSingleLine();
                // 设置边框和weight
                LinearLayout.LayoutParams lpDistance = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 2f);
                int[] draw = new int[4];
                int[] mark = {1, 2, 4, 8};
                for (int k = 0; k < 4; k++) {
                    if ((ints[j] & mark[k]) > 0) {
                        draw[k] = dip2px(getApplicationContext(), 1);
                        continue;
                    }
                    if ((ints[j] & (mark[k] * 16)) > 0) {
                        draw[k] = dip2px(getApplicationContext(), 3);
                        continue;
                    }
                    draw[k] = 0;
                }
                lpDistance.setMargins(draw[2], draw[1], draw[0], draw[3]);
                tvDistance.setLayoutParams(lpDistance);
                // 设置padding和背景颜色
                tvDistance.setPadding(padding, padding, padding, padding);
                tvDistance.setBackgroundColor(Color.parseColor("#FFFFFF"));

                int value = ints[j + 4];
                if (value > 0) {
                    tvDistance.setText("" + getTimeDescEng(update[value]));
                    // 填充文字数据
                    switch (status[value]) {
                        case Utils.STATUS_GUN_FREE:
                            tvDistance.setBackgroundColor(Color.parseColor("#20F010"));
                            break;
                        case Utils.STATUS_GUN_PLUGED:
                            tvDistance.setBackgroundColor(Color.parseColor("#E06020"));
                            break;
                        case Utils.STATUS_GUN_CHARGING:
                            tvDistance.setBackgroundColor(Color.parseColor("#FF2020"));
                            break;
                        case Utils.STATUS_GUN_CHARGED:
                            tvDistance.setBackgroundColor(Color.parseColor("#D0E010"));
                            break;
                        default:
                            tvDistance.setBackgroundColor(Color.parseColor("#A0A0A0"));
                            break;
                    }
                }
                linearLayout.addView(tvDistance);
            }
            newRow.addView(linearLayout);
            tableLayout.addView(newRow);
        }
    }

    public String getTimeDescEng(long lastTime) {
        long now = System.currentTimeMillis() / 1000;
        if (now - lastTime < 60) {
            return (now - lastTime) + "s";
        }
        now /= 60;
        lastTime /=60;
        if (now - lastTime < 60) {
            return (now - lastTime) + "m";
        }
        now /= 60;
        lastTime /=60;
        if (now - lastTime < 24) {
            return (now - lastTime) + "h";
        }
        now /= 24;
        lastTime /= 24;
        return (now - lastTime) + "d";
    }


    public String getTimeDesc(long lastTime) {
        long now = System.currentTimeMillis() / 1000;
        if (now - lastTime < 60) {
            return "刚刚**";
        }
        now /= 60;
        lastTime /=60;
        if (now - lastTime < 60) {
            return (now - lastTime) + "分钟前*";
        }
        now /= 60;
        lastTime /=60;
        if (now - lastTime < 24) {
            return (now - lastTime) + "小时前";
        }
        now /= 24;
        lastTime /= 24;
        return (now - lastTime) + "天前";
    }

    public String getDescription(int[] status, long[] update) {
        String[] description = new String[6];
        Arrays.fill(description, "");

        int[] counts = new int[6];
        Arrays.fill(counts, 0);

        for (int k = 1; k < 31; k++) {
            switch (status[k]) {
                case Utils.STATUS_GUN_FREE:
                    counts[1]++;
                    description[1] += "第" + k + "枪 " + getTimeDesc(update[k]) + " 空闲 **\n";
                    break;
                case Utils.STATUS_GUN_PLUGED:
                    counts[2]++;
                    description[2] += "第" + k + "枪 " + getTimeDesc(update[k]) + " 插枪未充电 --\n";
                    break;
                case Utils.STATUS_GUN_CHARGING:
                    counts[3]++;
                    description[3] += "第" + k + "枪 " + getTimeDesc(update[k]) + " 充电中 XX\n";
                    break;
                case Utils.STATUS_GUN_CHARGED:
                    counts[4]++;
                    description[4] += "第" + k + "枪 " + getTimeDesc(update[k]) + " 插枪已充满 ++\n";
                    break;
                default:
                    counts[5]++;
                    description[5] += "第" + k + "枪 " + getTimeDesc(update[k]) + " 异常\n";
                    break;
            }
        }
        return " =====空枪===== " + counts[1] + "\n" + description[1] + " =====充完占用===== " + counts[4] + "\n" + description[4] +
                " =====正在使用===== " + counts[3] + "\n" + description[3] + " =====等待充电===== " + counts[2] + "\n" + description[2] +
                " =====异常===== " + counts[5] + "\n" + description[5];
    }

    public void fileRead(String filename) {
        int[] status = null;
        long[] updateTime = null;
        long newTime = 0;
        try {
            dataItems.clear();
            File file = new File(Environment.getExternalStorageDirectory(), filename);
            if (!file.exists()) {
                dataItems.add(0, new DataItem(" , 1, 2, 3, 4, 5, 6, 7, 8, 9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,"));
                dataItems.add(0, new DataItem(" , 1, 2, 3, 4, 5, 6, 7, 8, 9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,"));
                return;
            }
            BufferedReader br = new BufferedReader(new FileReader(file));
            do {
                String s = br.readLine();
                if (s == null) break;
                dataItems.add(new DataItem(s));
                if (dataItems.size() > Utils.MAX_DISPLAY_RECORD) {
                    dataItems.remove(0);
                }

                //计算使用情况
                String[] Info = s.split(",");
                if (status == null) {
                    status = new int[Info.length];
                    updateTime = new long[Info.length];
                    for (int k = 1; k < Info.length - 1; k++) {
                        status[k] = Integer.parseInt(Info[k]);
                        updateTime[k] = Integer.parseInt(Info[31]);
                    }
                } else {
                    newTime = Integer.parseInt(Info[31]);
                    for (int k = 0; k < Info.length - 1; k++) {
                        switch (Info[k]) {
                            case "1" : //空闲
                                if (status[k] != Utils.STATUS_GUN_FREE) { //其他状态进入空闲
                                    status[k] = Utils.STATUS_GUN_FREE;
                                    updateTime[k] = newTime;
                                }
                                break;
                            case "2" : //插枪
                                if (status[k] == Utils.STATUS_GUN_FREE) { //空闲 插枪未充电
                                    status[k] = Utils.STATUS_GUN_PLUGED;
                                    updateTime[k] = newTime;
                                } else if (status[k] == Utils.STATUS_GUN_CHARGING) { // 充满，插枪
                                    status[k] = Utils.STATUS_GUN_CHARGED;
                                    updateTime[k] = newTime;
                                }
                                break;
                            case "3" : //使用中
                                if (status[k] != Utils.STATUS_GUN_CHARGING) { //其他状态进入充电状态
                                    status[k] = Utils.STATUS_GUN_CHARGING;
                                    updateTime[k] = newTime;
                                }
                                break;
                            default:   //异常
                                if (status[k] != Utils.STATUS_GUN_ERROR) { //其他状态进入异常
                                    status[k] = Utils.STATUS_GUN_ERROR;
                                    updateTime[k] = newTime;
                                }
                                break;
                        }
                    }
                }
            } while (true);

            initTableMap(status, updateTime);
            String result = getDescription(status, updateTime);
            Log.e(TAG, result);
            TextView tv = findViewById(R.id.result);
            tv.setText(new Date(newTime * 1000).toLocaleString() + "\n" + result);
            dataItems.add(0, new DataItem(" , 1, 2, 3, 4, 5, 6, 7, 8, 9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,"));
            dataItems.add(new DataItem(" , 1, 2, 3, 4, 5, 6, 7, 8, 9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,"));
            dataItems.add(new DataItem(" , 1, 2, 3, 4, 5, 6, 7, 8, 9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int dip2px(Context context, int dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
}