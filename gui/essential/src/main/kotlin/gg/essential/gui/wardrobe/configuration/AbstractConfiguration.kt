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
package gg.essential.gui.wardrobe.configuration

import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.modal.DangerConfirmationEssentialModal
import gg.essential.gui.common.modal.Modal
import gg.essential.gui.common.modal.configure
import gg.essential.gui.elementa.state.v2.*
import gg.essential.gui.elementa.state.v2.combinators.*
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.overlay.ModalManager
import gg.essential.gui.wardrobe.WardrobeState
import gg.essential.gui.wardrobe.configuration.ConfigurationUtils.divider
import gg.essential.gui.wardrobe.configuration.ConfigurationUtils.navButton
import gg.essential.util.*
import gg.essential.util.GuiEssentialPlatform.Companion.platform

sealed class AbstractConfiguration<I, T>(
    private val configurationType: ConfigurationType<I, T>,
    protected val state: WardrobeState,
) : LayoutDslComponent {

    protected val cosmeticsDataWithChanges = state.cosmeticsManager.cosmeticsDataWithChanges!!
    protected val referenceHolder = ReferenceHolderImpl()
    private val stateTriple = configurationType.stateSupplier(state)
    private val editingIdState = stateTriple.first
    private val editingState = stateTriple.second
    private val submenuMapState = editingState.map { editing -> if (editing != null) getSubmenus(editing).associateBy { it.id } else mapOf() }
    private val currentSubmenuId = mutableStateOf<String?>(null)

    override fun LayoutScope.layout(modifier: Modifier) {
        column(Modifier.fillParent().alignBoth(Alignment.Center), Arrangement.spacedBy(0f, FloatPosition.CENTER)) {
            bind({ editingState()?.id() to currentSubmenuId() }) { (id, submenuId) ->
                val submenuState = memo { submenuMapState()[submenuId] }
                if (id != null) {
                    column(Modifier.fillWidth().childBasedHeight(3f), Arrangement.spacedBy(3f, FloatPosition.CENTER)) {
                        text("Editing ${configurationType.displaySingular}")
                        text({ editingState()?.name() ?: "" })
                        text("($id)")
                        ifNotNull(submenuState) { submenu ->
                            text("Submenu: ${submenu.name}")
                        }
                    }
                    divider()
                    row(Modifier.fillWidth().fillRemainingHeight()) {
                        val scrollComponent = scrollable(Modifier.fillRemainingWidth().fillHeight(), vertical = true) {
                            column(Modifier.fillWidth(padding = 10f), Arrangement.spacedBy(3f)) {
                                spacer(height = 5f)
                                ifNotNull(submenuState) { submenu ->
                                    submenu()
                                } `else` {
                                    ifNotNull(editingState) { currentlyEditing ->
                                        columnLayout(currentlyEditing)
                                    }
                                }
                                spacer(height = 5f)
                            }
                        }
                        val scrollbar = box(Modifier.width(2f).fillHeight().color(EssentialPalette.LIGHTEST_BACKGROUND).hoverColor(EssentialPalette.SCROLLBAR).hoverScope())
                        scrollComponent.setVerticalScrollBarComponent(scrollbar, true)
                    }
                    divider()
                    row(Modifier.fillWidth().childBasedMaxHeight(3f), Arrangement.spacedBy(5f, FloatPosition.CENTER)) {
                        navButton("Reset", Modifier.fillWidth(0.3f)) {
                            platform.pushModal { manager -> getResetModal(manager, id) }
                        }
                        navButton("Delete", Modifier.fillWidth(0.3f)) {
                            platform.pushModal { manager -> getDeleteModal(manager, id) }
                        }
                        if_({ submenuState() != null }) {
                            navButton("Back", Modifier.fillWidth(0.3f)) {
                                currentSubmenuId.set(null)
                            }
                        } `else` {
                            navButton("Close", Modifier.fillWidth(0.3f)) {
                                editingIdState.set(null)
                            }
                        }
                    }
                } else {
                    column(Modifier.fillRemainingHeight(), Arrangement.spacedBy(5f, FloatPosition.CENTER)) {
                        text("${configurationType.displaySingular} with id")
                        text("${editingIdState.get().toString()} not found")
                        navButton("Close") {
                            editingIdState.set(null)
                        }
                    }
                }
            }
        }
    }

    protected open fun LayoutScope.columnLayout(editing: T) {
        submenuSelection(editing)
    }

    protected fun LayoutScope.submenuSelection(editing: T) {
        val submenus = getSubmenus(editing)
        text(if (submenus.isEmpty()) "No submenus..." else "Select a submenu:")
        spacer(height = 10f)
        for (submenu in submenus) {
            navButton("Edit ${submenu.name}") {
                currentSubmenuId.set(submenu.id)
            }
        }
    }

    protected open fun getDeleteModal(modalManager: ModalManager, toDelete: I): Modal {
        return DangerConfirmationEssentialModal(modalManager, "Delete", false).configure {
            titleText = "Are you sure you want to delete ${configurationType.displaySingular} with id $toDelete?"
        }.onPrimaryAction {
            toDelete.update(null)
            editingIdState.set(null)
        }
    }

    protected open fun getResetModal(modalManager: ModalManager, toReset: I): Modal {
        return DangerConfirmationEssentialModal(modalManager, "Reset", false).configure {
            titleText = "Are you sure you want to reset ${configurationType.displaySingular} with id $toReset back to initial loaded state?"
        }.onPrimaryAction {
            toReset.reset()
        }
    }

    protected open fun getSubmenus(editing: T): Set<AbstractConfigurationSubmenu<T>> = setOf()

    protected fun T.update(newItem: T?) = id().update(newItem)
    @JvmName("updateById")
    protected fun I.update(newItem: T?) = configurationType.updateHandler(cosmeticsDataWithChanges, this, newItem)

    protected fun T.reset() = id().reset()
    @JvmName("resetById")
    protected fun I.reset() = configurationType.resetHandler(cosmeticsDataWithChanges, this)

    protected fun T.idAndName() = configurationType.idAndNameMapper(this)

    protected fun T.id() = idAndName().first

    protected fun T.name() = idAndName().second

    protected sealed class AbstractConfigurationSubmenu<T>(val id: String, val name: String, val currentlyEditing: T) : LayoutDslComponent {

        abstract override fun LayoutScope.layout(modifier: Modifier)

    }

}
