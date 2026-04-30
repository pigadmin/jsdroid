package com.jiuzhuan.jsdroid.ui

import android.app.Activity
import android.os.Bundle
import androidx.constraintlayout.widget.ConstraintLayout
import com.jiuzhuan.jsdroid.R

class OverActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_over)
        val over = findViewById<ConstraintLayout>(R.id.over)
        over.setOnClickListener { finish() }
    }
}