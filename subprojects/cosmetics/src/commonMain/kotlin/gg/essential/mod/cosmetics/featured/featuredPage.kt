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
package gg.essential.mod.cosmetics.featured

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = FeaturedPageSerializer::class)
data class FeaturedPage(
    val rows: List<FeaturedPageComponent>,
)

@Serializable
private data class FeaturedPageRaw(
    val rows: List<List<FeaturedItem>>,
    val dividers: Map<Int, List<String?>>? = null,
)

sealed interface FeaturedPageComponent

data class FeaturedItemRow(val items: List<FeaturedItem>) : FeaturedPageComponent

sealed class BaseDivider : FeaturedPageComponent

data object BlankDivider : BaseDivider()

data class TextDivider(val text: String) : BaseDivider()

enum class DividerType {
    BLANK,
    TEXT
}

object FeaturedPageSerializer : KSerializer<FeaturedPage> {
    private val inner = FeaturedPageRaw.serializer()
    override val descriptor: SerialDescriptor = inner.descriptor

    override fun deserialize(decoder: Decoder): FeaturedPage {
        val featuredPageRaw = inner.deserialize(decoder)

        val rowList = featuredPageRaw.rows.map { row -> FeaturedItemRow(row) }
        val dividerList = featuredPageRaw.dividers ?: emptyMap()

        fun List<String?>.convertToDividerList() : List<BaseDivider> {
            return map { if (it == null) BlankDivider else TextDivider(it) }
        }

        val mergedList = rowList.foldIndexed(mutableListOf<FeaturedPageComponent>()) { index, acc, featuredItemRow ->
            acc += dividerList[index]?.convertToDividerList() ?: emptyList()
            acc += featuredItemRow
            acc
        }.apply {
            dividerList.keys.filter { it >= rowList.size }
                .sorted().forEach { addAll(dividerList[it]?.convertToDividerList() ?: emptyList()) }
        }
        return FeaturedPage(mergedList)
    }

    override fun serialize(encoder: Encoder, value: FeaturedPage) {
        val dividers = mutableMapOf<Int, MutableList<String?>>()
        var originalIndex = 0
        for (componentRow in value.rows) {
            when (componentRow) {
                is FeaturedItemRow -> {
                    originalIndex++
                }

                is BaseDivider -> {
                    dividers.computeIfAbsent(originalIndex) { mutableListOf() }.add(
                        if (componentRow is TextDivider) componentRow.text else null
                    )
                }
            }
        }

        val featuredPageRaw = FeaturedPageRaw(value.rows.filterIsInstance<FeaturedItemRow>().map { it.items }, dividers)
        inner.serialize(encoder, featuredPageRaw)
    }

}