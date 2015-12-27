package com.httpcamera;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ToggleButton;

import java.io.File;
import java.io.FileOutputStream;

import javax.security.auth.callback.CallbackHandler;

public class MainActivity extends Activity {
    private Bundle state;
    private ToggleButton serviceToggleButton;
    private Preview preview;
    private HttpService service;
    private ServiceConnection serviceConnection;
    private CameraHandler cameraHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        state = savedInstanceState; // == null ? new Bundle() : savedInstanceState;
        serviceToggleButton = (ToggleButton)findViewById(R.id.serviceToggleButton);
        SurfaceView surface = (SurfaceView)findViewById(R.id.surfaceView);
        // preview = new Preview(surface);
        cameraHandler = new CameraHandler(getMainLooper());
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                HttpService.LocalBinder localBinder = (HttpService.LocalBinder)binder;
                service = localBinder.service;
                service.start(cameraHandler);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                service = null;
            }
        };
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
        //preview.takePicture();
    }

    public void onService(View v) {
        Intent intent =
            // new Intent(this, HttpService.class);
            new Intent(this, HttpSurfaceService.class);
        // intent.putExtra(ConnectionService.PORT, port.intValue());
        if (serviceToggleButton.isChecked()) {
            startService(intent);
            //bindService(intent, serviceConnection, BIND_AUTO_CREATE);
        } else {
            stopService(intent);
            //unbindService(serviceConnection);
        }
    }

    class CameraHandler extends Handler {
        Handler pictureHandler;

        public CameraHandler(Looper looper) {
            super(looper);
            pictureHandler = null;
        }

        public void setPictureHandler(Handler picHandler) {
            pictureHandler = picHandler;
        }

        public void handleMessage(Message msg) {
            if (pictureHandler != null) {
                preview.takePicture(pictureHandler);
            }
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
