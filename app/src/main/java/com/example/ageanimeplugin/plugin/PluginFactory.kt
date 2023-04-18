package com.example.ageanimeplugin.plugin

import com.example.ageanimeplugin.plugin.components.*
import com.su.mediabox.pluginapi.IPluginFactory
import com.su.mediabox.pluginapi.components.*
import com.su.mediabox.pluginapi.util.PluginPreferenceIns
import com.example.ageanimeplugin.plugin.danmaku.OyydsDanmaku

/**
 * FileName: PluginFactory
 * Founder: Jiang Houren
 * Create Date: 2023/4/16 18:40
 * Profile:
 * 每个插件必须实现本类
 *
 * 注意包和类名都要相同，且必须提供公开的无参数构造方法
 */
class PluginFactory : IPluginFactory() {

    override val host: String = Const.host

    override fun pluginLaunch() {
        // https://api.danmu.oyyds.top/
        PluginPreferenceIns.initKey(OyydsDanmaku.OYYDS_DANMAKU_ENABLE, defaultValue = true)
    }

    override fun <T : IBasePageDataComponent> createComponent(clazz: Class<T>) = when (clazz) {
        IHomePageDataComponent::class.java -> HomePageDataComponent()
        IMediaDetailPageDataComponent::class.java -> MediaDetailPageDataComponent()  // 详情
        IVideoPlayPageDataComponent::class.java -> VideoPlayPageDataComponent() // 视频播放
        IMediaSearchPageDataComponent::class.java -> MediaSearchPageDataComponent()  // 搜索
        IMediaClassifyPageDataComponent::class.java -> MediaClassifyPageDataComponent()  // 媒体分类
        IMediaUpdateDataComponent::class.java -> MediaUpdateDataComponent
        //自定义页面，需要使用具体类而不是它的基类（接口）
        RankPageDataComponent::class.java -> RankPageDataComponent()  // 排行榜
        UpdateTablePageDataComponent::class.java -> UpdateTablePageDataComponent()  // 时间表
        UpdatePageDataComponent::class.java -> UpdatePageDataComponent()  // 最近更新
        RecommendPageDataComponent::class.java -> RecommendPageDataComponent()  // 每日推荐
        else -> null
    } as? T

}