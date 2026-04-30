package com.jiuzhuan.jsdroid.rhino.helper

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import com.jiuzhuan.jsdroid.base.activity
import com.jiuzhuan.jsdroid.utils.REQUEST_CODE_SCREEN_CAPTURE
import com.jiuzhuan.jsdroid.utils.getScreenInfo
import com.jiuzhuan.jsdroid.utils.logd

@SuppressLint("StaticFieldLeak")
object Screenshot {
    private lateinit var mActivity: Activity
    private lateinit var mMediaProjectionManager: MediaProjectionManager
    private lateinit var mVirtualDisplay: VirtualDisplay
    private lateinit var mMediaProjection: MediaProjection
    private lateinit var mImageReader: ImageReader

    private var isInitialized = false

    private var isStartCapture = false

    fun init(activity: Activity) {
        if (isInitialized) return
        isInitialized = true
        mActivity = activity
        mMediaProjectionManager =
            mActivity.getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                    as MediaProjectionManager
    }

    fun requestCapture() {
        val intent = mMediaProjectionManager.createScreenCaptureIntent()
        activity.startActivityForResult(intent, REQUEST_CODE_SCREEN_CAPTURE)
    }

    fun handleRequestCapture(resultCode: Int, data: Intent) {
        mMediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data)
//        val width = DisplayApi.getDisplayWidth()
//        val height = DisplayApi.getDisplayHeight()
//        val densityDpi = DisplayApi.getDisplayDensityDpi()

        val (width, height, densityDpi) = getScreenInfo()
        logd("screenInfo", width.toString(), height.toString(), densityDpi.toString())
        mImageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

        mImageReader.setOnImageAvailableListener({ reader ->
            if (!isStartCapture) {
                reader.acquireLatestImage()?.close()
            }
        }, Handler(Looper.getMainLooper()))

        mVirtualDisplay = mMediaProjection.createVirtualDisplay(
            "Screenshot",
            width,
            height,
            densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mImageReader.surface,
            object : VirtualDisplay.Callback() {},
            Handler(Looper.getMainLooper()),
        )
    }

    fun startCapture() {
        isStartCapture = true
    }

    fun stopCapture() {
        isStartCapture = false
    }

    fun getImageReader(): ImageReader {
        return mImageReader
    }

    fun destroy() {
        if (!isInitialized) return

        isInitialized = false
        isStartCapture = false
        mImageReader.close()
        mVirtualDisplay.release()
        mMediaProjection.stop()
    }
}
