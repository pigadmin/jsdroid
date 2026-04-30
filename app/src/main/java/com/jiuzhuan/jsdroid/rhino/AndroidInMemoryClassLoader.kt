package com.jiuzhuan.jsdroid.rhino

import com.android.dex.Dex
import dalvik.system.InMemoryDexClassLoader
import java.nio.ByteBuffer

class AndroidInMemoryClassLoader(private val parent: ClassLoader?) : AndroidClassLoader(parent) {
    private var lastDex: Dex? = null

    override fun loadClass(dex: Dex, name: String): Class<*> {
        lastDex = dex
        return InMemoryDexClassLoader(ByteBuffer.wrap(dex.bytes), parent).loadClass(name)
    }

    override fun getLastDex(): Dex? {
        return lastDex
    }

    override fun reset() {
        lastDex = null
    }
}
