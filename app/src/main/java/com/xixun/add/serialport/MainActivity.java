package com.xixun.add.serialport;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.xixun.Config;
import com.xixun.PhysicsAnalysisUtil;
import com.xixun.api.HttpResultSubscriber;
import com.xixun.api.HttpServerImpl;
import com.xixun.joey.uart.BytesData;
import com.xixun.joey.uart.IUartListener;
import com.xixun.joey.uart.IUartService;
import com.xixun.zfr.layouts.CustomLayout;

import java.lang.reflect.Method;
import java.util.Timer;
import java.util.TimerTask;

import io.reactivex.disposables.Disposable;


public class MainActivity extends Activity {


    private IUartService uart;
    WifiManager wifiManager;
    ConnectivityManager conManager;
    int face1 = 0;
    int face2 = 0;
    Typeface face;
    CustomLayout cl;
    int color;
    byte[] bb = new byte[1024];
    int count = 0;
    int infolen = 0;
    int leng = 12;
    byte comm = 3;
    byte dev;
    byte t;
    int ci = 0;
    TextInfo1 ti;
    TextInfo1 textInfo1;
    Disposable disposable;
    //y卡485地址
    //String port = "/dev/s3c2410_serial3";
    //e卡上地址
    String port = "/dev/ttyS0";

//    String port = "/dev/ttyS1";

//    String port = "/dev/ttyS4";

    private int leavel = 0;   //步骤
    private String liangdu;
    private String shidu;
    private String wendu;


    Thread.UncaughtExceptionHandler handler = new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            restartApp();
        }
    };
    ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.i("TAG", "================ onServiceConnected ====================");
            uart = IUartService.Stub.asInterface(iBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.i("TAG", "================== onServiceDisconnected ====================");
            uart = null;
            // restartApp();
        }
    };
    Handler handler1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        conManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        cl = findViewById(R.id.ml);
        t = getDev();

        Intent intent = new Intent("xixun.intent.action.UART_SERVICE");
        intent.setPackage("com.xixun.joey.cardsystem");
        bindService(intent, conn, Context.BIND_AUTO_CREATE);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                aidl();
            }
        }, 0, Config.TIME);
    }

    void aidl() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                do {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                } while (null == uart);
                try {
                    Log.i("TAG", "开始监听");
                    //监听/dev/ttyMT2，获取数据

                    uart.read(port, new IUartListener.Stub() {
                        @Override
                        public void onReceive(BytesData data) throws RemoteException {
                            StringBuilder builder = new StringBuilder();
                            switch (leavel) {
                                //第一步
                                case 0:
                                    uart.write(port, Config.liangdu1);
                                    leavel = 1;
                                    return;
                                case 1:
                                    uart.write(port, Config.liangdu2);
                                    leavel = 2;
                                    return;
                                //返回亮度 请求湿度
                                case 2:
                                    PhysicsAnalysisUtil.getLight(data.getData(), builder);
                                    liangdu = builder.toString();
                                    Log.e("亮度=====", liangdu);
                                    uart.write(port, Config.shidu);
                                    leavel = 3;
                                    return;
                                //返回湿度  获取温度
                                case 3:
                                    PhysicsAnalysisUtil.getHumid(data.getData(), builder);
                                    shidu = builder.toString();
                                    Log.e("湿度=====", shidu);
                                    uart.write(port, Config.wendu);
                                    leavel = 4;
                                    return;
                                //返回温度
                                case 4:
                                    PhysicsAnalysisUtil.getTemp(data.getData(), builder);
                                    wendu = builder.toString();
                                    Log.e("温度=====", wendu);
                                    updateData();
                                    leavel = 0;
                                    return;
                            }
                        }
                    });
                } catch (Exception e1) {
                    e1.printStackTrace();
                }

            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        if (disposable != null) {
            disposable.dispose();
            unbindService(conn);
        }

        super.onDestroy();
    }

    private void setDev(byte devadr) {
        SharedPreferences sharedPreferences = getSharedPreferences("Devadr", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("dev", devadr);
        editor.commit();
        Log.i("TAG", "保存地址成功");
    }

    //获取设备地址
    private byte getDev() {
        SharedPreferences sharedPreferences = getSharedPreferences("Devadr", MODE_PRIVATE);
        byte devadr = (byte) sharedPreferences.getInt("dev", 0);
        return devadr;
    }


    Class[] getArgArray = null;
    Class[] setArgArray = new Class[]{boolean.class};
    Object[] getArgInvoke = null;
    Method mGetMethod;
    Method mSetMethod;
    boolean isOpen;
    //网络初始状态
    boolean initial = false;

    //关闭网络
    private void closeNet() {
        if (wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(false);
            Log.i("TAG", "关闭wifi");
            initial = true;
        } else {
            try {
                mGetMethod = conManager.getClass().getMethod("getMobileDataEnabled", getArgArray);
                mSetMethod = conManager.getClass().getMethod("setMobileDataEnabled", setArgArray);
                isOpen = (Boolean) mGetMethod.invoke(conManager, getArgInvoke);
                if (isOpen) {
                    initial = true;
                    mSetMethod.invoke(conManager, false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 打开网络
    private void openNet() {
        if (initial == true) {
            if (!wifiManager.isWifiEnabled()) {
                wifiManager.setWifiEnabled(true);
                Log.i("TAG", "打开wifi");
            }
            try {
                mGetMethod = conManager.getClass().getMethod("getMobileDataEnabled", getArgArray);
                mSetMethod = conManager.getClass().getMethod("setMobileDataEnabled", setArgArray);
                isOpen = (Boolean) mGetMethod.invoke(conManager, getArgInvoke);
                if (!isOpen) {
                    mSetMethod.invoke(conManager, true);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void restartApp() {
        Log.i("TAG", "重新启动");
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getApplicationContext().startActivity(intent);
        android.os.Process.killProcess(android.os.Process.myPid());
    }


    /**
     * 上报数据
     */
    private void updateData() {
        Log.e("=========", "上报亮度=" + liangdu + "   上报温度=" + wendu + "   上报湿度=" + shidu);
        HttpServerImpl.createOrUpdateOne(liangdu, wendu, shidu).subscribe(new HttpResultSubscriber<String>() {
            @Override
            public void onSuccess(String s) {

            }

            @Override
            public void onFiled(String message) {

            }
        });
    }
}

