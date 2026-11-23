
package com.kokhinzaw.pos

import android.app.Activity
import android.os.Bundle
import android.os.Environment
import android.content.Intent
import android.net.Uri
import android.webkit.*
import android.view.Menu
import android.view.MenuItem
import android.print.PrintManager
import android.widget.Toast
import android.app.DownloadManager

class MainActivity : Activity() {

    private lateinit var webView: WebView
    private var fileCallback: ValueCallback<Array<Uri>>? = null

    private val websiteUrl =
        "https://kokhinmotorservice.base44.app"

    private val filePickerCode = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = true
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val uri = request.url
                val scheme = uri.scheme

                if (scheme == "http" || scheme == "https") {
                    return false
                }

                try {
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                } catch (_: Exception) {
                    Toast.makeText(
                        this@MainActivity,
                        "Cannot open this link",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                return true
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                view: WebView,
                callback: ValueCallback<Array<Uri>>,
                params: FileChooserParams
            ): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = callback

                return try {
                    val intent = params.createIntent()
                    startActivityForResult(
                        intent,
                        filePickerCode
                    )
                    true
                } catch (_: Exception) {
                    fileCallback = null
                    false
                }
            }
        }

        webView.setDownloadListener {
            url, userAgent, contentDisposition, mimeType, _ ->

            try {
                val request = DownloadManager.Request(
                    Uri.parse(url)
                )

                request.addRequestHeader(
                    "Cookie",
                    CookieManager.getInstance().getCookie(url) ?: ""
                )

                request.addRequestHeader(
                    "User-Agent",
                    userAgent
                )

                request.setMimeType(mimeType)
                request.setTitle(
                    URLUtil.guessFileName(
                        url,
                        contentDisposition,
                        mimeType
                    )
                )

                request.setDescription("Downloading POS file")
                request.setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )

                request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    URLUtil.guessFileName(
                        url,
                        contentDisposition,
                        mimeType
                    )
                )

                val manager =
                    getSystemService(DOWNLOAD_SERVICE) as DownloadManager

                manager.enqueue(request)

                Toast.makeText(
                    this,
                    "Download started",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    "Download failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        CookieManager.getInstance().setAcceptCookie(true)

        if (savedInstanceState == null) {
            webView.loadUrl(websiteUrl)
        } else {
            webView.restoreState(savedInstanceState)
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == filePickerCode) {
            val result =
                if (resultCode == RESULT_OK) {
                    WebChromeClient.FileChooserParams
                        .parseResult(resultCode, data)
                } else {
                    null
                }

            fileCallback?.onReceiveValue(result)
            fileCallback = null
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add("Print current page")
        menu.add("Reload POS")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.title.toString()) {
            "Print current page" -> printPage()
            "Reload POS" -> webView.reload()
        }
        return true
    }

    private fun printPage() {
        val manager =
            getSystemService(PRINT_SERVICE) as PrintManager

        val adapter = webView.createPrintDocumentAdapter(
            "Ko Khin Zaw POS Receipt"
        )

        manager.print(
            "Ko Khin Zaw POS",
            adapter,
            null
        )
    }

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        fileCallback?.onReceiveValue(null)
        webView.destroy()
        super.onDestroy()
    }
}
