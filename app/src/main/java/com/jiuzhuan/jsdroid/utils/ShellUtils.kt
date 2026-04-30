package com.jiuzhuan.jsdroid.utils

import android.os.Handler
import android.os.Looper
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import kotlin.concurrent.thread

object ShellUtils {
    fun execute(command: String, requireRoot: Boolean = false): CommandResult {
        var process: Process? = null
        var os: DataOutputStream? = null
        var successReader: BufferedReader? = null
        var errorReader: BufferedReader? = null

        return try {
            process = Runtime.getRuntime().exec(if (requireRoot) "su" else "sh")
            os = DataOutputStream(process.outputStream).apply {
                writeBytes("$command\n")
                writeBytes("exit\n")
                flush()
            }

            val successMsg = BufferedReader(InputStreamReader(process.inputStream)).use {
                it.readText().trim()
            }
            val errorMsg = BufferedReader(InputStreamReader(process.errorStream)).use {
                it.readText().trim()
            }

            val exitCode = process.waitFor()
            CommandResult(
                isSuccess = exitCode == 0,
                success = successMsg,
                error = errorMsg
            )
        } catch (e: Exception) {
            CommandResult(false, "", e.message ?: "Unknown error")
        } finally {
            process?.destroy()
        }
    }

    fun executeAsync(
        command: String,
        requireRoot: Boolean = false,
        callback: (success: String, error: String) -> Unit
    ) {
        thread(start = true) {
            val result = execute(command, requireRoot)
            Handler(Looper.getMainLooper()).post {
                callback(result.success, result.error)
            }
        }
    }

    data class CommandResult(
        val isSuccess: Boolean,
        val success: String,
        val error: String
    )
}
