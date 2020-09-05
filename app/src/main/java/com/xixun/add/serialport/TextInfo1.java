package com.xixun.add.serialport;

import android.os.Binder;

import com.xixun.joey.uart.IUartService;

public class TextInfo1 extends Binder{
    private IUartService uart;
    byte dev;
    String text;
    byte height;
    byte color;
    byte textType;
    String status;
   public TextInfo1( String status,byte dev, String text, byte textType, byte height, byte color) {
        this.status  = status;
        this.dev = dev;
        this.text = text;
        this.height = height;
        this.color = color;
        this.textType = textType;
    }
    public TextInfo1(String status) {
        this.status  = status;
    }
    public String getStatu(){
        return this.status;
    }
}

