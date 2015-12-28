package com.httpcamera;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import static com.httpcamera.HttpUtil.*;

public class HttpSurfaceService extends Service {
    static class ServerHandler extends Handler {
        private ServerSocket server;
        private Socket currentClient;
        private CameraHandler cameraHandler;

        public ServerHandler(Context context, ServerSocket serverSocket) {
            cameraHandler = new CameraHandler(context, this);
            server = serverSocket;
            currentClient = null;
        }

        public void nextClient() {
            currentClient = acceptClient(server);
        }

        private Socket acceptClient(ServerSocket server) {
            try {
                Socket client = server.accept();
                processRequest(client);
                return client;
            } catch (Exception e) {
                e.printStackTrace();
                close(server);
                getLooper().quit();
                return null;
            }
        }

        public void processRequest(Socket client) {
            String request = "";
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                request = readRequest(in);
                if (!request.equals("")) {
                    cameraHandler.obtainMessage().sendToTarget();
                }
            } catch (IOException e) {
                e.printStackTrace();
                close(client);
            }
        }

        public void respond(Socket client, byte[] picture) {
            try {
                OutputStream stream = client.getOutputStream();
                if (picture == null) {
                    sendError(stream, "No picture taken");
                } else {
                    sendJpeg(stream, picture);
                }
                stream.flush();
                close(client);
            } catch (IOException e) {
                e.printStackTrace();
                close(client);
            }
        }

        @Override
        public void handleMessage(Message msg) {
            byte[] picture = msg.getData().getByteArray("picture");
            respond(currentClient, picture);
            nextClient();
        }
    }

    static class CameraHandler extends Handler implements SurfaceHolder.Callback {
        private SurfaceView surface;
        private Camera camera;
        private Handler pictureHandler;

        public CameraHandler(Context context, Handler picHandler) {
            pictureHandler = picHandler;
            surface = new SurfaceView(context);
            surface.getHolder().addCallback(this);
            WindowManager wm = (WindowManager)context
                    .getSystemService(Context.WINDOW_SERVICE);
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    1, 1, //Must be at least 1x1
                    WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                    0,
                    //Don't know if this is a safe default
                    PixelFormat.UNKNOWN);

            //Don't set the preview visibility to GONE or INVISIBLE
            wm.addView(surface, params);
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            camera = Camera.open(0);
            camera.setDisplayOrientation(90);
            if (camera == null)
            {
                return;
            }
            try {
                camera.setPreviewDisplay(surface.getHolder());
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            camera.startPreview();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if (camera == null)
            {
                return;
            }
            camera.stopPreview();
            camera.release();
            camera = null;
        }

        @Override
        public void handleMessage(Message msg) {
            if (camera == null) {
                sendPicture(null);
            } else {
                camera.takePicture(null, null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        sendPicture(data);
                        camera.startPreview();
                    }
                });

            }
        }

        private void sendPicture(byte[] picture) {
            Message pictureMessage = pictureHandler.obtainMessage();
            pictureMessage.getData().putByteArray("picture", picture);
            pictureMessage.sendToTarget();
        }
    }

    class ServerThread extends Thread {
        private ServerHandler handler;
        private volatile ServerSocket server;

        ServerThread() {
            server = null;
        }

        @Override
        public void run() {
            try {
                server = new ServerSocket(8080);
                Looper.prepare();
                handler = new ServerHandler(HttpSurfaceService.this, server);
                handler.nextClient();
                Looper.loop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void shutdown() {
            if (server != null) {
                try {
                    server.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private volatile ServerThread thread;
    private LocalBinder localBinder;

    public static class LocalBinder extends Binder {
        public HttpSurfaceService service;
    }

    public HttpSurfaceService() {
        thread = new ServerThread();
        localBinder = new LocalBinder();
        localBinder.service = this;
    }

    @Override
    public void onCreate() {

    }

    @Override
    public void onDestroy() {
        if (thread != null) {
            thread.shutdown();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (thread.isAlive()) {
            thread.shutdown();
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            thread = new ServerThread();
            thread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return localBinder;
    }
}
