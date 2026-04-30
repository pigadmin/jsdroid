@file:JvmName("KeysApi")

package com.jiuzhuan.jsdroid.rhino.api

import com.ss.android.ugc.aweme.live.livehostimpl.handlePerformGlobalAction
import com.jiuzhuan.jsdroid.utils.GLOBAL_ACTION_BACK
import com.jiuzhuan.jsdroid.utils.GLOBAL_ACTION_HOME
import com.jiuzhuan.jsdroid.utils.GLOBAL_ACTION_LOCK_SCREEN
import com.jiuzhuan.jsdroid.utils.GLOBAL_ACTION_NOTIFICATIONS
import com.jiuzhuan.jsdroid.utils.GLOBAL_ACTION_POWER_DIALOG
import com.jiuzhuan.jsdroid.utils.GLOBAL_ACTION_QUICK_SETTINGS
import com.jiuzhuan.jsdroid.utils.GLOBAL_ACTION_RECENTS
import com.jiuzhuan.jsdroid.utils.GLOBAL_ACTION_TAKE_SCREENSHOT
import com.jiuzhuan.jsdroid.utils.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN

object KeysApi {
    @JvmStatic
    fun back(): Boolean {
        return handlePerformGlobalAction(GLOBAL_ACTION_BACK)
    }

    @JvmStatic
    fun home(): Boolean {
        return handlePerformGlobalAction(GLOBAL_ACTION_HOME)
    }

    @JvmStatic
    fun powerDialog(): Boolean {
        return handlePerformGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
    }

    @JvmStatic
    fun notifications(): Boolean {
        return handlePerformGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    }

    @JvmStatic
    fun quickSettings(): Boolean {
        return handlePerformGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
    }

    @JvmStatic
    fun recents(): Boolean {
        return handlePerformGlobalAction(GLOBAL_ACTION_RECENTS)
    }

    @JvmStatic
    fun splitScreen(): Boolean {
        return handlePerformGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)
    }

    @JvmStatic
    fun takeScreenshot(): Boolean {
        return handlePerformGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
    }

    @JvmStatic
    fun lockScreen(): Boolean {
        return handlePerformGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
    }

    @JvmStatic
    fun inputKey(keyCode: Int): Boolean {
        return handlePerformGlobalAction(keyCode)
    }
}
