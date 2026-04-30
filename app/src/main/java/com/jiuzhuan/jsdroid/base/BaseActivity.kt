package com.jiuzhuan.jsdroid.base

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity


lateinit var activity: Activity

abstract class BaseActivity : ComponentActivity() {
    protected abstract fun initView()
    protected abstract fun initData()
    protected abstract fun initListener()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity = this
        initView()
        initData()
        initListener()
    }
}
