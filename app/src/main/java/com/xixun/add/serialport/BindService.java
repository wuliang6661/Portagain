package com.xixun.add.serialport;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.AbsoluteLayout;
import com.xixun.joey.uart.BytesData;
import com.xixun.joey.uart.IUartListener;
import com.xixun.joey.uart.IUartService;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;

public class BindService extends Service {
    private TextInfo1 textinfo;
    private IUartService uart;
    private String porturl="/dev/ttyMT3";
    ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {

            //绑定服务
            uart = IUartService.Stub.asInterface(iBinder);

            Log.i("TAG", "================ onServiceConnected ====================");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.i("TAG", "================== onServiceDisconnected ====================");
            /**
             * 如果服务为null,重新绑定
             */
            uart = null;
            bindService(intent, conn, Context.BIND_AUTO_CREATE);
        }
    };
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mybinder;
    }
    Mybinder mybinder = new Mybinder();
    public class Mybinder extends Binder{
         IUartService getUart(){
            return uart;
        }
    }
    WifiManager wifiManager;
    ConnectivityManager conManager;
    AbsoluteLayout layout;
    byte[] bb = new byte[1024];
    int count = 0;
    int infolen = 0;
    int leng = 12;
    byte comm = 3;
    byte dev, t;
    int ci=0;
    @Override
    public void onCreate() {
        super.onCreate();
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        conManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        t = getDev();
        Log.i("TAG",t+"");
        testCardSystemUartAidl();
    }
    final Intent intent = new Intent("xixun.intent.action.UART_SERVICE");


    private void testCardSystemUartAidl() {

        intent.setPackage("com.xixun.joey.cardsystem");
        bindService(intent, conn, Context.BIND_AUTO_CREATE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                do {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } while (null == uart);
                try {
                    Log.i("TAG", "=================================================");
                    //监听/dev/ttyMT2，获取数据
                    uart.read(porturl, new IUartListener.Stub() {
                        @Override
                        public void onReceive(BytesData data) throws RemoteException {
                            String s1 = "";
                            String s = "";
                            //获取字节数组（）
                            for (byte a : data.getData()) {
                                bb[count] = a;
                                s1 += "0x" + Integer.toHexString(a & 0xFF) + " ";
                                if (count == 0) {
                                    if (bb[0] == 0xfe || bb[0] == -2) {
                                        count++;
                                    }
                                } else if (count == 1) {
                                    if (bb[1] == 0xfe || bb[1] == -2) {
                                        count++;
                                    } else {
                                        count = 0;
                                    }
                                } else {
                                    count++;
                                }
                                if (count == 5) {
                                    Log.i("TAG", "bb[2]):" + bb[2]);
                                    if (bb[2] < 0) {
                                        leng = bb[2] + 256 + 2;
                                    } else {
                                        leng = bb[2] + 2;
                                    }
                                    dev = bb[3];
                                    infolen = leng - 11;
                                    comm = bb[4];
                                    Log.i("TAG", "leng:" + leng);
                                    Log.i("TAG", "infolen:" + infolen);
                                    Log.i("TAG", "comm:" + comm);
                                }
                                //判断命令类型
                                if (comm == 0 && t == dev) {
                                    if (count == leng) {
//                            判断数组第二位是不是0xFE(如果不是，则受到的字节数组顺序有误，)
                                        byte[] info = new byte[leng - 11];
                                        System.arraycopy(bb, 10, info, 0, infolen);
                                        for (int i = 0; i <= count - 1; i++) {
                                            s += "0x" + Integer.toHexString(bb[i] & 0xFF) + " ";
                                        }
                                        Log.i("TAG", "s:" + s);
                                        //关闭网络closeNet()
                                        closeNet();
                                        try {
                                            Intent intent = new Intent();
                                            intent.setAction("BindService.action");
                                            intent.putExtra("statu", "show");
                                            intent.putExtra("dev", dev);
                                            intent.putExtra("info", new String(info, "GBK"));
                                            Log.i("TAG", "info:" + new String(info, "GBK"));
                                            intent.putExtra("textType", bb[7]);
                                            intent.putExtra("height", bb[8]);
                                            intent.putExtra("color", bb[9]);
                                            sendBroadcast(intent);

                                            //openNet();
                                        } catch (Exception e) {
                                            byte[] result1 = new byte[]{(byte) 0XFE, (byte) 0XFE, 5, dev, 0, 0, 4};
                                            uart.write(porturl, result1);
                                        }
                                    }
                                    //接收数组完成时，重置数据并准备接收下一个数组
                                } else if (comm == 1 && t == dev) {
                                    if (count >= leng) {
                                        try {
                                            Intent intent = new Intent();
                                            intent.setAction("BindService.action");
                                            intent.putExtra("statu", "close");
                                            sendBroadcast(intent);
                                        } catch (Exception e) {
                                            byte[] result1 = new byte[]{(byte) 0XFE, (byte) 0XFE, 5, dev, 1, 0, 4};
                                            uart.write(porturl, result1);
                                        }
                                        byte[] result = new byte[]{(byte) 0XFE, (byte) 0XFE, 5, dev, 1, 1, 4};
                                       uart.write(porturl, result);
                                    }
                                } else if (comm == 2) {
                                    if (count >= leng) {
                                        try {
                                            setDev(bb[5]);
                                            Log.i("TAG", "设置成功：设备地址为" + bb[5]);
                                            count = 0;
                                        } catch (Exception e) {
                                           // byte[] result1 = new byte[]{(byte) 0XFE, (byte) 0XFE, 5, dev, 2, 0, 4};
                                           /// uart.write("/dev/s3c2410_serial3", result1);
                                        }
                                        byte[] result = new byte[]{(byte) 0XFE, (byte) 0XFE, 5, dev, 2, 1, 4};
                                       uart.write(porturl, result);
                                    }
                                }
                                if (count >= leng) {
                                    Log.i("TAG", "count:" + count);
                                    Log.i("TAG", "初始化");
                                    count = 0;
                                    ci++;
                                    Log.i("TAG", "ci" + ci);
                                }
                            }
                        }
                    });
                } catch (RemoteException e) {
                    e.printStackTrace();
                    openNet();
                    byte[] result = new byte[]{bb[0], bb[0], 5, dev, comm, 0, 4};
                    try {
                        uart.write(porturl, result);
                    } catch (RemoteException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }).start();
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
   byte defultdev = 0;
    //设置设备地址
    private void setDev(byte adr) {
/*
       SharedPreferences preferences = getSharedPreferences("dev", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("dev", (byte) adr);
        editor.commit();
        defultdev = adr;*/
        int devadr = adr;
        String devadr1 = devadr+"";
        try {
           OutputStream os =  openFileOutput("Devadr.txt",MODE_PRIVATE);
           os.write(devadr1.getBytes());
           os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //获取设备地址
    private byte getDev() {
        byte devadr = 0;
        int i = 0;
        /*SharedPreferences preferences = getSharedPreferences("dev", MODE_PRIVATE);
        if (preferences != null) {
            devadr = (byte) preferences.getInt("dev", defultdev);
        }*/
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(openFileInput("Devadr.txt")));
            String adr = br.readLine();
            br.close();
            i = Integer.valueOf(adr);
        } catch (Exception e) {
            e.printStackTrace();
        }
        devadr = (byte)i;
        Log.i("TAG","devadr:"+devadr);
        return devadr;
    }
}
