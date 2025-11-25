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
package gg.essential.gui.modals.select

import gg.essential.elementa.components.GradientComponent
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.ChildBasedMaxSizeConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.effect
import gg.essential.elementa.dsl.minus
import gg.essential.elementa.dsl.percent
import gg.essential.elementa.dsl.pixel
import gg.essential.elementa.dsl.pixels
import gg.essential.elementa.dsl.plus
import gg.essential.elementa.effects.ScissorEffect
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.effect.HorizontalScissorEffect
import gg.essential.gui.common.modal.SearchableConfirmDenyModal
import gg.essential.gui.common.onSetValueAndNow
import gg.essential.gui.elementa.state.v2.MutableState
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.combinators.and
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.elementa.state.v2.stateBy
import gg.essential.gui.elementa.state.v2.toV1
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.overlay.ModalManager
import gg.essential.util.findChildrenOfType
import gg.essential.util.scrollGradient
import java.awt.Color

private typealias PrimaryActionListenerBlock<T> = (Set<T>) -> Unit
private typealias SelectionListenerBlock<T> = SelectModal<T>.(identifier: T, selected: Boolean) -> Unit

/**
 * To create an instance of this modal, see [SelectModalBuilder]
 */
class SelectModal<T>(
    modalManager: ModalManager,
    private val modalSimpleName: String,
    private val sections: List<Section<T, *>>,
    requiresButtonPress: Boolean,
    requiresSelection: Boolean,
    private val shadowsOnEntries: Boolean = true,
    private val initiallySelected: Set<T> = setOf(),
    whenEmpty: (LayoutScope.() -> Unit)? = null,
    extraContent: (LayoutScope.() -> Unit)? = null,
) : SearchableConfirmDenyModal(modalManager, requiresButtonPress) {
    private val selectedStates = mutableMapOf<T, MutableState<Boolean>>()
    private val filteredStates = mutableMapOf<T, State<Boolean>>()

    private val primaryActionListeners = mutableListOf<PrimaryActionListenerBlock<T>>()
    private val selectionListeners = mutableListOf<SelectionListenerBlock<T>>()

    private val sectionTitleScaleOffset = -1f

    override val modalName: String
        get() = "${super.modalName}-$modalSimpleName"

    /**
     * Returns a list of selected identifiers
     */
    val selectedIdentifiers: Set<T>
        get() {
            return sections.flatMap { selectedIdentifiersFor(it).get() }.toSet()
        }

    init {
        bottomSpacer.setHeight(10.pixels)
        val isEmpty = stateBy {
            sections.all { it.identifiers().isEmpty() }
        }
        scroller.layout {
            if_(isEmpty) {
                box(Modifier.fillWidth()) {
                    whenEmpty?.let { it() }
                }
            } `else` {
                column(Modifier.fillWidth(), Arrangement.spacedBy(7f)) {
                    val shouldDisplaySelected = stateBy {
                        sections.any { shouldDisplaySelectedRows(it)() }
                    }

                    if_(shouldDisplaySelected) {
                        column(Modifier.fillWidth(), Arrangement.spacedBy(6f)) {
                            sectionTitle("Selected")
                            column(Modifier.fillWidth(), Arrangement.spacedBy(2f)) {
                                sections.forEach {
                                    sectionRows(it, isSelectedSection = true)
                                }
                            }
                        }
                    }


                    sections.forEach { section ->
                        if_(shouldDisplaySection(section)) {
                            column(Modifier.fillWidth(), Arrangement.spacedBy(6f)) {
                                sectionTitle(section.title)
                                column(Modifier.fillWidth(), Arrangement.spacedBy(2f)) {
                                    sectionRows(section)
                                }
                            }
                        }
                    }
                }
            }

            scroller.scrollGradient(20.pixels)
        }

        val extraContentContainer = UIContainer().constrain {
            y = SiblingConstraint()
            width = 100.percent
            height = ChildBasedMaxSizeConstraint()
        }.apply {
            addChildModifier(Modifier.alignHorizontal(Alignment.Center))
            layout { extraContent?.let { it() } }
        }
        customContent.insertChildBefore(extraContentContainer, bottomSpacer)

        spacer.setHeight(9.pixels)
        middleSpacer.setHeight(10.pixels)

        onPrimaryAction { ->
            primaryActionListeners.forEach {
                it.invoke(selectedIdentifiers)
            }
        }

        if (requiresSelection) {
            // Only enable the primary action if we have selections
            val enabledState = stateBy {
                sections.any { selectedIdentifiersFor(it)().isNotEmpty() }
            }

            bindConfirmAvailable(enabledState.toV1(scroller))
        }

        if (shadowsOnEntries) {
            // Remove original scissor effect to allow shadows to be displayed correctly
            scrollContainer.removeEffect<ScissorEffect>()
            scroller.removeEffect<ScissorEffect>()
            scrollContainer.effect(HorizontalScissorEffect())
            // Add extra padding for shadow to display correctly at bottom of scroller
            UIContainer().constrain {
                y = SiblingConstraint()
                height = 1.pixel
            } childOf scroller
            // Extend gradient to cover shadows on the side of the scroller
            for (gradientComponent in scroller.findChildrenOfType<GradientComponent>()) {
                gradientComponent.constrain {
                    width += 1.pixel
                }
            }
        }

        isEmpty.onSetValueAndNow(this) {
            if (it) {
                hideSearchbar()
            } else {
                showSearchbar()
            }
        }
    }

    /**
     * Called when the primary action is invoked on the modal.
     * A list of selected identifiers will be supplied.
     */
    fun onPrimaryAction(block: PrimaryActionListenerBlock<T>) = apply {
        primaryActionListeners.add(block)
    }

    /**
     * Called when an item's selection state is changed
     */
    fun onSelection(block: SelectionListenerBlock<T>) = apply {
        selectionListeners.add(block)
    }

    /**
     * If any row from a [Section] should be shown in the selected section
     */
    private fun <S : Any> shouldDisplaySelectedRows(fromSection: Section<T, S>) = stateBy {
        fromSection.identifiers()
            .any { filteredStateFor(it, fromSection)() && selectedStateFor(fromSection, it)() }
    }

    /**
     * Whether there are any visible entries in the given section.
     * This is taking into account the search filter and selected entries (which show up in the virtual Selected section instead).
     */
    private fun <S : Any> shouldDisplaySection(section: Section<T, S>) = stateBy {
        section.identifiers()
            .any { filteredStateFor(it, section)() && !selectedStateFor(section, it)() }
    }

    private fun <S : Any> selectedIdentifiersFor(section: Section<T, S>) = stateBy {
        section.identifiers()
            .filter { selectedStateFor(section, it)() }
            .map(section.map)
    }

    private fun <S : Any> selectedStateFor(section: Section<T, S>, identifier: S): MutableState<Boolean> {
        val mappedIdentifier = section.map(identifier)
        return selectedStates.getOrPut(mappedIdentifier) {
            mutableStateOf(initiallySelected.contains(mappedIdentifier))
                .also { state ->
                    state.onSetValue(this) { selected ->
                        selectionListeners.forEach {
                            it.invoke(this, mappedIdentifier, selected)
                        }
                    }
                }
        }
    }

    private fun <S : Any> filteredStateFor(identifier: S, section: Section<T, S>): State<Boolean> {
        val mappedIdentifier = section.map(identifier)
        return filteredStates.getOrPut(mappedIdentifier) {
            section.filter(identifier, searchBarTextState)
        }
    }

    private fun <S : Any> LayoutScope.sectionRows(section: Section<T, S>, isSelectedSection: Boolean = false) {
        forEach(section.identifiers, cache = true) { identifier ->
            val selected = selectedStateFor(section, identifier)
            val isVisible = filteredStateFor(identifier, section)

            if_(isVisible and selected.map { it == isSelectedSection }) {

                var entryStyle = Modifier.color(EssentialPalette.BUTTON_HIGHLIGHT)

                if (shadowsOnEntries) {
                    entryStyle = entryStyle.shadow(Color.BLACK)
                }

                var entryInnerStyle = Modifier.color(EssentialPalette.COMPONENT_BACKGROUND_HIGHLIGHT)

                entryStyle = entryStyle.hoverColor(EssentialPalette.GRAY_OUTLINE_BUTTON_OUTLINE).hoverScope()
                entryInnerStyle = entryInnerStyle.hoverColor(EssentialPalette.BUTTON_HIGHLIGHT)

                box(entryStyle.height(19f).fillWidth()) {
                    box(entryInnerStyle.fillParent(padding = 1f)) {
                        section.layout(this, selected, identifier)
                    }
                }
            }
        }
    }

    private fun LayoutScope.sectionTitle(text: String) {
        row(Modifier.fillWidth(), Arrangement.spacedBy(3f)) {
            text(text, shadow = false, modifier = Modifier.color(EssentialPalette.TEXT_DISABLED))
            box(Modifier.height(1f).fillRemainingWidth().alignVertical(Alignment.Center(true)).color(EssentialPalette.DIVIDER))
        }
    }
}
