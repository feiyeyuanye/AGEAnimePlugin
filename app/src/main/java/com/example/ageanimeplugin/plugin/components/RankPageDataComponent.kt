package com.example.ageanimeplugin.plugin.components

import com.example.ageanimeplugin.plugin.actions.CustomAction
import com.example.ageanimeplugin.plugin.components.Const.host
import com.example.ageanimeplugin.plugin.util.JsoupUtil
import com.example.ageanimeplugin.plugin.util.ParseHtmlUtil
import com.su.mediabox.pluginapi.components.ICustomPageDataComponent
import com.su.mediabox.pluginapi.data.BaseData
import com.su.mediabox.pluginapi.data.ViewPagerData
import org.jsoup.nodes.Element

class RankPageDataComponent : ICustomPageDataComponent {

    override val pageName = "排行榜"
    override fun menus() = mutableListOf(CustomAction())

    override suspend fun getData(page: Int): List<BaseData>? {
        if (page != 1)
            return null
        val url = "$host/rank"
        val doc = JsoupUtil.getDocument(url)

        val content = doc.select(".rank_list_box--bd").select(".col-4")

        // 排行榜
        val weeklyCharts = content[0].let {
            object : ViewPagerData.PageLoader{
                override fun pageName(page: Int): String {
                    return "周榜 TOP50"
                }

                override suspend fun loadData(page: Int): List<BaseData> {
                    return getTotalRankData(it)
                }
            }
        }
        val freshReading = content[1].let {
            object : ViewPagerData.PageLoader{
                override fun pageName(page: Int): String {
                    return "月榜 TOP50"
                }

                override suspend fun loadData(page: Int): List<BaseData> {
                    return getTotalRankData(it)
                }
            }
        }
        val topTrends = content[2].let {
            object : ViewPagerData.PageLoader{
                override fun pageName(page: Int): String {
                    return "总榜 TOP50"
                }

                override suspend fun loadData(page: Int): List<BaseData> {
                    return getTotalRankData(it)
                }
            }
        }
        return listOf(ViewPagerData(mutableListOf(weeklyCharts,freshReading,topTrends)).apply {
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
    private suspend fun getTotalRankData(element:Element): List<BaseData> {
        val data = mutableListOf<BaseData>()
        data.addAll(ParseHtmlUtil.parseRankEm(element))
        return data
    }
}