package com.jiuzhuan.jsdroid.events

data class TaskEvent(val status: Int) {
    companion object {
        const val STOP_TASK: Int = 0
        const val START_TASK: Int = 1
        const val NEXT_TASK: Int = 2
    }
}