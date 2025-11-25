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
package gg.essential.api.gui

import gg.essential.api.EssentialAPI
import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.UIComponent
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIWrappedText
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.vigilance.utils.onLeftClick
import org.jetbrains.annotations.ApiStatus
import java.awt.Color

/**
 * Basic [WindowScreen] using essential colors and sizing.
 * Provides a great base for your mod's gui.
 *
 * @param guiTitle title of the Gui.
 */
@Suppress("unused")
open class EssentialGUI(
    version: ElementaVersion,
    guiTitle: String = "",
    newGuiScale: Int = EssentialAPI.getGuiUtil().getGuiScale(),
    restorePreviousGuiOnClose: Boolean = true,
    /**
     * Describes what Discord RP shows when the user is in this UI.
     *
     * Examples
     * Wardrobe - Customizing their character
     * ScreenshotBrowser - Browsing screenshots
     * SocialMenu - Messaging friends
     */
    val discordActivityDescription: String? = null,
) : WindowScreen(
    version,
    newGuiScale = newGuiScale,
    restoreCurrentGuiOnClose = restorePreviousGuiOnClose
) {
    @JvmOverloads
    constructor(
        version: ElementaVersion,
        guiTitle: String = "",
        newGuiScale: Int = EssentialAPI.getGuiUtil().getGuiScale(),
        restorePreviousGuiOnClose: Boolean = true,
    ) : this(version, guiTitle, newGuiScale, restorePreviousGuiOnClose, null)

    @Deprecated("Add ElementaVersion as the first argument to opt-in to improved behavior.")
    @Suppress("DEPRECATION")
    @JvmOverloads
    constructor(
        guiTitle: String = "",
        newGuiScale: Int = EssentialAPI.getGuiUtil().getGuiScale(),
        restorePreviousGuiOnClose: Boolean = true
    )
        : this(ElementaVersion.V0, guiTitle, newGuiScale, restorePreviousGuiOnClose)

    var backButtonVisible = true
        set(value) {
            if (field != value) {
                field = value
                if (!value) {
                    backContainer.hide(instantly = true)
                } else {
                    backContainer.unhide()
                }
            }
        }

    // Not in the companion object because that field cannot be hidden from the API while being internal
    @get:ApiStatus.Internal
    protected val outlineThickness = 3f


    private val background by UIBlock(BACKGROUND).constrain {
        width = 100.percent
        height = 100.percent
    } childOf window

    val scissorBox by UIContainer().constrain {
        x = CenterConstraint()
        y = CenterConstraint()
        width = 85.percent
        height = 75.percent
    } childOf window

    protected val container by UIContainer().constrain {
        x = CenterConstraint()
        y = CenterConstraint()
        width = 85.percent.coerceAtMost(100.percent - basicWidthConstraint { backContainer.getWidth() * 2 }).coerceAtLeast(0.pixels)
        height = 75.percent
    } childOf window

    private val leftDivider by UIBlock(DARK_GRAY).constrain {
        x = SiblingConstraint(alignOpposite = true)
        y = componentYConstraint(container)
        width = outlineThickness.pixels
        height = componentHeightConstraint(container)
    } childOf window

    val rightDivider by UIBlock(DARK_GRAY).constrain {
        x = SiblingConstraint() boundTo container
        y = componentYConstraint(container)
        width = outlineThickness.pixels
        height = componentHeightConstraint(container)
    } childOf window

    @get:ApiStatus.Internal
    val bottomDivider by UIBlock(DARK_GRAY).constrain {
        x = componentXConstraint(leftDivider)
        y = SiblingConstraint()
        width =
            componentWidthConstraint(container) + (outlineThickness.pixels * 2) // 2* outlineThickness so the corners aren't missing
        height = outlineThickness.pixels
    } childOf window


    val titleBar by UIBlock(DARK_GRAY).constrain {
        width = 100.percent
        height = 30.pixels
    } childOf container

    val titleText by UIWrappedText(guiTitle).constrain {
        x = 10.pixels
        y = 11.pixels
        color = TEXT_HIGHLIGHT.toConstraint()
    } childOf titleBar

    val content by UIContainer().constrain {
        y = SiblingConstraint()
        width = RelativeConstraint()
        height = FillConstraint()
    } childOf container

    private val backContainer: UIComponent by EssentialAPI.getEssentialComponentFactory().buildIconButton {
        iconResource = "/assets/essential/textures/arrow-left_5x7.png"
    }.constrain {
        x = (SiblingConstraint(18f, alignOpposite = true) boundTo leftDivider).coerceAtLeast(0.pixels)
        y = CenterConstraint() boundTo titleBar
        width = ChildBasedSizeConstraint() + 12.pixels
        height = ChildBasedSizeConstraint() + 10.pixels
    } childOf window


    init {
        // Notches in titlebar
        UIBlock(COMPONENT_HIGHLIGHT).constrain {
            x = 0.pixels(alignOutside = true) boundTo titleBar
            y = 0.pixels boundTo titleBar
            height = 100.percent boundTo titleBar
            width = outlineThickness.pixels
        } childOf window

        UIBlock(COMPONENT_HIGHLIGHT).constrain {
            x = 0.pixels(alignOpposite = true, alignOutside = true) boundTo titleBar
            y = 0.pixels boundTo titleBar
            height = 100.percent boundTo titleBar
            width = outlineThickness.pixels
        } childOf window

        backContainer.onLeftClick {
            backButtonPressed()
        }
    }


    @ApiStatus.Internal
    open fun backButtonPressed() {
        restorePreviousScreen()
    }

    fun setTitle(newTitle: String) {
        titleText.setText(newTitle)
    }
    private companion object EssentialGuiPalette {
        private val BACKGROUND = Color(0x181818)
        private val DARK_GRAY: Color = Color(0x232323)
        private val BUTTON = Color(0x323232)
        private val BUTTON_HIGHLIGHT = Color(0x474747)
        private var TEXT = Color(0xBFBFBF)
        private var TEXT_HIGHLIGHT = Color(0xE5E5E5)
        private val COMPONENT_HIGHLIGHT = Color(0x303030)
    }
}