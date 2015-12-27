package com.httpcamera;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

public class HttpUtil {
    public static String readRequest(BufferedReader in) throws IOException {
        String request = "";
        while(true) {
            String chunk = in.readLine();
            if (chunk == null) break;
            request += chunk;
            if (chunk.equals("")) break;
        }
        return request;
    }

    public static void sendJpeg(OutputStream stream, byte[] picture) throws IOException {
        sendContent(stream, "200 OK", "image/jpeg", picture);
    }

    public static void sendError(OutputStream stream, String text) throws IOException {
        sendContent(stream, "500 Error", "text/plain", text.getBytes());
    }

    public static void sendContent(OutputStream stream, String status, String type, byte[] data) throws IOException {
        String length = Integer.toString(data.length);
        writeString(stream, "HTTP/1.0 " + status + "\r\n");
        writeString(stream, "Content-Type: " + type  + "\r\n");
        writeString(stream, "Content-Length: " + length + "\r\n");
        writeString(stream, "\r\n");
        stream.write(data);
    }

    public static void writeString(OutputStream stream, String s) throws IOException {
        stream.write(s.getBytes());
    }

    public static void close(Closeable socket) {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
