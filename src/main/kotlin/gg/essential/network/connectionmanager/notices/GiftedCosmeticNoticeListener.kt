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

import gg.essential.gui.wardrobe.components.showGiftReceivedToast
import gg.essential.network.connectionmanager.cosmetics.CosmeticsData
import gg.essential.notices.NoticeType
import gg.essential.notices.model.Notice
import gg.essential.util.UUIDUtil
import gg.essential.util.thenAcceptOnMainThread
import java.util.UUID

class GiftedCosmeticNoticeListener(
    private val noticeManager: NoticesManager,
    private val cosmeticsData: CosmeticsData,
) : NoticeListener {

    override fun noticeAdded(notice: Notice) {
        if (notice.type != NoticeType.GIFTED_COSMETIC_TOAST) {
            return
        }
        val uuid = UUID.fromString(notice.metadata["gifted_by_id"] as? String ?: return)
        val cosmeticId = notice.metadata["cosmetic_id"] as? String ?: return
        val cosmetic = cosmeticsData.getCosmetic(cosmeticId) ?: return
        UUIDUtil.getName(uuid).thenAcceptOnMainThread { name ->
            showGiftReceivedToast(cosmetic, uuid, name)
            noticeManager.dismissNotice(notice.id)
        }
    }

    override fun noticeRemoved(notice: Notice?) {}

    override fun onConnect() {}
}