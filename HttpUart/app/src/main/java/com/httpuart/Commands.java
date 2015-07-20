package com.httpuart;

import com.hoho.android.usbserial.util.HexDump;

import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;

public class Commands {
    public static class Map extends HashMap<String, byte[]> {};

    private Map map;
    private Uart uart;

    public Commands(Uart uart, Map map) {
        this.uart = uart;
        this.map = map;
    }

    public String[] available() {
        String[] result = new String[map.keySet().size()];
        return map.keySet().toArray(result);
    }

    public void execute(String command) {
        if (!map.containsKey(command)) {
            throw new RuntimeException("No such command " + command);
        }
        byte[] bytes = map.get(command);
        uart.write(bytes);
    }

    public static Map quadrobot() {
        Map map = new Map();
        map.put("s", stop());
        map.put("f", forward());
        map.put("b", backward());
        map.put("l", left());
        map.put("r", right());
        return map;
    }

    public static Map fromString(String content) {
        Map map = new Map();
        try {
            JSONObject json = new JSONObject(content);
            Iterator<String> keys = json.keys();
            for(String key = keys.next(); keys.hasNext(); key = keys.next()) {
                String hex = json.getString(key);
                map.put(key, HexDump.hexStringToByteArray(hex));
            }
        } catch(Exception e) {
            throw new RuntimeException("Failed to load commands from " + content);
        }
        return map;
    }

    private static byte[] activate(byte[] pins) {
        byte[] bytes = new byte[pins.length * 3 + 1];
        int offset = 0;
        for(byte i: pins) {
            bytes[offset] = 0x0c;
            bytes[offset + 1] = i;
            bytes[offset + 2] = 1;
            offset += 3;
        }
        bytes[offset] = 0x0e;
        return bytes;
    }

    private static byte[] forward() {
        // 0c 00 00
        // 0c 01 01
        // 0c 02 00
        // 0c 03 01
        // 0c 04 00
        // 0c 05 01
        // 0c 06 00
        // 0c 07 01

        // 0c 08 00
        // 0c 09 01

        // 0e

        return activate(new byte[]{
                1, 3, 5, 7, /* LEDs */ 9
        });
    }

    private static byte[] backward() {
        return activate(new byte[]{
                0, 2, 4, 6, /* LEDs */ 9
        });
    }

    private static byte[] left() {
        return activate(new byte[]{
                1, 3, 4, 6, /* LEDs */ 8
        });
    }

    private static byte[] right() {
        return activate(new byte[]{
                0, 2, 5, 7, /* LEDs */ 8
        });
    }

    private static byte[] stop() {
        // 0c 00 00
        // 0c 01 00
        // 0c 02 00
        // 0c 03 00
        // 0c 04 00
        // 0c 05 00
        // 0c 06 00
        // 0c 07 00

        // 0c 08 00 // LED
        // 0c 09 00 // LED

        // 0e

        int n = 10;
        byte[] bytes = new byte[n * 3 + 1];
        int offset = 0;
        for(byte i = 0; i < n; i++) {
            offset = i * 3;
            bytes[offset] = 0x0c;
            bytes[offset + 1] = i;
            bytes[offset + 2] = 0;
        }
        bytes[offset] = 0x0e;
        return bytes;
    }
}
