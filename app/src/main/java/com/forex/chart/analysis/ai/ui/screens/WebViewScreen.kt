package com.forex.chart.analysis.ai.ui.screens

import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.forex.chart.analysis.ai.ui.theme.AppBackground
import com.forex.chart.analysis.ai.ui.theme.TextWhite
import com.forex.chart.analysis.ai.ui.theme.CardBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(navController: NavController, title: String, url: String) {
    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = { Text(title, color = TextWhite) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardBackground)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        webViewClient = WebViewClient()
                        // Ensure JavaScript is enabled if needed for the policy page
                        settings.javaScriptEnabled = true
                        loadUrl(url)
                    }
                },
                // Update the URL if the screen recomposes (e.g., URL changes)
                update = { webView ->
                    webView.loadUrl(url)
                }
            )
        }
    }
}