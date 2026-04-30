package com.jiuzhuan.jsdroid.rhino

import android.os.Build
import org.mozilla.javascript.Context
import org.mozilla.javascript.ContextFactory
import org.mozilla.javascript.GeneratedClassLoader
import java.io.File

class AndroidContextFactory(private val cacheDir: File) : ContextFactory() {
    init {
        initApplicationClassLoader(createClassLoader(AndroidClassLoader::class.java.classLoader))
    }

    override fun createClassLoader(parent: ClassLoader?): AndroidClassLoader {
        // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        //    return AndroidInMemoryClassLoader(parent)
        // }
        // return AndroidFileClassLoader(parent, cacheDir)

        return AndroidInMemoryClassLoader(parent)
    }

    override fun onContextReleased(cx: Context) {
        super.onContextReleased(cx)
        (cx.applicationClassLoader as AndroidClassLoader).reset()
    }

    override fun observeInstructionCount(cx: Context?, instructionCount: Int) {
        super.observeInstructionCount(cx, instructionCount)
    }
}
