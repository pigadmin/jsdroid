package com.jiuzhuan.jsdroid.rhino

import android.util.Log
import com.android.dex.Dex
import com.android.dx.cf.direct.DirectClassFile
import com.android.dx.cf.direct.StdAttributeFactory
import com.android.dx.command.dexer.DxContext
import com.android.dx.dex.DexOptions
import com.android.dx.dex.cf.CfOptions
import com.android.dx.dex.cf.CfTranslator
import com.android.dx.dex.file.DexFile
import com.android.dx.merge.CollisionPolicy
import com.android.dx.merge.DexMerger
import org.mozilla.javascript.GeneratedClassLoader

abstract class AndroidClassLoader(parent: ClassLoader?) : ClassLoader(parent),
    GeneratedClassLoader {
    override fun defineClass(name: String, data: ByteArray): Class<*> {
        try {
            val dexOptions = DexOptions()
            val dexFile = DexFile(dexOptions)
            val classFile = DirectClassFile(data, name.replace(".", "/") + ".class", true)
            classFile.setAttributeFactory(StdAttributeFactory.THE_ONE)
            classFile.magic
            val dxContext = DxContext()
            dexFile.add(
                CfTranslator.translate(
                    dxContext,
                    classFile,
                    null,
                    CfOptions(),
                    dexOptions,
                    dexFile
                )
            )
            var dex = Dex(dexFile.toDex(null, false))
            val oldDex = getLastDex()
            if (oldDex != null) {
                dex = DexMerger(
                    arrayOf(dex, oldDex),
                    CollisionPolicy.KEEP_FIRST,
                    dxContext
                ).merge()
            }
            return loadClass(dex, name)
        } catch (exception: Exception) {
            throw FatalLoadingException(exception)
        }
    }

    override fun linkClass(cl: Class<*>?) = Unit

    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        var loadedClass = findLoadedClass(name)
        if (loadedClass == null) {
            val dex = getLastDex()
            if (dex != null) {
                loadedClass = loadClass(dex, name)
            }
            if (loadedClass == null) {
                loadedClass = parent.loadClass(name)
            }
        }
        Log.e("AndroidClassLoader", "loadedClass=$loadedClass")
        return loadedClass
    }

    @Throws(ClassNotFoundException::class)
    abstract fun loadClass(dex: Dex, name: String): Class<*>

    abstract fun getLastDex(): Dex?

    abstract fun reset()

    class FatalLoadingException(throwable: Throwable) :
        RuntimeException("Failed to define class", throwable)
}
