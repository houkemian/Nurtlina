package com.nurtlina.app.ui.legal

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.roundToInt

private const val ASSET_ROOT = "file:///android_asset/"

/**
 * Displays the legal HTML bundled directly from the project's official-site directory.
 * JavaScript and content access stay disabled because these documents are static.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalDocumentScreen(
    assetFileName: String,
    fallbackTitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val backgroundColor = MaterialTheme.colorScheme.background.toArgb()
    val textZoom = (LocalDensity.current.fontScale * 100).roundToInt()
    var pageTitle by remember(assetFileName) { mutableStateOf(fallbackTitle) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = pageTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (webView?.canGoBack() == true) {
                                webView?.goBack()
                            } else {
                                onBack()
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        AndroidView(
            factory = { viewContext ->
                WebView(viewContext).apply {
                    setBackgroundColor(backgroundColor)
                    settings.apply {
                        javaScriptEnabled = false
                        domStorageEnabled = false
                        allowContentAccess = false
                        allowFileAccess = true
                        builtInZoomControls = false
                        displayZoomControls = false
                        this.textZoom = textZoom
                    }
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest,
                        ): Boolean = handleUri(context, request.url)

                        override fun onPageFinished(view: WebView, url: String) {
                            super.onPageFinished(view, url)
                            view.title?.takeIf(String::isNotBlank)?.let { pageTitle = it }
                        }
                    }
                    loadUrl("$ASSET_ROOT$assetFileName")
                    webView = this
                }
            },
            update = { view -> view.settings.textZoom = textZoom },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.stopLoading()
            webView?.destroy()
            webView = null
        }
    }
}

private fun handleUri(context: Context, uri: Uri): Boolean = when (uri.scheme) {
    "file" -> !uri.toString().startsWith(ASSET_ROOT)
    "mailto" -> {
        context.startSafely(Intent(Intent.ACTION_SENDTO, uri))
        true
    }
    "http", "https" -> {
        context.startSafely(
            Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)
        )
        true
    }
    else -> true
}

private fun Context.startSafely(intent: Intent) {
    runCatching { startActivity(intent) }
}
