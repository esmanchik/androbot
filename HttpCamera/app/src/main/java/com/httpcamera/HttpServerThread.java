package com.httpcamera;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

public class HttpServerThread extends Thread {
    private ServerSocket server;
    private MainActivity.CameraHandler cameraHandler;
    private LinkedList<Socket> clients;
    private JpegOutputHandler pictureHandler;

    public HttpServerThread() {
        cameraHandler = null;
        clients = new LinkedList<>();
    }

    public void setCameraHandler(MainActivity.CameraHandler camHandler) {
        cameraHandler = camHandler;
    }

    @Override
    public void run() {
        if (cameraHandler == null) {
            throw new RuntimeException("Please set camera handler first");
        }
        try {
            server = new ServerSocket(8080);
            Looper.prepare();
            pictureHandler = new JpegOutputHandler();
            cameraHandler.setPictureHandler(pictureHandler);
            pictureHandler.acceptClient();
            Looper.loop();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void pushClient(Socket client) {
        String request = "";
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            request = readRequest(in);
            cameraHandler.obtainMessage().sendToTarget();
            clients.add(client);
            if (request.contains("User-Agent: Control")) {
                // keep connection alive
            }
        } catch (IOException e) {
            e.printStackTrace();
            close(client);
        }
    }

    class JpegOutputHandler extends Handler {
        public void acceptClient() {
            try {
                Socket client = server.accept();
                pushClient(client);
                //Thread clientThread = new Thread(job);
                //clientThread.start();
            } catch (Exception e) {
                e.printStackTrace();
                close(server);
                getLooper().quit();
            }
        }

        public void handleMessage(Message msg) {
            if (clients.size() < 1) {
                // warning
                return;
            }
            Socket client = clients.removeFirst();
            try {
                OutputStream stream = client.getOutputStream();
                byte[] picture = msg.getData().getByteArray("picture");
                if (picture == null) {
                    sendError("No picture taken", stream);
                } else {
                    sendJpeg(picture, stream);
                }
            } catch (IOException e) {
                e.printStackTrace();
                close(client);
            }
            acceptClient();
        }
    }

    private String readRequest(BufferedReader in) throws IOException {
        String request = "";
        while(true) {
            String chunk = in.readLine();
            if (chunk == null) break;
            request += chunk;
            if (chunk.equals("")) break;
        }
        return request;
    }

    private void sendJpeg(byte[] picture, OutputStream stream) throws IOException {
        String length = Integer.toString(picture.length);
        String contentLength = "Content-Length: " + length + "\r\n";
        stream.write("HTTP/1.0 200 OK\r\n".getBytes());
        stream.write("Content-Type: image/jpeg\r\n".getBytes());
        stream.write(contentLength.getBytes());
        stream.write("\r\n".getBytes());
        stream.write(picture);
        stream.flush();
    }

    private void sendError(String text, OutputStream stream) throws IOException {
        String length = Integer.toString(text.length());
        String contentLength = "Content-Length: " + length + "\r\n";
        stream.write("HTTP/1.0 500 Error\r\n".getBytes());
        stream.write("Content-Type: text/plain\r\n".getBytes());
        stream.write(contentLength.getBytes());
        stream.write("\r\n".getBytes());
        stream.write(text.getBytes());
        stream.flush();
    }

    private static void close(Closeable socket) {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
