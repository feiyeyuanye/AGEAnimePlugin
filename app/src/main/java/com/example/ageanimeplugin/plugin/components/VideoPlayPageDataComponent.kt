package com.example.ageanimeplugin.plugin.components

import android.net.Uri
import android.util.Log
import com.example.ageanimeplugin.plugin.components.Const.host
import com.example.ageanimeplugin.plugin.components.Const.ua
import com.example.ageanimeplugin.plugin.danmaku.OyydsDanmaku
import com.example.ageanimeplugin.plugin.danmaku.OyydsDanmakuParser
import com.example.ageanimeplugin.plugin.util.JsoupUtil
import com.example.ageanimeplugin.plugin.util.Text.trimAll
import com.example.ageanimeplugin.plugin.util.oyydsDanmakuApis
import com.kuaishou.akdanmaku.data.DanmakuItemData
import com.su.mediabox.pluginapi.components.IVideoPlayPageDataComponent
import com.su.mediabox.pluginapi.data.VideoPlayMedia
import com.su.mediabox.pluginapi.util.AppUtil
import com.su.mediabox.pluginapi.util.PluginPreferenceIns
import com.su.mediabox.pluginapi.util.TextUtil.urlDecode
import com.su.mediabox.pluginapi.util.WebUtil
import com.su.mediabox.pluginapi.util.WebUtilIns
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import java.io.File

class VideoPlayPageDataComponent : IVideoPlayPageDataComponent {

    private var episodeDanmakuId = ""
    override suspend fun getDanmakuData(
        videoName: String,
        episodeName: String,
        episodeUrl: String
    ): List<DanmakuItemData>? {
        try {
            val config = PluginPreferenceIns.get(OyydsDanmaku.OYYDS_DANMAKU_ENABLE, true)
            if (!config)
                return null
            val name = videoName.trimAll()
            var episode = episodeName.trimAll()
            //剧集对集去除所有额外字符，增大弹幕适应性
            val episodeIndex = episode.indexOf("集")
            if (episodeIndex > -1 && episodeIndex != episode.length - 1) {
                episode = episode.substring(0, episodeIndex + 1)
            }
            Log.d("请求Oyyds弹幕", "媒体:$name 剧集:$episode")
            return oyydsDanmakuApis.getDanmakuData(name, episode).data.let { danmukuData ->
                val data = mutableListOf<DanmakuItemData>()
                danmukuData?.data?.forEach { dataX ->
                    OyydsDanmakuParser.convert(dataX)?.also { data.add(it) }
                }
                episodeDanmakuId = danmukuData?.episode?.id ?: ""
                data
            }
        } catch (e: Exception) {
            throw RuntimeException("弹幕加载错误：${e.message}")
        }
    }

    override suspend fun putDanmaku(
        videoName: String,
        episodeName: String,
        episodeUrl: String,
        danmaku: String,
        time: Long,
        color: Int,
        type: Int
    ): Boolean = try {
        Log.d("发送弹幕到Oyyds", "内容:$danmaku 剧集id:$episodeDanmakuId")
        oyydsDanmakuApis.addDanmaku(
            danmaku,
            //Oyyds弹幕标准时间是秒
            (time / 1000F).toString(),
            episodeDanmakuId,
            OyydsDanmakuParser.danmakuTypeMap.entries.find { it.value == type }?.key ?: "scroll",
            String.format("#%02X", color)
        )
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }

