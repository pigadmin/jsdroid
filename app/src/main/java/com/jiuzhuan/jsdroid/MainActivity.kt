package com.jiuzhuan.jsdroid

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.blankj.utilcode.util.ToastUtils
import com.jiuzhuan.jsdroid.api.getApp
import com.jiuzhuan.jsdroid.api.getMmkv
import com.jiuzhuan.jsdroid.base.BaseActivity
import com.jiuzhuan.jsdroid.databinding.ActivityMainBinding
import com.jiuzhuan.jsdroid.events.CapEvent
import com.jiuzhuan.jsdroid.service.KeepAliveService
import com.jiuzhuan.jsdroid.utils.REQUEST_CODE_MANAGE_STORAGE
import com.jiuzhuan.jsdroid.utils.REQUEST_CODE_SCREEN_CAPTURE
import com.jiuzhuan.jsdroid.utils.showException
import com.ss.android.ugc.aweme.live.livehostimpl.AudioAccessibilityService
import org.greenrobot.eventbus.EventBus
import kotlin.system.exitProcess

class MainActivity : BaseActivity() {
    private lateinit var mBinding: ActivityMainBinding
    private lateinit var mediaProjectionManager: MediaProjectionManager

    private var mLastPressedTime: Long = 0

    override fun initView() {
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        mediaProjectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun initData() {
        mBinding.browser.init(this)
        startCaptureService()
        prepareDisplay()
        checkFloatPermission()
        checkAccessibilityPermission()
        checkStoragePermission()
        requestScreenCapturePermission()
    }

    override fun initListener() {

    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.repeatCount == 0) {
            if (mBinding.browser.canGoBackOrForward(-1)) {
                mBinding.browser.goBack()
            } else {
                val currentTime = System.currentTimeMillis()
                val interval = currentTime - mLastPressedTime
                if (interval > 2000) {
                    mLastPressedTime = currentTime
                    ToastUtils.showShort(R.string.desc_exit)
                } else {
                    finish()
                }
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    // 处理授权结果
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE) {
            if (resultCode == RESULT_OK && data != null) {
                EventBus.getDefault().post(CapEvent(resultCode, data))
            } else {
                Toast.makeText(this, "屏幕捕获权限被拒绝", Toast.LENGTH_SHORT).show()
                exitProcess(0)
            }
        }
    }

    private fun checkFloatPermission() {
        if (!Settings.canDrawOverlays(this)) {
            AlertDialog.Builder(this).setTitle("需要悬浮窗权限").setMessage("请前往设置开启服务")
                .setCancelable(false).setPositiveButton("去开启") { _, _ ->
                    finish()
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                }.show()
        }
    }

    private fun checkAccessibilityPermission() {
        val serviceName = ComponentName(
            this, AudioAccessibilityService::class.java
        ).flattenToString()

        val enabledServices = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )

        if (enabledServices?.contains(serviceName) != true) {
            try {
                AlertDialog.Builder(this).setTitle("需要无障碍权限")
                    .setMessage("请前往设置开启服务")
                    .setPositiveButton("去开启") { _, _ ->
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }.show()
            } catch (e: Exception) {
                showException(e)
            }
        }
    }

    private fun prepareDisplay() {
        val mmkv = getMmkv()

        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            val bounds = windowMetrics.bounds
            mmkv.putInt("width", bounds.width())
            mmkv.putInt("height", bounds.height())
            mmkv.putInt("dpi", resources.configuration.densityDpi)
        } else {
            windowManager.defaultDisplay.getRealMetrics(metrics)
            mmkv.putInt("width", metrics.widthPixels)
            mmkv.putInt("height", metrics.heightPixels)
            mmkv.putInt("dpi", metrics.densityDpi)
        }

        if (!mmkv.contains("saveImageDir")) {
            mmkv.putString("saveImageDir", cacheDir.absolutePath)
        }
    }

    private fun startCaptureService() {
        val serviceIntent = Intent(this, KeepAliveService::class.java)
        startForegroundService(serviceIntent)
    }

    private fun requestScreenCapturePermission() {
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        startActivityForResult(captureIntent, REQUEST_CODE_SCREEN_CAPTURE)
    }


    override fun onDestroy() {
        super.onDestroy()
//        unbindService(mServiceConnection)
    }

    private fun checkStoragePermission() {
        if (!hasFullStorageAccess()) {
            requestFullStorageAccess(this)
        }
    }

    private fun hasFullStorageAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                getApp(), Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestFullStorageAccess(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = "package:${activity.packageName}".toUri()
            }
            activity.startActivityForResult(intent, REQUEST_CODE_MANAGE_STORAGE)
        } else {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_CODE_MANAGE_STORAGE
            )
        }
    }
}
