package com.example.udpserver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btnUdpStart, btnUdpClose, btnRcvClear, btnSendClear, btnUdpSend;
    private TextView txtRcv, txtSend;
    private EditText editSend, editIp, editPort;

    public static Context context;

    private MyHandler myHandler = new MyHandler(this);
    private MyBroadcastReceiver myBroadcastReceiver = new MyBroadcastReceiver();
    private static UdpServer udpServer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        BindWidget();
        Listening();
        BindReceiver();
    }

    private void BindWidget() {
        btnUdpClose = (Button) findViewById(R.id.btn_udpClose);
        btnUdpStart = (Button) findViewById(R.id.btn_udpStart);
        btnUdpSend = (Button) findViewById(R.id.btn_Send);
        btnRcvClear = (Button) findViewById(R.id.btn_CleanRcv);
        btnSendClear = (Button) findViewById(R.id.btn_CleanSend);
        txtRcv = (TextView) findViewById(R.id.txt_Rcv);
        txtSend = (TextView) findViewById(R.id.txt_Send);
        editIp = (EditText) findViewById(R.id.editIp);
        editPort = (EditText) findViewById(R.id.editPort);
        editSend = (EditText) findViewById(R.id.edit_Send);
    }

    private void Listening() {
        btnUdpStart.setOnClickListener(this);
        btnUdpClose.setOnClickListener(this);
        btnUdpSend.setOnClickListener(this);
        btnRcvClear.setOnClickListener(this);
        btnSendClear.setOnClickListener(this);

    }

    private void BindReceiver() {
        IntentFilter intentFilter = new IntentFilter("udpReceiver");
        registerReceiver(myBroadcastReceiver, intentFilter);
    }

    private class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            if (null != activity) {
                switch (msg.what) {
                    case 1:
                        String str = msg.obj.toString();
                        if (str.equals("FF03000000000000")) {
                            Thread thread1 = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        udpServer.Send("123456");
                                        Message message = new Message();
                                        message.what = 2;
                                        message.obj = editSend.getText().toString();
                                        myHandler.sendMessage(message);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                }
                            });
                            thread1.start();
                        }
                        txtRcv.append(str);
                        break;
                    case 2:
                        String stra = msg.obj.toString();
                        txtSend.append(stra);
                        break;
                    case 3:
                        break;
                }
            }
        }
    }

    private class MyBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String mAction = intent.getAction();
            switch (mAction) {
                case "udpReceiver":
                    String msg = intent.getStringExtra("udpReceiver");
                    Message message = new Message();
                    message.what = 1;
                    message.obj = msg;
                    myHandler.sendMessage(message);
                    break;
            }
        }
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_udpStart:
                Log.i("asd", "asdasd");

                if (!editIp.getText().toString().isEmpty() && !editIp.getText().toString().isEmpty()) {
                    int mPort = Integer.parseInt(editPort.getText().toString());
//String ipp = getIPAddress(MainActivity.this);
//                        String ipp = getIp(MainActivity.this);
                    udpServer = new UdpServer(editIp.getText().toString(), mPort);
                    Thread thread = new Thread(udpServer);
                    thread.start();
                    btnUdpStart.setEnabled(false);
                    btnUdpClose.setEnabled(true);
                } else {
                    Log.i("DebugInfo", "请输入Ip或者Port");
                }
                break;
            case R.id.btn_udpClose:
                btnUdpClose.setEnabled(false);
                final Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //关闭UDP
                        udpServer.setUdpLife(false);

                        while (udpServer.getLifeMsg()) ; //等待udp阻塞结束，这里就体现出超时的好处了

                        Looper.getMainLooper();
                        btnUdpStart.setEnabled(true);
                    }
                });
                thread.start();

                break;
            case R.id.btn_CleanRcv:
                txtRcv.setText("");
                break;
            case R.id.btn_CleanSend:
                txtSend.setText("");
                break;
            case R.id.btn_Send:
                Thread thread1 = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (editSend.getText().toString() != null) {
                            try {
                                udpServer.Send(editSend.getText().toString());
                                Message message = new Message();
                                message.what = 2;
                                message.obj = editSend.getText().toString();
                                myHandler.sendMessage(message);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            Log.i("DebugInfo", "请输入发送内容");
                        }

                    }
                });
                thread1.start();
                break;
        }
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


    /**
     * 获取IP
     *
     * @param context
     * @return
     */
    public static String getIp(final Context context) {
        String ip = null;
        ConnectivityManager conMan = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        // mobile 3G Data Network
//        android.net.NetworkInfo.State mobile = conMan.getNetworkInfo(
//                ConnectivityManager.TYPE_MOBILE).getState();
        // wifi
        android.net.NetworkInfo.State wifi = conMan.getNetworkInfo(
                ConnectivityManager.TYPE_WIFI).getState();

        // 如果3G网络和wifi网络都未连接，且不是处于正在连接状态 则进入Network Setting界面 由用户配置网络连接
//        if (mobile == android.net.NetworkInfo.State.CONNECTED
//                || mobile == android.net.NetworkInfo.State.CONNECTING) {
//            ip =  getLocalIpAddress();
//        }
        if (wifi == android.net.NetworkInfo.State.CONNECTED
                || wifi == android.net.NetworkInfo.State.CONNECTING) {
            //获取wifi服务
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            //判断wifi是否开启
            if (!wifiManager.isWifiEnabled()) {
                wifiManager.setWifiEnabled(true);
            }
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int ipAddress = wifiInfo.getIpAddress();
            ip = (ipAddress & 0xFF) + "." +
                    ((ipAddress >> 8) & 0xFF) + "." +
                    ((ipAddress >> 16) & 0xFF) + "." +
                    (ipAddress >> 24 & 0xFF);
        }
        return ip;

    }

    /**
     * @return 手机GPRS网络的IP
     */
    private static String getLocalIpAddress() {
        try {
            //Enumeration<NetworkInterface> en=NetworkInterface.getNetworkInterfaces();
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {//获取IPv4的IP地址
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

        return null;
    }

}
