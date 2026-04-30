//package com.jiuzhuan.jsdroid.rhino.engine
//
//import com.blankj.utilcode.util.StringUtils.getString
//import com.jiuzhuan.jsdroid.R
//import com.jiuzhuan.jsdroid.api.getMmkv
//import com.jiuzhuan.jsdroid.events.GameStatusEvent
//import com.jiuzhuan.jsdroid.events.GameStatusEvent.Companion.START
//import com.jiuzhuan.jsdroid.events.GameStatusEvent.Companion.STOP
//import com.jiuzhuan.jsdroid.rhino.api.AppApi
//import com.jiuzhuan.jsdroid.rhino.api.DisplayApi
//import com.jiuzhuan.jsdroid.rhino.api.GlobalApi
//import com.jiuzhuan.jsdroid.rhino.api.ImageApi
//import com.jiuzhuan.jsdroid.rhino.api.KeysApi
//import com.jiuzhuan.jsdroid.rhino.api.LogApi
//import com.jiuzhuan.jsdroid.rhino.api.LogApi.addLog
//import com.jiuzhuan.jsdroid.rhino.api.OcrApi
//import com.jiuzhuan.jsdroid.rhino.api.ShellApi
//import com.jiuzhuan.jsdroid.rhino.api.StoragesApi
//import com.jiuzhuan.jsdroid.rhino.api.ToastApi
//import com.jiuzhuan.jsdroid.room.level_error
//import com.jiuzhuan.jsdroid.utils.fetchJsContent
//import com.jiuzhuan.jsdroid.utils.logd
//import com.jiuzhuan.jsdroid.utils.showException
//import org.greenrobot.eventbus.EventBus
//import org.mozilla.javascript.BaseFunction
//import org.mozilla.javascript.Context
//import org.mozilla.javascript.Scriptable
//import org.mozilla.javascript.ScriptableObject
//import org.mozilla.javascript.Context as RhinoContext
//
//object ScriptEngine {
//    private var mRhinoContext: RhinoContext? = null
//    private var mRhinoScript: String = ""
//
//    private var mRhinoThread: Thread? = null
//
//    fun loadScript(script: String) {
//        mRhinoScript = script
//        EventBus.getDefault().post(GameStatusEvent(START))
//    }
//
//    fun start() {
//        if (mRhinoThread != null) {
//            stop()
//        }
//
//        mRhinoThread = Thread {
//            try {
//                mRhinoContext = RhinoContext.enter()
//                mRhinoContext?.isInterpretedMode = true
//
//                val rhinoScope = mRhinoContext?.initStandardObjects()
//                    ?: throw IllegalArgumentException()
//
//                // Register app
//                ScriptableObject.putProperty(
//                    rhinoScope,
//                    "app",
//                    RhinoContext.javaToJS(AppApi, rhinoScope)
//                )
//
//                // Register display
//                ScriptableObject.putProperty(
//                    rhinoScope,
//                    "display",
//                    RhinoContext.javaToJS(DisplayApi, rhinoScope)
//                )
//
//                // Register global
//                ScriptableObject.putProperty(
//                    rhinoScope,
//                    "global",
//                    RhinoContext.javaToJS(GlobalApi, rhinoScope)
//                )
//
//                // Register keys
//                ScriptableObject.putProperty(
//                    rhinoScope,
//                    "keys",
//                    RhinoContext.javaToJS(KeysApi, rhinoScope)
//                )
//
//                // Register log
//                ScriptableObject.putProperty(
//                    rhinoScope,
//                    "log",
//                    RhinoContext.javaToJS(LogApi, rhinoScope)
//                )
//
//                // Register ocr
//                ScriptableObject.putProperty(
//                    rhinoScope,
//                    "ocr",
//                    RhinoContext.javaToJS(OcrApi, rhinoScope)
//                )
//
//                // Register shell
//                ScriptableObject.putProperty(
//                    rhinoScope,
//                    "shell",
//                    RhinoContext.javaToJS(ShellApi, rhinoScope)
//                )
//
//                // Register image
//                ScriptableObject.putProperty(
//                    rhinoScope,
//                    "image",
//                    RhinoContext.javaToJS(ImageApi, rhinoScope)
//                )
//                // Register storage
//                ScriptableObject.putProperty(
//                    rhinoScope,
//                    "storages",
//                    RhinoContext.javaToJS(StoragesApi, rhinoScope)
//                )
//
//                // Register toast
//                ScriptableObject.putProperty(
//                    rhinoScope,
//                    "toast",
//                    RhinoContext.javaToJS(ToastApi, rhinoScope)
//                )
//
//                // require
//                ScriptableObject.putProperty(rhinoScope, "require", object : BaseFunction() {
//                    override fun call(
//                        thisContext: Context,
//                        thisScope: Scriptable,
//                        thisObj: Scriptable,
//                        args: Array<out Any>
//                    ): Any? {
//                        val commonUrl = (args.first()).toString()
//                        val commonJs = fetchJsContent(commonUrl)
//                        return mRhinoContext?.evaluateString(
//                            rhinoScope,
//                            commonJs,
//                            "JsCode",
//                            1,
//                            null
//                        )
//                    }
//                })
//
//                val mmkv = getMmkv()
//                val host = mmkv.getString("jsUrl", "")
//
//                val url = "$host$packageName.js"
//                logd("url", url)
//                val jsCode: String = fetchJsContent(url)
//                if (jsCode.isEmpty()) { // 网络因素无法获取jsCode
//                    addLog(getString(R.string.log_network_error), true, level_error)
//                    retryJs()
//                }
//                // Run script
//                mRhinoContext?.evaluateString(rhinoScope, mRhinoScript, "JsCode", 1, null)
//            } catch (exception: Exception) {
//                showException(exception)
//                addLog(exception.toString(), false, level_error)
//                Thread.currentThread().interrupt()
//            } finally {
//                RhinoContext.exit()
//                mRhinoThread = null
//                EventBus.getDefault().post(GameStatusEvent(STOP))
//            }
//        }
//        mRhinoThread?.start()
//    }
//
//    fun stop() {
//        try {
//            mRhinoThread?.interrupt()
//            mRhinoThread?.stop()
//        } catch (exception: Exception) {
//            showException(exception)
//        } finally {
//            mRhinoThread = null
//        }
//    }
//
//    fun isRunning(): Boolean {
//        val thread = mRhinoThread
//        return thread != null && thread.isAlive && !thread.isInterrupted
//    }
//}
