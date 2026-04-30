package com.jiuzhuan.jsdroid

import android.Manifest
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import com.jiuzhuan.jsdroid.base.BaseActivity
import com.jiuzhuan.jsdroid.service.IsolatedProcessService
import com.jiuzhuan.jsdroid.utils.logd
import com.jiuzhuan.jsdroid.utils.showException

class LaunchActivity : BaseActivity() {
    override fun initView() {
    }

    override fun initData() {
        requestPermissions()
        setLauncherWallPaper()
        ignoreBatteryOptimizer()
        startIsolatedProcessService()
        proceedToMain()
    }

    override fun initListener() {
    }

    private fun proceedToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.QUERY_ALL_PACKAGES),
                0x001
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        deviceId: Int
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
        if (requestCode == 0x001) {
            Log.e(
                "LaunchActivity",
                "grantResult=${grantResults.first() == PackageManager.PERMISSION_GRANTED}"
            )
            val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            for (app in apps) {
                if ((app.flags and ApplicationInfo.FLAG_SYSTEM) == 0) {
                    Log.e(
                        "LaunchActivity",
                        "app=[${packageManager.getApplicationLabel(app)}](${app.packageName})"
                    )
                }
            }
        }
    }

    private fun ignoreBatteryOptimizer() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            logd(
                "LaunchActivity",
                "isIgnoreBattery=${powerManager.isIgnoringBatteryOptimizations(packageName)}"
            )
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = "package:$packageName".toUri()
                startActivity(intent)
            }
        } catch (e: Exception) {
            showException(e)
        }
    }

    private fun startIsolatedProcessService() {
        val intent = Intent(this, IsolatedProcessService::class.java)
        startService(intent)
    }

    private fun setLauncherWallPaper() {
        try {
            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.black_bg)
            WallpaperManager.getInstance(this)
                .setBitmap(bitmap, null, false, WallpaperManager.FLAG_SYSTEM)
        } catch (ignored: Exception) {
            showException(ignored)
        }
    }
}
