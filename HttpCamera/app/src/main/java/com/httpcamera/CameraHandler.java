package com.httpcamera;

import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

public class CameraHandler extends Handler implements SurfaceHolder.Callback {
    private SurfaceView surface;
    private volatile Camera camera;
    private volatile Handler pictureHandler;

    public CameraHandler(SurfaceView surfaceView) {
        this(surfaceView, null);
    }

    public CameraHandler(SurfaceView surfaceView, Handler picHandler) {
        setPictureHandler(picHandler);
        surface = surfaceView;
        surface.getHolder().addCallback(this);
    }

    public void setPictureHandler(Handler picHandler) {
        pictureHandler = picHandler;
    }

    private void resetPreviewDisplay() {
        try {
            camera.setPreviewDisplay(surface.getHolder());
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (camera == null) {
            return;
        }
        resetPreviewDisplay();
        camera.startPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (camera != null) {
            camera.stopPreview();
            camera.setDisplayOrientation(width > height ? 0 : 90);
            Camera.Parameters parameters = camera.getParameters();
            List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
            float ratio = width / (float)height;
            Camera.Size best = null;
            for(Camera.Size size: supportedPreviewSizes) {
                if (best == null || Math.abs(size.width / (float)size.height - ratio) < 0.01) {
                    best = size;
                }
            }
            parameters.setPreviewSize(best.width, best.height);
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            camera.setParameters(parameters);
            resetPreviewDisplay();
            camera.startPreview();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (camera != null) {
            camera.stopPreview();
        }
    }

    public void openCamera() {
        camera = Camera.open(0);
        camera.setDisplayOrientation(90);
    }

    public void closeCamera() {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    @Override
    public void handleMessage(Message msg) {
        if (camera == null) {
            sendPicture(null);
        } else {
            camera.takePicture(null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    sendPicture(data);
                    camera.startPreview();
                }
            });

        }
    }

    private void sendPicture(byte[] picture) {
        if (pictureHandler != null) {
            Message pictureMessage = pictureHandler.obtainMessage();
            pictureMessage.getData().putByteArray("picture", picture);
            pictureMessage.sendToTarget();
        }
    }
}
