package com.jiuzhuan.jsdroid.rhino

import com.android.dex.Dex
import dalvik.system.PathClassLoader
import java.io.File

class AndroidFileClassLoader(
    private val parent: ClassLoader?,
    private val cacheDir: File
) : AndroidClassLoader(parent) {
    private var instanceCount = 0
    private val dexFile: File

    init {
        val id = instanceCount
        dexFile = File(cacheDir, "$id.dex")
        cacheDir.mkdirs()
        reset()
    }

    override fun loadClass(dex: Dex, name: String): Class<*> {
        try {
            dex.writeTo(dexFile)
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
        return PathClassLoader(dexFile.path, parent).loadClass(name)
    }

    override fun getLastDex(): Dex? {
        if (dexFile.exists()) {
            try {
                return Dex(dexFile)
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
        }
        return null
    }

    override fun reset() {
        dexFile.delete()
    }
}
