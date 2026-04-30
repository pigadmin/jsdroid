package com.jiuzhuan.jsdroid.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.jiuzhuan.jsdroid.BuildConfig
import com.jiuzhuan.jsdroid.R
import com.jiuzhuan.jsdroid.utils.BROADCAST_ACTION_MESSAGE
import com.jiuzhuan.jsdroid.utils.showException


class FloatToastService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var toastMsg: TextView

    private val handler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            try {
                when (msg.what) {
                    1 -> {
                        resetToast()
                    }
                }
            } catch (ignored: Exception) {
                ignored.printStackTrace()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createFloatingWindow()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val filter = IntentFilter();
        filter.addAction(BROADCAST_ACTION_MESSAGE)
        registerReceiver(receiver, filter, RECEIVER_EXPORTED)
        return START_STICKY
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            when (action) {
                BROADCAST_ACTION_MESSAGE -> {// 接受日志吐司广播
                    val message = intent.getStringExtra("message")
                    toastMsg.setBackgroundColor(randomMaterialColor())
                    toastMsg.text = message

                    handler.removeMessages(1)
                    handler.sendEmptyMessageDelayed(1, 3000)
                }
            }
        }
    }

    private fun createFloatingWindow() {
        try {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            floatingView = LayoutInflater.from(this).inflate(R.layout.float_toast, null)
            toastMsg = floatingView.findViewById(R.id.toastMsg)
            resetToast()

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )

            params.x = 0
            params.y = 10
            params.gravity = Gravity.BOTTOM or Gravity.CENTER

            windowManager.addView(floatingView, params)
        } catch (e: Exception) {// permission
            showException(e)
        }
    }

    private fun resetToast() {
        toastMsg.background = getDrawable(R.drawable.border)
        toastMsg.text = "${getString(R.string.app_name)} v ${BuildConfig.VERSION_NAME}"
    }

    private fun randomMaterialColor(): Int {
        val colors = arrayOf(
            Color.RED, Color.GREEN, Color.BLUE, Color.CYAN, Color.MAGENTA
        )
        return colors.random()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::windowManager.isInitialized && ::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
            unregisterReceiver(receiver)
        }
    }
}