@file:JvmName("ShellApi")

package com.jiuzhuan.jsdroid.rhino.api

import com.jiuzhuan.jsdroid.utils.execCmd
import com.jiuzhuan.jsdroid.utils.logd
import com.jiuzhuan.jsdroid.utils.showException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

object ShellApi {
    @JvmStatic
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
//                "ShellApi",
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

    @JvmStatic
    fun fastCmd(args: String): String {
        try {
            var start = System.currentTimeMillis()
            val commands = args.split("\n".toRegex()).toTypedArray()
            val result = execCmd(*commands)
            logd(
                "ShellApi",
                "cmd：${args}",
                "result：${result}",
                "time：${(System.currentTimeMillis() - start)}ms"
            )
            return result
        } catch (e: Exception) {
            showException(e)
            return ""
        }
    }
}
