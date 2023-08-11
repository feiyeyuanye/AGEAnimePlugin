package com.example.ageanimeplugin.plugin.components

import android.net.Uri
import com.example.ageanimeplugin.plugin.components.Const.host
import com.example.ageanimeplugin.plugin.util.JsoupUtil
import com.example.ageanimeplugin.plugin.util.ParseHtmlUtil
import com.su.mediabox.pluginapi.components.IMediaSearchPageDataComponent
import com.su.mediabox.pluginapi.data.BaseData
import org.jsoup.select.Elements

class MediaSearchPageDataComponent : IMediaSearchPageDataComponent {

    override suspend fun getSearchData(keyWord: String, page: Int): List<BaseData> {
        val searchResultList = mutableListOf<BaseData>()
        // https://www.agemys.org/search?query=%E9%BE%99&page=2
        val url = "${host}/search?query=${Uri.encode(keyWord, ":/-![].,%?&=")}&page=${page}"
        val document = JsoupUtil.getDocument(url)
        val lpic: Elements = document.select("#cata_video_list").select(".col-md-12")
        searchResultList.addAll(ParseHtmlUtil.parseSearchEm(lpic, url))
        return searchResultList
    }
}