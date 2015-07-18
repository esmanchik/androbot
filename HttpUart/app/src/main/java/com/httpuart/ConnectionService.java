package com.httpuart;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class ConnectionService extends Service {
    public static final String UART = "UART";
    public static final String UART_USB = "USB";
    public static final String UART_BLUETOOTH = "Bluetooth";

    private PrintWriter log = null;
    private Uart uart = null;
    private Thread thread = null;
    private ServerSocket server = null;
    private Commands commands;

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
                    } catch (Exception e) {
                        xcpt(e);
                        server.close();
                    }
                } catch (Exception e) {
                    xcpt(e);
                }
            }
        });
    }

    @Override
    public void onCreate() {
        logOpen();
        dbg("connection service created");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            commands = new Commands(uart, Commands.quadrobot());
            boolean bt = intent.getExtras().getString(UART).equals(UART_BLUETOOTH);
            uart = bt ? new BlueUart() : new UsbUart(this);
            uart.open();
            dbg(uart + " opened");
            thread.start();
            dbg("connection service started");
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

    private void serve(Socket client) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
            while (true) {
                String request = readRequest(in);
                if (request.equals("")) break;
                dbg(request);
                String response = "HTTP/1.0 500 Internal Server Error\r\n\r\nInternal Server Error";
                try {
                    response = servlet(request);
                } catch (Exception e) {
                    try {
                        uart.close();
                    } catch (Exception ce) {
                        xcpt(ce);
                    }
                    uart.open();
                    dbg(uart + " reopened");
                }
                out.write(response);
                out.flush();
                dbg(response);
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
        } else {
            for(String command: commands.available()) {
                if (request.startsWith("GET /" + command)) {
                    commands.execute(command);
                    response = "HTTP/1.0 200 OK\r\n\r\n" +
                            "Command " + command + " executed";
                }
            }
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
                dbg("Got chunk: " + chunk);
                if (chunk.equals("")) break;
            }
        } catch (IOException e) {
            xcpt(e);
        }
        return request;
    }

    void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    void logOpen() {
        try {
            String fileName = "httpuart.log.txt";
            File logFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
            FileOutputStream logStream = new FileOutputStream(logFile);
            //FileOutputStream logStream = openFileOutput(fileName, Context.MODE_APPEND | Context.MODE_WORLD_READABLE);
            log = new PrintWriter(logStream);
        } catch (Exception e) {
            toast("Failed to open log: " + e.toString());
        }
    }

    void logWrite(String msg) {
        log.println(msg);
        log.flush();
    }

    void dbg(String msg) {
        Log.d("HTTPUART", msg);
        logWrite("DEBUG: " + msg);
    }

    void err(String msg) {
        Log.e("HTTPUART", msg);
        logWrite("ERROR: " + msg);
    }

    void xcpt(Exception e) {
        err(formatThrowable(e));
    }

    private String formatThrowable(Throwable e) {
        String trace = "";
        for (StackTraceElement t: e.getStackTrace()) {
            trace += t.toString() + "\n";
        }
        String msg = e.toString() + " at " + trace;
        if (e.getCause() != null) {
            msg += "\nCaused by " + formatThrowable(e.getCause());
        }
        return msg;
    }
}
