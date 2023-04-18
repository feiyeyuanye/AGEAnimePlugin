package com.example.ageanimeplugin.plugin.components

import com.example.ageanimeplugin.plugin.util.JsoupUtil
import com.example.ageanimeplugin.plugin.util.ParseHtmlUtil
import com.su.mediabox.pluginapi.action.ClassifyAction
import com.su.mediabox.pluginapi.components.IMediaClassifyPageDataComponent
import com.su.mediabox.pluginapi.data.BaseData
import com.su.mediabox.pluginapi.data.ClassifyItemData
import com.su.mediabox.pluginapi.util.PluginPreferenceIns
import com.su.mediabox.pluginapi.util.WebUtil
import com.su.mediabox.pluginapi.util.WebUtilIns
import org.jsoup.Jsoup

class MediaClassifyPageDataComponent : IMediaClassifyPageDataComponent {

    override suspend fun getClassifyItemData(): List<ClassifyItemData> {
        val classifyItemDataList = mutableListOf<ClassifyItemData>()
        //示例：使用WebUtil解析动态生成的分类项
        val cookies = mapOf("cookie" to PluginPreferenceIns.get(JsoupUtil.cfClearanceKey, ""))
        val document = Jsoup.parse(
            WebUtilIns.getRenderedHtmlCode(
                Const.host + "/catalog/all-all-all-all-all-time-1", loadPolicy = object :
                    WebUtil.LoadPolicy by WebUtil.DefaultLoadPolicy {
                    override val headers = cookies
                    override val userAgentString = Const.ua
                    override val isClearEnv = false
                }
            )
        )
        document.getElementById("search-list")?.getElementsByTag("li")?.forEach {
            classifyItemDataList.addAll(ParseHtmlUtil.parseClassifyEm(it))
        }
        return classifyItemDataList
    }

    override suspend fun getClassifyData(
        classifyAction: ClassifyAction,
        page: Int
    ): List<BaseData> {
        val classifyList = mutableListOf<BaseData>()

        var url = classifyAction.url ?: ""
        // 将字符串按照 "-" 分割成多个部分
        val parts = url.split("-").toMutableList()
//        Log.e("TAG","url ${parts}")  // [all, all, all, all, all, time, 1, 中国, all, all]
        // 判断是否有足够的部分
        if (parts.size > 6) {
            // 修改第 6 个部分为 "2"，代表页数
            parts[6] = "${page}"
            // 使用 "-" 将部分拼接成新的字符串
            url = parts.joinToString("-")
        }
//        Log.e("TAG","url ${url}")  // all-all-all-all-all-time-2-中国-all-all
        if (!url.startsWith(Const.host))
            url = Const.host + "/catalog/"+ url
//        Log.d("TAG", "获取分类数据 $url")  // https://www.agemys.net/catalog/all-all-all-all-all-time-2-中国-all-all
        val document = JsoupUtil.getDocument(url)
        classifyList.addAll(
                ParseHtmlUtil.parseSearchEm(
                    document,
                    url
                )
            )
        return classifyList
    }
}