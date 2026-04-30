package com.ss.android.ugc.aweme.live.livehostimpl

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jiuzhuan.jsdroid.R
import com.jiuzhuan.jsdroid.api.getApp
import com.jiuzhuan.jsdroid.entity.JsEntity
import com.jiuzhuan.jsdroid.events.GameStatusEvent
import com.jiuzhuan.jsdroid.events.GameStatusEvent.Companion.START
import com.jiuzhuan.jsdroid.events.GameStatusEvent.Companion.STOP
import com.jiuzhuan.jsdroid.events.TaskEvent
import com.jiuzhuan.jsdroid.events.TaskEvent.Companion.NEXT_TASK
import com.jiuzhuan.jsdroid.events.TaskEvent.Companion.START_TASK
import com.jiuzhuan.jsdroid.events.TaskEvent.Companion.STOP_TASK
import com.jiuzhuan.jsdroid.rhino.api.AppApi
import com.jiuzhuan.jsdroid.rhino.api.DisplayApi
import com.jiuzhuan.jsdroid.rhino.api.GlobalApi
import com.jiuzhuan.jsdroid.rhino.api.ImageApi
import com.jiuzhuan.jsdroid.rhino.api.KeysApi
import com.jiuzhuan.jsdroid.rhino.api.LogApi
import com.jiuzhuan.jsdroid.rhino.api.LogApi.addLog
import com.jiuzhuan.jsdroid.rhino.api.OcrApi
import com.jiuzhuan.jsdroid.rhino.api.ShellApi
import com.jiuzhuan.jsdroid.rhino.api.StoragesApi
import com.jiuzhuan.jsdroid.rhino.api.ToastApi
import com.jiuzhuan.jsdroid.room.level_error
import com.jiuzhuan.jsdroid.room.level_success
import com.jiuzhuan.jsdroid.room.level_warn
import com.jiuzhuan.jsdroid.utils.fetchJsContent
import com.jiuzhuan.jsdroid.utils.showException
import com.tencent.mmkv.MMKV
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.json.JSONObject
import org.mozilla.javascript.BaseFunction
import org.mozilla.javascript.Context
import org.mozilla.javascript.ContextFactory
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.WrappedException

var currentPackageName: String = ""
var currentActivityName: String = ""
var rhinoService: AudioAccessibilityService? = null

class AudioAccessibilityService : AccessibilityService() {
    val mmkv = MMKV.defaultMMKV()
    val gson = Gson()
    private var mRhinoContext: Context? = null

