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
package gg.essential.gui.elementa.essentialmarkdown.ext.colorattribute.internal

import org.commonmark.node.Node
import org.commonmark.node.Nodes
import org.commonmark.node.SourceSpans
import org.commonmark.node.Text
import org.commonmark.parser.delimiter.DelimiterProcessor
import org.commonmark.parser.delimiter.DelimiterRun
import gg.essential.gui.elementa.essentialmarkdown.ext.colorattribute.ColorAttribute
import java.awt.Color

class ColorAttributeDelimiterProcessor : DelimiterProcessor {
    private val openingTag = "color:"
    private val closingTag = "color"

    override fun getOpeningCharacter(): Char = '{'
    override fun getClosingCharacter(): Char = '}'
    override fun getMinLength(): Int = 1

    override fun process(opener: DelimiterRun, closer: DelimiterRun): Int {
        val openerNode = opener.opener
        val colorNode = openerNode.next as? Text ?: return 0

        // Opening tag
        if (colorNode.literal.startsWith(openingTag)) {
            val hexCode = colorNode.literal.removePrefix(openingTag)

            val colorAttribute = ColorAttribute(
                try {
                    Color.decode(hexCode)
                } catch (exception: NumberFormatException) {
                    System.err.println("Invalid color code: $hexCode")
                    return 0
                }
            )

            val sourceSpans = SourceSpans()

            // Get all the nodes between the opening and closing tags
            val parent = openerNode.parent
            val sibling = openerNode.previous
            val allNodes = listOf(openerNode) + Nodes.between(openerNode, parent.lastChild) + if (parent.lastChild != openerNode) listOf(parent.lastChild) else emptyList()

            var openingColorTags = 0
            val nodesToColor = mutableListOf<Node>()
            for (node in allNodes) {
                nodesToColor.add(node)
                sourceSpans.addAll(node.sourceSpans)

                // Add to the color counter to skip its nested closing tag
                if (node != colorNode && isOpeningTag(node)) {
                    openingColorTags++
                }

                if (isClosingTag(node.previous)) {
                    // Stop once we hit the final closing tag
                    if (openingColorTags == 0) {
                        break
                    }

                    // Decrement the color counter now that we've reached its nested closing tag
                    openingColorTags--
                }
            }

            if (openingColorTags > 0) {
                // No final closing tag; invalid syntax
                return 0
            }

            colorAttribute.sourceSpans = sourceSpans.sourceSpans
            nodesToColor.forEach { colorAttribute.appendChild(it) }
            colorNode.unlink()

            if (sibling == null) {
                parent.prependChild(colorAttribute)
            } else {
                sibling.insertAfter(colorAttribute)
            }

            return 1
        }

        // Closing tag simply needs to be unlinked as it only acts as a marker for where the colored text should end
        if (isClosingTag(colorNode)) {
            colorNode.unlink()
            return 1
        }

        return 0
    }

    private fun isBraced(node: Node?): Boolean {
        val previous = node?.previous as? Text ?: return false
        val next = node.next as? Text ?: return false
        return previous.literal == "{" && next.literal == "}"
    }

    private fun isOpeningTag(node: Node?): Boolean {
        return (node as? Text)?.literal?.startsWith(openingTag) == true && isBraced(node)
    }

    private fun isClosingTag(node: Node?): Boolean {
        return (node as? Text)?.literal == closingTag && isBraced(node)
    }
}
