package com.httpcamera;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

import static com.httpcamera.HttpUtil.close;
import static com.httpcamera.HttpUtil.readRequest;
import static com.httpcamera.HttpUtil.sendError;
import static com.httpcamera.HttpUtil.sendJpeg;

class ClientHandler extends Handler {
    private Socket client;
    private Messenger messenger;
    private Handler cameraHandler;

    public ClientHandler(Socket socket, Handler camMessenger) {
        messenger = new Messenger(this);
        client = socket;
        cameraHandler = camMessenger;
    }

    public Socket handleClient() {
        try {
            return processRequest(client);
        } catch (Exception e) {
            e.printStackTrace();
            Looper looper = getLooper();
            if (looper != null) {
                looper.quit();
            }
            return null;
        }
    }

    @Override
    public void handleMessage(Message msg) {
        byte[] picture = msg.getData().getByteArray("picture");
        respond(client, picture);
    }

    public Socket processRequest(final Socket client) {
        String request = "";
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            // request = "GET / HTTP/1.0\r\n\r\n";
            request = readRequest(in);
        } catch (IOException e) {
            e.printStackTrace();
            close(client);
            return null;
        }
        if (request.equals("")) {
            close(client);
            return null;
        }
        if (cameraHandler != null) {
            Message msg = Message.obtain(cameraHandler, new Runnable() {
                @Override
                public void run() {

                }
            });

            msg.sendToTarget();
            return client;
        }
        return null;
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
