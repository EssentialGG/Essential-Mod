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
package gg.essential.api.commands

import gg.essential.api.EssentialAPI
import gg.essential.api.utils.SerialExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import java.lang.reflect.Method
import java.lang.reflect.Parameter
import java.util.*
import kotlin.coroutines.Continuation
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.kotlinFunction

/**
 * This is the meat and potatoes of Essential's Command API. To create a custom command, simply create a class,
 * or preferably object, that implements this class. The main selling point of this command API is that all command
 * arguments are automatically described by the parameters to your handler functions.
 *
 * To begin, if you want to perform an action when a user invokes your command with no arguments
 * (or if you have no subcommands), you must specify a function to handle this situation by annotating it with
 * [DefaultHandler] like such:
 * ```kt
 * @DefaultHandler
 * fun handle()
 * ```
 * This function will be automatically located by its annotation and invoked at the right time. Read more about default
 * handlers [here][DefaultHandler]. Note: The body of this function isn't specified, but is obviously required.
 *
 * The Command API also provides a way of specifying subcommands, such as `/$name $subcommand`. To
 * create one of these, annotate a function with [SubCommand], like such:
 * ```kt
 * @Subcommand("$subcommand")
 * fun subcommandHandler()
 * ```
 * where `$subcommand` is replaced with the relevant subcommand name.
 *
 * However, the most important part of the API: command arguments. Oftentimes you will want the user to provide
 * custom values to your handlers, so you can take specific action. To do so, simply specify parameters in your handler
 * functions where their type is the desired type of command argument. If I wished to create a handler that accepted
 * an integer as well as optionally a boolean, I could specify it like so in Kotlin:
 * ```kt
 * fun handle(number: Int, choice: Boolean?)
 * ```
 * or like so in Java:
 * ```java
 * void handle(int number, java.util.Optional<Boolean> choice)
 * ```
 * Note: There are no annotations on these functions, as they were omitted for the sake of brevity, you
 * must still use [DefaultHandler] or [SubCommand] to make them recognized by the engine.
 *
 * With the handler above, a user providing the arguments `"5"` will invoke the function with the arguments (5, null).
 * If the user provided the arguments `"5 false"`, the function will receive the arguments (5, false), as expected.
 * If the user provided the arguments `"ee true"`, the function will not be invoked, and information on how to use
 * the command will be printed like so: `Usage: /$command <number> [choice]`. If your build wipes parameter names
 * or you wish to provide custom names, you can additionally annotate parameters with the [DisplayName] annotation.
 *
 * If your parameter takes a String type, look into all the annotations provided to configure String parameters,
 * such as [Greedy] or [Quotable].
 *
 * @param name the name of the command, so your command begins as such: `/$name arguments...`
 * @param autoHelpSubcommand whether to automatically generate a help subcommand (i.e. `/$name help`) that
 *  shows all the available subcommands, their usages, and their descriptions.
 *  @param hideFromAutocomplete whether to hide this command from Minecraft's command tab completion.
 */
abstract class Command @JvmOverloads constructor(
    val name: String,
    val autoHelpSubcommand: Boolean = true,
    val hideFromAutocomplete: Boolean = false
) {
    /**
     * Global aliases for the command.
     */
    open val commandAliases: Set<Alias>? = emptySet()

    val defaultHandler = generateSequence<Class<*>>(this::class.java) {
        it.superclass
    }.flatMap<Class<*>, Method> { it.declaredMethods.toList() }.find { it.isAnnotationPresent(DefaultHandler::class.java) }?.let(::Handler)

    val uniqueSubCommands = this::class.java.declaredMethods.filter { it.isAnnotationPresent(SubCommand::class.java) }
        .map { Handler(it) to it.getAnnotation(SubCommand::class.java) }

    val subCommands = uniqueSubCommands.let { list ->
        val map = mutableMapOf<String, Handler>()
        list.forEach { (handler, ann) ->
            map[ann.value.lowercase(Locale.ENGLISH)] = handler
            ann.aliases.forEach { alias -> map[alias.lowercase(Locale.ENGLISH)] = handler }
        }
        map
    }

    /**
     * Registers this command with the Essential command system.
     */
    fun register() {
        EssentialAPI.getCommandRegistry().registerCommand(this)
    }

    /**
     * @param alias command alias.
     * @param hideFromAutocomplete whether the alias will be hidden from tab complete
     */
    data class Alias @JvmOverloads constructor(val alias: String, val hideFromAutocomplete: Boolean = false)

    class Handler(val method: Method) {
        val params: Array<Parameter> = method.parameters.filter { it.type != Continuation::class.java }.toTypedArray()
        // KReflect is slow to initialize, so we warm up its cache on a background thread
        val kParams by lazy {
            method.kotlinFunction?.parameters?.filter { it.kind == KParameter.Kind.VALUE }
        }.also {
            cacheCooker.execute { it.value }
        }

        private companion object {
            /** Warms the cache. One task at a time as to not flood the pool; concurrency won't help much anyway. */
            private val cacheCooker = SerialExecutor(Dispatchers.IO.asExecutor())
        }
    }
}
