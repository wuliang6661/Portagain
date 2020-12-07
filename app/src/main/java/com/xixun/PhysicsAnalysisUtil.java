package com.xixun;

import java.util.Objects;

/**
 * 获取温度湿度光感工具类
 *
 * @author yjk
 * @version 2020/12/2 17:19
 */
public class PhysicsAnalysisUtil {


    /**
     * 获取湿度
     *
     * @param b        报文
     * @param retValue 返回参数
     * @return boolean
     * @author yjk
     * @date 2020/12/2 17:24
     */
    public static boolean getHumid(byte[] b, StringBuilder retValue) {

        String mark = new String(b);
        String ad = Integer.toBinaryString(Integer.parseUnsignedInt(mark.substring(36, 38), 16));
        StringBuilder rx = new StringBuilder();
        int adLength = ad.length();
        if (adLength < 8) {
            for (int i = 0; i < 8 - adLength; i++) {
                rx.append("0");
            }
            ad = rx + ad;
        }
        retValue.append(Integer.parseUnsignedInt(ad.substring(1, 8), 2)).append("%/RH");
        return Objects.equals(String.valueOf(ad.charAt(0)), "1");

    }


    /**
     * 获取温度
     *
     * @param b        报文
     * @param retValue 返回参数
     * @return boolean
     * @author yjk
     * @date 2020/12/2 17:24
     */
    public static boolean getTemp(byte[] b, StringBuilder retValue) {

        String mark = new String(b);

        // 获取温度有效
        String tempMark = Integer.toBinaryString(Integer.parseUnsignedInt(mark.substring(34, 36), 16));
        StringBuilder rx = new StringBuilder();
        int tempMarkLength = tempMark.length();
        if (tempMarkLength < 8) {
            for (int i = 0; i < 8 - tempMarkLength; i++) {
                rx.append("0");
            }
            tempMark = rx + tempMark;
            System.out.println(tempMark);
        }

        // 获取温度数值
        int temp = Integer.parseUnsignedInt(mark.substring(38, 40), 16);
        double tempv = temp * 0.5;

        // 获取温度正负
        if (Objects.equals(String.valueOf(tempMark.charAt(8)), "1")) {
            retValue.append(tempv).append("摄氏度");
        } else {
            retValue.append("-").append(tempv).append("摄氏度");
        }

        return Objects.equals(String.valueOf(tempMark.charAt(0)), "1");
    }


    /**
     * 获取光强度
     *
     * @param b        报文
     * @param retValue 返回参数
     * @return boolean
     * @author yjk
     * @date 2020/12/2 17:24
     */
    public static boolean getLight(byte[] b, StringBuilder retValue) {

        String mark = new String(b);

        // 获取XX
        String XX = mark.substring(40, 42);
        // 十进制XX
        int XXD = Integer.parseUnsignedInt(XX, 16);
        // 二进制XX
        String XXB = Integer.toBinaryString(XXD);

        // 获取YY
        String YY = mark.substring(42, 44);
        int YYD = Integer.parseUnsignedInt(YY, 16);

        // A = (XX << 8) + YY
        int A = Integer.parseUnsignedInt(XXB + "00000000", 16) + YYD;

        // B = A & 0x7F
        String AB = Integer.toBinaryString(A);
        StringBuilder B = new StringBuilder(AB.substring(0, 8));
        for (int i = 0; i < 7; i++) {
            if (Objects.equals(AB.substring(9 + i, 10 + i), "1")) {
                B.append("1");
            } else {
                B.append("0");
            }
        }

        // ZZ = 0x02 + XX + YY
        String ZZ = Integer.toHexString(2 + YYD + XXD);

        // 返回B的值
        retValue.append(Integer.parseUnsignedInt(B.toString(), 2));

        return Objects.equals(String.valueOf(XXB.charAt(0)), "1");
    }


    public static void main(String[] args) {

//        String hexString = "AA55020000FE000000000000020000040100AD0957";
//        String hexString = "AA53000000FE000000000000000000040200804C2557";
//        byte[] b = hexString.getBytes();
//        System.out.println();
//        StringBuilder ret = new StringBuilder();
//        boolean humid = getHumid(b, ret);
//        System.out.println(ret.toString());
//        System.out.println(humid);
//        getTemp(b,null);
//        System.out.println(hexString.charAt(4));


    }
}
