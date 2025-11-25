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
package gg.essential.gui.elementa.essentialmarkdown

import gg.essential.elementa.impl.commonmark.ext.gfm.strikethrough.Strikethrough
import gg.essential.elementa.impl.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import gg.essential.elementa.impl.commonmark.ext.ins.Ins
import gg.essential.elementa.impl.commonmark.ext.ins.InsExtension
import gg.essential.elementa.impl.commonmark.node.*
import gg.essential.elementa.impl.commonmark.parser.Parser
import gg.essential.gui.elementa.essentialmarkdown.ext.colorattribute.ColorAttribute
import gg.essential.gui.elementa.essentialmarkdown.ext.colorattribute.ColorAttributeExtension
import gg.essential.gui.elementa.essentialmarkdown.drawables.*
import java.awt.Color
import java.net.URL

class MarkdownRenderer @JvmOverloads constructor(
    text: String,
    md: EssentialMarkdown,
    config: MarkdownConfig = MarkdownConfig(),
) {

    private val impl = MarkdownRendererImpl(text, md, config)

    fun render(): DrawableList = impl.render()
}

// Separate as to not expose the CommonMark implementation detail
private class MarkdownRendererImpl(
    private val text: String,
    private val md: EssentialMarkdown,
    private val config: MarkdownConfig,
) : AbstractVisitor() {

    private val drawables = mutableListOf<Drawable>()
    private val style = MutableStyle()

    private val marks = mutableListOf<Int>()

    private fun mark() {
        marks.add(drawables.size)
    }

    fun render(): DrawableList {
        val enabledBlockTypes = mutableSetOf<Class<out Block>>()
        with(enabledBlockTypes) {
            if (config.headerConfig.enabled) add(Heading::class.java)
            if (config.codeBlockConfig.enabled) {
                add(FencedCodeBlock::class.java)
                add(IndentedCodeBlock::class.java)
            }
            if (config.blockquoteConfig.enabled) add(BlockQuote::class.java)
            if (config.listConfig.enabled) add(ListBlock::class.java)
        }

        // This is a bit of a workaround for the fact that a delimiter processor extension (see ColorAttributeDelimiterProcessor)
        // cannot share the same opening/closing characters as another delimiter processor extension.
        val replacedText = text
            .replace(OPENING_COLOR_TAG_REGEX, "{$1}")
            .replace(CLOSING_COLOR_TAG_REGEX, "{$1}")

        val document = Parser.builder()
            .extensions(extensions)
            .enabledBlockTypes(enabledBlockTypes)
            .build()
            .parse(replacedText)

        document.accept(this)
        return DrawableList(md, drawables)
    }

    private fun unmarkAndCollect(): DrawableList {
        val lastMark = marks.removeAt(marks.lastIndex)
        val slice = drawables.subList(lastMark, drawables.size).toList()
        repeat(slice.size) {
            drawables.removeAt(drawables.lastIndex)
        }
        return DrawableList(md, slice)
    }

    override fun visit(emphasis: Emphasis) {
        style.isItalic = true
        super.visit(emphasis)
        style.isItalic = false
    }

    override fun visit(strongEmphasis: StrongEmphasis) {
        style.isBold = true
        super.visit(strongEmphasis)
        style.isBold = false
    }

    override fun visit(text: Text) {
        if (text.firstChild != null)
            TODO()
        drawables.add(TextDrawable(md, text.literal, style.toTextStyle()))
    }

    override fun visit(paragraph: Paragraph) {
        mark()
        super.visit(paragraph)
        drawables.add(ParagraphDrawable(md, unmarkAndCollect()))
    }

    override fun visit(blockQuote: BlockQuote) {
        mark()
        super.visit(blockQuote)
        drawables.add(BlockquoteDrawable(md, unmarkAndCollect()))
    }

    override fun visit(bulletList: BulletList) {
        mark()
        super.visit(bulletList)
        val children = unmarkAndCollect()
        if (children.any { it !is DrawableList && it !is ListDrawable })
            TODO()

        drawables.add(
            ListDrawable(
            md,
            children,
            isOrdered = false,
            isLoose = !bulletList.isTight
        )
        )
    }

    override fun visit(listItem: ListItem) {
        mark()
        super.visit(listItem)
        drawables.add(DrawableList(md, unmarkAndCollect()))
    }

    override fun visit(orderedList: OrderedList) {
        mark()
        super.visit(orderedList)
        val children = unmarkAndCollect()
        if (children.any { it !is DrawableList && it !is ListDrawable })
            TODO()

        drawables.add(
            ListDrawable(
            md,
            children,
            isOrdered = true,
            isLoose = !orderedList.isTight
        )
        )
    }

    override fun visit(code: Code) {
        style.isCode = true
        drawables.add(TextDrawable(md, code.literal, style.toTextStyle()))
        style.isCode = false
    }

    override fun visit(fencedCodeBlock: FencedCodeBlock) {
        //TODO("Not yet implemented")
    }

    override fun visit(hardLineBreak: HardLineBreak) {
        if (hardLineBreak.firstChild != null)
            TODO()
        drawables.add(HardBreakDrawable(md))
    }

    override fun visit(heading: Heading) {
        mark()
        super.visit(heading)
        val children = unmarkAndCollect()
        drawables.add(HeaderDrawable(md, heading.level, ParagraphDrawable(md, children)))
    }

    override fun visit(thematicBreak: ThematicBreak) {
        //TODO("Not yet implemented")
    }

    override fun visit(htmlInline: HtmlInline) {
        // HtmlBlocks are disabled, but HTML tags will still be parsed as inline HTML.
        // This is fine, as if the text inside of the angle brackets contains any
        // formatting (or anything that makes it an invalid HTML tag), it will not
        // be parsed as an HTML tag. If we're here, it is unformatted and we just
        // handle it like raw text.
        drawables.add(TextDrawable(md, htmlInline.literal, style.toTextStyle()))
    }

    override fun visit(image: Image) {
        mark()
        super.visit(image)
        val fallback = unmarkAndCollect()
        drawables.add(gg.essential.gui.elementa.essentialmarkdown.drawables.ImageDrawable(md, URL(image.destination), fallback))
    }

    override fun visit(indentedCodeBlock: IndentedCodeBlock) {
        //TODO("Not yet implemented")
    }

    override fun visit(link: Link) {
        if (config.urlConfig.enabled) {
            style.linkLocation = link.destination
        }
        super.visit(link)
        style.linkLocation = null
    }

    override fun visit(softLineBreak: SoftLineBreak) {
        if (softLineBreak.firstChild != null)
            TODO()
        drawables.add(SoftBreakDrawable(md))
    }

    override fun visit(linkReferenceDefinition: LinkReferenceDefinition) {
        //TODO("Not yet implemented")
    }

    override fun visit(customBlock: CustomBlock) {
        //TODO("Not yet implemented")
    }

    override fun visit(customNode: CustomNode) {
        when (customNode) {
            is Strikethrough -> {
                style.isStrikethrough = true
                super.visit(customNode)
                style.isStrikethrough = false
            }
            is Ins -> {
                style.isUnderline = true
                super.visit(customNode)
                style.isUnderline = false
            }
            is ColorAttribute  -> {
                if (config.allowColors) {
                    style.color = customNode.color
                }

                super.visit(customNode)
                style.color = null
            }
            else -> TODO()
        }
    }

    data class MutableStyle(
        var isBold: Boolean = false,
        var isItalic: Boolean = false,
        var isStrikethrough: Boolean = false,
        var isUnderline: Boolean = false,
        var isCode: Boolean = false,
        var color: Color? = null,
        var linkLocation: String? = null
    ) {
        fun toTextStyle() = TextDrawable.Style(
            isBold,
            isItalic,
            isStrikethrough,
            isUnderline,
            isCode,
            color,
            linkLocation
        )
    }

    companion object {
        val OPENING_COLOR_TAG_REGEX = "<(color:#[0-9a-fA-F]{6,})>".toRegex()
        val CLOSING_COLOR_TAG_REGEX = "</(color)>".toRegex()

        private val extensions = listOf(
            StrikethroughExtension.create(),
            InsExtension.create(),
            ColorAttributeExtension.create(),
        )
    }
}
