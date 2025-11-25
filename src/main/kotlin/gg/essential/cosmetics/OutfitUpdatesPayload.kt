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
package gg.essential.cosmetics

import gg.essential.cosmetics.IngameEquippedOutfitsManager.Update
import gg.essential.mod.Model
import gg.essential.mod.Skin
import gg.essential.mod.cosmetics.CosmeticSlot
import gg.essential.mod.cosmetics.settings.CosmeticSetting
import gg.essential.util.UIdentifier
import io.netty.buffer.ByteBuf
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import net.minecraft.network.PacketBuffer
import java.util.*

//#if MC>=12002
//$$ import net.minecraft.network.packet.CustomPayload
//#endif

//#if MC>=11600
//$$ import gg.essential.util.toMC
//#endif

class OutfitUpdatesPayload(
    val updates: List<Pair<UUID, List<Update>>>,
)
    //#if MC>=12002
    //$$ : CustomPayload
    //#endif
{
    //#if MC>=12006
    //$$ override fun getId(): CustomPayload.Id<out CustomPayload> = ID
    //#elseif MC>=12002
    //$$ override fun id() = CHANNEL_ID_MC
    //$$ override fun write(buf: PacketByteBuf) = encode(buf, this)
    //#endif

    companion object {
        val CHANNEL_ID = UIdentifier("essential", "outfits")
        @JvmField
        //#if MC>=11600
        //$$ val CHANNEL_ID_MC = CHANNEL_ID.toMC()
        //#else
        val CHANNEL_ID_MC = CHANNEL_ID.toString()
        //#endif

        //#if MC>=12006
        //$$ val ID: CustomPayload.Id<OutfitUpdatesPayload> = CustomPayload.Id(CHANNEL_ID_MC)
        //#endif

        @JvmStatic
        fun encode(byteBuf: ByteBuf, payload: OutfitUpdatesPayload) {
            byteBuf.writeByte(0) // version/reserved for future changes

            val buf = PacketBuffer(byteBuf)
            for ((uuid, playerUpdates) in payload.updates) {
                buf.writeUniqueId(uuid)
                for (update in playerUpdates) {
                    val type = when (update) {
                        Update.Remove -> 1
                        is Update.Cosmetic -> 2
                        is Update.Skin -> 3
                        is Update.AnimationEvent -> 4
                    }
                    buf.writeByte(type)
                    when (update) {
                        Update.Remove -> {}
                        is Update.Cosmetic -> {
                            buf.writeString(update.slot.id)
                            val cosmetic = update.value
                            if (cosmetic == null) {
                                buf.writeString("")
                            } else {
                                buf.writeString(cosmetic.id)
                                for (setting in cosmetic.settings) {
                                    buf.writeString(CosmeticSetting.json.encodeToString(setting))
                                }
                                buf.writeString("") // end-of-settings marker
                            }
                        }
                        is Update.Skin -> {
                            val skin = update.skin
                            if (skin == null) {
                                buf.writeByte(-1)
                            } else {
                                buf.writeByte(skin.model.ordinal)
                                buf.writeString(skin.hash)
                            }
                        }
                        is Update.AnimationEvent -> {
                            buf.writeString(update.slot.id)
                            buf.writeString(update.event)
                        }
                    }
                }
                buf.writeByte(0) // end-of-playerUpdates marker
            }
        }

        @JvmStatic
        fun decode(byteBuf: ByteBuf): OutfitUpdatesPayload {
            if (byteBuf.readByte() != 0.toByte()) {
                return OutfitUpdatesPayload(emptyList())
            }

            val result = mutableListOf<Pair<UUID, List<Update>>>()

            val buf = PacketBuffer(byteBuf)
            while (buf.isReadable) {
                val uuid = buf.readUniqueId()
                val updates = mutableListOf<Update>()
                while (true) {
                    updates.add(when (val type = buf.readByte().toUByte().toInt()) {
                        0 -> break
                        1 -> Update.Remove
                        2 -> Update.Cosmetic(
                            CosmeticSlot.of(buf.readString(256)),
                            buf.readString(256).ifEmpty { null }?.let { id ->
                                val settings = mutableListOf<CosmeticSetting>()
                                while (true) {
                                    val json = buf.readString(Short.MAX_VALUE.toInt())
                                    if (json.isEmpty()) break
                                    settings.add(CosmeticSetting.json.decodeFromString(json))
                                }
                                EquippedCosmeticId(id, settings)
                            },
                        )
                        3 -> Update.Skin(
                            buf.readByte().toUByte().toInt().takeUnless { it == -1 }?.let { modelId ->
                                Skin(buf.readString(64), Model.entries[modelId])
                            },
                        )
                        4 -> Update.AnimationEvent(
                            CosmeticSlot.of(buf.readString(256)),
                            buf.readString(256),
                        )
                        else -> throw UnsupportedOperationException("Don't know how to handle $type")
                    })
                }
                result.add(uuid to updates)
            }

            return OutfitUpdatesPayload(result)
        }
    }
}