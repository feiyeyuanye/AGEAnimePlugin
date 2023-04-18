package com.example.ageanimeplugin.plugin.components

import com.example.ageanimeplugin.plugin.actions.CustomAction
import com.example.ageanimeplugin.plugin.components.Const.host
import com.example.ageanimeplugin.plugin.util.JsoupUtil
import com.example.ageanimeplugin.plugin.util.ParseHtmlUtil
import com.su.mediabox.pluginapi.components.ICustomPageDataComponent
import com.su.mediabox.pluginapi.data.BaseData
import com.su.mediabox.pluginapi.data.ViewPagerData

class RankPageDataComponent : ICustomPageDataComponent {

    override val pageName = "排行榜"
    override fun menus() = mutableListOf(CustomAction())

    override suspend fun getData(page: Int): List<BaseData>? {
        if (page != 1)
            return null
        val url = host
        val doc = JsoupUtil.getDocument(url)

        val head = doc.select("#nav")
        val headNav = head.select(".nav_button")

        //排行榜
        // 搜有动画排行榜
        val allTag = headNav[4].let {
            object : ViewPagerData.PageLoader{
                override fun pageName(page: Int): String {
                    return "所有动画播放排行"
                }

                override suspend fun loadData(page: Int): List<BaseData> {
                    return getTotalRankData()
                }
            }
        }

        return listOf(ViewPagerData(mutableListOf(allTag)).apply {
            layoutConfig = BaseData.LayoutConfig(
                itemSpacing = 0,
                listLeftEdge = 0,
                listRightEdge = 0
            )
        })
    }

    /**
     * 所有排行
     */
    private suspend fun getTotalRankData(): List<BaseData> {
        val document = JsoupUtil.getDocument("$host/rank?tag=all")
        val data = mutableListOf<BaseData>()
        document.select("div[class='blockcontent div_right_r_3']").first()?.also {
            data.addAll(ParseHtmlUtil.parseRankEm(it))
        }
        return data
    }
}