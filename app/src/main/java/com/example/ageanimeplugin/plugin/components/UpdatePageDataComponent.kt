package com.example.ageanimeplugin.plugin.components

import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
import android.view.Gravity
import com.example.ageanimeplugin.plugin.util.JsoupUtil
import com.su.mediabox.pluginapi.action.DetailAction
import com.su.mediabox.pluginapi.components.ICustomPageDataComponent
import com.su.mediabox.pluginapi.data.BaseData
import com.su.mediabox.pluginapi.data.MediaInfo1Data
import com.su.mediabox.pluginapi.data.SimpleTextData
import com.su.mediabox.pluginapi.data.TagData
import com.su.mediabox.pluginapi.data.TextData
import com.su.mediabox.pluginapi.data.ViewPagerData
import com.su.mediabox.pluginapi.util.UIUtil.dp
import org.jsoup.select.Elements

/**
 * FileName: UpdatePageDataComponent
 * Founder: Jiang Houren
 * Create Date: 2023/4/18 08:57
 * Profile: 最近更新
 */
class UpdatePageDataComponent : ICustomPageDataComponent {
    private val layoutSpanCount = 12

    override val pageName: String
        get() = "一周更新"

    private val days = mutableListOf<String>()
    private lateinit var updateList: Elements

    override suspend fun getData(page: Int): List<BaseData>? {
        if (page != 1)
            return null
        val url = Const.host + "/update"
        val doc = JsoupUtil.getDocument(url).select("#recent_update_video_wrapper").first() ?: return null
        days.clear()
        doc.select(".position-absolute").forEach {
            days.add(it.text())
        }
        updateList = doc.select(">div")?: return null
        val updateLoader = object : ViewPagerData.PageLoader {
            override fun pageName(page: Int): String = days[page]

            override suspend fun loadData(page: Int): List<BaseData> {
//                Log.e("TAG", "获取更新列表 $page ${updateList[page]}")
                //ul元素
                val target = updateList[page]
                val data = mutableListOf<BaseData>()
                val li = target.select(".video_list_box--bd").select(".col")
                for (em in li) {
                    val titleEm = em.select("a")
                    val itemTitle = titleEm.text()
                    val coverUrl = em.select("img").attr("data-original")
                    val episode = em.select("span").first()?.text()
                    val itemUrl = titleEm.attr("href")
                    if (!itemTitle.isNullOrBlank() && !episode.isNullOrBlank() && !url.isNullOrBlank()) {
                        Log.d("添加更新", "$itemTitle $episode $url")
                        data.add(MediaInfo1Data(itemTitle, coverUrl, itemUrl, episode ?: "")
                            .apply {
                                spanSize = layoutSpanCount / 3
                                action = DetailAction.obtain(itemUrl)
                            })
                    }
                }
                data[0].layoutConfig = BaseData.LayoutConfig(spanCount = layoutSpanCount)
                return data
            }
        }
        return listOf(ViewPagerData(mutableListOf<ViewPagerData.PageLoader>().apply {
            repeat(7) {
                add(updateLoader)
            }
        }).apply {
            layoutConfig = BaseData.LayoutConfig(
                itemSpacing = 0,
                listLeftEdge = 0,
                listRightEdge = 0
            )
        })
    }
}