package com.httpuart;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class BlueUart implements Uart {
    // public final String TAG = "HC-06";

    private final UUID MY_UUID = UUID.fromString("0001101-0000-1000-8000-00805F9B34FB");
    private BluetoothSocket socket = null;
    private OutputStream stream = null;


    @Override
    public void open() {
        String deviceName = "HC-06";
        String address = null;
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            for (BluetoothDevice d: adapter.getBondedDevices()) {
                if (d.getName().equals(deviceName)) address = d.getAddress();
            }
            // Log.d(TAG, address);
            if (address == null) {
                // throw new RuntimeException("Failed to determine address out of " + adapter.getBondedDevices());
            }
            BluetoothDevice device = adapter.getRemoteDevice(address); // Get the BluetoothDevice object
            socket = device.createRfcommSocketToServiceRecord(MY_UUID);
            adapter.cancelDiscovery();
            socket.connect();
            stream = socket.getOutputStream();
            // Log.d(TAG, socket.toString());
        } catch (Exception e){
            throw new RuntimeException("Failed to open " + deviceName, e);
        }
    }

    @Override
    public void write(byte[] bytes) {
        try {
            stream.write(bytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write " + bytes + " to " + stream + " of " + socket, e);
        }
    }

    @Override
    public void close() {
        try {
            socket.close();
            // Log.d(TAG, socket.toString() + " closed");
        } catch (Exception e) {
            throw new RuntimeException("Failed to close " + socket, e);
        }
    }
}
