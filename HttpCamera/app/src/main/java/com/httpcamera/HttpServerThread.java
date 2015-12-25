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
                    sendError(stream, "No picture taken");
                } else {
                    sendJpeg(stream, picture);
                }
                stream.flush();
                close(client); // if not keep alive
                // otherwise read one more request
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

    private void sendJpeg(OutputStream stream, byte[] picture) throws IOException {
        sendContent(stream, "200 OK", "image/jpeg", picture);
    }

    private void sendError(OutputStream stream, String text) throws IOException {
        sendContent(stream, "500 Error", "text/plain", text.getBytes());
    }

    private void sendContent(OutputStream stream, String status, String type, byte[] data) throws IOException {
        String length = Integer.toString(data.length);
        writeString(stream, "HTTP/1.0 " + status + "\r\n");
        writeString(stream, "Content-Type: " + type  + "\r\n");
        writeString(stream, "Content-Length: " + length + "\r\n");
        writeString(stream, "\r\n");
        stream.write(data);
    }

    private void writeString(OutputStream stream, String s) throws IOException {
        stream.write(s.getBytes());
    }

    private static void close(Closeable socket) {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}