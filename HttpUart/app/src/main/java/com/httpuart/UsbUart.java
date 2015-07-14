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
            throw new RuntimeException("No USB serial drivers available ");
        }

        // Open a connection to the first available driver.
        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
        if (connection == null) {
            // You probably need to call UsbManager.requestPermission(driver.getDevice(), ..)
            throw new RuntimeException("Failed to open connection to " + driver.getDevice());
        }

        // RMost have just one port (port 0).
        List<UsbSerialPort> availablePorts = driver.getPorts();
        if (availablePorts.isEmpty()) {
            throw new RuntimeException("No USB serial port available at " + driver);
        }
        try {
            port = availablePorts.get(0);
            port.open(connection);
            port.setParameters(9600, 8, 1, 0);
        } catch (Exception e) {
            throw new RuntimeException("Failed to open " + port);
        }
    }

    @Override
    public void write(byte[] bytes) {
        try {
            port.write(bytes, 3000);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write " + bytes + " to " + port, e);
        }
    }

    @Override
    public void close() {
        try {
            port.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to close " + port, e);
        }
    }
}
