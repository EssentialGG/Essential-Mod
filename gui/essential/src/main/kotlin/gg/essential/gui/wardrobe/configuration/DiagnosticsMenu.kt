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
import gg.essential.gui.common.modal.Modal
import gg.essential.gui.layoutdsl.Alignment
import gg.essential.gui.layoutdsl.Arrangement
import gg.essential.gui.layoutdsl.LayoutDslComponent
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.alignVertical
import gg.essential.gui.layoutdsl.box
import gg.essential.gui.layoutdsl.childBasedHeight
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.column
import gg.essential.gui.layoutdsl.fillParent
import gg.essential.gui.layoutdsl.fillRemainingWidth
import gg.essential.gui.layoutdsl.fillWidth
import gg.essential.gui.layoutdsl.height
import gg.essential.gui.layoutdsl.hoverColor
import gg.essential.gui.layoutdsl.hoverScope
import gg.essential.gui.layoutdsl.icon
import gg.essential.gui.layoutdsl.row
import gg.essential.gui.layoutdsl.scrollable
import gg.essential.gui.layoutdsl.spacer
import gg.essential.gui.layoutdsl.text
import gg.essential.gui.layoutdsl.underline
import gg.essential.gui.layoutdsl.width
import gg.essential.gui.layoutdsl.wrappedText
import gg.essential.gui.overlay.ModalManager
import gg.essential.gui.wardrobe.WardrobeState
import gg.essential.model.util.toJavaColor
import gg.essential.network.cosmetics.Cosmetic
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import gg.essential.util.onLeftClick
import java.awt.Color

class DiagnosticsMenu(
    private val wardrobeState: WardrobeState,
    private val localPath: String,
) : LayoutDslComponent {
    override fun LayoutScope.layout(modifier: Modifier) {
        scrollable(modifier, vertical = true) {
            box(Modifier.fillWidth().alignVertical(Alignment.Start)) {
                ifNotNull({ wardrobeState.cosmetics().find { it.localPath == localPath }}) { cosmetic ->
                    val diagnostics = cosmetic.diagnostics
                    if (diagnostics == null) {
                        column {
                            spacer(height = 20f)
                            text("Still loading..", Modifier.color(EssentialPalette.TEXT))
                        }
                        return@ifNotNull
                    }
                    if (diagnostics.isEmpty()) {
                        column {
                            spacer(height = 20f)
                            text("No more issues!", Modifier.color(EssentialPalette.TEXT))
                        }
                        return@ifNotNull
                    }
                    column(Modifier.fillWidth(padding = 10f).childBasedHeight(padding = 10f), Arrangement.spacedBy(10f)) {
                        for (diagnostic in diagnostics) {
                            diagnosticEntry(cosmetic, diagnostic)
                        }
                    }
                }
            }
        }
    }

    private fun LayoutScope.diagnosticEntry(cosmetic: Cosmetic, diagnostic: Cosmetic.Diagnostic) {
        column(Modifier.fillWidth(), Arrangement.spacedBy(5f)) {
            row(Modifier.fillWidth(), Arrangement.SpaceBetween) {
                row(Arrangement.spacedBy(3f)) {
                    icon(EssentialPalette.ROUND_WARNING_7X, Modifier.color(when (diagnostic.type) {
                        Cosmetic.Diagnostic.Type.Fatal -> Color.RED
                        Cosmetic.Diagnostic.Type.Error -> EssentialPalette.BANNER_RED
                        Cosmetic.Diagnostic.Type.Warning -> EssentialPalette.BANNER_YELLOW
                    }))
                    val lineColumn = diagnostic.lineColumn
                    val file = (diagnostic.file ?: "(unknown file)") + if (lineColumn != null) {
                        val (line, column) = lineColumn
                        ":$line:$column"
                    } else ""
                    text(file, Modifier.color(EssentialPalette.TEXT_MID_GRAY))
                }
                row(Arrangement.spacedBy(3f)) {
                    val variantName = diagnostic.variant
                    if (variantName != null) {
                        text("Variant:", Modifier.color(EssentialPalette.TEXT_DISABLED))
                        val color = cosmetic.variants?.find { it.name == variantName }?.color
                        if (color != null) {
                            box(Modifier.width(8f).height(8f).color(color.toJavaColor()))
                        }
                        text(variantName, Modifier.color(EssentialPalette.TEXT_MID_GRAY))
                    }
                    val skin = diagnostic.skin
                    if (skin != null) {
                        text("[${skin.name.lowercase()}]", Modifier.color(EssentialPalette.TEXT_MID_GRAY))
                    }
                }
            }
            row(Modifier.fillWidth()) {
                spacer(width = 6f)
                column(Modifier.fillRemainingWidth(), Arrangement.spacedBy(5f), Alignment.Start) {
                    wrappedText(diagnostic.message)

                    val stacktrace = diagnostic.stacktrace
                    if (stacktrace != null) {
                        text("Show stacktrace", Modifier.underline().hoverScope()
                            .color(EssentialPalette.TEXT_DARK_DISABLED).hoverColor(EssentialPalette.TEXT_DISABLED)
                        ).onLeftClick { click ->
                            click.stopPropagation()
                            platform.pushModal { StacktraceModal(it, stacktrace) }
                        }
                    }
                }
            }
        }
    }

    private class StacktraceModal(manager: ModalManager, private val stacktrace: String) : Modal(manager) {
        override fun LayoutScope.layoutModal() {
            scrollable(Modifier.fillParent(), vertical = true) {
                box(Modifier.fillWidth(padding = 10f).childBasedHeight(10f)) {
                    wrappedText(stacktrace.replace("\t", "    "))
                }
            }
        }
    }
}