package com.jiuzhuan.jsdroid.widget

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Context.DOWNLOAD_SERVICE
import android.net.Uri
import android.util.AttributeSet
import android.webkit.CookieManager
import android.webkit.CookieSyncManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import com.jiuzhuan.jsdroid.BuildConfig
import com.jiuzhuan.jsdroid.MainActivity
import com.jiuzhuan.jsdroid.R
import com.jiuzhuan.jsdroid.utils.logd
import com.jiuzhuan.jsdroid.api.web.WebBaseApi
import com.jiuzhuan.jsdroid.api.web.WebDeviceApi
import com.jiuzhuan.jsdroid.api.web.WebJsApi
import com.jiuzhuan.jsdroid.api.web.WebLogApi
import com.jiuzhuan.jsdroid.api.web.WebMmkvApi
import com.jiuzhuan.jsdroid.api.web.WebShellApi
import com.jiuzhuan.jsdroid.api.web.WebToastApi
import com.tencent.mmkv.MMKV

class Browser : WebView {
    private val TAG: String = javaClass.simpleName

    private var activity: Activity? = null
    private var mmkv: MMKV = MMKV.defaultMMKV()
    private var downloadId: Long = -1
    private var downloadManager: DownloadManager? = null

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    fun init(ac: Activity) {
        activity = ac
        mmkv = MMKV.defaultMMKV()
        downloadManager = ac.getSystemService(DOWNLOAD_SERVICE) as DownloadManager

        CookieSyncManager.createInstance(activity)
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookie()
        clearHistory()
        clearCache(true)
        clearFormData()

        webViewClient = MyWebViewClient()
        iniConfig(settings, MyWebChromeClient())

        registerApi()
    }

    private fun registerApi() {
        addJavascriptInterface(WebBaseApi(), "_base")
        addJavascriptInterface(WebJsApi(), "_js")
        addJavascriptInterface(WebLogApi(this), "_log")
        addJavascriptInterface(WebMmkvApi(), "_mmkv")
        addJavascriptInterface(WebToastApi(), "_toast")
        addJavascriptInterface(WebShellApi(), "_shell")
        addJavascriptInterface(WebDeviceApi(), "_device")
    }

    private fun iniConfig(webSettings: WebSettings, webChromeClient: MyWebChromeClient) {
        webSettings.javaScriptEnabled = true
        webSettings.allowFileAccess = true
        webSettings.domStorageEnabled = true
        webSettings.useWideViewPort = true
        webSettings.loadWithOverviewMode = true
        webSettings.loadsImagesAutomatically = true
        webSettings.allowUniversalAccessFromFileURLs = true
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT

        setWebChromeClient(webChromeClient)

        var url = BuildConfig.BASE_URL
//        if (BuildConfig.DEBUG) {
//            url = "http://192.168.1.8:8888/a11y/20250407/"
//            url = "http://192.168.1.8:8888/a11y/20250409/"
//            url = "http://192.168.1.8:8888/a11y/20250422/"
//        }
        if (url != null) {
            loadUrl(url)
        }

        setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            logd(url, contentDisposition, mimetype, contentLength.toString())

            toDownload(url, contentDisposition)
        }
    }

    private fun toDownload(urlString: String, contentDisposition: String) {
        try {
            val cleanUrl = urlString.split("?", "#")[0]
            val fileName = cleanUrl.substringAfterLast('/')
            var lastName = ""
            if (fileName.contains('.')) {
                lastName = fileName.substringAfterLast('.', "").lowercase()
            }

            val request = DownloadManager.Request(Uri.parse(urlString))
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            request.setDestinationInExternalPublicDir(
                activity?.cacheDir?.absolutePath, "${System.currentTimeMillis()}.$lastName"
            )
            request.setVisibleInDownloadsUi(true)
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            downloadId = downloadManager!!.enqueue(request)
        } catch (_: Exception) {
        }
    }

    private class MyWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            view.loadUrl(url)
            return true
        }

//        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap) {
//            try {
////                view.evaluateJavascript("javascript:showwait('正在拉取页面...')", null)
//            } catch (e: Exception) {
//            }
//        }

        override fun onPageFinished(view: WebView, url: String) {
            try {
//                view.evaluateJavascript("javascript:showwait('')", null)
            } catch (ignored: Exception) {
            }
        }

        override fun onReceivedError(
            view: WebView, errorCode: Int, description: String?, failingUrl: String?
        ) {
            super.onReceivedError(view, errorCode, description, failingUrl)
            view.loadData(getCustomErrorHtml()!!, "text/html", "UTF-8")
        }

        private fun getCustomErrorHtml(): String? {
            return "<html><body style=\"text-align: center;height:100vh;font-size: 50px;\"><h1>404</h1><p>请检查您的网络连接。</p></body></html>"
        }
    }

    private inner class MyWebChromeClient : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            try {
                val view = activity as MainActivity
                val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
                if (newProgress == 100) {
                    progressBar.visibility = GONE
                } else {
                    if (progressBar.visibility == GONE) progressBar.visibility = VISIBLE
                    progressBar.progress = newProgress
                }
            } catch (_: Exception) {

            }
            super.onProgressChanged(view, newProgress)
        }
    }
}
