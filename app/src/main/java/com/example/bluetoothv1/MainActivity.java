package com.example.bluetoothv1;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static int DISCOVERY_REQUEST = 1;

    private Handler handler = new Handler();
    private ArrayList<BluetoothDevice> foundDevices = new ArrayList<BluetoothDevice>();
    private ArrayAdapter<BluetoothDevice> aa;
    private ListView list;

    private BluetoothAdapter bluetooth;
    private BluetoothSocket socket;
    private UUID uuid = UUID.fromString("a60f35f0-b93a-11de-8a39-08002009c666");

    BluetoothDevice demo_remoteDevice;
    Set<BluetoothDevice> bondedDevices;
    String remoteDeviceAddress = "";//demo

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Get the Bluetooth Adapter
        configureBluetooth();
        // Setup the ListView of discovered devices
        setupListView();
        // Setup search button
        setupSearchButton();
        // Setup listen button
        setupListenButton();
    }

    private void configureBluetooth() {
        bluetooth = BluetoothAdapter.getDefaultAdapter();
        String toastText;
        if (bluetooth.isEnabled()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            toastText = bluetooth.getName() + " : " + bluetooth.getAddress();
        } else {
            toastText = "No bluetooth";

            String actionStateChanged = BluetoothAdapter.ACTION_STATE_CHANGED;
            String actionRequestEnable = BluetoothAdapter.ACTION_REQUEST_ENABLE;
            registerReceiver(bluetoothState, new IntentFilter(actionStateChanged));
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            startActivityForResult(new Intent(actionRequestEnable), 0);
        }

        Toast.makeText(this, toastText, Toast.LENGTH_LONG).show();

        registerReceiver(discoveryMonitor, new IntentFilter(dFinished));

        registerReceiver(discoveryResult2, new IntentFilter(
                BluetoothDevice.ACTION_FOUND));
        if (!bluetooth.isDiscovering())
            bluetooth.startDiscovery();
    }

    private void setupListenButton() {
        Button listenButton = (Button) findViewById(R.id.escuchar);
        listenButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent disc = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                startActivityForResult(disc, DISCOVERY_REQUEST);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == DISCOVERY_REQUEST) {
            boolean isDiscoverable = resultCode > 0;
            if (isDiscoverable) {
                list.setVisibility(View.VISIBLE);
                String name = "bluetoothserver";
                try {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    final BluetoothServerSocket btserver = bluetooth.listenUsingRfcommWithServiceRecord(name, uuid);

                    AsyncTask<Integer, Void, BluetoothSocket> acceptThread = new AsyncTask<Integer, Void, BluetoothSocket>() {

                        @Override
                        protected BluetoothSocket doInBackground(Integer... params) {
                            try {
                                socket = btserver.accept(params[0] * 1000);
                                return socket;
                            } catch (IOException e) {
                                Log.d("BLUETOOTH", e.getMessage());
                            }
                            return null;
                        }

                        @Override
                        protected void onPostExecute(BluetoothSocket result) {
                            if (result != null)
                                switchUI();
                        }
                    };
                    acceptThread.execute(resultCode);
                } catch (IOException e) {
                    Log.d("BLUETOOTH", e.getMessage());
                }
            }
        }
    }

    private void setupListView() {
        aa = new ArrayAdapter<BluetoothDevice>(this,
                android.R.layout.simple_list_item_1, foundDevices);

        list = (ListView) findViewById(R.id.listView);
        list.setAdapter(aa);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0, View view, int index,
                                    long arg3) {

                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                Toast.makeText(MainActivity.this, foundDevices.get(index).getName(), Toast.LENGTH_SHORT).show();

                AsyncTask<Integer, Void, Void> connectTask = new AsyncTask<Integer, Void, Void>() {
                    @Override
                    protected Void doInBackground(Integer... params) {
                        try {
                            BluetoothDevice device = foundDevices.get(params[0]);
                            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                // TODO: Consider calling
                                //    ActivityCompat#requestPermissions
                                // here to request the missing permissions, and then overriding
                                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                //                                          int[] grantResults)
                                // to handle the case where the user grants the permission. See the documentation
                                // for ActivityCompat#requestPermissions for more details.
                                return null;
                            }
                            socket = device.createRfcommSocketToServiceRecord(uuid);
                            socket.connect();
                        } catch (IOException e) {
                            Log.d("BLUETOOTH_CLIENT", e.getMessage());
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void result) {
                        switchUI();
                    }
                };
                connectTask.execute(index);
            }
        });
    }

    private void setupSearchButton() {
        Button searchButton = (Button) findViewById(R.id.buscar);

        searchButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                registerReceiver(discoveryResult, new IntentFilter(
                        BluetoothDevice.ACTION_FOUND));

                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                if (!bluetooth.isDiscovering()) {
                    foundDevices.clear();
                    bluetooth.startDiscovery();
                }
            }
        });
    }

    private void switchUI() {
        final TextView messageText = (TextView) findViewById(R.id.mensaje);
        final EditText textEntry = (EditText) findViewById(R.id.mensajes);

        messageText.setVisibility(View.VISIBLE);
        list.setVisibility(View.GONE);
        textEntry.setEnabled(true);

        textEntry.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
                if ((keyEvent.getAction() == KeyEvent.ACTION_DOWN)
                        && (keyCode == KeyEvent.KEYCODE_DPAD_CENTER)) {
                    sendMessage(socket, textEntry.getText().toString());
                    textEntry.setText("");
                    return true;
                }
                if ((keyEvent.getAction() == KeyEvent.ACTION_DOWN)
                        && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    sendMessage(socket, textEntry.getText().toString());
                    textEntry.setText("");
                    return true;
                }

                return false;
            }
        });
        BluetoothSocketListener bsl = new BluetoothSocketListener(socket, handler,
                messageText);
        Thread messageListener = new Thread(bsl);
        messageListener.start();
    }

    private void sendMessage(BluetoothSocket socket, String msg) {
        OutputStream outStream;
        try {
            String toastText;
            toastText = msg;
            Toast.makeText(this, toastText, Toast.LENGTH_LONG).show();
            outStream = socket.getOutputStream();
            byte[] byteString = (msg + " ").getBytes();
            byteString[byteString.length - 1] = 0;
            outStream.write(byteString);
        } catch (IOException e) {
            Log.d("BLUETOOTH_COMMS", e.getMessage());
        }
    }

    BroadcastReceiver discoveryResult = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice remoteDevice;
            remoteDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            if (bluetooth.getBondedDevices().contains(remoteDevice)) {
                foundDevices.add(remoteDevice);
                aa.notifyDataSetChanged();
            }
        }
    };

    private class MessagePoster implements Runnable {
        private TextView textView;
        private String message;

        public MessagePoster(TextView textView, String message) {
            this.textView = textView;
            this.message = message;
        }

        public void run() {
            textView.setText(message);
        }
    }

    private class BluetoothSocketListener implements Runnable {

        private BluetoothSocket socket;
        private TextView textView;
        private Handler handler;

        public BluetoothSocketListener(BluetoothSocket socket, Handler handler,
                                       TextView textView) {
            this.socket = socket;
            this.textView = textView;
            this.handler = handler;
        }

        public void run() {
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            try {
                InputStream instream = socket.getInputStream();
                int bytesRead = -1;
                String message = "";
                while (true) {
                    message = "";
                    bytesRead = instream.read(buffer);
                    if (bytesRead != -1) {
                        while ((bytesRead == bufferSize) && (buffer[bufferSize - 1] != 0)) {
                            message = message + new String(buffer, 0, bytesRead);
                            bytesRead = instream.read(buffer);
                        }
                        message = message + new String(buffer, 0, bytesRead - 1);

                        handler.post(new MessagePoster(textView, message));
                        socket.getInputStream();
                    }
                }
            } catch (IOException e) {
                Log.d("BLUETOOTH_COMMS", e.getMessage());
            }
        }
    }


    BroadcastReceiver bluetoothState = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String prevStateExtra = BluetoothAdapter.EXTRA_PREVIOUS_STATE;
            String stateExtra = BluetoothAdapter.EXTRA_STATE;
            int state = intent.getIntExtra(stateExtra, -1);
            int previousState = intent.getIntExtra(prevStateExtra, -1);
            String tt = "";
            switch (state) {
                case (BluetoothAdapter.STATE_TURNING_ON): {
                    tt = "Bluetooth turning on";
                    break;
                }
                case (BluetoothAdapter.STATE_ON): {
                    tt = "Bluetooth on";
                    unregisterReceiver(this);
                    break;
                }
                case (BluetoothAdapter.STATE_TURNING_OFF): {
                    tt = "Bluetooth turning off";
                    break;
                }
                case (BluetoothAdapter.STATE_OFF): {
                    tt = "Bluetooth off";
                    break;
                }
                default:
                    break;
            }
            Toast.makeText(context, tt, Toast.LENGTH_LONG).show();
        }
    };

    String dStarted = BluetoothAdapter.ACTION_DISCOVERY_STARTED;
    String dFinished = BluetoothAdapter.ACTION_DISCOVERY_FINISHED;

    BroadcastReceiver discoveryMonitor = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (dStarted.equals(intent.getAction())) {
                // Discoveryhasstarted.
                Toast.makeText(getApplicationContext(), "Discovery Started ... ",
                        Toast.LENGTH_SHORT).show();
            } else if (dFinished.equals(intent.getAction())) {
                // Discoveryhascompleted.
                Toast.makeText(getApplicationContext(), "Discovery Completed ... ",
                        Toast.LENGTH_SHORT).show();
            }
        }
    };
    // registerReceiver(discoveryMonitor,new IntentFilter(dStarted));
    // registerReceiver(discoveryMonitor,new IntentFilter(dFinished));

    BroadcastReceiver discoveryResult2 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String remoteDeviceName = intent
                    .getStringExtra(BluetoothDevice.EXTRA_NAME);

            BluetoothDevice remoteDevice;
            remoteDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            remoteDeviceAddress = remoteDevice.getAddress();
            Toast.makeText(getApplicationContext(), "Discovered:" + remoteDeviceName,
                    Toast.LENGTH_SHORT).show();
            foundDevices.add(remoteDevice);
            aa.notifyDataSetChanged();
            // TODODosomethingwiththeremoteBluetoothDevice.

            //注意以下代码不能方在configblue下按顺序执行，remoteDeviceAddress还没取得值
            //配对
            demo_remoteDevice = bluetooth.getRemoteDevice(remoteDeviceAddress);//"01:23:77:35:2F:AA");
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            bondedDevices = bluetooth.getBondedDevices();
            //
            registerReceiver(discoveryPairResult, new IntentFilter(BluetoothDevice.ACTION_FOUND));

        }
    };
    // registerReceiver(discoveryResult,
    // newIntentFilter(BluetoothDevice.ACTION_FOUND));
    // if(!bluetooth.isDiscovering())
    // bluetooth.startDiscovery();

    BroadcastReceiver discoveryPairResult = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice remoteDevice = intent
                    .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (remoteDevice.equals(remoteDevice) && (bondedDevices.contains(remoteDevice))) {
                // TODO
                //Target device is paired and discoverable

                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                Toast.makeText(getApplicationContext(), "配对:" + remoteDevice.getName(),
                        Toast.LENGTH_SHORT).show();

            }
        };
    };

}