    override suspend fun getVideoPlayMedia(episodeUrl: String): VideoPlayMedia {
        val url = host + episodeUrl
        val document = JsoupUtil.getDocument(url)

        val cookies = mapOf("cookie" to PluginPreferenceIns.get(JsoupUtil.cfClearanceKey, ""))
        //解析链接
        val videoUrl = withContext(Dispatchers.Main) {
            val iframeUrl = withTimeoutOrNull(10 * 1000) {
                WebUtilIns.interceptResource(
                    url, "(.*)\\.m3u8(.*)",
                    loadPolicy = object : WebUtil.LoadPolicy by WebUtil.DefaultLoadPolicy {
                        override val headers = cookies
                        override val userAgentString = ua
                        override val isClearEnv = false
                    }
                )
            } ?: ""
            async {
                Log.e("TAG","1 $iframeUrl")
                when {
                    iframeUrl.isBlank() -> iframeUrl
                    // https://wolongzywcdn3.com:65/H7UdaMTN/index.m3u8
                    // https://hnzy.bfvvs.com/play/BeXnrLWb/index.m3u8
                    iframeUrl.endsWith(".m3u8") -> iframeUrl
                    // https://vip.sp-flv.com:8443/playurl/tu/794a73566c73727a324e517658516e5130416e6179673d3d.m3u8?vid=10b1MPhBtCb1uZLhRgbcJuLw6rnamlwEdEYUHHDKncO2yeFgHO1OjofxxcyuIcjLmLdlxfPB91GPh6omccJp5-5lvknQ8BWmYG6aTfVPM56Rl1DIGHAVZI0IJujPlI2LhVXU0lqjQ8cmpa5eY2E0mE8dMwLMYv_BrcnloyHBrxmGi80Q7v1z0L2F7xdEwdpYB9AWjDTL-sUH8MYYuVj0Jxib27ae970UspQXo3Uvi9gyhrxiDP360Hd4mxsFSduLLn8-u0Q--mh75Z_E2WtDY7RU-DdvFXNNejc4Y0ykDer--rDUzIaCgGCZG4vPZX4MJSNQPhIXBwEmfSEnPKiN4E6cgoDuchuIB49GWdbT0PeUJiottZ8cY3UvD2PJ28CWSrg1XoU0l-wg3d6yfnkaG6Vmx0H6C6Skq84&type=m3u8&client_netip=112.98.80.89&sid=de01ec7ffc1f3028edf3f401191a3bbf&app_ver=V3.0&url=https%3A%2F%2Fdy.jx1024.com%3A8443%2Fuploads%2F%E8%BF%9B%E5%87%BB%E7%9A%84%E5%B7%A8%E4%BA%BA%E7%AC%AC%E4%B8%89%E5%AD%A3Part2%E7%AC%AC01%E9%9B%86.m3u8&media_type=jpg&ts=324236546a3350595934716670344a3535734d6857673d3d&sign=YzZia1lCNzdLZmhWU1I5WWxQdVNOekxnQ3VmRFg5VFQycDNVYytTM3JabHhzWWt5MHkwa1FKbzNkWEN1VE5jcVNOK2U1NjkrV0pnbVlQdk5TKzF4Zk1kTmhvdzlFTTlmR0hITmRTWUU3RjFnWWwySTNUMXNPa3l4UGl1VXNRVXNJY2dpbSttem1INFFlN1U3QTVlVGt2YTNZbjVvNE9YT3Z6OHhkaE9MNDRiU0tDUzY1YjVsS1dNUGZ1VnQ2c29JN01vUDV0bTYyRFFZempDYXlGZmZidWZiUVMxRlNqQ1V6Z2U1WlR3TGJTQzQ1UFJNS3Erbk5lV2YrVjU3R21UZkxENG9QN245QVlZMHlhSG40SHpRbm1xSUw5QVJnY05TZWk4aW1zZjZMcHd2NnVpVU95ek5rWlI4Z3NlckhaWVlJNmExT3hCZkttVnhWelYrbjk4MWJhN2N4L3AwcitTUjE2RWdMd0pVME54VW9RUlBicmJ2TzJvSFk1eVNSWVdrOFJTNUJtOWkyYklxR0U2bk9WTWJxakFZMk9VOXpHYnVVZ0U1SnEzY24vOVFsaWVOMUJCSU9UckxTbVp6OTNEUkROcS9OcnFOUDFDaXdVNUtlMzgwa2tSRjZac1NxaG1LNTN3aWlqZS9aa01mbWhtYVROWU9OdExya293VWxpM0VMRGF4cHVkczdvUUt1SDhZekxydCtsbjNKVUJXUmhEVmlIVURQWHRTdjNyR05iSEkyZ0pvVWlvRm4wUThnd1JKa0QrWTN6MnBmV3dINUZ4STdPdk92cGhvTS9VWC9aVHkwUG10emtwemppVmk5VXZWWnowOE1GWUxBazRLeFpQVCtRSjhvSUU0d3pCN1gxZlE3SXFlK1QvZzg2eGxqYklaWWRYNlZLZ2poR200Skh0WXduK1lKcDY3MjQ0Sk1SQUg3VWJ2QmpXTVk2RTM0QXg1S2o5ZFNyOHFKQy9naSs3VjVFdFZYRTEwY0VWN3VnYWFoSWUyRUk1MHdVbk55OHc1aHhldWwvT09GdE9Zc1cyZGxQZ05vUHI2VUVXQjVURFVud3pLeW5RaHBDekt2Q1Q4cW9RanRDa1RqRVBNS0hqVSs2L1loMHNRMVI1ckh5cjVmeUg5NUNrUXJ6VktxdXh2Y3UvdXZta3BrNDBSVFJSdjNNMUJ3OHVEMlNXNDI2VThxaDVsbkRwQVN4SHFqTXYyTkhqNmNOYlR2OE5CQy92bytCdVNiQXhWWjNuQ3l3QWJDRnoyU1JEQThEblBZTnMzUk4wWldJa2ZIblA1ZmNOQXFyVEljUT09
                    // 截取之后为 https://dy.jx1024.com:8443/uploads/进击的巨人第三季Part2第01集.m3u8 --》这样无效
//                    iframeUrl.contains("playurl") -> iframeUrl.substringAfter("url=")
//                        .substringBefore("&")
//                        .urlDecode()
                    else -> {}
                }
            }
        }

        //剧集名
        val name = withContext(Dispatchers.Default) {
            async {
                ""
            }
        }
        Log.e("TAG","2 ${videoUrl.await() as String}")
        return VideoPlayMedia(name.await(), videoUrl.await() as String)
    }
}