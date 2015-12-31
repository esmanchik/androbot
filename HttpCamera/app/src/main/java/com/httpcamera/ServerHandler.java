package com.httpcamera;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import static com.httpcamera.HttpUtil.close;
import static com.httpcamera.HttpUtil.readRequest;
import static com.httpcamera.HttpUtil.sendError;
import static com.httpcamera.HttpUtil.sendJpeg;

class ServerHandler extends Handler {
    private ServerSocket server;
    private Socket currentClient;
    private CameraHandler cameraHandler;

    public ServerHandler(ServerSocket serverSocket) {
        this(serverSocket, null);
    }

    public ServerHandler(ServerSocket serverSocket, CameraHandler camHandler) {
        currentClient = null;
        server = serverSocket;
        setCameraHandler(camHandler);
    }

    @Override
    public void handleMessage(Message msg) {
        byte[] picture = msg.getData().getByteArray("picture");
        respond(currentClient, picture);
        nextClient();
    }

    public void setCameraHandler(CameraHandler camHandler) {
        cameraHandler = camHandler;
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
            Looper looper = getLooper();
            if (looper != null) {
                looper.quit();
            }
            return null;
        }
    }

    public void processRequest(Socket client) {
        String request = "";
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            request = "GET / HTTP/1.0\r\n\r\n"; //readRequest(in);
            if (cameraHandler != null && !request.equals("")) {
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
}