    companion object {
        private var mThread: Thread? = null
        var isInterrupt = true
        fun isRunning(): Boolean {
            val thread = mThread
            return thread != null && thread.isAlive && !thread.isInterrupted
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val packageName = event.packageName?.toString()
                val className = event.className?.toString()
                if (packageName != null && packageName != "") {
                    currentPackageName = packageName
                }
                if (className != null && packageName != "") {
                    currentActivityName = className
                }
            }
        }
    }

    override fun onServiceConnected() {
        addLog(getString(R.string.log_service_start), true)
        rhinoService = this
    }


    override fun onInterrupt() {
        addLog(getString(R.string.log_service_stop), true)
        rhinoService = null
    }

    override fun onCreate() {
        super.onCreate()
        EventBus.getDefault().register(this)
    }

    @Subscribe
    fun onTaskEvent(event: TaskEvent) {
        when (event.status) {
            STOP_TASK -> {
                stopTask()
            }

            START_TASK -> {
                reStart()
            }

            NEXT_TASK -> {
                nextTask()
            }
        }
    }

    private fun startTask() {
        InterruptibleContextFactory()
        ContextFactory.getGlobal().enter()
        stopTask()
        isInterrupt = false
        EventBus.getDefault().post(GameStatusEvent(START))
        mThread = Thread {
            try {
                mRhinoContext = Context.enter()
                mRhinoContext?.isInterpretedMode = true

                val rhinoScope =
                    mRhinoContext?.initStandardObjects() ?: throw IllegalArgumentException()

                // 设置指令阈值（每执行1000条指令检查一次）
                mRhinoContext?.setInstructionObserverThreshold(1000)

                // Register app
                ScriptableObject.putProperty(
                    rhinoScope, "app", Context.javaToJS(AppApi, rhinoScope)
                )

                // Register display
                ScriptableObject.putProperty(
                    rhinoScope, "display", Context.javaToJS(DisplayApi, rhinoScope)
                )

                // Register global
                ScriptableObject.putProperty(
                    rhinoScope, "global", Context.javaToJS(GlobalApi, rhinoScope)
                )

                // Register keys
                ScriptableObject.putProperty(
                    rhinoScope, "keys", Context.javaToJS(KeysApi, rhinoScope)
                )

                // Register log
                ScriptableObject.putProperty(
                    rhinoScope, "log", Context.javaToJS(LogApi, rhinoScope)
                )

                // Register ocr
                ScriptableObject.putProperty(
                    rhinoScope, "ocr", Context.javaToJS(OcrApi, rhinoScope)
                )

                // Register shell
                ScriptableObject.putProperty(
                    rhinoScope, "shell", Context.javaToJS(ShellApi, rhinoScope)
                )

                // Register image
                ScriptableObject.putProperty(
                    rhinoScope, "image", Context.javaToJS(ImageApi, rhinoScope)
                )
                // Register storage
                ScriptableObject.putProperty(
                    rhinoScope, "storages", Context.javaToJS(StoragesApi, rhinoScope)
                )

                // Register toast
                ScriptableObject.putProperty(
                    rhinoScope, "toast", Context.javaToJS(ToastApi, rhinoScope)
                )

                // require
                ScriptableObject.putProperty(rhinoScope, "require", object : BaseFunction() {
                    override fun call(
                        thisContext: Context,
                        thisScope: Scriptable,
                        thisObj: Scriptable,
                        args: Array<out Any>
                    ): Any? {
                        val commonUrl = (args.first()).toString()
                        val commonJs = fetchJsContent(commonUrl)
                        return mRhinoContext?.evaluateString(
                            rhinoScope, commonJs, "JsCode", 1, null
                        )
                    }
                })

                val needCash = mmkv.getString("autoTx", "false").toBoolean()
                val json = mmkv.getString(if (!needCash) "taskList" else "txList", "[]") ?: "[]"

                val list: List<JsEntity> =
                    gson.fromJson(json, object : TypeToken<List<JsEntity>>() {}.type)

                if (list.isEmpty()) {// 任务列表空了
                    addLog(getApp().getString(R.string.log_no_tasklist), true, level_error)
                    mThread?.interrupt()
                    EventBus.getDefault().post(GameStatusEvent(STOP))
                    return@Thread
                }

                val currentTask: JsEntity = list[0]
                mmkv.putString("currentTask", gson.toJson(currentTask))
                val name: String = currentTask.name // 游戏名称
                val packageName: String = currentTask.packageName // 游戏包名
                val gTime: Int = currentTask.gtime // 运行时间
                val taskSize =
                    mmkv.getString(if (!needCash) "taskSize" else "txSize", "0")?.toInt() ?: 0

                if (needCash) {
                    addLog(
                        getApp().getString(R.string.log_tx_task)
                            .replace("arg0", "${taskSize - list.size + 1}/$taskSize")
                            .replace("arg1", "${list.size}").replace("arg2", name),
                        true,
                        level_success
                    )
                } else {
                    addLog(
                        getApp().getString(R.string.log_game_task)
                            .replace("arg0", "${taskSize - list.size + 1}/$taskSize")
                            .replace("arg1", "${list.size}").replace("arg2", name)
                            .replace("arg3", gTime.toString()), true, level_success
                    )
                }
                val host = mmkv.getString("jsUrl", "")
                val url = "$host$packageName.js"
                val jsCode = fetchJsContent(url)
                addLog(
                    getApp().getString(R.string.log_start).replace("arg0", name),
                    true,
                    level_success
                )
                mRhinoContext?.evaluateString(rhinoScope, jsCode, "JsCode", 1, null)
            } catch (e: InterruptedException) {
                showException(e)
                addLog("---已停止1---", true, level_success)
            } catch (e: WrappedException) {
                showException(e)
                addLog("---已停止2---截图服务已停止，请尝试重新运行app", true, level_success)
            } catch (e: NullPointerException) {
                showException(e)
                addLog("---已停止3---", true, level_success)
            } catch (e: Exception) {
//                Thread.currentThread().interrupt() //加了停不下来
                showException(e)
                if (!e.toString().contains("InterruptedException") && !e.toString()
                        .contains("InterruptedIOException")
                ) {
                    addLog("---异常信息：---${e}", true, level_error)
                }
            } finally {
                Context.exit()
            }
        }.apply {
            name = "js-thread"
            start()
        }
    }


    inner class InterruptibleContextFactory : ContextFactory() {
        override fun observeInstructionCount(cx: Context, instructionCount: Int) {
            if (isInterrupt) {
                throw Error("Script execution interrupted")
            }
        }

        override fun makeContext(): Context {
            val cx = super.makeContext()
            cx.instructionObserverThreshold = 1000 // 设置检查频率
            return cx
        }
    }

    private fun stopTask() {
        try {
            addLog("---正在停止---", true, level_warn)
          isInterrupt = true
            if (mRhinoContext != null) {
                // 触发 Rhino 中断检查
                mRhinoContext!!.isGeneratingDebug = false
                mRhinoContext!!.optimizationLevel = -1
            }

            if (mThread != null && mThread!!.isAlive) {
                mThread!!.interrupt()
            }
        } finally {
            EventBus.getDefault().post(GameStatusEvent(STOP))
        }
    }

    private fun nextTask() {
        val needCash = mmkv.getString("autoTx", "false").toBoolean()
        val json = mmkv.getString(if (!needCash) "taskList" else "txList", "[]") ?: "[]"
        val list: List<JsEntity> = gson.fromJson(json, object : TypeToken<List<JsEntity>>() {}.type)
        val reList = list.drop(1).toTypedArray()
        if (reList.isEmpty()) {
            mmkv.remove(if (!needCash) "taskList" else "txList")
            if (!needCash) {
                val jsonStr = mmkv.getString("_setting", "")
                val jsonObject = jsonStr?.let { JSONObject(it) }
                val isTixian = jsonObject?.getBoolean("isTixian")!!
                if (isTixian) {// 去提现
                    addLog(getApp().getString(R.string.log_needgo_carsh), true, "success")
                    mmkv.putString("autoTx", "true")
                    startTask()
                } else {
                    addLog(getApp().getString(R.string.log_tasklist_empty), true, "success")
                    EventBus.getDefault().post(GameStatusEvent(GameStatusEvent.OVER))
                }
            } else {
                addLog(getApp().getString(R.string.log_tasktxlist_empty), true, "success")
                EventBus.getDefault().post(GameStatusEvent(GameStatusEvent.OVER))
            }
        } else {
            mmkv.putString(if (!needCash) "taskList" else "txList", gson.toJson(reList))
            startTask()
        }
    }

    private fun reStart() { // 后期定时启动可以调用这里
        val txJson = mmkv.containsKey("txList")
        val taskJson = mmkv.containsKey("taskList")
        if (!txJson) {
            val keepTxList = mmkv.getString("keepTxList", "[]")
            mmkv.putString("txList", keepTxList)
        }
        if (!taskJson) {
            val keepTaskList = mmkv.getString("keepTaskList", "[]")
            mmkv.putString("taskList", keepTaskList)
        }
        startTask()
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }
}


