package com.dtu.mark.dexcomble;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends Activity {
    private final static String TAG = MainActivity.class.getSimpleName();

    private TextView list;
    private Button btn;

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice dexcom;

    //private BluetoothGatt bluetoothGatt;
    private BluetoothLeService bluetoothLeService;

    private boolean isConnected = false;

    private final String DEXCOM_BLE_NAME = "DEXCOMRX";
    private final String DEXCOM_BLE_ADDRESS = "EC:53:DF:6D:EC:1E";

    private final int REQUEST_ENABLE_BT = 1;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!bluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            bluetoothLeService.connect(DEXCOM_BLE_ADDRESS);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bluetoothLeService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        list = (TextView) findViewById(R.id.pairList);
        btn = (Button) findViewById(R.id.btn);

        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }


    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
        if (bluetoothLeService != null) {
            final boolean result = bluetoothLeService.connect(DEXCOM_BLE_ADDRESS);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                isConnected = true;
                //updateConnectionState(R.string.connected);
                //invalidateOptionsMenu();

                String message = "Connected";
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                Log.d(TAG, message);

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                isConnected = false;
                //updateConnectionState(R.string.disconnected);
                //invalidateOptionsMenu();
                //clearUI();

                String message = "Disconnected";
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                Log.d(TAG, message);

            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(bluetoothLeService.getSupportedGattServices());






            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                //displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                Toast.makeText(getApplicationContext(), intent.getStringExtra(BluetoothLeService.EXTRA_DATA), Toast.LENGTH_SHORT).show();
            }
        }
    };

    public void displayGattServices(List<BluetoothGattService> services) {
        for (BluetoothGattService service : services) {
            Log.d(TAG, "Found service: " + service.getUuid().toString());
            List<BluetoothGattCharacteristic> chs = service.getCharacteristics();
            for (BluetoothGattCharacteristic ch : chs) {
                Log.d(TAG, " - " + ch.getUuid().toString());
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(gattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        bluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onClickBtn(View view) {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        String deviceInfo = "";


        for(BluetoothDevice device : pairedDevices) {
            if (device.getName().equals(DEXCOM_BLE_NAME) && device.getAddress().equals(DEXCOM_BLE_ADDRESS)) {

                String name = device.getName();
                String address = device.getAddress();
                String btclass = device.getBluetoothClass() + "";
                String bondstate = device.getBondState() + "";
                String type = device.getType() + "";
                //String uuid = device.getUuids().toString();
//
                deviceInfo =    "Name: " +              name + "\n" +
                                "Address: " +           address + "\n" +
                                "Bluetooth Class: " +   btclass + "\n" +
                                "Bond State: " +        bondstate + "\n" +
                                "Type: " +              type + "\n";
//                               // "UUID: " +              uuid + "\n";
//
                list.setText(deviceInfo);
                dexcom = device;
                break;
            }
        }

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

//        if (!isConnected) {
//            isConnected = true;
//            startBluetooth();
//        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}
