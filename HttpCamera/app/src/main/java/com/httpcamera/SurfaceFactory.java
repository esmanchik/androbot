package com.httpcamera;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.SurfaceView;
import android.view.WindowManager;

public class SurfaceFactory {
    public static SurfaceView create(Context context) {
        SurfaceView surface = new SurfaceView(context);
        WindowManager wm = (WindowManager)context
                .getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                90, 160, //Must be at least 1x1
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                0,
                //Don't know if this is a safe default
                PixelFormat.UNKNOWN);

        //Don't set the preview visibility to GONE or INVISIBLE
        wm.addView(surface, params);
        return surface;
    }
}
