package com.hbde.courseschedule.importer.webview

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

/**
 * WebView 导入状态
 */
sealed class WebViewImportState {
    data object Idle : WebViewImportState()
    data object Loading : WebViewImportState()
    data class Progress(val progress: Int) : WebViewImportState()
    data class LoginPage(val url: String) : WebViewImportState()
    data class SchedulePage(val url: String) : WebViewImportState()
    data class Extracted(val html: String, val cookies: Map<String, String>) : WebViewImportState()
    data class Error(val message: String) : WebViewImportState()
}

/**
 * 教务系统 WebView 导入组件
 *
 * @param loginUrl 教务系统登录页面 URL
 * @param scheduleUrlPattern 课表页面 URL 匹配模式（用于自动检测跳转）
 * @param onHtmlExtracted 提取到课表 HTML 后的回调
 * @param onNavigateBack 返回回调
 */
@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseImportWebViewScreen(
    loginUrl: String,
    scheduleUrlPattern: Regex = Regex(".*(?:kb|schedule|timetable|课表).*", RegexOption.IGNORE_CASE),
    onHtmlExtracted: (String, Map<String, String>) -> Unit = { _, _ -> },
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var importState by remember { mutableStateOf<WebViewImportState>(WebViewImportState.Idle) }
    var currentUrl by remember { mutableStateOf(loginUrl) }
    var pageTitle by remember { mutableStateOf("") }
    var progress by remember { mutableFloatStateOf(0f) }

    val cookieManager = remember { CookieManager.getInstance() }

    // 初始化 Cookie
    LaunchedEffect(Unit) {
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(null, true)
    }

    // 清理 WebView
    DisposableEffect(Unit) {
        onDispose {
            cookieManager.flush()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("教务系统导入")
                        if (pageTitle.isNotBlank()) {
                            Text(
                                text = pageTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 进度条
            if (importState is WebViewImportState.Loading ||
                importState is WebViewImportState.Progress ||
                progress in 0.01f..0.99f
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                setSupportZoom(true)
                                builtInZoomControls = true
                                displayZoomControls = false
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    progress = newProgress / 100f
                                    if (newProgress in 1..99) {
                                        importState = WebViewImportState.Progress(newProgress)
                                    } else if (newProgress == 100) {
                                        importState = WebViewImportState.Idle
                                    }
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(
                                    view: WebView?,
                                    url: String?,
                                    favicon: Bitmap?
                                ) {
                                    importState = WebViewImportState.Loading
                                    url?.let { currentUrl = it }
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    url?.let {
                                        currentUrl = it
                                        pageTitle = view?.title ?: ""

                                        // 检测是否跳转到课表页面
                                        if (scheduleUrlPattern.matches(it)) {
                                            importState = WebViewImportState.SchedulePage(it)
                                            // 延迟提取 HTML，等待页面完全渲染
                                            val webViewRef = view
                                            postDelayed({
                                                webViewRef?.let { wv ->
                                                    extractScheduleHtml(wv) { html ->
                                                        val cookies = getCookies(it)
                                                        importState = WebViewImportState.Extracted(html, cookies)
                                                        onHtmlExtracted(html, cookies)
                                                    }
                                                }
                                            }, 1500)
                                        } else if (isLoginPage(it)) {
                                            importState = WebViewImportState.LoginPage(it)
                                            // 尝试自动填充（如果有保存的账号信息）
                                        } else {
                                            // 其他页面
                                        }
                                    }
                                    progress = 1f
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    return false
                                }
                            }

                            loadUrl(loginUrl)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // 加载中指示器
                if (importState is WebViewImportState.Loading && progress < 0.1f) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

/**
 * 从 WebView 提取课表 HTML
 */
private fun extractScheduleHtml(webView: WebView, onResult: (String) -> Unit) {
    webView.evaluateJavascript(
        """
        (function() {
            var table = document.querySelector('#kbtable, table.kb, .kbcontent, #timetable, .timetable, table[border="1"]');
            if (table) {
                return '<html><body>' + table.outerHTML + '</body></html>';
            }
            // 如果没有找到特定表格，返回整个 body
            return document.body ? document.body.innerHTML : document.documentElement.outerHTML;
        })()
        """.trimIndent(),
    ) { result ->
        val html = result?.removeSurrounding("\"")?.replace("\\\"", "\"")
            ?.replace("\\n", "\n")?.replace("\\t", "\t") ?: ""
        onResult(html)
    }
}

/**
 * 获取当前站点的 Cookies
 */
private fun getCookies(url: String): Map<String, String> {
    val cookieManager = CookieManager.getInstance()
    val cookieString = cookieManager.getCookie(url) ?: return emptyMap()

    return cookieString.split(";").mapNotNull { cookie ->
        val parts = cookie.trim().split("=", limit = 2)
        if (parts.size == 2) {
            parts[0] to parts[1]
        } else null
    }.toMap()
}

/**
 * 判断是否为登录页面
 */
private fun isLoginPage(url: String): Boolean {
    return url.contains("login", ignoreCase = true) ||
           url.contains("signin", ignoreCase = true) ||
           url.contains("auth", ignoreCase = true)
}

/**
 * WebView Cookie 管理器
 * 负责 Cookie 的持久化存储和恢复
 */
class WebViewCookieManager(
    private val cookieStore: CookieStore
) {
    private val cookieManager = CookieManager.getInstance()

    /**
     * 保存指定 URL 的 Cookies
     */
    suspend fun saveCookies(url: String) {
        val domain = extractDomain(url) ?: return
        val cookieString = cookieManager.getCookie(url) ?: return
        cookieStore.saveCookies(domain, cookieString)
    }

    /**
     * 恢复 Cookies 到 WebView
     */
    suspend fun restoreCookies(url: String) {
        val domain = extractDomain(url) ?: return
        val cookieString = cookieStore.getCookies(domain) ?: return

        cookieString.split(";").forEach { cookie ->
            val trimmed = cookie.trim()
            if (trimmed.isNotBlank()) {
                cookieManager.setCookie(url, trimmed)
            }
        }
        cookieManager.flush()
    }

    /**
     * 清除指定域名的 Cookies
     */
    suspend fun clearCookies(domain: String) {
        cookieStore.clearCookies(domain)
    }

    /**
     * 检查是否有保存的 Cookies
     */
    suspend fun hasSavedCookies(url: String): Boolean {
        val domain = extractDomain(url) ?: return false
        return cookieStore.getCookies(domain) != null
    }

    private fun extractDomain(url: String): String? {
        return try {
            Uri.parse(url).host
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * Cookie 存储接口
 */
interface CookieStore {
    suspend fun saveCookies(domain: String, cookies: String)
    suspend fun getCookies(domain: String): String?
    suspend fun clearCookies(domain: String)
}
