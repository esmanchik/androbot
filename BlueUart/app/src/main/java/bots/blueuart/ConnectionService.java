package bots.blueuart;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;

public class ConnectionService extends Service {
    public final String TAG = "ConnectionService";

    class HC06 {
        private final UUID MY_UUID = UUID.fromString("0001101-0000-1000-8000-00805F9B34FB");
        private BluetoothSocket socket = null;
        private OutputStream stream = null;

        public void connect() {
            String deviceName = "HC-06";
            String address = null;
            try {
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                for(BluetoothDevice d: adapter.getBondedDevices()){
                    if (d.getName().equals(deviceName)) address = d.getAddress();
                }
                Log.d(TAG, address);
                BluetoothDevice device = adapter.getRemoteDevice(address); // Get the BluetoothDevice object
                socket = device.createRfcommSocketToServiceRecord(MY_UUID);
                adapter.cancelDiscovery();
                socket.connect();
                stream = socket.getOutputStream();
                Log.d(TAG, socket.toString());
            } catch (Exception e){
                Log.e(TAG, e.toString());
            }
        }

        public void write(byte[] buffer) {
            try {
                stream.write(buffer);
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }

        public void close() {
            try {
                socket.close();
                Log.d(TAG, socket.toString() + " closed");
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
            stream = null;
            socket = null;
        }
    }

    private HC06 hc06 = null;
    private Thread thread = null;
    private ServerSocket server = null;

    public ConnectionService() {
        hc06 = new HC06();
        Log.d(TAG, hc06 + " connected");
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    server = new ServerSocket(8080);
                    Log.d(TAG, server.toString() + " started");
                    try {
                        while(true) {
                            Socket client = server.accept();
                            Log.d(TAG, "Accepted " + client.toString());
                            //Thread clientThread = new Thread(new Runnable() {
                            //    @Override
                            //    public void run() {
                            serve(client);
                            //    }
                            //});
                            //clientThread.start();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, e.toString());
                        server.close();
                    }
                } catch (IOException e) {
                    Log.e(TAG, e.toString());
                }
            }
        });
    }

    private void serve(Socket client) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
            String request = readRequest(in);
            Log.d(TAG, request);
            String response = "HTTP/1.0 404 Not Found\r\n\r\nNot Found";
            if (request.startsWith("GET /favicon.ico HTTP")) {
                // Not Found
            } else if (request.startsWith("GET /s")) {
                stop();
                response = "HTTP/1.0 200 OK\r\n\r\nStopped";
            } else if (request.startsWith("GET /f")) {
                stop();
                forward();
                response = "HTTP/1.0 200 OK\r\n\r\nMoving Forward";
            } else if (request.startsWith("GET /b")) {
                stop();
                backward();
                response = "HTTP/1.0 200 OK\r\n\r\nMoving Backward";
            } else if (request.startsWith("GET /l")) {
                stop();
                left();
                response = "HTTP/1.0 200 OK\r\n\r\nMoving Left";
            } else if (request.startsWith("GET /r")) {
                stop();
                right();
                response = "HTTP/1.0 200 OK\r\n\r\nMoving Right";
            }
            out.write(response);
            out.flush();
            Log.d(TAG, response);
            client.close();
            Log.d(TAG, client.toString() + " closed");
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }

    private String readRequest(BufferedReader in) {
        String request = "";
        try {
            String chunk = in.readLine();
            Log.d(TAG, "Got chunk: " + chunk);
            if (chunk != null) request += chunk;
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
        return request;
    }

    private void activate(byte[] pins) {
        for(byte i: pins) {
            hc06.write(new byte[]{0x0c, i, 0x01});
        }
        hc06.write(new byte[]{0x0e});
    }

    private void forward() {
        hc06.write(new byte[]{0x0c, 0x08, 0x00});
        hc06.write(new byte[]{0x0c, 0x09, 0x01});
        activate(new byte[] {1, 3, 5, 7});
    }

    private void backward() {
        hc06.write(new byte[]{0x0c, 0x08, 0x00});
        hc06.write(new byte[]{0x0c, 0x09, 0x01});
        activate(new byte[] {0, 2, 4, 6});
    }

    private void left() {
        hc06.write(new byte[]{0x0c, 0x08, 0x01});
        hc06.write(new byte[]{0x0c, 0x09, 0x00});
        activate(new byte[] {1, 3, 4, 6});
    }

    private void right() {
        hc06.write(new byte[]{0x0c, 0x08, 0x01});
        hc06.write(new byte[]{0x0c, 0x09, 0x00});
        activate(new byte[] {0, 2, 5, 7});
    }

    private void stop() {
        hc06.write(new byte[]{0x0c, 0x08, 0x00});
        hc06.write(new byte[]{0x0c, 0x09, 0x00});
        for(byte i = 0; i < 8; i++) {
            hc06.write(new byte[]{0x0c, i, 0x00});
        }
        hc06.write(new byte[]{0x0e});
    }

    @Override
    public void onCreate() {
        Toast.makeText(this, "connection service created", Toast.LENGTH_SHORT).show();
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        hc06.connect();
        thread.start();
        Toast.makeText(this, "connection service started", Toast.LENGTH_SHORT).show();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        thread.interrupt();
        try {
            server.close();
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
        hc06.close();
        Toast.makeText(this, "connection service destroyed", Toast.LENGTH_SHORT).show();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
