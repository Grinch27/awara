package me.rerere.awara.ui.page.message

import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import me.rerere.awara.ui.component.common.BackButton
import me.rerere.compose_setting.preference.mmkvPreference

@Composable
fun MessagePage() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages") },
                navigationIcon = {
                    BackButton()
                }
            )
        }
    ) {
        Box(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
        ) {
            AndroidView(
                factory = {
                    WebView(it).apply {
                        // Enable JavaScript
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.userAgentString = "Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.159 Mobile Safari/537.36"
                        settings.setSupportZoom(true)

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)

                                val token = mmkvPreference.getString("refresh_token", null) ?: ""

                                // get current token
                                evaluateJavascript("localStorage.getItem('token')") { result ->
                                    val currentToken = result?.removeSurrounding("\"")
                                    if(currentToken != token) {
                                        // refresh token
                                        evaluateJavascript("localStorage.setItem('token', '$token')") {
                                            Log.i("Message", "MessagePage: update token => $it")
                                            reload()
                                        }
                                    } else {
                                        Log.i("Message", "MessagePage: token is up to date")
                                    }
                                }
                            }
                        }

                        loadUrl("https://www.iwara.tv/messages")
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}