package com.example.ageanimeplugin.plugin.components

import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
import android.view.Gravity
import com.example.ageanimeplugin.plugin.components.Const.host
import com.example.ageanimeplugin.plugin.util.JsoupUtil
import com.su.mediabox.pluginapi.action.DetailAction
import com.su.mediabox.pluginapi.components.ICustomPageDataComponent
import com.su.mediabox.pluginapi.data.*
import com.su.mediabox.pluginapi.util.PluginPreferenceIns
import com.su.mediabox.pluginapi.util.UIUtil.dp
import com.su.mediabox.pluginapi.util.WebUtil
import com.su.mediabox.pluginapi.util.WebUtilIns
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import java.util.*

class UpdateTablePageDataComponent : ICustomPageDataComponent {

    override val pageName = "时间表"
    //override fun isShowBack() = false

    private val days = mutableListOf<String>()
    private lateinit var updateList: Elements
    private val SPAN_COUNT = 16

    override suspend fun getData(page: Int): List<BaseData>? {
//        Log.e("抓取更新数据", "page=$page")
        if (page != 1)
            return null
        val doc = JsoupUtil.getDocument(host)
            .select("[class=div_right baseblock]").select("[class=blockcontent]")
            .first() ?: return null
        //星期
        days.clear()
        doc.select("#new_anime_btns").select("li").forEach{
            Log.d("星期", it.text())
            days.add(it.text())
        }
        //当前星期
        val cal: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
        }
        val w = cal.get(Calendar.DAY_OF_WEEK).let {
            if (it == Calendar.SUNDAY) 6 else it - 2
        }
//        Log.d("当前星期", "$w ${days[w]}")

        //更新列表元素
        val updateLoader = object : ViewPagerData.PageLoader {
            override fun pageName(page: Int): String = days[page]

            override suspend fun loadData(page: Int): List<BaseData> {
                val loadPage = page+1
                val cookies = mapOf("cookie" to PluginPreferenceIns.get(JsoupUtil.cfClearanceKey, ""))
                // ul 数据由 js 的 on_new_anime_page_btn 方法动态修改
                // 这里每次获取数据都是解析了整个网页
                val document = Jsoup.parse(
                    WebUtilIns.getRenderedHtmlCode(
                        Const.host, loadPolicy = object :
                            WebUtil.LoadPolicy by WebUtil.DefaultLoadPolicy {
                            override val headers = cookies
                            override val userAgentString = Const.ua
                            override val isClearEnv = false
                            override val actionJs: String  // 数据由 js 动态改变
                                get() = "on_new_anime_page_btn(this, ${loadPage})"
                        }
                    )
                )
                updateList = document.select("#new_anime_page")
                val li = updateList.select("li")
                val ups = mutableListOf<TextData>()
                var index = 0
                for (liE in li){
                    index++
                    val title = liE.select(".one_new_anime_name").text()
                    val episode = liE.select(".one_new_anime_ji").text() +" "+ liE.select(".one_anime_new").text()
                    val url = liE.select(".one_new_anime_name").attr("href")
                    if (!title.isNullOrBlank() && !episode.isNullOrBlank() && !url.isNullOrBlank()) {
//                        Log.e("TAG", "添加更新: $title $episode $url")
                        // 序号
                        ups.add(TagData("$index").apply {
                            spanSize = 2
                            paddingLeft = 6.dp
                        })
                        // 名称
                        ups.add(
                            SimpleTextData(title).apply {
                                spanSize = 9
                                fontStyle = Typeface.BOLD
                                fontColor = Color.BLACK
                                paddingTop = 6.dp
                                paddingBottom = 6.dp
                                paddingLeft = 0.dp
                                paddingRight = 0.dp
                                action = DetailAction.obtain(url)
                            })
                        //更新集数
                        ups.add(SimpleTextData(episode).apply {
                            spanSize = (SPAN_COUNT / 4) + 1
                            fontStyle = Typeface.BOLD
                            paddingRight = 6.dp
                            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
                        })
                    }
                }
                ups[0].layoutConfig = BaseData.LayoutConfig(spanCount = SPAN_COUNT)
                return ups
            }
        }

        return listOf(ViewPagerData(mutableListOf<ViewPagerData.PageLoader>().apply {
            repeat(7) {
                add(updateLoader)
            }
        }, w).apply {
            layoutConfig = BaseData.LayoutConfig(
                itemSpacing = 0,
                listLeftEdge = 0,
                listRightEdge = 0
            )
        })
    }
}