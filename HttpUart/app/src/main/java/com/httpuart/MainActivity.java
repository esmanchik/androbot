package com.httpuart;

import android.app.Activity;
import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.hoho.android.usbserial.util.HexDump;
import com.lamerman.FileDialogOptions;

import org.apache.http.conn.util.InetAddressUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;


public class MainActivity extends ActionBarActivity {
    private Uart uart;
    private Commands commands;
    private Commands.Map map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        uart = new UsbUart(this);
        map = Commands.quadrobot();
        commands = new Commands(uart, map);
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

    public void onUartClick(View v) {
        Button button = (Button)findViewById(R.id.uartButton);
        try {
            if (button.getText().toString().startsWith("Open")) {
                Switch usbSwitch = (Switch)findViewById(R.id.usbSwitch);
                uart = usbSwitch.isChecked() ? new UsbUart(this) : new BlueUart();
                uart.open();
                commands = new Commands(uart, map);
                button.setText("Close UART");
            } else {
                button.setText("Open UART");
                uart.close();
            }
        } catch (Exception e) {
            xcpt(e);
        }
    }

    public synchronized void onActivityResult(final int requestCode,
                                              int resultCode, final Intent data) {
        try {
            if (resultCode == Activity.RESULT_OK) {
                // String path = data.getStringExtra("currentPath");
                String path = FileDialogOptions.readResultFile(data);
                EditText fileEdit = (EditText)findViewById(R.id.filePathEditText);
                fileEdit.setText(path);
            }
        } catch (Exception e) {
            xcpt(e);
        }
    }

    private void parsedCommandsToast(String[] commands) {
        String parsed = "";
        for (String command : commands) {
            parsed += command + " ";
        }
        toast("Parsed commands " + parsed);
    }

    public void onFileDialogClick(View v) {
        FileDialogOptions options = new FileDialogOptions();
        options.currentPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        options.selectFolderMode = false;
        options.allowCreate = true;
        Intent intent = options.createFileDialogIntent(this);
        startActivityForResult(intent, 0);
    }

    public void onLoadClick(View v) {
        try {
            EditText fileEdit = (EditText)findViewById(R.id.filePathEditText);
            EditText commandsEdit = (EditText)findViewById(R.id.commandsEditText);
            String path = fileEdit.getText().toString();
            String content;
            if (path.equals("")) {
                content = commandsEdit.getText().toString();
            } else {
                File file = new File(path);
                content = new Scanner(file).useDelimiter("\\Z").next();
                commandsEdit.setText(content);
            }
            map = Commands.fromString(content);
            commands = new Commands(uart, map);
            parsedCommandsToast(commands.available());
        } catch (Exception e) {
            xcpt(e);
        }
    }

    public void onSaveClick(View v) {
        try {
            String fileName = "httpuart.commands.txt";
            // File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
            EditText fileEdit = (EditText)findViewById(R.id.filePathEditText);
            File file = new File(fileEdit.getText().toString());
            // fileEdit.setText(file.getAbsolutePath());

            JSONObject json = new JSONObject();
            for(String cmd: map.keySet()) {
                String hex = HexDump.toHexString(map.get(cmd));
                json.put(cmd, hex);
            }

            EditText commandsEdit = (EditText)findViewById(R.id.commandsEditText);
            commandsEdit.setText(json.toString());

            //EditText fileEdit = (EditText)findViewById(R.id.filePathEditText);
            //File file = new File(fileEdit.getText().toString());
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            PrintWriter writer = new PrintWriter(fileOutputStream);
            writer.println(json.toString());
            writer.flush();
            writer.close();
        } catch (Exception e) {
            xcpt(e);
        }
    }

    public void onGoClick(View v) {
        try {
            commands.execute("f");
        } catch (Exception e) {
            xcpt(e);
        }
    }

    public void onStopClick(View v) {
        try {
            commands.execute("s");
        } catch (Exception e) {
            xcpt(e);
        }
    }

    private static String getIpAddress() throws SocketException {
        List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
        for (NetworkInterface intf : interfaces) {
            List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
            for (InetAddress addr : addrs) {
                if (!addr.isLoopbackAddress()) {
                    String sAddr = addr.getHostAddress().toUpperCase();
                    boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                    if (isIPv4)
                        return sAddr;
                }
            }
        }
        throw new SocketException("Failed to determinels IP address");
    }

    public void onStartServiceClick(View v) {
        try {
            Button button = (Button)findViewById(R.id.startServiceButton);
            Switch usbSwitch = (Switch)findViewById(R.id.usbSwitch);
            EditText commandsEdit  = (EditText)findViewById(R.id.commandsEditText);
            EditText portEdit  = (EditText)findViewById(R.id.portEditText);
            TextView statusText = (TextView)findViewById(R.id.statusTextView);
            Integer port = Integer.valueOf(portEdit.getText().toString());

            Intent intent = new Intent(this, ConnectionService.class);
            intent.putExtra(
                    ConnectionService.UART, usbSwitch.isChecked() ?
                            ConnectionService.UART_USB :
                            ConnectionService.UART_BLUETOOTH
            );
            intent.putExtra(ConnectionService.PORT, port.intValue());
            intent.putExtra(ConnectionService.COMMANDS, commandsEdit.getText().toString());
            if (button.getText().toString().startsWith("Start")) {
                startService(intent);
                button.setText("Stop Service");
            } else {
                stopService(intent);
                button.setText("Start Service");
            }

            statusText.setText("IP: " + getIpAddress());
        } catch (Exception e) {
            xcpt(e);
        }
    }

    void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    void xcpt(Exception e) {
        toast(e.toString());
    }
}
