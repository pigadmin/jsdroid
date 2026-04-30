package com.jiuzhuan.jsdroid.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.jiuzhuan.jsdroid.BuildConfig
import com.jiuzhuan.jsdroid.R
import com.jiuzhuan.jsdroid.api.getMmkv
import com.jiuzhuan.jsdroid.events.ChangeFloatEvent
import com.jiuzhuan.jsdroid.events.GameStatusEvent
import com.jiuzhuan.jsdroid.events.GameStatusEvent.Companion.OVER
import com.jiuzhuan.jsdroid.events.GameStatusEvent.Companion.START
import com.jiuzhuan.jsdroid.events.GameStatusEvent.Companion.STOP
import com.jiuzhuan.jsdroid.events.MessageEvent
import com.jiuzhuan.jsdroid.events.TaskEvent
import com.jiuzhuan.jsdroid.events.TaskEvent.Companion.START_TASK
import com.jiuzhuan.jsdroid.events.TaskEvent.Companion.STOP_TASK
import com.jiuzhuan.jsdroid.rhino.api.ImageApi.save
import com.jiuzhuan.jsdroid.rhino.api.ToastApi.shortToast
import com.jiuzhuan.jsdroid.rhino.helper.Screencap.getImageReader
import com.jiuzhuan.jsdroid.ui.OverActivity
import com.jiuzhuan.jsdroid.utils.getScreenInfo
import com.jiuzhuan.jsdroid.utils.showException
import com.jiuzhuan.jsdroid.utils.toBitmap
import com.ss.android.ugc.aweme.live.livehostimpl.AudioAccessibilityService
import com.ss.android.ugc.aweme.live.livehostimpl.rhinoService
import com.tencent.mmkv.MMKV
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class FloatLogoService : Service(), View.OnClickListener, View.OnTouchListener {
    private lateinit var animation: Animation
    private lateinit var triple: Triple<Int, Int, Int>
    private lateinit var logoParams: WindowManager.LayoutParams
    private lateinit var windowManager: WindowManager
    private lateinit var logoRootView: View
    private lateinit var toastRootView: View
    private lateinit var logo: ImageView
    private lateinit var menuLayout: LinearLayout
    private lateinit var status: Button
    private lateinit var screen: Button
    private var mmkv: MMKV? = null

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

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
        EventBus.getDefault().register(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createLogoWindow()
        createToastWindow()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onGameStatusEvent(event: GameStatusEvent) {
        when (event.status) {
            OVER -> {
                val mIntent = Intent(this, OverActivity::class.java)
                mIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(mIntent)
            }

            STOP -> {
                updateStatus()
            }

            START -> {
                updateStatus()
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onChangeFloatEvent(event: ChangeFloatEvent) {
        try {
            val position = event.position.split(",")
            logoParams.x = position[0].toInt()
            logoParams.y = position[1].toInt()
            mmkv!!.putInt("startX", logoParams.x)
            mmkv!!.putInt("startY", logoParams.y)
            windowManager.updateViewLayout(logoRootView, logoParams)
        } catch (e: Exception) {
            showException(e)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: MessageEvent) {
        setMessage(event.message)
    }

    @SuppressLint("RtlHardcoded")
    private fun createLogoWindow() {
        try {

            logoRootView = LayoutInflater.from(this).inflate(R.layout.float_logo, null)

            logo = logoRootView.findViewById(R.id.logo)

            menuLayout = logoRootView.findViewById(R.id.menuLayout)
            status = logoRootView.findViewById(R.id.status)
            screen = logoRootView.findViewById(R.id.screen)

            animation = AnimationUtils.loadAnimation(this, R.anim.anim_translationxy)
            val lir = LinearInterpolator()
            animation.interpolator = lir

            logoParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )

            triple = getScreenInfo()

            mmkv = getMmkv()

            logoParams.x = mmkv!!.getInt("startX", 0)
            logoParams.y = mmkv!!.getInt("startY", 300)

            logoParams.gravity = Gravity.TOP or Gravity.LEFT

            windowManager.addView(logoRootView, logoParams)
            initView()
            initListener()
        } catch (e: Exception) {
            showException(e)
        }
    }


    private fun createToastWindow() {
        try {
            toastRootView = LayoutInflater.from(this).inflate(R.layout.float_toast, null)
            toastMsg = toastRootView.findViewById(R.id.toastMsg)
            resetToast()

            val toastParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )

            toastParams.x = 0
            toastParams.y = 10
            toastParams.gravity = Gravity.BOTTOM or Gravity.CENTER

            windowManager.addView(toastRootView, toastParams)
        } catch (e: Exception) {// permission
            showException(e)
        }
    }

    private fun initView() {

    }

    private fun initListener() {
        status.setOnClickListener(this)
        screen.setOnClickListener(this)

        logo.setOnClickListener(this)
        logo.setOnTouchListener(this)
    }

    private fun updateStatus() {
        if (AudioAccessibilityService.isRunning()) {// 已开始
            logo.startAnimation(animation)
            status.setText(R.string.stop)
        } else {
            logo.clearAnimation()
            status.setText(R.string.start)
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            logo -> {
                showOrCloseMenu()
            }

            status -> {
                if (AudioAccessibilityService.isRunning()) {
                    EventBus.getDefault().post(TaskEvent(STOP_TASK))
                } else {
                    if (rhinoService == null) {
                        shortToast("无障碍服务未启用")
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                    } else {
                        EventBus.getDefault().post(TaskEvent(START_TASK))
                    }
                }
            }

            screen -> {
                // 因部分app不能截图，上述代码就注释不用了
                takeTempCapture()
            }
        }
    }

    private fun takeTempCapture() {
        getImageReader().acquireLatestImage()?.let { image ->
            try {
                val imgPath =
                    "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/1_${System.currentTimeMillis()}.jpg";
                val bitmap = image.toBitmap()
                if (bitmap.save(imgPath, 100)) {
                    setMessage("截图成功 $imgPath")
                } else {
                    setMessage("截图失败")
                }
                bitmap.recycle()
            } catch (e: Exception) {
                showException(e)
                setMessage("截图失败")
            } finally {
                image.close()
            }
        }
    }

    private fun setMessage(message: String) {
        toastMsg.setBackgroundColor(randomMaterialColor())
        toastMsg.text = message

        handler.removeMessages(1)
        handler.sendEmptyMessageDelayed(1, 3000)
    }

    private fun showOrCloseMenu() {
        if (!menuLayout.isVisible) {
            menuLayout.visibility = View.VISIBLE
        } else {
            menuLayout.visibility = View.GONE
        }
        updateStatus()
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = logoParams.x
                initialY = logoParams.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
            }

            MotionEvent.ACTION_MOVE -> {
                logoParams.x = initialX + (event.rawX - initialTouchX).toInt()
                logoParams.y = initialY + (event.rawY - initialTouchY).toInt()
                windowManager.updateViewLayout(logoRootView, logoParams)
            }

            MotionEvent.ACTION_UP -> {
                mmkv!!.putInt("startX", logoParams.x)
                mmkv!!.putInt("startY", logoParams.y)
            }
        }
        return false
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
        if (::windowManager.isInitialized && ::logoRootView.isInitialized) {
            windowManager.removeView(logoRootView)
        }
        if (::windowManager.isInitialized && ::toastRootView.isInitialized) {
            windowManager.removeView(toastRootView)
        }
        EventBus.getDefault().unregister(this)
    }
}