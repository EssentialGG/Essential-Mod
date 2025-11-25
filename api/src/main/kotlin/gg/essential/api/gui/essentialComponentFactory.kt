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

import com.mojang.authlib.GameProfile
import gg.essential.api.EssentialAPI
import gg.essential.api.profile.WrappedGameProfile
import gg.essential.api.profile.wrapped
import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.inspector.Inspector
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import org.jetbrains.annotations.ApiStatus
import kotlin.reflect.KProperty

/**
 * Utility for using some of Essential's [Elementa](https://github.com/sk1erllc/elementa) components
 * in your mod's guis.
 */
interface EssentialComponentFactory {
    /**
     * Build an emulated player component like the one seen in Essential's character customizer gui.
     *
     * @param showCape render the player's cape
     * @param draggable allow the player's angle to be moved with the mouse
     * @return emulated player component
     */
    @Deprecated(
        "Does not support all options.",
        replaceWith = ReplaceWith(
            "this.buildEmulatedPlayer {\nthis@buildEmulatedPlayer.showCape = showCape\nthis@buildEmulatedPlayer.draggable = draggable }",
            "gg.essential.api.gui.buildEmulatedPlayer",
        )
    )
    fun buildEmulatedPlayer(showCape: Boolean = true, draggable: Boolean = true): UIComponent = buildEmulatedPlayer {
        this.showCape = showCape
        this.draggable = draggable
    }

    /**
     * Build an emulated player component like the one seen in Essential's character customizer gui
     * using values from an [EmulatedPlayerBuilder].
     *
     * @return emulated player component
     */
    fun build(builder: EmulatedPlayerBuilder): UIComponent

    /**
     * Build a confirmation model component like the ones seen in Essential's friends gui
     * using values from a [ConfirmationModalBuilder].
     */
    fun build(builder: ConfirmationModalBuilder): UIComponent

    @ApiStatus.Internal
    fun build(builder: IconButtonBuilder): UIComponent
}

class EmulatedPlayerBuilder {

    /**
     * [GameProfile] of player to be emulated.
     */
    @Deprecated(
        "GameProfile does not have a correct `equals` or `hashCode` implementation.",
        replaceWith = ReplaceWith("wrappedProfileState")
    )
    var profileState: State<GameProfile?> = BasicState(null)

    /**
     * [GameProfile] of player to be emulated.
     */
    var profile: GameProfile?
        get() = wrappedProfileState?.get()?.profile ?: profileState.get()
        set(value) {
            wrappedProfileState?.set(value?.wrapped())

            // `set` won't do anything if `oldValue == value`, and therefore `get` will continue returning `oldValue`.
            // To avoid this, we must set `profileState` to a different value before setting the new value.
            if (profileState.get() == value && profileState.get()?.wrapped() != profile?.wrapped()) {
                profileState.set(null) // workaround for GameProfile having broken equals implementation
            }
            profileState.set(value)
        }

    /**
     * [WrappedGameProfile] of player to be emulated.
     */
    var wrappedProfileState: State<WrappedGameProfile?>? = null

    /**
     * Show cape (if present) on emulated player.
     */
    var showCapeState: State<Boolean> = BasicState(true)

    /**
     * Show cape (if present) on emulated player.
     */
    var showCape: Boolean by showCapeState

    /**
     * Allow the emulated player's angle to be moved with the mouse.
     */
    var draggableState: State<Boolean> = BasicState(true)

    /**
     * Allow the emulated player's angle to be moved with the mouse.
     */
    var draggable: Boolean by draggableState

    /**
     * Render name tag on emulated player
     */
    var renderNameTagState: State<Boolean> = BasicState(false)

    /**
     * Render name tag on emulated player
     */
    var renderNameTag: Boolean by renderNameTagState

    @JvmOverloads
    fun build(factory: EssentialComponentFactory = EssentialAPI.getEssentialComponentFactory()): UIComponent =
        factory.build(this)
}
@ApiStatus.Internal
class IconButtonBuilder {

    var iconResourceState: State<String> = BasicState("")
    var tooltipTextState: State<String> = BasicState("")
    var enabledState: State<Boolean> = BasicState(true)
    var buttonTextState: State<String> = BasicState("")
    var iconShadowState: State<Boolean> = BasicState(true)
    var textShadowState: State<Boolean> = BasicState(true)

    var iconResource: String by iconResourceState
    var tooltipText: String by tooltipTextState
    var enabled: Boolean by enabledState
    var buttonText: String by buttonTextState
    var iconShadow: Boolean by iconShadowState
    var textShadow: Boolean by textShadowState
    var tooltipBelowComponent: Boolean = true

    @JvmOverloads
    fun build(factory: EssentialComponentFactory = EssentialAPI.getEssentialComponentFactory()): UIComponent =
        factory.build(this)
}




class ConfirmationModalBuilder {
    /**
     * Modal text.
     */
    var text: String = ""
    var secondaryText: String? = null
    var inputPlaceholder: String? = null
    var confirmButtonText: String = "Confirm"
    var denyButtonText: String = "Decline"

    /**
     * Ran when deny button is clicked.
     */
    var onDeny: () -> Unit = {}

    /**
     * Ran when confirm button is clicked.
     */
    var onConfirm: (userInput: String) -> Unit = {}

    @JvmOverloads
    fun build(factory: EssentialComponentFactory = EssentialAPI.getEssentialComponentFactory()): UIComponent =
        factory.build(this)
}

private operator fun <T> State<T>.setValue(obj: Any, property: KProperty<*>, t: T) = set(t)
private operator fun <T> State<T>.getValue(obj: Any, property: KProperty<*>): T = get()

inline fun EssentialComponentFactory.buildEmulatedPlayer(block: EmulatedPlayerBuilder.() -> Unit): UIComponent =
    EmulatedPlayerBuilder().apply(block).build(this)

inline fun EssentialComponentFactory.buildConfirmationModal(block: ConfirmationModalBuilder.() -> Unit): UIComponent =
    ConfirmationModalBuilder().apply(block).build(this)

@ApiStatus.Internal
inline fun EssentialComponentFactory.buildIconButton(block: IconButtonBuilder.() -> Unit): UIComponent =
    IconButtonBuilder().apply(block).build(this)

@Suppress("unused")
private val init = run {
    Inspector.registerComponentFactory(null)
    Inspector.registerComponentFactory(EssentialComponentFactory::class.java)
    Inspector.registerComponentFactory(EmulatedPlayerBuilder::class.java)
    Inspector.registerComponentFactory(IconButtonBuilder::class.java)
    Inspector.registerComponentFactory(ConfirmationModalBuilder::class.java)
}
