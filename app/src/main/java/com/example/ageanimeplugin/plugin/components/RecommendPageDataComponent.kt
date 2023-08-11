package com.example.ageanimeplugin.plugin.components

import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
import com.example.ageanimeplugin.plugin.util.JsoupUtil
import com.su.mediabox.pluginapi.action.DetailAction
import com.su.mediabox.pluginapi.components.ICustomPageDataComponent
import com.su.mediabox.pluginapi.data.BaseData
import com.su.mediabox.pluginapi.data.MediaInfo1Data
import com.su.mediabox.pluginapi.data.SimpleTextData
import com.su.mediabox.pluginapi.util.UIUtil.dp

/**
 * FileName: RecommendPageDataComponent
 * Founder: Jiang Houren
 * Create Date: 2023/4/18 09:29
 * Profile: 每日推荐
 */
class RecommendPageDataComponent  : ICustomPageDataComponent {
    private val layoutSpanCount = 12

    override val pageName: String
        get() = "每日推荐"

    override suspend fun getData(page: Int): List<BaseData>? {
        val url = Const.host + "/recommend/$page"
        val document = JsoupUtil.getDocument(url)
        val data = mutableListOf<BaseData>()
        val content = document.select(".video_list_box--bd").select(".row")
        val li = content.select(".col")
        for (liE in li){
            val name = liE.select("a").text()
            val coverUrl = liE.select("img").attr("data-original")
            val videoUrl = liE.select("a").first()?.attr("href")?:""
            val episode = liE.select("span").text()
            data.add(
                MediaInfo1Data(name, coverUrl, videoUrl, episode ?: "")
                .apply {
                    spanSize = layoutSpanCount / 3
                    action = DetailAction.obtain(videoUrl)
                })
        }
        data[0].layoutConfig = BaseData.LayoutConfig(spanCount = layoutSpanCount)
        return data
    }
}