package com.jiuzhuan.jsdroid.events

data class GameStatusEvent(val status: Int) {
    companion object {
        const val OVER: Int = -1
        const val STOP: Int = 0
        const val START: Int = 1
    }
}
