eg:
  val inAndroid = false

        if (inAndroid) {
            val rhinoHelper = AndroidRhinoHelper(this)
            val rhinoContext = rhinoHelper.enter()
            rhinoContext.isInterpretedMode = true
            val rhinoScope = ImporterTopLevel(rhinoContext)

            // case 1
            val rhinoResult =
                rhinoContext.evaluateString(rhinoScope, "var a = 1; a;", "unknown", 1, null)
            // rhinoResult=1
            Log.e("RhinoService", "rhinoResult=${RhinoContext.toString(rhinoResult)}")

            // case 2
            ScriptableObject.putProperty(
                rhinoScope, "RhinoTester", RhinoContext.javaToJS(RhinoTester, rhinoScope)
            )
            rhinoContext.evaluateString(rhinoScope, "RhinoTester.v();", "unknown", 1, null)
            rhinoContext.evaluateString(
                rhinoScope, "RhinoTester.v('rhino for android');", "unknown", 1, null
            )
        } else {
            val rhinoContext = RhinoContext.enter()
            rhinoContext.isInterpretedMode = true
            val rhinoScope = rhinoContext.initStandardObjects()

//            ScriptableObject.putProperty(
//                rhinoSpt, "back", FunctionObject(
//                    "back", KeysApi::class.java.getMethod("back", String::class.java), rhinoSpt
//                )
//            )

//            ScriptableObject.putProperty(
//                rhinoScope,
//                "back",
//                KeysApi::class.java.getMethod("back", String::class.java)
//            )

//            ScriptableObject.putProperty(rhinoScope, "Keys", KeysApi())

            ScriptableObject.putProperty(rhinoScope, "RhinoTester", RhinoTester)
            rhinoContext.evaluateString(rhinoScope, "RhinoTester.v();", "unknown", 1, null)
//            rhinoContext.evaluateString(
//                rhinoScope,
//                "RhinoTester.v('rhino for jvm');",
//                "unknown",
//                1,
//                null
//            )

            // rhinoContext.evaluateString(rhinoScope, "Key.back('xxx')", "xxx", 1, null)
//            rhinoContext.evaluateString(rhinoScope, "Keys.home();", "xxxxxx", 1, null)
        }

//        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
//        startActivity(intent)

//        rhinoCtx = Context.enter()
//        rhinoSpt = rhinoCtx.initStandardObjects()

//        init()