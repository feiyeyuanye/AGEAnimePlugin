package com.example.ageanimeplugin.plugin.components

import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
import android.view.Gravity
import com.example.ageanimeplugin.plugin.bean.AnimeItem
import com.example.ageanimeplugin.plugin.components.Const.host
import com.example.ageanimeplugin.plugin.util.JsoupUtil
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.su.mediabox.pluginapi.action.DetailAction
import com.su.mediabox.pluginapi.components.ICustomPageDataComponent
import com.su.mediabox.pluginapi.data.BaseData
import com.su.mediabox.pluginapi.data.SimpleTextData
import com.su.mediabox.pluginapi.data.TagData
import com.su.mediabox.pluginapi.data.TextData
import com.su.mediabox.pluginapi.data.ViewPagerData
import com.su.mediabox.pluginapi.util.UIUtil.dp
import org.jsoup.select.Elements
import java.util.Calendar
import java.util.regex.Matcher
import java.util.regex.Pattern

class UpdateTablePageDataComponent : ICustomPageDataComponent {

    override val pageName = "时间表"

    private val days = mutableListOf<String>()
    private lateinit var updateList: Elements
    private val SPAN_COUNT = 16

    override suspend fun getData(page: Int): List<BaseData>? {
        if (page != 1)
            return null
        val doc = JsoupUtil.getDocument(host)
            .select(".weekly_list").first() ?: return null
        //星期
        days.clear()
        doc.select(".nav-tabs").select("li").forEach{
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
        updateList = doc.getElementsByClass("tab-content").first()?.children() ?: return null

        val updateLoader = object : ViewPagerData.PageLoader {
            override fun pageName(page: Int): String = days[page]

            override suspend fun loadData(page: Int): List<BaseData> {
//                Log.e("TAG", "获取更新列表 $page ${updateList[page]}")
                //ul元素
                val target = updateList[page]
                val ups = mutableListOf<TextData>()
                var index = 0
                val li = target.select("ul").select("li")
                for (em in li) {
                    index++
                    val titleEm = em.select("a").first()
                    val title = titleEm?.text()
                    val episode = em.select(".title_sub").first()?.text()
                    val url = titleEm?.attr("href")
                    if (!title.isNullOrBlank() && !episode.isNullOrBlank() && !url.isNullOrBlank()) {
                        Log.d("添加更新", "$title $episode $url")
                        //序号
                        ups.add(TagData("$index").apply {
                            spanSize = 2
                            paddingLeft = 6.dp
                        })
                        //名称
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

//
////        val jsonData = doc.select("script").dataNodes()
////        Log.e("TAG","----  ${jsonData}")
//        val scriptElements = doc.select("script")
//        var dataList : List<AnimeItem>? = null
//        for (script in scriptElements) {
//            if (script.html().contains("var new_anime_list")) {
//                // 找到包含目标数据的<script>标签
//                val scriptText = script.html()
//                // 进一步处理scriptText，提取需要的数据
//                val pattern = "var new_anime_list = (\\[.*?\\]);"
//                val r: Pattern = Pattern.compile(pattern)
//                val matcher: Matcher = r.matcher(scriptText)
//                if (matcher.find()) {
//                    val newAnimeList = matcher.group(1)
//                    // 使用Gson将JSON字符串转换为List对象
//                    val gson = Gson()
//                    dataList = gson.fromJson(newAnimeList, object : TypeToken<List<AnimeItem?>?>() {}.type)
////                    for ((index,dataBean) in dataList.withIndex()){
////                        Log.e("TAG","$index ------ ${dataBean}")
////                    }
//                }
//                break
//            }
//        }
//
//        //更新列表元素
//        val updateLoader = object : ViewPagerData.PageLoader {
//            override fun pageName(page: Int): String = days[page]
//
//            override suspend fun loadData(page: Int): List<BaseData> {
//                val loadPage = if (page == 6){
//                    0
//                }else{
//                    page + 1
//                }
//                val ups = mutableListOf<TextData>()
//                dataList?.also {
//                    for (dataBean in it){
//                        if (loadPage == dataBean.wd){
////                            Log.e("TAG","------ ${dataBean}")
//                            // 名称
//                            ups.add(
//                                SimpleTextData(dataBean.name).apply {
//                                    spanSize = SPAN_COUNT / 2
//                                    fontStyle = Typeface.BOLD
//                                    fontColor = Color.BLACK
//                                    paddingTop = 6.dp
//                                    paddingBottom = 6.dp
//                                    paddingLeft = 0.dp
//                                    paddingRight = 0.dp
//                                    action = DetailAction.obtain("/detail/"+dataBean.id)
//                                })
//                            //更新集数
//                            ups.add(SimpleTextData(dataBean.namefornew).apply {
//                                spanSize = SPAN_COUNT / 2
//                                fontStyle = Typeface.BOLD
//                                paddingRight = 6.dp
//                                gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
//                            })
//                        }
//                    }
//                }
//
////                val cookies = mapOf("cookie" to PluginPreferenceIns.get(JsoupUtil.cfClearanceKey, ""))
////                // ul 数据由 js 的 on_new_anime_page_btn 方法动态修改
////                // 这里每次获取数据都是解析了整个网页
////                val document = Jsoup.parse(
////                    WebUtilIns.getRenderedHtmlCode(
////                        Const.host, loadPolicy = object :
////                            WebUtil.LoadPolicy by WebUtil.DefaultLoadPolicy {
////                            override val headers = cookies
////                            override val userAgentString = Const.ua
////                            override val isClearEnv = false
////                            override val actionJs: String  // 数据由 js 动态改变
////                                get() = "on_new_anime_page_btn(this, ${loadPage})"
////                        }
////                    )
////                )
////                updateList = document.select("#new_anime_page")
////                val li = updateList.select("li")
////                val ups = mutableListOf<TextData>()
////                var index = 0
////                for (liE in li){
////                    index++
////                    val title = liE.select(".one_new_anime_name").text()
////                    val episode = liE.select(".one_new_anime_ji").text() +" "+ liE.select(".one_anime_new").text()
////                    val url = liE.select(".one_new_anime_name").attr("href")
////                    if (!title.isNullOrBlank() && !episode.isNullOrBlank() && !url.isNullOrBlank()) {
//////                        Log.e("TAG", "添加更新: $title $episode $url")
////                        // 序号
////                        ups.add(TagData("$index").apply {
////                            spanSize = 2
////                            paddingLeft = 6.dp
////                        })
////                        // 名称
////                        ups.add(
////                            SimpleTextData(title).apply {
////                                spanSize = 9
////                                fontStyle = Typeface.BOLD
////                                fontColor = Color.BLACK
////                                paddingTop = 6.dp
////                                paddingBottom = 6.dp
////                                paddingLeft = 0.dp
////                                paddingRight = 0.dp
////                                action = DetailAction.obtain(url)
////                            })
////                        //更新集数
////                        ups.add(SimpleTextData(episode).apply {
////                            spanSize = (SPAN_COUNT / 4) + 1
////                            fontStyle = Typeface.BOLD
////                            paddingRight = 6.dp
////                            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
////                        })
////                    }
////                }
//                ups[0].layoutConfig = BaseData.LayoutConfig(spanCount = SPAN_COUNT)
//                return ups
//            }
//        }

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