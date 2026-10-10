package com.example.myfirstapp.network

import com.example.myfirstapp.domain.model.Course
import com.example.myfirstapp.parser.HbustJsoupParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

object HbustCourseFetcher {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private const val TARGET_SCHEDULE_URL = "https://jwgl.hbust.edu.cn/jsxsd/framework/xsMain_new.jsp?t1=1"
    private const val FALLBACK_SCHEDULE_URL = "http://jwgl.hbust.edu.cn/jsxsd/framework/xsMain_new.jsp?t1=1"

    suspend fun fetchAndParseSchedule(cookieString: String): List<Course> {
        return withContext(Dispatchers.IO) {
            if (cookieString.isBlank()) {
                throw IllegalArgumentException("登录状态已失效，请重新登录教务系统")
            }

            val html = try {
                downloadHtml(TARGET_SCHEDULE_URL, cookieString)
            } catch (_: SocketTimeoutException) {
                throw IllegalArgumentException("网络连接超时 (net::ERR_CONNECTION_TIMED_OUT)：教务系统未响应，请检查网络或是否需要连接校园网")
            } catch (_: ConnectException) {
                try {
                    downloadHtml(FALLBACK_SCHEDULE_URL, cookieString)
                } catch (_: Exception) {
                    throw IllegalArgumentException("网络连接失败：无法建立与教务系统的连接，请检查校园网/VPN网络状态")
                }
            } catch (_: UnknownHostException) {
                throw IllegalArgumentException("域名解析失败：无法找到教务系统服务器，请检查网络连接")
            } catch (e: IOException) {
                val msg = e.message.orEmpty()
                if (msg.contains("TIMED_OUT", ignoreCase = true) || msg.contains("timeout", ignoreCase = true)) {
                    throw IllegalArgumentException("网络连接超时 (net::ERR_CONNECTION_TIMED_OUT)：教务系统响应超时，请重试")
                } else {
                    throw IllegalArgumentException("网络连接异常 (${e.localizedMessage})，请稍后重试")
                }
            } catch (e: IllegalArgumentException) {
                throw e
            } catch (e: Exception) {
                throw IllegalArgumentException("抓取课表失败：${e.localizedMessage}")
            }

            if (html.isBlank() || !html.contains("kb_table")) {
                throw IllegalArgumentException("登录状态已失效或获取数据为空，请重新登录教务系统")
            }

            HbustJsoupParser.parseScheduleHtml(html)
        }
    }

    private fun downloadHtml(url: String, cookieString: String): String {
        val request = Request.Builder()
            .url(url)
            .addHeader("Cookie", cookieString)
            .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            .get()
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IllegalArgumentException("教务系统响应异常 (HTTP ${response.code})，请重新登录")
        }

        return response.body?.string() ?: throw IllegalArgumentException("教务系统返回内容为空，请重新登录")
    }
}
