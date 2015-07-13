package com.httpuart;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;


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

    public void onStartServiceClick(View v) {
        Button button = (Button)findViewById(R.id.startServiceButton);
        Switch usbSwitch = (Switch)findViewById(R.id.usbSwitch);
        Intent intent = new Intent(this, ConnectionService.class);
        intent.putExtra("UART", usbSwitch.isChecked() ? "USB" : "Bluetooth");
        if (button.getText().toString().startsWith("Start")) {
            startService(intent);
            button.setText("Stop Service");
        } else {
            stopService(intent);
            button.setText("Start Service");
        }
    }
}
