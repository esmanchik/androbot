package com.httpcamera;

import android.os.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

import static com.httpcamera.HttpUtil.close;
import static com.httpcamera.HttpUtil.readRequest;
import static com.httpcamera.HttpUtil.sendJpeg;

public class ClientTask implements Runnable {
    private Socket client;
    private CameraHandler cameraHandler;

    public ClientTask(Socket socket, CameraHandler handler) {
        client = socket;
        cameraHandler = handler;
    }

    @Override
    public void run() {
        String request = "";
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            // request = "GET / HTTP/1.0\r\n\r\n";
            request = readRequest(in);
            if (request.equals("")) {
                close(client);
                return;
            }
            if (cameraHandler != null) {
                class PictureTask implements Runnable, CameraHandler.PictureHolder {
                    private volatile byte[] picture = null;

                    @Override
                    public void run() {
                        try {
                            cameraHandler.shot(this);
                            Thread.sleep(1500); // don't tease system to close app
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void setPicture(byte[] pic) {
                        picture = pic;
                    }

                    public byte[] waitPicture() throws InterruptedException {
                        while(picture == null) {
                            Thread.sleep(100);
                        }
                        return picture;
                    }
                }
                PictureTask pictureTask = new PictureTask();
                Message.obtain(cameraHandler, pictureTask).sendToTarget();
                try {
                    byte[] picture = pictureTask.waitPicture();
                    OutputStream stream = client.getOutputStream();
                    sendJpeg(stream, picture);
                    stream.flush();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            close(client);
        } catch (Exception e) {
            e.printStackTrace();
            close(client);
        }
    }
}