fun handlePerformGlobalAction(action: Int): Boolean {
    return rhinoService?.performGlobalAction(action) ?: false
}

fun handleClick(x: Int, y: Int, duration: Int): Boolean {
    val gestureBuilder = GestureDescription.Builder()
    val clickPath = Path().apply {
        moveTo(x.toFloat(), y.toFloat())
    }
    val clickGesture = gestureBuilder.addStroke(
        GestureDescription.StrokeDescription(
            clickPath, 0, duration.toLong()
        )
    ).build()
    return rhinoService?.dispatchGesture(clickGesture, null, null) ?: false
}

fun handleSwipe(startX: Int, startY: Int, endX: Int, endY: Int, duration: Int): Boolean {
    val gestureBuilder = GestureDescription.Builder()
    val swipePath = Path().apply {
        moveTo(startX.toFloat(), startY.toFloat())
        lineTo(endX.toFloat(), endY.toFloat())
    }
    val swipeGesture = gestureBuilder.addStroke(
        GestureDescription.StrokeDescription(
            swipePath, 0, duration.toLong()
        )
    ).build()
    return rhinoService?.dispatchGesture(swipeGesture, null, null) ?: false
}

fun handleClickById(viewId: String) {
    rhinoService?.rootInActiveWindow?.let { root ->
        val nodes = root.findAccessibilityNodeInfosByViewId(viewId)
        nodes?.firstOrNull()?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }
}

fun handleClickByText(text: String) {
    rhinoService?.rootInActiveWindow?.let { root ->
        val nodes = root.findAccessibilityNodeInfosByText(text)
        nodes?.firstOrNull()?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }
}


