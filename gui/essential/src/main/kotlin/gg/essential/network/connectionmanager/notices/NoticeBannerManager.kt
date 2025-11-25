/*
 * Copyright (c) 2024 ModCore Inc. All rights reserved.
 *
 * This code is part of ModCore Inc.'s Essential Mod repository and is protected
 * under copyright registration # TX0009138511. For the full license, see:
 * https://github.com/EssentialGG/Essential/blob/main/LICENSE
 *
 * You may not use, copy, reproduce, modify, sell, license, distribute,
 * commercialize, or otherwise exploit, or create derivative works based
 * upon, this file or any other in this repository, all of which is reserved by Essential.
 */
package gg.essential.network.connectionmanager.notices

import gg.essential.elementa.utils.withAlpha
import gg.essential.gui.EssentialPalette
import gg.essential.gui.elementa.state.v2.*
import gg.essential.gui.wardrobe.WardrobeCategory
import gg.essential.notices.NoticeType
import gg.essential.notices.model.Notice
import java.awt.Color

class NoticeBannerManager(private val noticesManager: NoticesManager) : NoticeListener {
    private val noticeBanners: MutableListState<NoticeBanner> = mutableListStateOf()

    override fun noticeAdded(notice: Notice) {
        if (notice.type == NoticeType.WARDROBE_BANNER) {
            var categories: MutableList<WardrobeCategory>? = null
            if (notice.metadata["categories"] != null) {
                categories = mutableListOf()
                for (category in notice.metadata["categories"] as List<String>) {
                    WardrobeCategory.get(category.lowercase())?.let { categories.add(it) }
                }
            }
            noticeBanners.add(
                NoticeBanner(
                    notice.id,
                    notice.metadata["lines"] as List<String>,
                    WardrobeBannerColor.valueOf((notice.metadata["color"] as String).uppercase()),
                    notice.metadata["sticky"] as Boolean,
                    notice.isDismissible,
                    categories,
                    notice.metadata["associated_sale_name"] as? String,
                )
            )
        }
    }

    override fun noticeRemoved(notice: Notice) {
        noticeBanners.get().filter { it.id == notice.id }.forEach {
            noticeBanners.remove(it)
        }
    }

    override fun onConnect() {
        noticeBanners.clear()
    }

    fun dismiss(banner: NoticeBanner) {
        noticesManager.dismissNotice(banner.id)
        noticeBanners.remove(banner)
    }

    fun getNoticeBanners() = noticeBanners
}

data class NoticeBanner(
    val id: String,
    val lines: List<String>,
    val color: WardrobeBannerColor,
    val sticky: Boolean,
    val dismissible: Boolean,
    val categories: List<WardrobeCategory>?,
    val associatedSale: String?,
)

enum class WardrobeBannerColor(
    val main: Color,
    val button: Color,
    val buttonHighlight: Color,
    val background: Color = main.withAlpha(0.11f),
) {
    BLUE(EssentialPalette.BANNER_BLUE, EssentialPalette.BLUE_BUTTON, EssentialPalette.BLUE_BUTTON_HOVER),
    RED(EssentialPalette.BANNER_RED, EssentialPalette.RED_BUTTON, EssentialPalette.RED_BUTTON_HOVER),
    GREEN(EssentialPalette.BANNER_GREEN, EssentialPalette.GREEN_BUTTON, EssentialPalette.GREEN_BUTTON_HOVER),
    GRAY(EssentialPalette.BANNER_GRAY, EssentialPalette.GRAY_BUTTON, EssentialPalette.GRAY_BUTTON_HOVER),
    YELLOW(EssentialPalette.BANNER_YELLOW, EssentialPalette.YELLOW_BUTTON, EssentialPalette.YELLOW_BUTTON_HOVER),
    PURPLE(EssentialPalette.BANNER_PURPLE, EssentialPalette.PURPLE_BUTTON, EssentialPalette.PURPLE_BUTTON_HOVER, EssentialPalette.BANNER_PURPLE_BACKGROUND),
}
