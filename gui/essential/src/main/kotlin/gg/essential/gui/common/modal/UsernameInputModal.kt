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
package gg.essential.gui.common.modal

import gg.essential.gui.common.EssentialTooltip
import gg.essential.gui.common.LoadingIcon
import gg.essential.gui.common.and
import gg.essential.gui.common.bindParent
import gg.essential.gui.common.not
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.combinators.or
import gg.essential.gui.elementa.state.v2.combinators.zip
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.elementa.state.v2.stateBy
import gg.essential.gui.elementa.state.v2.toV1
import gg.essential.gui.overlay.ModalManager
import gg.essential.util.Client
import gg.essential.util.PlayerNotFoundException
import gg.essential.util.RateLimitException
import gg.essential.util.UuidNameLookup
import gg.essential.util.bindEssentialTooltip
import gg.essential.util.centered
import gg.essential.gui.util.hoveredState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import java.util.UUID

/**
 * An input modal that allows completion if the username is valid
 */
open class UsernameInputModal(
    modalManager: ModalManager,
    placeholderText: String,
    initialText: String = "",
    whenValidated: (UUID, String, UsernameInputModal) -> Unit,
) : CancelableInputModal(modalManager, placeholderText, initialText, maxLength = 16) {

    val errorOverride = mutableStateOf<String?>(null)
    private val rateLimited = mutableStateOf(false)
    private val unknownUser = mutableStateOf(false)
    private val disabledAddButton = rateLimited or unknownUser or errorOverride.map { it != null }
    private val defaultErrors = stateBy {
        when {
            rateLimited() -> "Too many requests,\nplease wait"
            unknownUser() -> "Username doesn't exist"
            else -> ""
        }
    }
    private val tooltipText = errorOverride.zip(defaultErrors) { override, default -> override ?: default }

    init {
        addAllowedCharacters('A'..'Z')
        addAllowedCharacters('a'..'z')
        addAllowedCharacters('0'..'9')
        addAllowedCharacters('_')

        inputTextState.onSetValue(this) {
            // User changed input, reset states
            unknownUser.set(false)
            errorOverride.set(null)
        }

        bindConfirmAvailable(!disabledAddButton.toV1(this))

        bindPrimaryButtonText(rateLimited.map { if (it) "" else "Add" }.toV1(this))

        LoadingIcon(1.0).centered().bindParent(primaryActionButton, rateLimited)

        primaryActionButton.bindEssentialTooltip(
            primaryActionButton.hoveredState() and disabledAddButton.toV1(this),
            tooltipText.toV1(this),
            EssentialTooltip.Position.ABOVE,
        )

        tooltipText.onSetValue(this) {
            if (it.isNotBlank()) {
                setError(it)
            } else {
                clearError()
            }
        }

        primaryButtonAction = {
            val inputText = inputTextState.get()
            primaryButtonEnableStateOverride.set(false)
            UuidNameLookup.getUUID(inputText).whenCompleteAsync(
                { uuid, exception ->
                    primaryButtonEnableStateOverride.set(true)
                    if (exception == null) {
                        whenValidated(uuid, inputText, this)
                    } else {
                        when (exception.cause) {
                            is PlayerNotFoundException -> unknownUser.set(true)
                            is RateLimitException -> {
                                delay(31000L) {
                                    rateLimited.set(false)
                                }
                                rateLimited.set(true)
                            }
                            else -> {
                                exception.printStackTrace()
                                setError("Unknown error occurred")
                            }
                        }
                    }
                }, Dispatchers.Client.asExecutor()
            )
        }
    }

}
