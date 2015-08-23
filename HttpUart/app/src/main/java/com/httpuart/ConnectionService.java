package com.httpuart;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;

public class ConnectionService extends Service {
    public static final String COMMANDS = "COMMANDS";
    public static final String PORT = "PORT";
    public static final String UART = "UART";
    public static final String UART_USB = "USB";
    public static final String UART_BLUETOOTH = "Bluetooth";

    private PrintWriter log = null;
    private Uart uart = null;
    private Thread thread = null;
    private ServerSocket server = null;
    private Commands commands;
    private Handler handler;
    private byte[] picture = null;

    @Override
    public void onCreate() {
        logOpen();
        super.onCreate();
        dbg("connection service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message message) {
                    // This is where you do your work in the UI thread.
                    // Your worker tells you in the message what to do.
                    try {
                        takePhoto();
                    } catch (Exception e) {
                        xcpt(e);
                    }
                }
            };
            thread = connectionThread(intent.getExtras().getInt(PORT));
            thread.start();
            dbg("connection service started");
            boolean bt = intent.getExtras().getString(UART).equals(UART_BLUETOOTH);
            uart = bt ? new BlueUart() : new UsbUart(this);
            uart.open();
            dbg(uart + " opened");
            String commandsText = intent.getExtras().getString(COMMANDS);
            dbg("Got commands " + commandsText);
            commands = new Commands(uart, Commands.fromString(commandsText));
            String parsed = "";
            for(String command: commands.available()) {
                parsed += command + " ";
            }
            dbg("Parsed commands " + parsed);
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

    private Thread connectionThread(int port) {
        final int p = port;
        return new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    server = new ServerSocket(p);
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
                    xcpt(e);
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
                if (!request.contains("User-Agent: Control")) break;
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
        } else if (request.startsWith("GET /camera HTTP")) {
            response = camera();
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

    private String camera() {
        synchronized (handler) {
            picture = null;
        }
        handler.obtainMessage().sendToTarget();
        dbg("Send message to LooperThread");
        byte[] picture = null;
        for (int i = 0; picture == null && i < 30; i++) {
            SystemClock.sleep(100);
            synchronized (handler) {
                picture = this.picture;
            }
            dbg("Try "+ i +": picture is " + picture);
        }
        String response = "HTTP/1.0 500 Internal System Error\r\n\r\n";
        if (picture != null) {
            String length = Integer.toString(picture.length);
            dbg("Got picture of " + length + " bytes");
            String decoded = null;
            try {
                decoded = new String(picture, /*"ASCII"*/"ISO-8859-1");
                response = "HTTP/1.0 200 OK\r\nContent-Type: image/jpeg\r\n" +
                        "Content-Length: " + decoded.length() + "\r\n\r\n" + decoded;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return response;
    }

    @SuppressWarnings("deprecation")
    private void takePhoto() {
        final SurfaceView preview = new SurfaceView(this);
        SurfaceHolder holder = preview.getHolder();
        // deprecated setting, but required on Android versions prior to 3.0
        // holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            //The preview must happen at or after this point or takePicture fails
            public void surfaceCreated(SurfaceHolder holder) {
                dbg("Surface created");

                Camera camera = null;

                try {
                    camera = Camera.open();
                    dbg("Opened camera");

                    try {
                        camera.setPreviewDisplay(holder);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    camera.startPreview();
                    dbg("Started preview");

                    camera.takePicture(null, null, new Camera.PictureCallback() {
                        @Override
                        public void onPictureTaken(byte[] data, Camera camera) {
                            dbg("Took picture of " + Integer.toString(data.length) + " bytes");
                            spit("httpuart.jpg", data);
                            dbg("Saved picture to file");
                            synchronized (handler) {
                                ConnectionService.this.picture = data;
                            }
                            dbg("Saved picture to property");
                            camera.release();
                        }
                    });
                } catch (Exception e) {
                    if (camera != null)
                        camera.release();
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }
        });

        WindowManager wm = (WindowManager)this
                .getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                1, 1, //Must be at least 1x1
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                0,
                //Don't know if this is a safe default
                PixelFormat.UNKNOWN);

        //Don't set the preview visibility to GONE or INVISIBLE
        wm.addView(preview, params);
    }

    private void spit(String fileName, byte[] data) {
        try {
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
            FileOutputStream stream = new FileOutputStream(file);
            stream.write(data);
            stream.close();
        } catch(Exception e) {
            xcpt(e);
        }
    }

    void logOpen() {
        try {
            String fileName = "httpuart.log.txt";
            File logFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
            FileOutputStream logStream = new FileOutputStream(logFile);
            //FileOutputStream logStream = openFileOutput(fileName, Context.MODE_APPEND | Context.MODE_WORLD_READABLE);
            log = new PrintWriter(logStream);
        } catch (Exception e) {
            Log.e("HTTPUART", "Failed to open log: " + e.toString());
        }
    }

    void logWrite(String msg) {
        if (log == null) return;
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
