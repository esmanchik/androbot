package com.httpuart;

import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import com.hoho.android.usbserial.util.HexDump;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Iterator;
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
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    public void onLoadClick(View v) {
        try {
            EditText commandsEdit = (EditText)findViewById(R.id.commandsEditText);
            EditText fileEdit = (EditText)findViewById(R.id.filePathEditText);
            String path = fileEdit.getText().toString();

            String content;
            if (path.equals("")) {
                content = commandsEdit.getText().toString();
            } else {
                content = new Scanner(new File(path)).useDelimiter("\\Z").next();
                commandsEdit.setText(content);
            }
            JSONObject json = new JSONObject(content);

            String[] cmds = new String[map.keySet().size()];
            for(String cmd: map.keySet().toArray(cmds)) {
                map.remove(cmd);
            }
            Iterator<String> keys = json.keys();
            for(String key = keys.next(); keys.hasNext(); key = keys.next()) {
                String hex = json.getString(key);
                map.put(key, HexDump.hexStringToByteArray(hex));
            }
        } catch (Exception e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    public void onSaveClick(View v) {
        try {
            String fileName = "httpuart.commands.txt";
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
            EditText fileEdit = (EditText)findViewById(R.id.filePathEditText);
            fileEdit.setText(file.getAbsolutePath());

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
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    public void onGoClick(View v) {
        try {
            commands.execute("f");
        } catch (Exception e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    public void onStopClick(View v) {
        try {
            commands.execute("s");
        } catch (Exception e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    public void onStartServiceClick(View v) {
        Button button = (Button)findViewById(R.id.startServiceButton);
        Switch usbSwitch = (Switch)findViewById(R.id.usbSwitch);
        Intent intent = new Intent(this, ConnectionService.class);
        intent.putExtra(
                ConnectionService.UART, usbSwitch.isChecked() ?
                        ConnectionService.UART_USB :
                        ConnectionService.UART_BLUETOOTH
        );
        if (button.getText().toString().startsWith("Start")) {
            startService(intent);
            button.setText("Stop Service");
        } else {
            stopService(intent);
            button.setText("Start Service");
        }
    }
}
