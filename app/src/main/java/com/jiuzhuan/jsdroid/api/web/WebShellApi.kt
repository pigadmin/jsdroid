package com.jiuzhuan.jsdroid.api.web

import android.webkit.JavascriptInterface
import com.jiuzhuan.jsdroid.utils.execCmd
import com.jiuzhuan.jsdroid.utils.showException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

class WebShellApi {
    private val TAG: String = javaClass.simpleName

    @JavascriptInterface
    fun cmd(args: String): String {
        try {
            val start = System.currentTimeMillis()
            val commands = args.split("\n".toRegex()).toTypedArray()
            val msg = runBlocking {
                val deferred = GlobalScope.async {
                    execCmd(*commands)
                }
                deferred.await()
            }
            val result = msg.ifEmpty {
                ""
            }
//            logd(
//                TAG,
//                "cmd：${args}",
//                "result：${result}",
//                "time：${(System.currentTimeMillis() - start)}ms"
//            )
            return result
        } catch (e: Exception) {
            showException(e)
            return ""
        }
    }

    @JavascriptInterface
    fun fastCmd(args: String): String {
        try {
            val start = System.currentTimeMillis()
            val commands = args.split("\n".toRegex()).toTypedArray()
            val result = execCmd(*commands)
//            logd(
//                TAG,
//                "cmd：${args}",
//                "result：${result}",
//                "time：${(System.currentTimeMillis() - start)}ms"
//            )
            return result
        } catch (e: Exception) {
            showException(e)
            return ""
        }
    }
}
