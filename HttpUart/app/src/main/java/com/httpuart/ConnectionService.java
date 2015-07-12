package com.httpuart;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class ConnectionService extends Service {
    public final String TAG = "ConnectionService";

    private Uart uart = null;
    private Thread thread = null;
    private ServerSocket server = null;

    public ConnectionService() {
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
            while (true) {
                String request = readRequest(in);
                if (request == null) break;
                Log.d(TAG, request);
                String response = servlet(request);
                out.write(response);
                out.flush();
                Log.d(TAG, response);
                if (request.contains("Connection: close")) break;
            }
            client.close();
            Log.d(TAG, client.toString() + " closed");
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }

    private String servlet(String request) {
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
        return response;
    }

    private String readRequest(BufferedReader in) {
        String request = "";
        try {
            while(true) {
                String chunk = in.readLine();
                if (chunk == null) break;
                request += chunk;
                Log.d(TAG, "Got chunk: " + chunk);
                if (chunk.equals("")) break;
            }
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
        return request;
    }

    private void activate(byte[] pins) {
        for(byte i: pins) {
            uart.write(new byte[]{0x0c, i, 0x01});
        }
        uart.write(new byte[]{0x0e});
    }

    private void forward() {
        uart.write(new byte[]{0x0c, 0x08, 0x00});
        uart.write(new byte[]{0x0c, 0x09, 0x01});
        activate(new byte[] {1, 3, 5, 7});
    }

    private void backward() {
        uart.write(new byte[]{0x0c, 0x08, 0x00});
        uart.write(new byte[]{0x0c, 0x09, 0x01});
        activate(new byte[] {0, 2, 4, 6});
    }

    private void left() {
        uart.write(new byte[]{0x0c, 0x08, 0x01});
        uart.write(new byte[]{0x0c, 0x09, 0x00});
        activate(new byte[] {1, 3, 4, 6});
    }

    private void right() {
        uart.write(new byte[]{0x0c, 0x08, 0x01});
        uart.write(new byte[]{0x0c, 0x09, 0x00});
        activate(new byte[] {0, 2, 5, 7});
    }

    private void stop() {
        uart.write(new byte[]{0x0c, 0x08, 0x00});
        uart.write(new byte[]{0x0c, 0x09, 0x00});
        for(byte i = 0; i < 8; i++) {
            uart.write(new byte[]{0x0c, i, 0x00});
        }
        uart.write(new byte[]{0x0e});
    }

    @Override
    public void onCreate() {
        Toast.makeText(this, "connection service created", Toast.LENGTH_SHORT).show();
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean usb = intent.getExtras().getString("UART") == "USB";
        uart = usb ? new UsbUart(this) : new BlueUart();
        Log.d(TAG, uart + " connected");
        uart.open();
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
        uart.close();
        Toast.makeText(this, "connection service destroyed", Toast.LENGTH_SHORT).show();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
