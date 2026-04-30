package com.jiuzhuan.jsdroid.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.inputmethodservice.InputMethodService
import android.util.Base64
import android.view.InputDevice
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import android.widget.TextView
import com.jiuzhuan.jsdroid.R


class ImeService : InputMethodService() {
    private val IME_INPUT = "IME_INPUT"
    private val IME_MESSAGE = "ADB_INPUT_TEXT"
    private val IME_CHARS = "ADB_INPUT_CHARS"
    private val IME_KEYCODE = "ADB_INPUT_CODE"
    private val IME_META_KEYCODE = "ADB_INPUT_MCODE"
    private val IME_EDITORCODE = "ADB_EDITOR_CODE"
    private val IME_MESSAGE_B64 = "ADB_INPUT_B64"
    private val IME_CLEAR_TEXT = "ADB_CLEAR_TEXT"
    private var im_content: TextView? = null

    override fun onCreateInputView(): View {
        val mInputView: View = layoutInflater.inflate(R.layout.activity_ime, null)
        im_content = mInputView.findViewById(R.id.im_content)

        val filter = IntentFilter(IME_MESSAGE)
        filter.addAction(IME_INPUT)
        filter.addAction(IME_CHARS)
        filter.addAction(IME_KEYCODE)
        filter.addAction(IME_MESSAGE)
        filter.addAction(IME_EDITORCODE)
        filter.addAction(IME_MESSAGE_B64)
        filter.addAction(IME_CLEAR_TEXT)
        registerReceiver(mReceiver, filter, RECEIVER_EXPORTED)

        return mInputView;
    }

    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (intent.action == IME_INPUT) {
                val msg = intent.getStringExtra("data")
                im_content?.setText(msg)
                if (msg != null) {
                    val ic: InputConnection = getCurrentInputConnection()
                     ic.commitText(msg, 1)
                }
            }

            if (intent.action == IME_MESSAGE) {
                val msg = intent.getStringExtra("msg")
                if (msg != null) {
                    val ic: InputConnection = getCurrentInputConnection()
                    if (ic != null) ic.commitText(msg, 1)
                }
                val metaCodes = intent.getStringExtra("mcode")
                if (metaCodes != null) {
                    val mcodes = metaCodes.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                    if (mcodes != null) {
                        var i: Int
                        val ic: InputConnection = getCurrentInputConnection()
                        i = 0
                        while (i < mcodes.size - 1) {
                            if (ic != null) {
                                val ke: KeyEvent
                                if (mcodes[i].contains("+")) {
                                    val arrCode = mcodes[i].split("\\+".toRegex())
                                        .dropLastWhile { it.isEmpty() }
                                        .toTypedArray()
                                    ke = KeyEvent(
                                        0, 0, KeyEvent.ACTION_DOWN,
                                        mcodes[i + 1].toString().toInt(),
                                        0,
                                        arrCode[0].toString().toInt() or arrCode[1].toString()
                                            .toInt(),
                                        0,
                                        0,
                                        KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE,
                                        InputDevice.SOURCE_KEYBOARD
                                    )
                                } else {
                                    ke = KeyEvent(
                                        0, 0, KeyEvent.ACTION_DOWN,
                                        mcodes[i + 1].toString().toInt(),
                                        0,
                                        mcodes[i].toString().toInt(),
                                        0,
                                        0,
                                        KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE,
                                        InputDevice.SOURCE_KEYBOARD
                                    )
                                }
                                ic.sendKeyEvent(ke)
                            }
                            i = i + 2
                        }
                    }
                }
            }

            if (intent.action == IME_MESSAGE_B64) {
                val data = intent.getStringExtra("msg")

                val b64: ByteArray = Base64.decode(data, Base64.DEFAULT)
                var msg = "NOT SUPPORTED"
                try {
                    msg = String(b64, charset("UTF-8"))
                } catch (e: Exception) {
                }

                if (msg != null) {
                    val ic: InputConnection = getCurrentInputConnection()
                    if (ic != null) ic.commitText(msg, 1)
                }
            }

            if (intent.action == IME_CHARS) {
                val chars = intent.getIntArrayExtra("chars")
                if (chars != null) {
                    val msg = String(chars, 0, chars.size)
                    val ic: InputConnection = getCurrentInputConnection()
                    if (ic != null) ic.commitText(msg, 1)
                }
            }

            if (intent.action == IME_KEYCODE) {
                val code = intent.getIntExtra("code", -1)
                if (code != -1) {
                    val ic: InputConnection = getCurrentInputConnection()
                    if (ic != null) ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
                }
            }

            if (intent.action == IME_EDITORCODE) {
                val code = intent.getIntExtra("code", -1)
                if (code != -1) {
                    val ic: InputConnection = getCurrentInputConnection()
                    if (ic != null) ic.performEditorAction(code)
                }
            }

            if (intent.action == IME_CLEAR_TEXT) {
                val ic: InputConnection = getCurrentInputConnection()
                if (ic != null) {
                    val curPos = ic.getExtractedText(ExtractedTextRequest(), 0).text
                    val beforePos = ic.getTextBeforeCursor(curPos.length, 0)
                    val afterPos = ic.getTextAfterCursor(curPos.length, 0)
                    ic.deleteSurroundingText(beforePos!!.length, afterPos!!.length)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mReceiver);
    }
}
