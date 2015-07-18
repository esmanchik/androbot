package com.httpuart;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;


public class MainActivity extends ActionBarActivity {
    private Uart uart;
    private Commands commands;

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

    public void onUartClick(View v) {
        Button button = (Button)findViewById(R.id.uartButton);
        try {
            if (button.getText().toString().startsWith("Open")) {
                Switch usbSwitch = (Switch)findViewById(R.id.usbSwitch);
                uart = usbSwitch.isChecked() ? new UsbUart(this) : new BlueUart();
                uart.open();
                commands = new Commands(uart, Commands.quadrobot());
                button.setText("Close UART");
            } else {
                button.setText("Open UART");
                uart.close();
            }
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
