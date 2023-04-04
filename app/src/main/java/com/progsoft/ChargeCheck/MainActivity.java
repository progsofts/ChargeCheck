package com.progsoft.ChargeCheck;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    // 声明变量
    TableLayout tableLayout;
    private final ArrayList<DataItem> dataItems = new ArrayList<>();
    private final String TAG = "MainActivity";
    Thread mThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.e(TAG, "onCreate");

        // 添加要显示的数据
        dataItems.add(new DataItem("新星花园2号楼1001室", "(21, 104)", "90"));
        for (int i = 0; i < 2; i++)
            dataItems.add(new DataItem("地址" + i, "(22, 115)", "89"));
        // 绘制表格
//        initTable();

        MyLoop myRunnable = new MyLoop();
        if (mThread != null) {
            mThread.interrupt();
            mThread = null;
        }
        mThread = new Thread(myRunnable, "Timer");
        mThread.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        FileRead("progsoft/ChargeCheck3.txt");
        initTable();
    }

    class MyRunnable implements Runnable {
        @Override
        public void run() {
            String result = HttpClient.doGet("https://www.baidu.com/");
            if (result.length() < 1000) {
                FileWrite("progsoft/ChargeCheck_log.txt", "GET result:" + result, true);
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
            } while (k < 40);
            @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("HHmm");
            dataItems.add(new DataItem(sdf.format(new Date()) + "," + newRecord));
            FileWrite("progsoft/ChargeCheck_log.txt", sdf.format(new Date()) + "," + newRecord, true);
            FileWrite("progsoft/ChargeCheck3.txt", sdf.format(new Date()) + "," + newRecord, false);
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
                    FileWrite("progsoft/ChargeCheck_log.txt", "My loop:" + loop, true);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }


    @SuppressLint("SetTextI18n")
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

    public void FileWrite(String filename, String context, boolean tFlag) {
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

    public void FileRead(String filename) {
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
                if (dataItems.size() > 10) {
                    dataItems.remove(0);
                }
            } while (true);
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