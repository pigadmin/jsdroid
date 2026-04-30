package com.jiuzhuan.jsdroid.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.jiuzhuan.jsdroid.utils.logd

class IsolatedProcessService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        logd("IsolatedProcessService", "onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        logd("IsolatedProcessService", "onDestroy")
    }
}
