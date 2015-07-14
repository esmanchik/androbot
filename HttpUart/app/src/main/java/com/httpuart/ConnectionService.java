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
    private Uart uart = null;
    private Thread thread = null;
    private ServerSocket server = null;

    public ConnectionService() {
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    server = new ServerSocket(8080);
                    dbg(server.toString() + " started");
                    try {
                        while(true) {
                            Socket client = server.accept();
                            dbg("Accepted " + client.toString());
                            //Thread clientThread = new Thread(new Runnable() {
                            //    @Override
                            //    public void run() {
                            serve(client);
                            //    }
                            //});
                            //clientThread.start();
                        }
                    } catch (IOException e) {
                        xcpt(e);
                        server.close();
                    }
                } catch (IOException e) {
                    xcpt(e);
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
                if (request.equals("")) break;
                dbg(request);
                String response = servlet(request);
                out.write(response);
                out.flush();
                // dbg(response);
                if (request.contains("Connection: close")) break;
            }
            client.close();
            dbg(client.toString() + " closed");
        } catch (Exception e) {
            xcpt(e);
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
                // dbg("Got chunk: " + chunk);
                if (chunk.equals("")) break;
            }
        } catch (IOException e) {
            xcpt(e);
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
        activate(new byte[]{1, 3, 5, 7});
    }

    private void backward() {
        uart.write(new byte[]{0x0c, 0x08, 0x00});
        uart.write(new byte[]{0x0c, 0x09, 0x01});
        activate(new byte[]{0, 2, 4, 6});
    }

    private void left() {
        uart.write(new byte[]{0x0c, 0x08, 0x01});
        uart.write(new byte[]{0x0c, 0x09, 0x00});
        activate(new byte[]{1, 3, 4, 6});
    }

    private void right() {
        uart.write(new byte[]{0x0c, 0x08, 0x01});
        uart.write(new byte[]{0x0c, 0x09, 0x00});
        activate(new byte[]{0, 2, 5, 7});
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
        // dbg("connection service created");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            boolean usb = intent.getExtras().getString("UART") == "USB";
            uart = usb ? new UsbUart(this) : new BlueUart();
            uart.open();
            dbg(uart + " opened");
            thread.start();
            // dbg("connection service started");
        } catch(Exception e) {
            xcpt(e);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        try {
            thread.interrupt();
            server.close();
            uart.close();
            dbg("connection service destroyed");
        } catch(Exception e) {
            xcpt(e);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    void dbg(String msg) {
        toast(msg);
    }

    void err(String msg) {
        toast("Error: " + msg);
    }

    void xcpt(Exception e) {
        err(e.toString() + " at " + e.getStackTrace().toString());
    }
}
