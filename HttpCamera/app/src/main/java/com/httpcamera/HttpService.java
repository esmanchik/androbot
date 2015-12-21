package com.httpcamera;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;

public class HttpService extends Service {
    private HttpServerThread thread;
    private LocalBinder localBinder;

    public static class LocalBinder extends Binder {
        public HttpService service;
    }

    public HttpService() {
        thread = new HttpServerThread();
        localBinder = new LocalBinder();
        localBinder.service = this;
    }

    @Override
    public void onCreate() {

    }

    @Override
    public void onDestroy() {
        if (thread != null) {
            thread.interrupt();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return localBinder;
    }

    public void start(MainActivity.CameraHandler camHandler) {
        thread.setCameraHandler(camHandler);
        thread.start();
    }
}
