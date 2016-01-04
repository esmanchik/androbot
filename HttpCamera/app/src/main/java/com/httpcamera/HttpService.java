package com.httpcamera;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.os.Messenger;
import android.view.SurfaceView;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import static com.httpcamera.HttpUtil.close;

public class HttpService extends Service {
    // private volatile CameraThread cameraThread;
    private volatile CameraHandler cameraHandler;
    private volatile Messenger cameraMessenger;
    private volatile ServerThread serverThread;
    private volatile SurfaceView surfaceView;

    public class LocalBinder extends Binder {
        public HttpService getService() {
            return HttpService.this;
        }
    }

    public HttpService() {
        //cameraThread = new CameraThread();
        serverThread = new ServerThread();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        surfaceView = SurfaceFactory.create(this);
        cameraHandler = new CameraHandler(surfaceView);
        cameraMessenger = new Messenger(cameraHandler);
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
        if (serverThread.isAlive()) {
            serverThread.shutdown();
            try {
                serverThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        /*if (cameraThread.isAlive()) {
            cameraThread.shutdown();
            try {
                cameraThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }*/
        try {
            //cameraThread = new CameraThread();
            //cameraThread.start();
            serverThread = new ServerThread();
            serverThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        serverThread.shutdown();
    }

    class ServerThread extends Thread {
        private volatile ServerSocket server;

        ServerThread() {
            server = null;
        }

        @Override
        public void run() {
            LinkedList<Thread> clients = new LinkedList<>();
            try {
                // cameraThread.start();
                // CameraHandler cameraHandler = cameraThread.getCameraHandlerFuture().get();
                server = new ServerSocket(8080);
                while(true) {
                    Socket client = server.accept();
                    Thread clientThread = new Thread(new ClientTask(client, cameraHandler));
                    clients.add(clientThread);
                    clientThread.start();
                }
            } catch (Exception e) {
                e.printStackTrace();
                close(server);
                for (Thread clientThread : clients) {
                    clientThread.interrupt();
                }
                // cameraThread.shutdown();
            }
        }

        public void shutdown() {
            if (server != null) {
                close(server);
                server = null;
            }
        }
    }

    class ClientThread extends Thread {
        private volatile Socket client;
        private Messenger cameraHandler;

        ClientThread(Socket socket, Messenger handler) {
            client = socket;
            cameraHandler = handler;
        }

        @Override
        public void run() {
            try {
                Looper.prepare();
                // new ClientHandler(client, cameraHandler).handleClient();
                Looper.loop();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                close(client);
            }
        }

        public void shutdown() {
            close(client);
        }
    }

    class CameraThread extends Thread {
        private volatile CameraHandler cameraHandler;
        private volatile FutureTask<CameraHandler> future;

        CameraThread() {
            cameraHandler = null;
            future = new FutureTask<CameraHandler>(new Callable<CameraHandler>() {
                @Override
                public CameraHandler call() throws Exception {
                    cameraHandler = new CameraHandler(surfaceView);
                    cameraHandler.openCamera();
                    return cameraHandler;
                }
            });
        }

        public FutureTask<CameraHandler> getCameraHandlerFuture() {
            return future;
        }

        @Override
        public void run() {
            Looper.prepare();
            future.run();
            Looper.loop();
            cameraHandler.closeCamera();
        }

        public void shutdown() {
            if (cameraHandler != null) {
                Looper looper = cameraHandler.getLooper();
                if (looper != null) {
                    looper.quit();
                }
            }
        }
    }
}
