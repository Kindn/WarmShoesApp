package com.example.bttest2;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BlueToothController {

    private BluetoothAdapter mAdapter;
    private UUID mUUID;
    private String mName;
    private AcceptThread thread;
    private OutputStream os;
    private BluetoothSocket socket;
    private Handler handler;

    public BlueToothController(UUID uuid, String name, Handler hdl)
    {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mUUID = uuid;
        mName = name;

        handler = hdl;
        thread = new AcceptThread(mUUID, mName);
        thread.start();
    }

    /**
     * 是否支持蓝牙
     * @return
     */
    public boolean isSupportBlueTooth()
    {
        if (mAdapter != null)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * 判断当前蓝牙状态
     * @return
     */
    public boolean getBlueToothStatus()
    {
        assert (mAdapter != null);
        return mAdapter.isEnabled();
    }

    /**
     * 打开蓝牙
     */
    public void turnOnBlueTooth(Activity activity, int requestCode)
    {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * 关闭蓝牙
     */
    public void turnOffBlueTooth()
    {
        mAdapter.disable();
    }

    /**
     * 打开蓝牙可见性
     */
    public void enableVisibility(Context context)
    {
        Intent discoverableIntent = new
                Intent((BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE));
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        context.startActivity(discoverableIntent);
    }

    /**
     * 查找设备
     */
    public void findDevice()
    {
        assert (mAdapter != null);
        if (mAdapter.isDiscovering())
        {
            mAdapter.cancelDiscovery();
        }
        mAdapter.startDiscovery();
    }

    /**
     * 获取绑定设备
     * @return
     */
    public List<BluetoothDevice> getBondedDeviceList()
    {
        return new ArrayList<>(mAdapter.getBondedDevices());
    }


    /**
     * 连接设备
     * @return isConnected
     */
    public boolean connect(final String mac) {
        boolean isConnected = false;

        if (mAdapter.isDiscovering()) {
            mAdapter.cancelDiscovery();
        }
        try {
            BluetoothDevice rmDevice = mAdapter.getRemoteDevice(mac);
            socket = rmDevice.createRfcommSocketToServiceRecord(mUUID);
            socket.connect();
            os = socket.getOutputStream();

            isConnected =  true;
        } catch (IOException e) {
            isConnected =  false;
        }
        return isConnected;

    }

    /**
     * 发送消息
     * @return isSent
     */
    public boolean send(final String sendDat)
    {
        try{
            if (socket == null) {
                BluetoothDevice rmDevice = mAdapter.getRemoteDevice(mName);
                socket = rmDevice.createRfcommSocketToServiceRecord(mUUID);
                socket.connect();
                OutputStream os = socket.getOutputStream();
            }

            //获取连接设备的输出流
            if (os != null){
                os.write(sendDat.getBytes("UTF-8"));
            }

            return true;
        }catch(IOException e){
            return false;
        }
    }

    // 服务端接收信息线程类
    private class AcceptThread extends Thread {
        private BluetoothServerSocket serverSocket;
        private BluetoothSocket clientSocket;
        private InputStream is;
        private OutputStream os;

        //public String recv_msg;

        public AcceptThread(UUID uuid, String name) {
            try {
                serverSocket = mAdapter.listenUsingRfcommWithServiceRecord(name, uuid);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void run() {
            try {
                // 接收其客户端的接口
                clientSocket = serverSocket.accept();
                // 获取到输入流
                is = clientSocket.getInputStream();
                // 获取到输出流
                os = clientSocket.getOutputStream();

                // 无线循环来接收数据
                while (true) {
                    // 创建一个128字节的缓冲
                    byte[] buffer = new byte[128];
                    // 每次读取128字节，并保存其读取的角标
                    int count = is.read(buffer);
                    // 创建Message类，向handler发送数据
                    Message msg = new Message();
                    // 发送一个String的数据，让他向上转型为obj类型
                    msg.obj = new String(buffer, 0, count, "utf-8");
                    // 发送数据
                    handler.sendMessage(msg);
                }
            } catch (Exception e) {
                // TODO: handle exception
                e.printStackTrace();
            }
        }
    }
}
