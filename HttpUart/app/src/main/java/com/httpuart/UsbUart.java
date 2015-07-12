package com.httpuart;

import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Parcel;
import android.os.Parcelable;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.List;

public class UsbUart implements Uart {
    private Context context;
    private UsbSerialPort port;

    public UsbUart(Context context) {
        this.context = context;
    }

    @Override
    public void open() {
        // Find all available drivers from attached devices.
        UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            return;
        }

        // Open a connection to the first available driver.
        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
        if (connection == null) {
            // You probably need to call UsbManager.requestPermission(driver.getDevice(), ..)
            return;
        }

        // RMost have just one port (port 0).
        port = driver.getPorts().get(0);
        try {
            port.open(connection);
            port.setParameters(9600, 8, 1, 0);
        } catch (IOException e) {
            // error("Open port failed")
        }
    }

    @Override
    public void write(byte[] bytes) {
        try {
            port.write(bytes, 3000);
        } catch (IOException e) {
            // error("Write failed")
        }
    }

    @Override
    public void close() {
        try {
            port.close();
        } catch (IOException e) {
            // error("Close port failed")
        }
    }
}
