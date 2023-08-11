package com.example.ageanimeplugin.plugin.components

import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.ImageView
import com.example.ageanimeplugin.plugin.components.Const.host
import com.example.ageanimeplugin.plugin.util.JsoupUtil
import com.su.mediabox.pluginapi.action.CustomPageAction
import com.su.mediabox.pluginapi.action.DetailAction
import com.su.mediabox.pluginapi.components.IHomePageDataComponent
import com.su.mediabox.pluginapi.data.BaseData
import com.su.mediabox.pluginapi.data.MediaInfo1Data
import com.su.mediabox.pluginapi.data.SimpleTextData
import com.su.mediabox.pluginapi.util.UIUtil.dp

/**
 * FileName: HomePageDataComponent
 * Founder: Jiang Houren
 * Create Date: 2023/4/16 19:09
 * Profile:
 */
class HomePageDataComponent : IHomePageDataComponent {
    private val layoutSpanCount = 12

    override suspend fun getData(page: Int): List<BaseData>? {
        if (page != 1)
            return null
        val url = host
        val doc = JsoupUtil.getDocument(url)
        val data = mutableListOf<BaseData>()

        //排行榜
        data.add(
            MediaInfo1Data(
                "", Const.Icon.RANK, "", "排行榜",
                otherColor = 0xff757575.toInt(),
                coverScaleType = ImageView.ScaleType.FIT_CENTER,
                coverHeight = 24.dp,
                gravity = Gravity.CENTER
            ).apply {
                layoutConfig = BaseData.LayoutConfig(layoutSpanCount, 14.dp)
                spanSize = layoutSpanCount / 4
                action = CustomPageAction.obtain(RankPageDataComponent::class.java)
            })

        //更新表
        data.add(
            MediaInfo1Data(
                "", Const.Icon.TABLE, "", "时间表",
                otherColor = 0xff757575.toInt(),
                coverScaleType = ImageView.ScaleType.FIT_CENTER,
                coverHeight = 24.dp,
                gravity = Gravity.CENTER
            ).apply {
                spanSize = layoutSpanCount / 4
                action = CustomPageAction.obtain(UpdateTablePageDataComponent::class.java)
            })
        // 每日推荐
        data.add(
            MediaInfo1Data(
                "", Const.Icon.TOPIC, "", "每日推荐",
                otherColor = 0xff757575.toInt(),
                coverScaleType = ImageView.ScaleType.FIT_CENTER,
                coverHeight = 24.dp,
                gravity = Gravity.CENTER
            ).apply {
                spanSize = layoutSpanCount / 4
                action = CustomPageAction.obtain(RecommendPageDataComponent::class.java)
            })
        //最近更新
        data.add(
            MediaInfo1Data(
                "", Const.Icon.UPDATE, "", "最近更新",
                otherColor = 0xff757575.toInt(),
                coverScaleType = ImageView.ScaleType.FIT_CENTER,
                coverHeight = 24.dp,
                gravity = Gravity.CENTER
            ).apply {
                spanSize = layoutSpanCount / 4
                action = CustomPageAction.obtain(UpdatePageDataComponent::class.java)
            })

        val content = doc.select(".body_content_wrapper").select(".row")
        val videoListBox = content.select(".video_list_box")
        for (box in videoListBox){
            // 分类
            val hd = box.select(".video_list_box--hd")
            val typeName = hd.select(".title").first()?.ownText()
//            val typeUrl = hd.select("a").attr("href")
            if (!typeName.isNullOrBlank()) {
                data.add(SimpleTextData(typeName).apply {
                    fontSize = 15F
                    fontStyle = Typeface.BOLD
                    fontColor = Color.BLACK
                    spanSize = layoutSpanCount / 2
                })
                data.add(SimpleTextData("查看更多 >").apply {
                    fontSize = 12F
                    gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
                    fontColor = Const.INVALID_GREY
                    spanSize = layoutSpanCount / 2
                }.apply {
                    if (typeName.contains("推荐")){
                        action = CustomPageAction.obtain(RecommendPageDataComponent::class.java)
                    }else if (typeName.contains("更新")){
                        action = CustomPageAction.obtain(UpdatePageDataComponent::class.java)
                    }
                })
            }
            // 视频
            val bd = box.select(".video_list_box--bd")
            val items = bd.select(".video_item")
            for (video in items) {
                val name = video.select(".video_item-title").select("a").text()
                val videoUrl = video.select(".video_item-title").select("a").attr("href")
                // https://cdn.aqdstatic.com:966/age/20230127.jpg
                val coverUrl = video.select(".video_item--image").select("img").attr("data-original")
                val episode = video.select(".video_item--image").select("span").text()
                if (!name.isNullOrBlank() && !videoUrl.isNullOrBlank() && !coverUrl.isNullOrBlank()) {
                    data.add(MediaInfo1Data(name, coverUrl, videoUrl, episode ?: "")
                        .apply {
                            spanSize = layoutSpanCount / 2
                            action = DetailAction.obtain(videoUrl)
                        })
//                                Log.e("TAG", "添加视频 ($name) ($videoUrl) ($coverUrl) ($episode)")
                }
            }
        }
        return data
    }
}