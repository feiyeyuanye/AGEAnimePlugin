package com.example.ageanimeplugin.plugin.components

import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
import android.view.Gravity
import com.example.ageanimeplugin.plugin.util.JsoupUtil
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
        var upState = ""
        val url =  partUrl
        val document = JsoupUtil.getDocument(url)
        val tags = mutableListOf<TagData>()
        val details = mutableListOf<BaseData>()

        //TODO 并发优化
        val left = document.getElementsByClass("video_detail_left").first()
        left?.let {
            cover = it.select(".video_detail_cover").select("img").attr("data-original")
            // https://cdn.aqdstatic.com:966/age/20230133.jpg
            val list = it.select(".detail_imform_list").select("li")
            for (li in list){
                val tag = li.select(".detail_imform_tag").text()
                val value = li.select(".detail_imform_value").text()
                when(tag){
                    "播放状态：" ->{  upState = value }
                    "首播时间：" ->{
                        tags.add(TagData(value.split("-")[0]).apply {
                            action = ClassifyAction.obtain(
                                "",
                                "", value
                            )
                        })
                    }
                    "地区：" ->{
                        tags.add(TagData(value).apply {
                            action = ClassifyAction.obtain(
                                "",
                                "", value
                            )
                        })
                    }
                    "标签：" ->{
                        value.split(" ").forEach{ tag->
                            tags.add(TagData(tag).apply {
                                action = ClassifyAction.obtain(
                                    "",
                                    "", tag
                                )
                            })
                        }
                    }
                    else -> {}
                }
            }
        }

        val right = document.getElementsByClass("video_detail_right").first()
        right?.let {
            title = it.select(".video_detail_title").text()
            val playlistDiv = it.select(".video_detail_playlist_wrapper")
            val playNameList = playlistDiv.select("ul[class='nav nav-pills']").select("li")
            val playEpisodeList = playlistDiv.select(".tab-content").select(">div")
            for (index in 0..playNameList.size) {
                val playName = playNameList.getOrNull(index)
                val playEpisode = playEpisodeList.getOrNull(index)
                if (playName != null && playEpisode != null) {
                    val episodes = parseEpisodes(playEpisode)
                    if (episodes.isNullOrEmpty())
                        continue
                    details.add(
                        SimpleTextData(
                            playName.select("button").text() + "(${episodes.size}集)"
                        ).apply {
                            fontSize = 16F
                            fontColor = Color.WHITE
                        }
                    )
                    details.add(EpisodeListData(episodes))
                }
            }
            desc = it.select(".video_detail_desc").text()
        }

//        Log.e("TAG", "系列动漫推荐---->")
        document.select(".video_list_box--bd").first()?.also {
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
            add(Cover1Data(cover).apply {
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
        val results: Elements = element.select(".col")
        for (i in results.indices) {
            val cover = results[i].select(".video_item--image").select("img").attr("data-original")
            val title = results[i].select(".video_item-title").select("a").text()
            val url = results[i].select(".video_item-title").select("a").attr("href")
            val item = MediaInfo1Data(
                title, cover,  url,
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