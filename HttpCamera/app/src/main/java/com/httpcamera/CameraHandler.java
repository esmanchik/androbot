package com.httpcamera;

import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

public class CameraHandler extends Handler implements SurfaceHolder.Callback {
    private SurfaceView surface;
    private volatile Camera camera;

    public CameraHandler(SurfaceView surfaceView) {
        surface = surfaceView;
        surface.getHolder().addCallback(this);
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
            float surfaceRatio = width / (float)height;
            Camera.Size best = null;
            for(Camera.Size size: supportedPreviewSizes) {
                float previewSizeRatio = size.width / (float)size.height;
                if (best == null) {
                    best = size;
                    continue;
                }
                float bestSizeRatio = size.width / (float)size.height;
                if (Math.abs(previewSizeRatio - surfaceRatio) < Math.abs(bestSizeRatio - surfaceRatio)) {
                    best = size;
                }
                if (Math.abs(previewSizeRatio - surfaceRatio) < 0.001) {
                    break;
                }
            }
            parameters.setPreviewSize(best.width, best.height);
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            camera.setParameters(parameters);
            // resetPreviewDisplay();
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
        try {
            camera = Camera.open(0);
            camera.setDisplayOrientation(90);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void closeCamera() {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    private static void setFlashMode(Camera camera, String mode) {
        Camera.Parameters p = camera.getParameters();
        p.setFlashMode(mode);
        camera.setParameters(p);
    }

    public interface PictureHolder {
        void setPicture(byte[] picture);
    }

    public void shot(PictureHolder pictureHolder) {
        final PictureHolder holder = pictureHolder;
        if (camera == null) {
            holder.setPicture(new byte[1]); // dummy picture here
        } else {
            camera.stopPreview();
            setFlashMode(camera, Camera.Parameters.FLASH_MODE_TORCH);
            camera.startPreview();
            camera.takePicture(null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    holder.setPicture(data);
                    setFlashMode(camera, Camera.Parameters.FLASH_MODE_OFF);
                    camera.startPreview();
                }
            });
        }
    }

    @Override
    public void handleMessage(Message msg) {
        final Message source = msg;
        if (camera == null) {
            sendPicture(source, null);
        } else {
            camera.stopPreview();
            setFlashMode(camera, Camera.Parameters.FLASH_MODE_TORCH);
            camera.startPreview();
            camera.takePicture(null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    sendPicture(source, data);
                    setFlashMode(camera, Camera.Parameters.FLASH_MODE_OFF);
                    camera.startPreview();
                }
            });

        }
    }

    private void sendPicture(Message source, byte[] picture) {
        int socketId = source.getData().getInt("socket");
        Message pictureMessage = Message.obtain();
        pictureMessage.getData().putByteArray("picture", picture);
        Messenger dest = source.replyTo;
        if (dest != null) {
            try {
                dest.send(pictureMessage);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}
