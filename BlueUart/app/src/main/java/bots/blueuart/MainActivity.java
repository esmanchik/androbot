package bots.blueuart;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;


public class MainActivity extends ActionBarActivity {
    // Unique UUID for this application
    private static final UUID MY_UUID = UUID.fromString("0001101-0000-1000-8000-00805F9B34FB");
    private BluetoothSocket socket = null;
    private OutputStream stream = null;
    private boolean green = true;

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

    public void onConnectClick(View v) {
        String deviceName = "HC-06";
        String address = null;
        try {
            Button button = (Button)findViewById(R.id.connectButton);
            if (socket == null) {
                say("\nConnecting");
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                for(BluetoothDevice d: adapter.getBondedDevices()){
                    if (d.getName().equals(deviceName)) address = d.getAddress();
                }
                say("Address is " + address);
                BluetoothDevice device = adapter.getRemoteDevice(address); // Get the BluetoothDevice object
                say(device.toString());
                socket = device.createRfcommSocketToServiceRecord(MY_UUID);
                say(socket.toString());
                adapter.cancelDiscovery();
                socket.connect();
                stream = socket.getOutputStream();
                say(stream.toString());
                button.setText("Close");
            } else {
                stream = null;
                socket.close();
                socket = null;
                button.setText("Connect");
            }
        } catch (Exception e){
            say(e.toString());
        }
    }

    private void stop() {
        try {
            for(byte i = 0; i < 8; i++) {
                stream.write(new byte[]{0x0c, i, 0x00});
            }
            stream.write(new byte[]{0x0e});
        } catch (Exception e) {
            say(e.toString());
        }
    }

    public void onTestClick(View v) {
        try {
            if (green) {
                say("Run");
                stream.write(new byte[]{0x0c, 0x08, 0x01});
                stream.write(new byte[]{0x0c, 0x09, 0x01});
                for(byte i: new byte[] {1, 3, 5, 7}) {
                    stream.write(new byte[]{0x0c, i, 0x01});
                }
                stream.write(new byte[]{0x0e});
                green = false;
            } else {
                say("Stop");
                stream.write(new byte[]{0x0c, 0x08, 0x00});
                stream.write(new byte[]{0x0c, 0x09, 0x00});
                stop();
                green = true;
            }
            // Execute some code after 2 seconds have passed
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    say("Stop");
                    stop();
                }
            }, 3000);
        } catch (Exception e){
            say(e.toString());
        }
    }

    public void say(String message) {
        TextView status = (TextView)findViewById(R.id.textView);
        status.append(message + "\n");
    }
}
