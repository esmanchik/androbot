package com.httpcamera;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.view.SurfaceView;

import java.io.IOException;
import java.net.ServerSocket;

public class HttpService extends Service {
    private volatile ServerThread thread;
    private volatile SurfaceView surfaceView;
    private volatile CameraHandler cameraHandler;

    public class LocalBinder extends Binder {
        public HttpService getService() {
            return HttpService.this;
        }
    }

    public HttpService() {
        thread = new ServerThread();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        surfaceView = SurfaceFactory.create(this);
        cameraHandler = new CameraHandler(surfaceView);
        cameraHandler.openCamera();
    }

    @Override
    public void onDestroy() {
        shutdown();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder();
    }

    public void start() {
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
    }

    public void shutdown() {
        cameraHandler.closeCamera();
        thread.shutdown();
    }

    public void setCameraHandler(CameraHandler camHandler) {
        cameraHandler = camHandler;
    }

    class ServerThread extends Thread {
        private volatile ServerSocket server;

        ServerThread() {
            server = null;
        }

        @Override
        public void run() {
            try {
                server = new ServerSocket(8080);
                Looper.prepare();
                ServerHandler handler = new ServerHandler(server);
                cameraHandler.setPictureHandler(handler);
                handler.setCameraHandler(cameraHandler);
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
                server = null;
            }
        }
    }
}
