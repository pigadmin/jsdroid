package com.jiuzhuan.jsdroid.utils

import android.app.AlertDialog
import android.content.Intent
import android.provider.Settings
import com.jiuzhuan.jsdroid.base.activity

fun openAccessibility() {
    if (activity == null) return
    AlertDialog.Builder(activity).setTitle("需要无障碍权限").setMessage("请前往设置开启服务")
        .setPositiveButton("去开启") { _, _ ->
            activity.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }.show()
}
