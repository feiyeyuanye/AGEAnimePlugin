package com.example.ageanimeplugin.plugin.components

import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
import android.view.Gravity
import com.example.ageanimeplugin.plugin.util.JsoupUtil
import com.example.ageanimeplugin.plugin.util.ParseHtmlUtil.getImageUrl
import com.su.mediabox.pluginapi.components.IMediaDetailPageDataComponent
import com.su.mediabox.pluginapi.action.ClassifyAction
import com.su.mediabox.pluginapi.action.DetailAction
import com.su.mediabox.pluginapi.action.PlayAction
import com.su.mediabox.pluginapi.data.*
import com.su.mediabox.pluginapi.util.TextUtil.urlEncode
import com.su.mediabox.pluginapi.util.UIUtil.dp
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

class MediaDetailPageDataComponent : IMediaDetailPageDataComponent {

    override suspend fun getMediaDetailData(partUrl: String): Triple<String, String, List<BaseData>> {
        var cover = ""
        var title = ""
        var desc = ""
        var score = -1F
        var upState = ""
        val url = Const.host + partUrl
        val document = JsoupUtil.getDocument(url)
        val tags = mutableListOf<TagData>()

        val details = mutableListOf<BaseData>()

        //TODO 并发优化
//        Log.e("TAG","番剧头部信息")
        val left = document.getElementsByClass("div_left").first()
        left?.let {
            val imgLeft = it.select(".poster")
            cover = imgLeft.attr("src")
            title = imgLeft.attr("alt")
            //更新状况
            upState = it.select(".detail_imform_kv")[7].text()

            //年份
            val yearEm = it.select(".detail_imform_kv")[6].select(".detail_imform_value")
            val year = Regex("\\d+").find(yearEm.text())?.value
            if (year != null)
                tags.add(TagData(year).apply {
                    action = ClassifyAction.obtain(
                        yearEm.attr("href"),
                        "", year
                    )
                })
            //地区
            val animeArea = it.select(".detail_imform_kv")[0].select(".detail_imform_value")
            tags.add(TagData(animeArea.text()).apply {
                action = ClassifyAction.obtain(
                    animeArea.attr("href"),
                    "",
                    animeArea.text()
                )
            })
//            (标签与类型内容一致,并且无 href)
            //类型
//            val typeElements = it.select(".detail_imform_kv")[8].select(".detail_imform_value")
//            tags.add(TagData(typeElements.text()).apply {
//                action = ClassifyAction.obtain(
//                    typeElements.attr("href"),
//                    "",
//                    typeElements.text()
//                )
//            })

//            //标签
//            val tagElements = it.select(".detail_imform_kv")[9].select(".detail_imform_value")
//            val name = tagElements.text()
//            tags.add(TagData(name).apply {
//                action = ClassifyAction.obtain(
//                    tagElements.attr("href"),
//                    "", name
//                )
//            })

            //评分
            score = 0.0F
        }

        val right = document.getElementsByClass("div_right").first()
        right?.let {
            val playlistDiv = it.select("#playlist-div")
            val playNameList = playlistDiv.select("li")
            val playEpisodeList = playlistDiv.select("[class=movurl]")
            Log.e("TAG","番剧头部信息 ${playNameList.size}")

            for (index in 0..playNameList.size) {
                val playName = playNameList.getOrNull(index)
                val playEpisode = playEpisodeList.getOrNull(index)
                if (playName != null && playEpisode != null) {
                    val episodes = parseEpisodes(playEpisode)
                    if (episodes.isNullOrEmpty())
                        continue
                    details.add(
                        SimpleTextData(
                            playName.select("li").text() + "(${episodes.size}集)"
                        ).apply {
                            fontSize = 16F
                            fontColor = Color.WHITE
                        }
                    )
                    details.add(EpisodeListData(episodes))
                }
            }

            desc = it.select(".detail_imform_desc_pre").select("p").text()
        }

//        Log.e("TAG", "系列动漫推荐---->")
        document.select("#recommend_block").first()?.also {
            val series = parseSeries(it)
            if (series.isNotEmpty()) {
                Log.e("其他系列作品", "size=${series.size}")
                details.add(
                    SimpleTextData("其他系列作品").apply {
                        fontSize = 16F
                        fontColor = Color.WHITE
                    }
                )
                details.addAll(series)
            }
        }

        return Triple(cover, title, mutableListOf<BaseData>().apply {
            add(Cover1Data(cover, score = score).apply {
                layoutConfig =
                    BaseData.LayoutConfig(
                        itemSpacing = 12.dp,
                        listLeftEdge = 12.dp,
                        listRightEdge = 12.dp
                    )
            })
            add(
                SimpleTextData(title).apply {
                    fontColor = Color.WHITE
                    fontSize = 20F
                    gravity = Gravity.CENTER
                    fontStyle = 1
                }
            )
            add(TagFlowData(tags))
            add(
                LongTextData(desc).apply {
                    fontColor = Color.WHITE
                }
            )
            add(LongTextData(douBanSearch(title)).apply {
                fontSize = 14F
                fontColor = Color.WHITE
                fontStyle = Typeface.BOLD
            })
            add(SimpleTextData("·$upState").apply {
                fontSize = 14F
                fontColor = Color.WHITE
                fontStyle = Typeface.BOLD
            })
            addAll(details)
        })
    }

    private fun parseEpisodes(element: Element): List<EpisodeData> {
        val episodeList = mutableListOf<EpisodeData>()
        val elements: Elements = element.select("ul").select("li")
        for (k in elements.indices) {
            val episodeUrl = elements[k].select("a").attr("href")
            episodeList.add(
                EpisodeData(elements[k].select("a").text(), episodeUrl).apply {
                    action = PlayAction.obtain(episodeUrl)
                }
            )
        }
        return episodeList
    }

    private fun parseSeries(element: Element): List<MediaInfo1Data> {
        val videoInfoItemDataList = mutableListOf<MediaInfo1Data>()
        val results: Elements = element.select("ul").select("li")
        for (i in results.indices) {
            val cover = results[i].select("img").attr("src")
            val title = results[i].select("a")[1].text()
            val url = results[i].select("a")[1].attr("href")
            val item = MediaInfo1Data(
                title, cover, Const.host + url,
                nameColor = Color.WHITE, coverHeight = 120.dp
            ).apply {
                action = DetailAction.obtain(url)
            }
            videoInfoItemDataList.add(item)
        }
        return videoInfoItemDataList
    }

    private fun douBanSearch(name: String) =
        "·豆瓣评分：https://m.douban.com/search/?query=${name.urlEncode()}"
}