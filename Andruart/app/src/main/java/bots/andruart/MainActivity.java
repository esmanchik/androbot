package bots.andruart;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.List;


public class MainActivity extends ActionBarActivity {
    private Camera camera;

    private Camera openFrontCamera() {
        int cameraCount = 0;
        Camera cam = null;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();
        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                try {
                    cam = Camera.open(camIdx);
                } catch (Exception e) {
                    TextView status = (TextView)findViewById(R.id.textView);
                    status.setText("Camera failed to open: " + e.getMessage());
                }
            }
        }

        return cam;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        camera = openFrontCamera();
        if (camera != null) {
            CameraPreview preview = new CameraPreview(this, camera);
            FrameLayout frame = (FrameLayout) findViewById(R.id.camera_preview);
            frame.addView(preview);
        }
    }

    @Override
    protected void onDestroy() {
        camera.release();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void stop(View v) {
        exchange((byte) 1);
    }

    public void forward(View v) {
        exchange((byte) 7);
    }

    public void backward(View v) {
        exchange((byte) 6);
    }

    public void left(View v) {
        exchange((byte) 5);
    }

    public void right(View v) {
        exchange((byte) 3);
    }

    private byte exchange(byte code) {
        byte result = (byte) -1;
        TextView status = (TextView)findViewById(R.id.textView);
        try {
            CheckBox measureCheckBox = (CheckBox)findViewById(R.id.measureCheckBox);
            boolean measure = measureCheckBox.isChecked();
            UsbManager manager = (UsbManager)getSystemService(Context.USB_SERVICE);
            List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
            if (availableDrivers.isEmpty()) {
                status.setText(R.string.no_device_found);
            } else {
                status.setText(R.string.all_is_fine);
            }
            UsbSerialDriver driver = availableDrivers.get(0);
            UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
            if (connection == null) {
                status.setText(R.string.connect_failed);
            }
            UsbSerialPort port = driver.getPorts().get(0);
            try {
                port.open(connection);
                port.setParameters(9600, 8, 1, 0);
                byte buf[] = new byte[1];
                if (code == -1) {
                    int n = port.read(buf, 1000);
                    //status.setText(getResources().getQuantityString(R.plurals.bytes_read, n, n));
                    status.setText(String.format("%d bytes read: %02x", n, buf[0]));
                    result = buf[0];
                } else {
                    if (measure) {
                        code |= 8;
                    }
                    buf[0] = code;
                    int n = port.write(buf, 1000);
                    if (measure) {
                        int i, value, attempts;
                        attempts = value = i = 0;
                        while (i < 4) {
                            buf = new byte[4 - i];
                            n = port.read(buf, 1000);
                            if (n > 0) {
                                for (int j = 0; j < n; j++) {
                                    value |=  (buf[j] & 0xf) << ((i + j) * 4);
                                }
                                attempts = 0;
                                i += n;
                                status.setText(String.format("%d bytes read: %04x", i, value));
                            } else {
                                attempts++;
                                status.setText(String.format("%d attempt", attempts));
                                if (attempts++ < 32) {
                                    continue;
                                } else {
                                    break;
                                }
                            }
                        }
                        if (i < 4) {
                            status.setText(String.format("%d bytes read: %04x", i, value));
                        } else {
                            status.setText(String.format("%d (0x%04x) cycles to the wall", value, value));
                        }
                    } else {
                        status.setText(String.format("%d bytes written: %02x", n, buf[0]));
                    }
                }
            } catch (IOException e) {
                status.setText(R.string.open_port_failed);
            } finally {
                try {
                    port.close();
                } catch (IOException e) {
                }
            }
        } catch (Throwable t) {
            status.setText(t.getMessage());
        }
        return result;
    }
}
