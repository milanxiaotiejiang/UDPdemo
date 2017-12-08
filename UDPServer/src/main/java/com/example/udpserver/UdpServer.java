package com.example.udpserver;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Created by 朱浩 on 2016/5/18.
 */
public class UdpServer implements Runnable {

    private String ip = null;
    private int port = 0;
    private DatagramPacket dpRcv = null, dpSend = null;
    private static DatagramSocket ds = null;
    private InetSocketAddress inetSocketAddress = null;
    private byte[] msgRcv = new byte[1024];
    private boolean udpLife = true;     //udp生命线程
    private boolean udpLifeOver = true; //生命结束标志，false为结束


    public UdpServer(String mIp, int mPort) {
        this.ip = mIp;
        this.port = mPort;
    }

    private void SetSoTime(int ms) throws SocketException {
        ds.setSoTimeout(ms);
    }

    //返回udp生命线程因子是否存活
    public boolean isUdpLife() {
        if (udpLife) {
            return true;
        }

        return false;
    }

    //返回具体线程生命信息是否完结
    public boolean getLifeMsg() {
        return udpLifeOver;
    }

    //更改UDP生命线程因子
    public void setUdpLife(boolean b) {
        udpLife = b;
    }

    private boolean isConnect;

    public void Send(String sendStr) throws IOException {
        Log.i("SocketInfo", "客户端IP：" + dpRcv.getAddress().getHostAddress() + "客户端Port:" + dpRcv.getPort());

        dpSend = new DatagramPacket(sendStr.getBytes(), sendStr.getBytes().length, dpRcv.getAddress(), 8008);
        ds.send(dpSend);
    }


    @Override
    public void run() {
        inetSocketAddress = new InetSocketAddress(ip, port);
        try {
            ds = new DatagramSocket(port);
            Log.i("SocketInfo", "UDP服务器已经启动");

            SetSoTime(3000);
            //设置超时，不需要可以删除
        } catch (SocketException e) {
            e.printStackTrace();
        }

        dpRcv = new DatagramPacket(msgRcv, msgRcv.length);
        while (udpLife) {
            try {
                Log.i("SocketInfo", "UDP监听中");
                ds.receive(dpRcv);

                String string = new String(dpRcv.getData(), dpRcv.getOffset(), dpRcv.getLength());
                Log.i("SocketInfo", "收到信息：" + string);
                if (string.equals("FF03000000000000")) {
                    if (!isConnect) {
                        isConnect = true;
                        Intent intent = new Intent();
                        intent.setAction("udpReceiver");
                        intent.putExtra("udpReceiver", string);
                        MainActivity.context.sendBroadcast(intent);

                    }
                } else {

                    Intent intent = new Intent();
                    intent.setAction("udpReceiver");
                    intent.putExtra("udpReceiver", string);
                    MainActivity.context.sendBroadcast(intent);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        ds.close();
        Log.i("SocketInfo", "UDP监听关闭");
        //udp生命结束
        udpLifeOver = false;
    }

    public static String getIPAddress(Context context) {
        NetworkInfo info = ((ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (info != null && info.isConnected()) {
            if (info.getType() == ConnectivityManager.TYPE_MOBILE) {//当前使用2G/3G/4G网络
                try {
                    //Enumeration<NetworkInterface> en=NetworkInterface.getNetworkInterfaces();
                    for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                        NetworkInterface intf = en.nextElement();
                        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                            InetAddress inetAddress = enumIpAddr.nextElement();
                            if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                                return inetAddress.getHostAddress();
                            }
                        }
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                }

            } else if (info.getType() == ConnectivityManager.TYPE_WIFI) {//当前使用无线网络
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                String ipAddress = intIP2StringIP(wifiInfo.getIpAddress());//得到IPV4地址
                return ipAddress;
            }
        } else {
            //当前无网络连接,请在设置中打开网络
        }
        return null;
    }

    /**
     * 将得到的int类型的IP转换为String类型
     *
     * @param ip
     * @return
     */
    public static String intIP2StringIP(int ip) {
        return (ip & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                (ip >> 24 & 0xFF);
    }

}