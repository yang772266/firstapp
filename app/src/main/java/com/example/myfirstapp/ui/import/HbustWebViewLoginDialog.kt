package com.example.myfirstapp.ui.import

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.concurrent.atomic.AtomicBoolean

private const val SSO_OAUTH_URL =
    "http://sso.hbust.edu.cn:28000/login/oauth/authorize?client_id=fba3737ecd3358c5b67a&response_type=code&redirect_uri=http://sso.hbust.edu.cn:28000/&scope=read&state=iam"
private const val JWGL_DIRECT_URL = "https://jwgl.hbust.edu.cn/"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HbustWebViewLoginDialog(
    onDismiss: () -> Unit,
    onCookieCaptured: (String) -> Unit
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var webLoadError by remember { mutableStateOf<String?>(null) }
    val hasRedirectedToJwgl = remember { AtomicBoolean(false) }
    val isCookieCaptured = remember { AtomicBoolean(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🏫 湖北科技学院统一身份认证",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = {
                            CookieManager.getInstance().removeAllCookies(null)
                            CookieManager.getInstance().flush()
                            webViewRef?.clearCache(true)
                            hasRedirectedToJwgl.set(false)
                            isCookieCaptured.set(false)
                            webLoadError = null
                            webViewRef?.loadUrl(SSO_OAUTH_URL)
                        }
                    ) {
                        Text("清缓存重试")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    OutlinedButton(onClick = onDismiss) {
                        Text("关闭")
                    }
                }

                Text(
                    text = "流程：统一身份认证 -> 用户登录 -> 自动进入教务系统获取课表。APP 不会记录或上传您的账号密码。",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                if (webLoadError != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = webLoadError.orEmpty(),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        webLoadError = null
                                        hasRedirectedToJwgl.set(false)
                                        webViewRef?.loadUrl(SSO_OAUTH_URL)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("重试 SSO 登录", fontSize = 11.5.sp)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = {
                                        webLoadError = null
                                        hasRedirectedToJwgl.set(true)
                                        webViewRef?.loadUrl(JWGL_DIRECT_URL)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("直连教务系统", fontSize = 11.5.sp)
                                }
                            }
                            Text(
                                text = "💡 提示：若在校外，统一身份认证端口 28000 可能限制访问。您可以点击【直连教务系统】，或使用 HTML 源码/截图 OCR 导入方式。",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // WebView 登录组件
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.setSupportMultipleWindows(false)
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            settings.cacheMode = WebSettings.LOAD_DEFAULT

                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    return false
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    if (url != null && (url.contains("sso.hbust.edu.cn") || url.contains("jwgl.hbust.edu.cn"))) {
                                        webLoadError = null
                                    }
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    val currentUrl = url.orEmpty()

                                    // 1. 当 SSO 认证完成跳转回 sso 回调页面时，在同一个 WebView 内载入教务系统
                                    if (currentUrl.contains("sso.hbust.edu.cn:28000") &&
                                        !currentUrl.contains("oauth/authorize") &&
                                        !hasRedirectedToJwgl.get()
                                    ) {
                                        if (hasRedirectedToJwgl.compareAndSet(false, true)) {
                                            view?.loadUrl(JWGL_DIRECT_URL)
                                        }
                                        return
                                    }

                                    // 2. 确认已成功进入 jwgl.hbust.edu.cn 后提取 Cookie
                                    if (currentUrl.contains("jwgl.hbust.edu.cn")) {
                                        val jwglCookie = CookieManager.getInstance().getCookie("https://jwgl.hbust.edu.cn").orEmpty()
                                        val ssoCookie = CookieManager.getInstance().getCookie("http://sso.hbust.edu.cn:28000").orEmpty()
                                        
                                        val combinedCookie = listOf(jwglCookie, ssoCookie)
                                            .filter { it.isNotBlank() }
                                            .joinToString("; ")

                                        if (combinedCookie.isNotBlank() && isCookieCaptured.compareAndSet(false, true)) {
                                            onCookieCaptured(combinedCookie)
                                        }
                                    }
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    super.onReceivedError(view, request, error)
                                    if (request?.isForMainFrame == true) {
                                        val desc = error?.description?.toString().orEmpty()
                                        val errorCode = error?.errorCode ?: -1
                                        webLoadError = when {
                                            desc.contains("ERR_CONNECTION_TIMED_OUT") || errorCode == ERROR_TIMEOUT -> {
                                                "⚠️ 网络连接超时 (net::ERR_CONNECTION_TIMED_OUT)：无法连通 SSO 登录服务器 (:28000)"
                                            }
                                            errorCode == ERROR_HOST_LOOKUP || desc.contains("ERR_NAME_NOT_RESOLVED") -> {
                                                "⚠️ 无法解析服务器域名，请检查网络连接"
                                            }
                                            errorCode == ERROR_CONNECT || desc.contains("ERR_CONNECTION_REFUSED") -> {
                                                "⚠️ 服务器拒绝连接，请稍后重试或尝试直连教务系统"
                                            }
                                            else -> {
                                                "⚠️ 页面加载异常 ($errorCode)：${desc.ifBlank { "网络超时" }}"
                                            }
                                        }
                                    }
                                }

                                @SuppressLint("WebViewClientOnReceivedSslError")
                                override fun onReceivedSslError(
                                    view: WebView?,
                                    handler: SslErrorHandler?,
                                    error: SslError?
                                ) {
                                    // 允许校内证书重定向
                                    val failingUrl = error?.url.orEmpty()
                                    if (failingUrl.contains("hbust.edu.cn")) {
                                        handler?.proceed()
                                    } else {
                                        handler?.cancel()
                                    }
                                }
                            }

                            loadUrl(SSO_OAUTH_URL)
                            webViewRef = this
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = {
                        val jwglCookie = CookieManager.getInstance().getCookie("https://jwgl.hbust.edu.cn").orEmpty()
                        val ssoCookie = CookieManager.getInstance().getCookie("http://sso.hbust.edu.cn:28000").orEmpty()
                        
                        val combinedCookie = listOf(jwglCookie, ssoCookie)
                            .filter { it.isNotBlank() }
                            .joinToString("; ")

                        if (isCookieCaptured.compareAndSet(false, true)) {
                            onCookieCaptured(combinedCookie)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("我已进入教务系统，提取并导入课表")
                }
            }
        }
    }
}
