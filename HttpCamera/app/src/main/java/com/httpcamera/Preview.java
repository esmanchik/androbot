package com.httpcamera;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Handler;
import android.os.Message;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.FileOutputStream;
import java.io.IOException;

class Preview implements SurfaceHolder.Callback {
    SurfaceView mSurfaceView;
    SurfaceHolder mHolder;
    Camera mCamera;

    Preview(SurfaceView surfaceView) {
        mSurfaceView = surfaceView;
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mCamera = Camera.open(0);
        if (mCamera == null)
        {
            return;
        }
        try {
            mCamera.setPreviewDisplay(mHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Important: Call startPreview() to start updating the preview
        // surface. Preview must be started before you can take a picture.
        mCamera.startPreview();
    }

        @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (mCamera == null)
        {
            return;
        }
        mCamera.stopPreview();
        // Now that the size is known, set up the camera parameters and begin
        // the preview.
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(w, h);
        mCamera.setParameters(parameters);

        // Important: Call startPreview() to start updating the preview surface.
        // Preview must be started before you can take a picture.
        mCamera.startPreview();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera == null)
        {
            return;
        }
        // Call stopPreview() to stop updating the preview surface.
        mCamera.stopPreview();

        // Important: Call release() to release the camera for use by other
        // applications. Applications should release the camera immediately
        // during onPause() and re-open() it during onResume()).
        mCamera.release();
        mCamera = null;
    }

    public void takePicture(final Handler pictureHandler) {
        if (mCamera == null) {
            Message pictureMessage = pictureHandler.obtainMessage();
            pictureMessage.getData().putByteArray("picture", null);
            pictureMessage.sendToTarget();
            return;
        }
        mCamera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                /*try {
                    FileOutputStream stream = new FileOutputStream("/storage/sdcard0/httpcamera.jpg");
                    stream.write(data);
                    stream.close();
                } catch(Exception e) {
                    e.printStackTrace();
                }*/
                Message pictureMessage = pictureHandler.obtainMessage();
                pictureMessage.getData().putByteArray("picture", data);
                pictureMessage.sendToTarget();
                camera.startPreview();
            }
        });

    }
}