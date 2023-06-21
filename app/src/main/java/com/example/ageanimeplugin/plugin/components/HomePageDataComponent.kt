package com.example.ageanimeplugin.plugin.components

import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
import android.view.Gravity
import android.widget.ImageView
import com.example.ageanimeplugin.plugin.actions.TodoAction
import com.example.ageanimeplugin.plugin.components.Const.host
import com.example.ageanimeplugin.plugin.util.JsoupUtil
import com.example.ageanimeplugin.plugin.util.ParseHtmlUtil
import com.example.ageanimeplugin.plugin.util.ParseHtmlUtil.getImageUrl
import com.su.mediabox.pluginapi.action.ClassifyAction
import com.su.mediabox.pluginapi.action.CustomPageAction
import com.su.mediabox.pluginapi.action.DetailAction
import com.su.mediabox.pluginapi.components.IHomePageDataComponent
import com.su.mediabox.pluginapi.data.BaseData
import com.su.mediabox.pluginapi.data.HorizontalListData
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

        // 各类推荐
        val types = doc.getElementsByClass("div_left baseblock").first() ?: return null
        var hasUpdate = false
        val update = mutableListOf<BaseData>()
        for (em in types.children()) {
            Log.d("元素", em.className())
            when (em.className()) {
                //分类
                "blocktitle" -> {
                    val type = em.select("a")
                    val typeName = type.text()
                    val typeUrl = type.attr("href")
                    if (!typeName.isNullOrBlank()) {
                        typeName.contains("推荐").also {
                            if (!it && hasUpdate) {
                                //示例使用水平列表视图组件
                                data.add(HorizontalListData(update, 120.dp).apply {
                                    spanSize = layoutSpanCount
                                })
                            }
                            hasUpdate = it
                        }

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
//                        Log.d("视频分类", "typeName=$typeName url=$typeUrl")
                    }
                }
                //分类下的视频
                "blockcontent" -> {
                    for (video in em.select("li")) {
                        video.getElementsByClass("anime_icon1_name_a").first()?.apply {
                            val name = select(".anime_icon1_name").text()
                            val videoUrl = attr("href")
                            val coverUrl = video.select("img").first()?.attr("src")?.getImageUrl()
                            Log.d("TAG","${coverUrl}")
                            val episode = video.select("[title]").first()?.text()
                            if (!name.isNullOrBlank() && !videoUrl.isNullOrBlank() && !coverUrl.isNullOrBlank()) {
                                (if (hasUpdate) update else data).add(
                                    MediaInfo1Data(name, coverUrl, videoUrl, episode ?: "")
                                        .apply {
                                            spanSize = layoutSpanCount / 3
                                            action = DetailAction.obtain(videoUrl)
                                            if (hasUpdate) {
                                                paddingRight = 8.dp
                                            }
                                        })
//                                Log.d("添加视频", "($name) ($videoUrl) ($coverUrl) ($episode)")
                            }
                        }
                    }
                }
            }
        }
        return data
    }
}