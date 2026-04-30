package com.jiuzhuan.jsdroid.base

import android.app.Service
import android.content.Intent
import android.os.IBinder

abstract class BaseService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }
}
