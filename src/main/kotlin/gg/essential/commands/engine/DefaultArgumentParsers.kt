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
package gg.essential.commands.engine

import com.sparkuniverse.toolbox.chat.model.Channel
import gg.essential.Essential
import gg.essential.universal.UMinecraft
import net.minecraft.entity.player.EntityPlayer
import gg.essential.api.commands.*
import gg.essential.gui.elementa.state.v2.State
import gg.essential.util.UUIDUtil
import gg.essential.util.UuidNameLookup
import gg.essential.util.getOtherUser
import gg.essential.util.isAnnouncement
import java.lang.reflect.Parameter
import java.util.*
import java.util.concurrent.CompletableFuture

class IntArgumentParser : ArgumentParser<Int> {
    override fun parse(arguments: ArgumentQueue, param: Parameter): Int {
        return arguments.poll().toInt()
    }
}

class BooleanArgumentParser : ArgumentParser<Boolean> {
    override fun parse(arguments: ArgumentQueue, param: Parameter): Boolean? {
        return arguments.poll().let {
            when (it) {
                "true" -> true
                "false" -> false
                else -> null
            }
        }
    }

    override fun complete(arguments: ArgumentQueue, param: Parameter): List<String> {
        val word = arguments.poll()
        return listOf("true", "false").filter { it.startsWith(word) }
    }
}

class DoubleArgumentParser : ArgumentParser<Double> {
    override fun parse(arguments: ArgumentQueue, param: Parameter): Double {
        return arguments.poll().toDouble()
    }
}

class FloatArgumentParser : ArgumentParser<Float> {
    override fun parse(arguments: ArgumentQueue, param: Parameter): Float {
        return arguments.poll().toFloat()
    }
}

class StringArgumentParser : ArgumentParser<String> {
    override fun parse(arguments: ArgumentQueue, param: Parameter): String? {
        if (param.annotations.isEmpty()) {
            return arguments.poll()
        }

        val sb = StringBuilder()
        val wordOne = arguments.poll()

        // TODO: Perhaps Greedy + Quotable should work? Or Take + Quotable?
        return when (val ann = param.annotations.firstOrNull { it is Greedy || it is Take || it is Quotable || it is Options }) {
            is Greedy -> greedy(sb, wordOne, arguments)
            is Take -> take(sb, wordOne, arguments, ann)
            is Quotable -> quotable(sb, wordOne, arguments)
            is Options -> if (wordOne in ann.value) wordOne else null
            else -> wordOne
        }
    }

    override fun complete(arguments: ArgumentQueue, param: Parameter): List<String> {
        if (!param.isAnnotationPresent(Options::class.java))
            return emptyList()

        val options = param.getAnnotation(Options::class.java)
        return options.value.toList()
    }

    private fun greedy(sb: StringBuilder, wordOne: String, arguments: ArgumentQueue): String {
        sb.append(wordOne)

        while (arguments.peek() != null) {
            sb.append(" ${arguments.poll()}")
        }

        return sb.toString()
    }

    private fun take(sb: StringBuilder, wordOne: String, arguments: ArgumentQueue, take: Take): String {
        sb.append(wordOne)

        var i = 0

        while (arguments.peek() != null && i < take.value) {
            sb.append(" ${arguments.poll()}")

            i++
        }

        if (!take.allowLess && i < take.value - 1) {
            throw IllegalArgumentException("Needed ${take.value} words!")
        }

        return sb.toString()
    }

    private fun quotable(sb: StringBuilder, wordOne: String, arguments: ArgumentQueue): String {
        if (!wordOne.startsWith("\"")) {
            sb.append(wordOne)
            return sb.toString()
        }

        sb.append(wordOne.substring(1))

        while (arguments.peek() != null) {
            val word = arguments.poll()

            if (word.endsWith("\"")) {
                sb.append(" ${word.substring(0, word.length - 1)}")
                break
            }

            sb.append(" $word")
        }

        return sb.toString()
    }
}

class PlayerArgumentParser : ArgumentParser<EntityPlayer> {
    override fun parse(arguments: ArgumentQueue, param: Parameter): EntityPlayer? {
        val name = arguments.poll()
        //#if MC < 11400
        return UMinecraft.getWorld()?.playerEntities?.find { it.name == name }
        //#else
        //$$ return UMinecraft.getWorld()?.players?.find { it.name.string == name }
        //#endif
    }

    override fun complete(arguments: ArgumentQueue, param: Parameter): List<String> {
        val nameStart = arguments.poll()
        //#if MC < 11400
        return UMinecraft.getWorld()?.playerEntities?.map { it.name }?.filter { it.startsWith(nameStart) }
            ?: emptyList()
        //#else
        //$$        return UMinecraft.getWorld()?.players?.map { it.name.string }?.filter { it.startsWith(nameStart) }
        //$$            ?: emptyList()
        //#endif
    }
}

class EssentialFriendArgumentParser : ArgumentParser<EssentialFriend> {
    companion object{
        private val cm = Essential.getInstance().connectionManager.chatManager
    }

    val friends: List<EssentialFriend>
        // Exclude announcement channels to avoid messaging the bot(s)
        get() = cm.channels.values.filter { !it.isAnnouncement() }.mapNotNull { channel ->
            channel.getOtherUser()?.let { EssentialFriend(UUIDUtil.getName(it).get(), it, channel) }
        }

    override fun parse(arguments: ArgumentQueue, param: Parameter): EssentialFriend? {
        val name = arguments.poll()
        return friends.find { friend ->
            friend.ign.lowercase() == name.lowercase()
        }
    }

    override fun complete(arguments: ArgumentQueue, param: Parameter): List<String> {
        val nameStart = arguments.poll().lowercase()
        return friends.map { it.ign }.filter { it.lowercase().startsWith(nameStart) }
    }
}

data class EssentialFriend(val ign: String, val uuid: UUID, val channel: Channel)

object EssentialUserArgumentParser : ArgumentParser<EssentialUser> {

    private val connectionManager = Essential.getInstance().connectionManager

    private fun getSpsInvites(): Set<UUID> {
        return connectionManager.spsManager.invitedUsers
    }

    val users: Map<UUID, State<String?>>
        // SPS invited users is added because users who are not friend with the host can join the world through Discord
        get() = (connectionManager.relationshipManager.friends.keys + getSpsInvites())
            .associateWith { UuidNameLookup.nameState(it, null) }

    override fun parse(arguments: ArgumentQueue, param: Parameter): EssentialUser? {
        val targetName = arguments.poll()
        return users.firstNotNullOfOrNull { (uuid, nameState) ->
            val name = nameState.getUntracked()
            if (name?.lowercase() == targetName.lowercase()) EssentialUser(uuid, name) else null
        }
    }

    override fun complete(arguments: ArgumentQueue, param: Parameter): List<String> {
        val nameStart = arguments.poll().lowercase()
        return users.mapNotNull { it.value.getUntracked() }.filter { it.lowercase().startsWith(nameStart) }
    }
}

data class EssentialUser(val uuid: UUID, val name: String) {
    val username: CompletableFuture<String> = UUIDUtil.getName(uuid)
}