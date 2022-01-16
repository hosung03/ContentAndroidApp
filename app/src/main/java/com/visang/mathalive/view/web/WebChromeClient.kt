package com.visang.mathalive.view.web

import android.os.Build
import android.webkit.ConsoleMessage
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import com.visang.mathalive.util.Logger


class WebChromeClient : WebChromeClient() {

    override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
        val message = "console msg: ${consoleMessage.message()}"

        when (consoleMessage.messageLevel()) {
            ConsoleMessage.MessageLevel.ERROR -> Logger.e(message)
            ConsoleMessage.MessageLevel.WARNING -> Logger.w(message)
            else -> Logger.d(message)
        }

        return true
    }

    override fun onPermissionRequest(request: PermissionRequest?) {
//        super.onPermissionRequest(request)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (request != null) {
                request.grant(request.resources)
            }
        }
    }
}
