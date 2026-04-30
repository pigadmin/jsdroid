package com.jiuzhuan.jsdroid.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.jiuzhuan.jsdroid.base.BaseService
import com.jiuzhuan.jsdroid.events.CapEvent
import com.jiuzhuan.jsdroid.rhino.helper.Screencap
import com.jiuzhuan.jsdroid.utils.getScreenInfo
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class KeepAliveService : BaseService() {
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null

    //    private var mediaRecorder: MediaRecorder? = null
    private lateinit var mImageReader: ImageReader
    private val mMediaProjectionCallback = object : MediaProjection.Callback() {}

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "screen_capture_channel"
        const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        EventBus.getDefault().register(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundServiceWithNotification()
        return START_STICKY
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onCapEvent(cap: CapEvent) {
        // 获取 MediaProjection 权限结果
        startCapture(cap.resultCode, cap.resultData)
    }

    private fun startCapture(resultCode: Int, resultData: Intent) {
        // 2. 初始化 MediaProjection
        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            mediaProjection?.registerCallback(mMediaProjectionCallback, Handler())
        }
        // 3. 设置录制配置
        setupMediaRecorder()

        // 4. 创建虚拟显示
        createVirtualDisplay()

        // 5. 开始录制
//        mediaRecorder?.start()
    }

    private fun setupMediaRecorder() {
//        mediaRecorder = MediaRecorder().apply {
//            setVideoSource(MediaRecorder.VideoSource.SURFACE)
//            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
//            setOutputFile("${getExternalFilesDir(null)}/screen_record.mp4")
//            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
//            setVideoSize(1080, 1920) // 根据设备调整
//            setVideoFrameRate(30)
//            setVideoEncodingBitRate(5 * 1024 * 1024)
//            prepare()
//        }
        val info = getScreenInfo()
        mImageReader = ImageReader.newInstance(info.first, info.second, PixelFormat.RGBA_8888, 3)
        mImageReader.setOnImageAvailableListener({ reader ->
            if (reader != null) {
                Screencap.setImageReader(reader)
            }
        }, Handler())
    }

    private fun createVirtualDisplay() {
        val info = getScreenInfo()
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCaptures",
            info.first,
            info.second,
            info.third,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mImageReader.surface,
            object : VirtualDisplay.Callback() {},
            Handler()
        )
    }

    private fun startForegroundServiceWithNotification() {
        createNotificationChannel()
        val notification = buildNotification()
        startForeground(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        )
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "App正在运行",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "App正在前台运行"
        }

        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("屏幕录制中")
            .setContentText("正在捕获屏幕内容...")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder {
        return KeepAliveBinder()
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    private fun stopCapture() {
//        mediaRecorder?.apply {
//            stop()
//            reset()
//            release()
//        }
//        mediaRecorder = null

        virtualDisplay?.release()
        virtualDisplay = null

        mediaProjection?.stop()
        mediaProjection = null
    }

    inner class KeepAliveBinder : Binder() {
        fun getKeepAliveService(): KeepAliveService {
            return this@KeepAliveService
        }
    }
}
