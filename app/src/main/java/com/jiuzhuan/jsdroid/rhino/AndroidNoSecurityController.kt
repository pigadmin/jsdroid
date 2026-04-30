package com.jiuzhuan.jsdroid.rhino

import org.mozilla.javascript.Context
import org.mozilla.javascript.GeneratedClassLoader
import org.mozilla.javascript.SecurityController

class AndroidNoSecurityController : SecurityController() {
    override fun createClassLoader(
        parentLoader: ClassLoader?,
        securityDomain: Any?
    ): GeneratedClassLoader {
        return Context.getCurrentContext().createClassLoader(parentLoader)
    }

    override fun getDynamicSecurityDomain(securityDomain: Any?): Any? {
        return null
    }
}
