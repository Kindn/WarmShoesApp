package com.example.bttest2;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    public static final int REQUEST_CODE = 0;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private List<String> mDeviceList = new ArrayList<>();
    private List<String> mBondedDeviceList = new ArrayList<>();

    // 创建handler，因为我们接收是采用线程来接收的，在线程中无法操作UI，所以需要handler
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);
            TextView re_msg = (TextView) findViewById(R.id.recv_msg);
            re_msg.setText((String)msg.obj);
        }
    };


    private final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final String name = "Bluetooth_Socket";
    private BlueToothController mController = new BlueToothController(myUUID, name, handler);
    private ListView mListView;
    // ListView的字符串数组适配器
    private ArrayAdapter<String> arrayAdapter;



    private Toast mToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // 因为蓝牙搜索到设备和完成搜索都是通过广播来告诉其他应用的
        // 这里注册找到设备和完成搜索广播
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        registerReceiver(receiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver, filter);
        filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);

        mListView = (ListView)findViewById(R.id.lv_devices);
        arrayAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1,
                mDeviceList);
        // 为listView绑定适配器
        mListView.setAdapter(arrayAdapter);
        // 为listView设置item点击事件侦听
        mListView.setOnItemClickListener((AdapterView.OnItemClickListener) this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            }
        }

    }

    protected void onDestroy(){
        super.onDestroy();//解除注册
        unregisterReceiver(receiver);
    }

    /*广播*/
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                showToast("开始扫描");
            }
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                showToast("ACTION_FOUND");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mDeviceList.add("设备名："+device.getName()+"\n" +"设备地址："+device.getAddress() + "\n"); //将搜索到的蓝牙名称和地址添加到列表。
                //arrayList.add( device.getAddress()); //将搜索到的蓝牙地址添加到列表。
                arrayAdapter.notifyDataSetChanged(); //更新
            }
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                showToast("扫描结束");
            }
        }
    };


    public void turnOnVisibility(View view)
    {
        boolean ret = mController.getBlueToothStatus();
        if (!ret)
        {
            mController.turnOnBlueTooth(this, REQUEST_CODE);
        }
        mController.enableVisibility(this);
    }

    public void searchForDevices(View view)
    {
        //setTitle("正在连接...");
        mDeviceList.clear();
        mController.findDevice();
    }

    public void showBondedDevices(View view)
    {
        List<BluetoothDevice> devices = mController.getBondedDeviceList();
        mDeviceList.clear();
        for (BluetoothDevice device : devices)
        {
            mDeviceList.add("设备名："+device.getName()+"\n" +"设备地址："+device.getAddress() + "\n");
        }
        //mDeviceList = mBondedDeviceList;
        arrayAdapter.notifyDataSetChanged(); //更新
    }

    public void sendData(View view)
    {
        EditText msgStr = (EditText) findViewById(R.id.data_to_be_sent);
        String dataStr = msgStr.getText().toString();
        if (mController.send(dataStr)){
            showToast("发送成功");
        }
        else{
            showToast("发送失败");
        }
    }

    private void showToast(String text)
    {
        if (mToast == null)
        {
            mToast  = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        }
        else
        {
            mToast.setText(text);
        }
        mToast.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK)
        {
            showToast("打开成功");
        }
        else
        {
            showToast("打开失败");
        }
    }

    // 开启权限
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                }
                break;
        }
    }

    @Override
    public void onItemClick (AdapterView<?> parent, View view, int position,
                             long id) {
        String s = arrayAdapter.getItem(position);
        String address = s.substring(s.indexOf("\n") + 6).trim();
        String devName = s.substring(4).trim();

        if (mController.connect(address)) {
            showToast("成功连接：" + devName);
        }
        else {
            showToast("连接失败：" + address);
        }

    }



}