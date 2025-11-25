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
package gg.essential.gui.proxies

import gg.essential.gui.common.IconFlag
import gg.essential.gui.common.NoticeFlag
import gg.essential.gui.elementa.state.v2.ReferenceHolderImpl
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.effect
import gg.essential.util.UDrawContext

class NoticeFlagProxy(essentialId: String, initialPosID: Int) :
    EssentialProxyElement<NoticeFlag>(essentialId, initialPosID, NoticeFlag::class.java) {

    private val refHolder = ReferenceHolderImpl()

    init {
        // background hiding observer
        effect(refHolder) {
            essentialComponentState()?.backgroundHidden?.set(State {
                drawBehaviourState() == ProxyDrawBehaviour.PROXY_DRAWS
            })
        }
    }

    override fun NoticeFlag.click() =
        this.runAction()

    override fun handleRender(context: UDrawContext, vanillaRender: (UDrawContext) -> Unit) {
        if (drawBehaviour == ProxyDrawBehaviour.ESSENTIAL_DRAWS_TEXT_PROXY_DRAWS_BACKGROUND) {
            val current = vanillaTextAccess
            vanillaTextAccess = ""
            vanillaRender(context)
            vanillaTextAccess = current
        } else if (drawBehaviour == ProxyDrawBehaviour.PROXY_DRAWS) {
            // handle normally, i.e. vanilla render
            super.handleRender(context, vanillaRender)
        }
    }

    override fun drawBehaviourFromComponentState(): ProxyDrawBehaviour {
        return when {
            essentialComponent == null
                -> ProxyDrawBehaviour.ESSENTIAL_DRAWS

            hasProxyContentBeenModified
                // we treat icon flags differently as they don't have mutable icons so we will allow fancy menu users to
                // fully modify the button text normally if desired
                -> if (essentialComponent is IconFlag) ProxyDrawBehaviour.PROXY_DRAWS
                    else ProxyDrawBehaviour.ESSENTIAL_DRAWS_TEXT_PROXY_DRAWS_BACKGROUND

            else -> ProxyDrawBehaviour.ESSENTIAL_DRAWS
        }
    }

}