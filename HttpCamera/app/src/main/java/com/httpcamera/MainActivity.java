package com.httpcamera;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ToggleButton;

public class MainActivity extends Activity {
    private Bundle state;
    private ToggleButton serviceToggleButton;
    private ServiceConnection serviceConnection;
    private HttpService service;
    private CameraHandler cameraHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        state = savedInstanceState; // == null ? new Bundle() : savedInstanceState;
        serviceToggleButton = (ToggleButton)findViewById(R.id.serviceToggleButton);
        SurfaceView surface = (SurfaceView)findViewById(R.id.surfaceView);
        cameraHandler = new CameraHandler(surface);
        if (state == null || !state.getBoolean("serviceToggleButtonState")) {
            try {
                cameraHandler.openCamera();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                HttpService.LocalBinder localBinder = (HttpService.LocalBinder)binder;
                service = localBinder.getService();
                // service.setCameraHandler(cameraHandler);
                service.start();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                service.shutdown();
                service = null;
                //cameraHandler.openCamera();
            }
        };
    }

    @Override
    protected void onDestroy() {
        cameraHandler.closeCamera();
        super.onDestroy();
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

    public void onTakePicture(View v) {
        cameraHandler.obtainMessage().sendToTarget();
    }

    public void onService(View v) {
        Intent intent =
            new Intent(this, HttpService.class);
        // intent.putExtra(ConnectionService.PORT, port.intValue());
        if (serviceToggleButton.isChecked()) {
            cameraHandler.closeCamera();
            //startService(intent);
            bindService(intent, serviceConnection, BIND_AUTO_CREATE);
        } else {
            //stopService(intent);
            unbindService(serviceConnection);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (state == null) {
            state = new Bundle();
        }
        state.putBoolean("serviceToggleButtonState", serviceToggleButton.isChecked());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (state != null) {
            serviceToggleButton.setChecked(state.getBoolean("serviceToggleButtonState"));
        }
    }
}
