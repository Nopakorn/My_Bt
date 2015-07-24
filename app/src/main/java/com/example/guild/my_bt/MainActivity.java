package com.example.guild.my_bt;

import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.ParcelUuid;
import android.support.v7.app.ActionBarActivity;
import android.app.ActionBar;
import android.view.View;
import android.view.View.OnClickListener;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.util.Log;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.Button;
import android.widget.Toast;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends ActionBarActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    //private static final UUID MY_UUID = UUID.fromString("0000110A-0000-1000-8000-00805F9B34FB");
    private UUID applicationUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    //A2DP 00001101-0000-1000-8000-00805F9B34FB
    // Unique UUID for this application
    private static final UUID MY_UUID_SECURE =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    public static final int SUCCESS_CONNECT = 0;
    private static final int MESSAGE_READ = 2;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice device;
    private Handler mHandler;
    ArrayAdapter<String> mArrayAdapter;
    private Button onBtn;
    private Button offBtn;
    private Button findBtn;
    private Button pairedBtn;
    private TextView textStatus;
    private ListView listText;
//    private Toast toast;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textStatus = (TextView) findViewById(R.id.textStatus);
        onBtn = (Button) findViewById(R.id.turnOn);
        offBtn = (Button) findViewById(R.id.turnOff);
        findBtn = (Button) findViewById(R.id.find);
        pairedBtn = (Button) findViewById(R.id.paired);
        listText = (ListView) findViewById(R.id.listView);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            textStatus.setText("Status: not supported");
            Log.d("test","Object is NULL");
        }else{
            Log.d("test", "Object already exist: " + mBluetoothAdapter.getName());

            if (mBluetoothAdapter.isEnabled()){
                textStatus.setText("Status: Enabled");
                Toast.makeText(getApplicationContext(),"Bluetooth is already enable", Toast.LENGTH_LONG).show();
            }

            onBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    turnOn(v);

                }
            });

            offBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    turnOff(v);
                }
            });

            findBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    //find(v);
                    search(v);
                }
            });

            pairedBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    find(v);
                }
            });

            mArrayAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1);
            listText.setAdapter(mArrayAdapter);

            listText.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String info = (String) parent.getItemAtPosition(position);
                    String address = info.substring(info.length() - 17);
                    device = mBluetoothAdapter.getRemoteDevice(address);

                    ConnectThread connect = new ConnectThread(device);

                    connect.start();
                    Log.d("item", "item was clicked : " + info);
                    Log.d("item", "device address : " + address);


                }
            });
            registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            mHandler = new Handler(){
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    switch (msg.what){
                        case SUCCESS_CONNECT:
                            Toast.makeText(getApplicationContext(),"CONNECT SUCCESS",Toast.LENGTH_LONG).show();
                            break;
                        default:
                            Toast.makeText(getApplicationContext(),"CANNOT CONNECTED",Toast.LENGTH_LONG).show();
                            break;
                    }
                }
            };
        }

    }
    public ParcelUuid[] servicesFromDevice(BluetoothDevice device) {
        try {
            Class cl = Class.forName("android.bluetooth.BluetoothDevice");
            Class[] par = {};
            Method method = cl.getMethod("getUuids", par);
            Object[] args = {};
            ParcelUuid[] retval = (ParcelUuid[]) method.invoke(device, args);
            return retval;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    public void turnOn(View v){
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            Toast.makeText(getApplicationContext(),"Bluetooth is enable", Toast.LENGTH_LONG).show();
        }else{
            Log.d("test", "Already enable");
            textStatus.setText("Status: Enabled");
            Toast.makeText(getApplicationContext(),"Bluetooth is already enable", Toast.LENGTH_LONG).show();
        }
    }

    public void turnOff(View v){
        mBluetoothAdapter.disable();
        textStatus.setText("Status: Disconnected");
        Log.d("test", "Disable bluetooth");
        Toast.makeText(getApplicationContext(),"Bluetooth is disable", Toast.LENGTH_LONG).show();
    }

    public void find(View v){
       // mArrayAdapter.clear();
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                Log.d("test","paired device: "+device.getName());
            }
        }
    }

    final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                Log.d("test", "search device: " + device.getName());
            }
        }
    };


    public void search(View v){
        if(mBluetoothAdapter.isDiscovering()){
            mBluetoothAdapter.cancelDiscovery();
        }else{
            mArrayAdapter.clear();
            mBluetoothAdapter.startDiscovery();

            registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));

        }
//        mBluetoothAdapter.cancelDiscovery();
//        mBluetoothAdapter.startDiscovery();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_ENABLE_BT){
            textStatus.setText("Status: Enabled");
        }else{
            textStatus.setText("Status: Disabled");
        }
    }
    //Connection part
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;
            boolean secure = true;

            mSocketType = secure ? "Secure" : "In Secure";

            ParcelUuid[] uuids = servicesFromDevice(mmDevice);
            // Get a BluetoothSocket to connect with the given BluetoothDevice

            try{
                // MY_UUID is the app's UUID string, also used by the server code
                //tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                Method m = device.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
                tmp = (BluetoothSocket) m.invoke(device,Integer.valueOf(1));
            } catch (NoSuchMethodException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
//            try {
//
//                    tmp = mmDevice.createRfcommSocketToServiceRecord(uuids[0].getUuid());
//
//            } catch ( IOException e ) {
//                Log.e( "Bluetooth Socket", "Bluetooth not available, or insufficient permissions" );
//            } catch ( NullPointerException e ) {
//                Log.e( "Bluetooth Socket", "Null Pointer One" );
//            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
                Log.d("test", "connect success");

            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                Log.d("test","can't connect");
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                return;
            }

            // Do work to manage the connection (in a separate thread)
           // manageConnectedSocket(mmSocket, mmDevice);
            //just wanna check it connected success or not ?
            mHandler.obtainMessage(SUCCESS_CONNECT).sendToTarget();
        }

        public void manageConnectedSocket(BluetoothSocket mmSocket, BluetoothDevice mmDevice){
            String checkSocket = mmSocket.getRemoteDevice().getName();
            Log.d("test","socket device: "+checkSocket);
            Log.d("test","device: "+mmDevice.getName());

        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }


    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main Activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main Activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
}
