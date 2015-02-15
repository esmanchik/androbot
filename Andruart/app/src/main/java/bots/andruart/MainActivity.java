package bots.andruart;

import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.List;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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

    public void read(View v) {
        exchange((byte) -1);
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
        CheckBox measureCheckBox = (CheckBox)findViewById(R.id.measureCheckBox);
        boolean measure = measureCheckBox.isChecked();
        TextView status = (TextView)findViewById(R.id.textView);
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
                    int i, value = 0;
                    for (i = 0; i < 4; i++) {
                        n = port.read(buf, 1000);
                        if (n < buf.length) {
                            status.setText(String.format("%d bytes read: %04x", i, value));
                            break;
                        } else {
                            value |=  (buf[0] & 0xf) << (i * 4);
                        }
                    }
                    if (i < 4) {
                        status.setText(String.format("%d bytes read: %04x", i, value));
                    } else {
                        status.setText(String.format("%d (0x%04x) cycles to the wall", value, value));
                    }
                    /*
                    buf = new byte[4];
                    n = port.read(buf, 1000);
                    if (n < buf.length) {
                        status.setText(String.format("%d bytes read: %02x", n, buf[0]));
                    } else {
                        n = buf[0] & 0xf;
                        n |=  (buf[1] & 0xf) << 4;
                        n |=  (buf[2] & 0xf) << 8;
                        n |=  (buf[3] & 0xf) << 12;
                        status.setText(String.format("Range is %d (0x%04x)", n, n));
                    }
                    */
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
        return result;
    }
}
