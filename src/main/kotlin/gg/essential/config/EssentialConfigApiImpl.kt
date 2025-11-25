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
package gg.essential.config

import com.sparkuniverse.toolbox.relationships.enums.FriendRequestPrivacySetting
import gg.essential.config.EssentialConfig
import gg.essential.api.config.EssentialConfig as EssentialConfigApi

@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
object EssentialConfigApiImpl : EssentialConfigApi {
    override var friendConnectionStatus: Boolean by EssentialConfig::friendConnectionStatus
    override var openToFriends: Boolean = true
    override var disableAllNotifications: Boolean by EssentialConfig::disableAllNotifications
    override var messageReceivedNotifications: Boolean by EssentialConfig::messageReceivedNotifications
    override var groupMessageReceivedNotifications: Boolean by EssentialConfig::groupMessageReceivedNotifications
    override var messageSound: Boolean by EssentialConfig::messageSound
    override var updateModal: Boolean by EssentialConfig::updateModal
    override var showEssentialIndicatorOnTab: Boolean by EssentialConfig::showEssentialIndicatorOnTab
    override var showEssentialIndicatorOnNametag: Boolean by EssentialConfig::showEssentialIndicatorOnNametag
    override var sendServerUpdates: Boolean by EssentialConfig::sendServerUpdates
    override var friendRequestPrivacy: Int
        get() = EssentialConfig.friendRequestPrivacy.ordinal
        set(value) { EssentialConfig.friendRequestPrivacy = FriendRequestPrivacySetting.entries.getOrNull(value) ?: FriendRequestPrivacySetting.ANY_ONE }
    override var currentMultiplayerTab: Int by EssentialConfig::currentMultiplayerTab
    override var modCoreWarning: Boolean by EssentialConfig::modCoreWarning
    override var linkWarning: Boolean by EssentialConfig::linkWarning
    override var zoomSmoothCamera: Boolean by EssentialConfig::zoomSmoothCamera
    override var smoothZoomAnimation: Boolean by EssentialConfig::smoothZoomAnimation
    override var smoothZoomAlgorithm: Int by EssentialConfig::smoothZoomAlgorithm
    override var toggleToZoom: Boolean by EssentialConfig::toggleToZoom
    override var essentialFull: Boolean by EssentialConfig::essentialFull
    override var essentialGuiScale: Int
        get() = 0
        set(value) {}
    override var autoRefreshSession: Boolean by EssentialConfig::autoRefreshSession
    //#if MC<11400
    override var windowedFullscreen: Boolean by EssentialConfig::windowedFullscreen
    //#endif
    override var streamerMode: Boolean by EssentialConfig::streamerMode
    override var discordSdk: Boolean = false
    override var discordRichPresence: Boolean by EssentialConfig::discordRichPresence
    override var discordAllowAskToJoin: Boolean by EssentialConfig::discordAllowAskToJoin
    override var discordShowUsernameAndAvatar: Boolean by EssentialConfig::discordShowUsernameAndAvatar
    override var discordShowCurrentServer: Boolean by EssentialConfig::discordShowCurrentServer
    override var hideCosmeticsWhenServerOverridesSkin: Boolean by EssentialConfig::hideCosmeticsWhenServerOverridesSkin
    override var essentialScreenshots: Boolean by EssentialConfig::essentialScreenshots
    override var screenshotSounds: Boolean by EssentialConfig::screenshotSounds
    override var enableVanillaScreenshotMessage: Boolean by EssentialConfig::enableVanillaScreenshotMessage
    override var cosmeticArmorSetting: Int = 0
    override var timeFormat: Int
        get() = EssentialConfig.timeFormat.ordinal
        set(value) { EssentialConfig.timeFormat = if (value == 0) EssentialConfig.TimeFormat.AM_PM else EssentialConfig.TimeFormat.REGULAR }
    override var overrideGuiScale: Boolean
        get() = essentialGuiScale != 5
        set(value) {}
}
