package com.xixun.add.serialport;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.widget.AbsoluteLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.xixun.joey.uart.BytesData;
import com.xixun.joey.uart.IUartListener;
import com.xixun.joey.uart.IUartService;
import com.xixun.zfr.layouts.CustomLayout;

import java.lang.reflect.Method;
import io.reactivex.disposables.Disposable;


public class MainActivity extends Activity {
    private IUartService uart;
    WifiManager wifiManager;
    ConnectivityManager conManager;
    int face1 = 0;
    int face2 =0;
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
    String port = "/dev/ttyMT3";

git
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
        cl=findViewById(R.id.ml);
        t = getDev();
        handler1 = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                TextInfo1 value = (TextInfo1) msg.obj;
                TextView tv=new TextView(MainActivity.this);
                if (value != null && value.getStatu() == "show") {
                    ti = value;
                    Log.i("TAG", "ti.text:" + ti.text);
                    Log.i("TAG", "ti.height:" + ti.height);
                    Log.i("TAG", "ti.color:" + ti.color);
                    Log.i("TAG", "ti.textType:" + ti.textType);
                    face1 = ti.textType;
                    if (tv == null) {
                        Log.i("TAG", "tv==null");
                    }
                    if (ti.color == 0) {
                        color = Color.BLUE;
                    } else if (ti.color == 1) {
                        color = Color.GRAY;
                    } else if (ti.color == 2) {
                        color = Color.GREEN;
                    } else if (ti.color == 3) {
                        color = Color.RED;
                    } else if (ti.color == 4) {
                        color = Color.YELLOW;
                    } else if (ti.color == 5) {
                        color = Color.MAGENTA;
                    } else if (ti.color == 6) {
                        color = Color.CYAN;
                    } else if (ti.color == 7) {
                        color = Color.WHITE;
                    } else if (ti.color == 8) {
                        color = Color.BLACK;
                    } else if (ti.color == 9) {
                        color = Color.DKGRAY;
                    }
                    Log.i("TAG", "Color"+color);
                   if (face2!=face1) {
                       Log.i("TAG", "字体改变");
                         if (ti.textType == 1) {
                            face = Typeface.createFromAsset(getAssets(), "fonts/宋体.TTF");
                        } else if (ti.textType == 2) {
                            face = Typeface.createFromAsset(getAssets(), "fonts/华文新魏.ttf");
                        } else if (ti.textType == 3) {
                            face = Typeface.createFromAsset(getAssets(), "fonts/华文行楷.ttf");
                        } else if (ti.textType == 4) {
                            face = Typeface.createFromAsset(getAssets(), "fonts/准圆.TTF");
                        } else if (ti.textType == 5) {
                            face = Typeface.createFromAsset(getAssets(), "fonts/经典行书简.TTF");
                        } else if (ti.textType == 6) {
                            face = Typeface.createFromAsset(getAssets(), "fonts/微软雅黑粗体.ttf");
                        }
                        face2 = face1;

                    }else{
                       Log.i("TAG", "字体没有改变");
                   }
                   //设置字体

                    tv.setSingleLine(true);
                    tv.setTypeface(face);
                   //设置文本
                    tv.setText(ti.text);
                    //设置文本大小
                    tv.setTextSize(ti.height);
                    //设置文字颜色
                    tv.setTextColor(color);
                    cl.addView(tv);
                    Log.i("TAG", "已显示文本");
                } else if (value.getStatu() == "close"&&tv!=null) {

                    cl.removeAllViews();
                    Log.i("TAG", "已清除显示");
                }
            }
        };

        Intent intent = new Intent("xixun.intent.action.UART_SERVICE");
        intent.setPackage("com.xixun.joey.cardsystem");
        bindService(intent, conn, Context.BIND_AUTO_CREATE);
        aidl();
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
                            Message msg = Message.obtain();
                            String s1 = "";
                            String s = "";
                            //获取字节数组（）
                            for (byte a : data.getData()) {
                                bb[count] = a;
                                s1 += "0x" + Integer.toHexString(a & 0xFF) + " ";
                                  Log.i("TAG","收到的数据为："+s1);
                                if (count == 0) {
                                    Log.i("TAG", "开始接受数据");
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
                                    Log.i("TAG", "t:" + t);
                                    Log.i("TAG", "dev:" + dev);
                                }
                                //判断命令类型
                                //命令类型为0，显示数据
                                if (comm == 0 && getDev() == dev) {
                                    if (count == leng) {
                                        Log.i("TAG", s1);
//                            判断数组第二位是不是0xFE(如果不是，则受到的字节数组顺序有误，)
                                        byte[] info = new byte[leng - 11];
                                        System.arraycopy(bb, 10, info, 0, infolen);
                                        for (int i = 0; i <= count - 1; i++) {
                                            s += "0x" + Integer.toHexString(bb[i] & 0xFF) + " ";
                                        }
                                        Log.i("TAG", "s:" + s);
                                        //关闭网络closeNet()
                                        //closeNet();
                                        try {
                                            textInfo1 = new TextInfo1("show", dev, new String(info, "GBK"), bb[7], bb[8], bb[9]);
                                            //openNet();
                                            Log.i("TAG", "info:" + new String(info, "GBK"));
                                            msg.obj = textInfo1;
                                            handler1.sendMessage(msg);
                                        } catch (Exception e) {
                                            byte[] result1 = new byte[]{(byte) 0XFE, (byte) 0XFE, 5, dev, 0, 0, 4};
                                            uart.write(port, result1);
                                        }
                                        byte[] result = new byte[]{(byte) 0XFE, (byte) 0XFE, 5, dev, 0, 1, 4};
                                        uart.write(port, result);
                                    }
                                    //接收数组完成时，重置数据并准备接收下一个数组
                                } else if (comm == 1 && getDev() == dev) {
                                    if (count >= leng) {
                                        try {
                                            textInfo1 = new TextInfo1("close");
                                            msg.obj = textInfo1;
                                            handler1.sendMessage(msg);
                                        } catch (Exception e) {
                                            byte[] result1 = new byte[]{(byte) 0XFE, (byte) 0XFE, 5, dev, 1, 0, 4};
                                            uart.write(port, result1);
                                        }
                                        byte[] result = new byte[]{(byte) 0XFE, (byte) 0XFE, 5, dev, 1, 1, 4};
                                        uart.write(port, result);
                                    }
                                } else if (comm == 2 && getDev() == dev) {
                                    if (count >= leng) {
                                        try {
                                            setDev(bb[5]);
                                            Log.i("TAG", "设置成功：设备地址为" + bb[5]);
                                            count = 0;
                                        } catch (Exception e) {
                                            byte[] result1 = new byte[]{(byte) 0XFE, (byte) 0XFE, 5, dev, 2, 0, 4};
                                            uart.write(port, result1);
                                        }
                                        byte[] result = new byte[]{(byte) 0XFE, (byte) 0XFE, 5, dev, 2, 1, 4};
                                        uart.write(port, result);
                                    }
                                } else if (comm == 3) {
                                    if (count >= leng) {
                                        try {
                                            textInfo1 = new TextInfo1("dev");
                                            count = 0;
                                        } catch (Exception e) {
                                            byte[] result1 = new byte[]{(byte) 0XFE, (byte) 0XFE, 5, dev, 3, 0, 4};
                                            uart.write(port, result1);
                                        }
                                        byte[] result = new byte[]{(byte) 0XFE, (byte) 0XFE, 6, dev, 3, getDev(), t, 5};
                                        uart.write(port, result);
                                    }
                                }
                                if (count >= leng) {
                                    Log.i("TAG", "count:" + count);
                                    Log.i("TAG", "leng" + leng);
                                    Log.i("TAG", "=====================初始化=================");
                                    count = 0;
                                    ci++;
                                    leng = 12;
                                    Log.i("TAG", "ci" + ci);
                                    System.gc();
                                }
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
        if(disposable!=null){
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
}

