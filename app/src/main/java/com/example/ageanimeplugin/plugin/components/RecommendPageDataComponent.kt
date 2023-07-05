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
    private val layoutSpanCount = 6

    override val pageName: String
        get() = "每日推荐"

    override suspend fun getData(page: Int): List<BaseData>? {
//        Log.e("TAG","page: ${page}")
        val url = Const.host + "/recommend?page=$page"
        val document = JsoupUtil.getDocument(url)
        val data = mutableListOf<BaseData>()

        data.add(SimpleTextData("第${page}页").apply {
            layoutConfig = BaseData.LayoutConfig(layoutSpanCount, 14.dp)
            fontSize = 15F
            fontStyle = Typeface.BOLD
            fontColor = Color.BLACK
            spanSize = layoutSpanCount
        })
        val li = document.select(".ul_li_a6").select("li")
        for (liE in li){
            val img = liE.select("img")
            val name = img.attr("alt")
            val coverUrl = img.attr("src")
            val videoUrl = liE.select("a").first()?.attr("href")?:""
            val episode = img.attr("title")
            data.add(
                MediaInfo1Data(name, coverUrl, videoUrl, episode ?: "")
                .apply {
                    spanSize = layoutSpanCount / 3
                    action = DetailAction.obtain(videoUrl)
                })
        }
        return data
    }
}