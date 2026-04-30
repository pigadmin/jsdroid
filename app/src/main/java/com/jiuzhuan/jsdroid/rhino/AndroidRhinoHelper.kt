package com.jiuzhuan.jsdroid.rhino

import org.mozilla.javascript.ContextFactory
import org.mozilla.javascript.SecurityController
import java.io.File
import android.content.Context as AndroidContext
import org.mozilla.javascript.Context as RhinoContext

class AndroidRhinoHelper(private val cacheDir: File) {
    constructor(context: AndroidContext) : this(File(context.cacheDir, "classes"))

    fun enter(): RhinoContext {
        if (!SecurityController.hasGlobal()) {
            SecurityController.initGlobal(AndroidNoSecurityController())
        }
        val rhinoContext = getContextFactory().enterContext()
        rhinoContext.isInterpretedMode = true
        return rhinoContext
    }

    fun getContextFactory(): AndroidContextFactory {
        val factory: AndroidContextFactory
        if (!ContextFactory.hasExplicitGlobal()) {
            factory = createAndroidContextFactory(cacheDir)
            ContextFactory.getGlobalSetter().contextFactoryGlobal = factory
        } else if (ContextFactory.getGlobal() !is AndroidContextFactory) {
            throw IllegalStateException("Can't initialize factory for Android Rhino: There is already anther factory")
        } else {
            factory = ContextFactory.getGlobal() as AndroidContextFactory
        }
        return factory
    }

    fun createAndroidContextFactory(cacheDir: File): AndroidContextFactory {
        return AndroidContextFactory(cacheDir)
    }
}
