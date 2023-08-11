package com.example.ageanimeplugin.plugin.util

import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
import android.view.Gravity
import com.example.ageanimeplugin.plugin.components.Const.host
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import com.su.mediabox.pluginapi.action.ClassifyAction
import com.su.mediabox.pluginapi.action.DetailAction
import com.su.mediabox.pluginapi.data.*
import com.su.mediabox.pluginapi.util.UIUtil.dp
import java.net.URL

object ParseHtmlUtil {

    fun parseTopli(
        element: Element
    ): List<SimpleTextData> {
        val animeShowList = mutableListOf<SimpleTextData>()
        val elements: Elements = element.select("ul").select("li")
        for (i in elements.indices) {
            var url: String
            var title: String
            if (elements[i].select("a").size >= 2) {    //最近更新，显示地区的情况
                url = elements[i].select("a")[1].attr("href")
                title = elements[i].select("a")[1].text()
                if (elements[i].select("span")[0].children().size == 0) {     //最近更新，不显示地区的情况
                    url = elements[i].select("a")[0].attr("href")
                    title = elements[i].select("a")[0].text()
                }
            } else {                                            //总排行榜
                url = elements[i].select("a")[0].attr("href")
                title = elements[i].select("a")[0].text()
            }

            val areaUrl = elements[i].select("span").select("a")
                .attr("href")
            val areaTitle = elements[i].select("span").select("a").text()
            var episodeUrl = elements[i].select("b").select("a")
                .attr("href")
            val episodeTitle = elements[i].select("b").select("a").text()
            val date = elements[i].select("em").text()
            if (episodeUrl == "") {
                episodeUrl = url
            }
            animeShowList.add(SimpleTextData(title).apply {
                action = DetailAction.obtain(url)
            })
        }
        return animeShowList
    }

    fun getCoverUrl(cover: String, imageReferer: String): String {
        return when {
            cover.startsWith("//") -> {
                try {
                    "${URL(imageReferer).protocol}:$cover"
                } catch (e: Exception) {
                    e.printStackTrace()
                    cover
                }
            }
            cover.startsWith("/") -> {
                //url不全的情况
                host + cover
            }
            else -> cover
        }
    }

    /**
     * 解析搜索/分类下的元素
     *
     * @param element ul的父元素
     */
    fun parseSearchEm(element: Elements, imageReferer: String): List<BaseData> {
        val videoInfoItemDataList = mutableListOf<BaseData>()

//        val results: Elements = element.select(".blockcontent1>div")
        val results: Elements = element.select(">div")
        for (i in results.indices){
            var cover = results[i].select("img").attr("data-original")
            if (imageReferer.isNotBlank())
                cover = getCoverUrl(cover, imageReferer)
            val title = results[i].select(".card-title").text()
            // http://www.agemys.org/detail/20080083
            val url = results[i].select(".card-title").select("a").attr("href")
            val infos = results[i].select(".video_detail_info")
            var episode = ""
            val tags = mutableListOf<TagData>()
            var describe = ""
            for (index in infos.indices){
                val spanText = infos[index].select("span").text()
                val own = infos[index].ownText()
                when(spanText){
                    "播放状态：" -> {
                       episode = own +" [AGE动漫]"
                    }
                    "剧情类型：" -> {
                        val types = own
                        for (type in types.split(" "))
                            tags.add(TagData(type))
                    }
                    "简介：" -> {
                        describe = own
                    }
                    else ->{}
                }
            }

//            val episode = results[i].select(".newname").text()
//            val types = results[i].select(".cell_imform_kvs").select(".cell_imform_kv")[6].select(".cell_imform_value").text()
//            val tags = mutableListOf<TagData>()
//            for (type in types.split(" "))
//                tags.add(TagData(type))
//            val describe = results[i].select(".cell_imform_desc").text()

            val item = MediaInfo2Data(
                title, cover, host + url,
                episode, describe, tags
            ).apply {
                action = DetailAction.obtain(url)
            }
            videoInfoItemDataList.add(item)
        }
        return videoInfoItemDataList
    }

    /**
     * 解析分类元素
     */
    fun parseClassifyEm(element: Element): List<ClassifyItemData> {
        val classifyItemDataList = mutableListOf<ClassifyItemData>()
        val classifyCategory = element.select(".filter_type").text()
            element.select(".filter_type_list").select("a").forEach {
                classifyItemDataList.add(ClassifyItemData().apply {
                    action = ClassifyAction.obtain(
                        it.attr("href").apply {
                            Log.d("分类链接", this)
                        },
                        classifyCategory,
                        it.text()
                    )
                })
            }
        return classifyItemDataList
    }
    /**
     * 解析排行
     */
    fun parseRankEm(element: Element): List<BaseData> {
//        val rankInfoItemDataList = mutableListOf<BaseData>()
        val rankInfoItemDataList = mutableListOf<TextData>()
        val SPAN_COUNT = 16

        val ul = element.select(">div")
//        var index = 0
//        for (e in ul.indices){
//            index++
            // 循环 3 次
//            val li = ul[e].select("li")
            for (li in ul){
                val rankIdx = li.select(".rank_list_item_no").text()
                val textName = li.select("a").text()
                val href = li.select("a").attr("href")
                val rankValue = li.select(".rank_list_item_views").text()
                // 序号
                rankInfoItemDataList.add(TagData(rankIdx).apply {
                    spanSize = 2
                    paddingLeft = 6.dp
                })
                // 名称
                rankInfoItemDataList.add(
                    SimpleTextData(textName).apply {
                        spanSize = 9
                        fontStyle = Typeface.BOLD
                        fontColor = Color.BLACK
                        paddingTop = 6.dp
                        paddingBottom = 6.dp
                        paddingLeft = 0.dp
                        paddingRight = 0.dp
                        action = DetailAction.obtain(href)
                    })
                //更新集数
                rankInfoItemDataList.add(SimpleTextData(rankValue).apply {
                    spanSize = (SPAN_COUNT / 4) + 1
                    fontStyle = Typeface.BOLD
                    paddingRight = 6.dp
                    gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
                })
//                val item = SimpleTextData("[$rankIdx] $textName --$rankValue").apply {
//                    action = DetailAction.obtain(href)
//                }
//                rankInfoItemDataList.add(item)
            }
//        }
        rankInfoItemDataList[0].layoutConfig = BaseData.LayoutConfig(spanCount = SPAN_COUNT)
        return rankInfoItemDataList
    }
}