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
package gg.essential.gui.sps.categories

import gg.essential.config.EssentialConfig
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.ChildBasedMaxSizeConstraint
import gg.essential.elementa.constraints.ChildBasedSizeConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.Spacer
import gg.essential.gui.common.and
import gg.essential.gui.common.bindParent
import gg.essential.gui.common.or
import gg.essential.gui.common.shadow.EssentialUIText
import gg.essential.gui.elementa.GuiScaleOffsetConstraint
import gg.essential.gui.sps.options.PinState
import gg.essential.gui.sps.options.SettingInformation
import gg.essential.gui.sps.options.SpsOption
import gg.essential.mixins.transformers.feature.gamerules.MixinGameRulesAccessor
import gg.essential.mixins.transformers.feature.gamerules.MixinGameRulesValueAccessor
import gg.essential.util.holdScrollVerticalLocation
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.I18n
import net.minecraft.world.GameRules

//#if MC>=12111
//$$ import net.minecraft.world.rule.GameRuleCategory
//#endif

class GameRulesCategory : WorldSettingsCategory(
    "Game Rules",
    "No game rules found",
    scrollAcceleration = 2.5f,
) {

    // The integrated server should always be running if this UI is opened
    private val gameRules = Minecraft.getMinecraft().integratedServer!!
        //#if MC>=12111
        //$$ .saveProperties.gameRules
        //#elseif MC>=11600
        //$$ .gameRules
        //#else
        .getWorld(0).gameRules
        //#endif

    private val gameRuleSettings = mutableMapOf<String, String>()

    private val gameRuleStates = mutableMapOf<String, GameRuleState>()

    private var pinnedGameRules: Set<String>
        get() = EssentialConfig.spsPinnedGameRules.split(",").filter { it.isNotEmpty() }.toSet()
        set(value) {
            EssentialConfig.spsPinnedGameRules = value.joinToString(",")
        }

    init {

        val categories = mutableMapOf<Category, CategoryComponent>()
        var anyPinned = BasicState(false)

        //#if MC>=12111
        //$$ gameRules.streamRules().forEach { rule ->
        //$$     val value = gameRules.getValue(rule)
        //#else
        (gameRules as MixinGameRulesAccessor).rules.forEach { (rule, value) ->
        //#endif
            // Pinned entry is bound to this state instead of directly to pinnedState because
            // we want to be able to lock the scroll position when showing/hiding it
            val delayedPinState = BasicState(rule.toString() in pinnedGameRules)
            val pinnedState = BasicState(rule.toString() in pinnedGameRules).apply {
                onSetValue { pinned ->
                    if (pinned) {
                        pinnedGameRules += rule.toString()
                    } else {
                        pinnedGameRules -= rule.toString()
                    }
                }
            }

            anyPinned = anyPinned.or(delayedPinState)

            // Regular entry
            val unpinnedComponent by createGameRuleEntry(
                rule,
                value,
                false,
                pinnedState,
            ) ?: return@forEach

            pinnedState.onSetValue { pinned ->
                // Hold scroll position to avoid changing what the user is looking at when adding the child to the top of scroller
                scroller.holdScrollVerticalLocation(unpinnedComponent) {
                    delayedPinState.set { pinned }
                    sort()
                }
            }

            //#if MC>=12111
            //$$ val category = when (rule.category) {
            //$$     GameRuleCategory.PLAYER -> Category.PLAYER
            //$$     GameRuleCategory.MOBS -> Category.MOBS
            //$$     GameRuleCategory.SPAWNING -> Category.SPAWNING
            //$$     GameRuleCategory.DROPS -> Category.DROPS
            //$$     GameRuleCategory.UPDATES -> Category.WORLD_UPDATES
            //$$     GameRuleCategory.CHAT -> Category.CHAT
            //$$     GameRuleCategory.MISC -> Category.MISCELLANEOUS
            //$$     else -> Category.OTHER
            //$$ }
            //#else
            val category = Category.fromRule(rule.toString())
            //#endif
            categories.getOrPut(category) {
                CategoryComponent(category).also {
                    it.componentName = category.name
                } childOf scroller
            }.addOption(unpinnedComponent)

            // Pinned entry
            val pinnedComponent = createGameRuleEntry(
                rule,
                value,
                true,
                pinnedState,
            ) ?: return@forEach

            val pinnedCategory = categories.getOrPut(Category.PINNED) { CategoryComponent(Category.PINNED) } // Parent setup after loop
            pinnedCategory.bindOption(pinnedComponent, delayedPinState)
        }

        categories[Category.PINNED]?.bindParent(scroller, anyPinned)
        Spacer(height = 10f) childOf scroller
        sort()
    }

    private fun getTitle(
        rule: GameRuleKey,
    ): String {
        //#if MC>=12111
        //$$ val key = rule.translationKey
        //#else
        val key = "gamerule.$rule"
        //#endif
        return I18n.format(key).let {
            if (it == key) {
                "$rule" // Using placeholder because type changes from string on 1.16+
            } else {
                it
            }
        }
    }

    /**
     * Creates a [SpsOption] with the provided [rule] and [value]. If [pinnedComponent] then this entry will be listed in the pinned seciton.
     */
    private fun createGameRuleEntry(
        rule: GameRuleKey,
        value: GameRuleValue,
        pinnedComponent: Boolean,
        pinState: State<Boolean>,
    ): SpsOption? {
        val ruleName = getTitle(rule)
        //#if MC>=12111
        //$$ val ruleDescriptionKey = rule.translationKey + ".description"
        //#else
        val ruleDescriptionKey= "gamerule.${rule}.description"
        //#endif
        val ruleDescription = I18n.format(ruleDescriptionKey).let {
            if (it == ruleDescriptionKey) {
                "No description available."
            } else {
                it
            }
        }

        val info = SettingInformation.SettingWithDescription(
            ruleName,
            ruleDescription,
            PinState(pinnedComponent, pinState),
        )

        //#if MC>=12111
        //$$ val currentValue = gameRules.getValue(rule).toString()
        //#else
        val currentValue = gameRules.getString(rule).toString()
        //#endif

        val gameRuleState = createGameRuleState(
            rule,
            value,
            currentValue,
        )

        return when (gameRuleState) {
            is GameRuleState.BooleanState -> SpsOption.createToggleOption(info, gameRuleState.state)
            is GameRuleState.IntState -> SpsOption.createNumberOption(info, gameRuleState.state)
            null -> null
        }
    }

    /**
     * Creates a [GameRuleState] for the provided [rule] and [value].
     */
    private fun createGameRuleState(
        rule: GameRuleKey,
        value: GameRuleValue,
        currentValue: String,
    ): GameRuleState? {
        fun update(value: String) {
            gameRuleSettings[rule.toString()] = value
            spsManager.updateWorldGameRules(gameRules, gameRuleSettings)
        }
        //#if MC>=12111
        //$$ return if (value is Boolean) {
        //#elseif MC<=11202
        return if (value.type == GameRules.ValueType.BOOLEAN_VALUE) {
        //#else
        //$$ return if (value is GameRules.BooleanValue) {
        //#endif
            gameRuleStates.computeIfAbsent(rule.toString()) {
                GameRuleState.BooleanState(BasicState(currentValue.toBoolean()).apply {
                    onSetValue { update(it.toString()) }
                })
            }
        //#if MC>=12111
        //$$ } else if (value is Int) {
        //#elseif MC<=11202
        } else if (value.type == GameRules.ValueType.NUMERICAL_VALUE) {
        //#else
        //$$ } else if (value is GameRules.IntegerValue) {
        //#endif
            gameRuleStates.computeIfAbsent(rule.toString()) {
                GameRuleState.IntState(BasicState(currentValue.toInt()).apply {
                    onSetValue { update(it.toString()) }
                })
            }
        } else {
            null
        }
    }


    /**
     * Sorts the scroller contents based on pinned state.
     */
    override fun sort() {
        scroller.sortChildren { component ->
            when (component) {
                is CategoryComponent -> {
                    component.sortContent()
                    component.category.ordinal
                }
                else -> Int.MAX_VALUE
            }
        }
    }

    inner class CategoryComponent(
        val category: Category,
    ) : UIContainer() {

        private val searchText = BasicState("")

        private val titleContainer by UIContainer().constrain {
            height = ChildBasedMaxSizeConstraint()
            width = 100.percent
        } childOf this

        private val titleDivider by UIBlock(EssentialPalette.LIGHT_DIVIDER).constrain {
            y = CenterConstraint()
            width = 100.percent
            height = GuiScaleOffsetConstraint()
        } childOf titleContainer

        private val titleTextContainer by UIBlock(EssentialPalette.GUI_BACKGROUND).constrain {
            x = CenterConstraint()
            width = ChildBasedSizeConstraint() + 6.pixels
            height = ChildBasedSizeConstraint()
        } childOf titleContainer

        //#if MC>=12111
        //$$ private val i18nName = when (category) {
        //$$     Category.PLAYER -> "minecraft.player"
        //$$     Category.MOBS -> "minecraft.mobs"
        //$$     Category.SPAWNING -> "minecraft.spawning"
        //$$     Category.DROPS -> "minecraft.drops"
        //$$     Category.WORLD_UPDATES -> "minecraft.updates"
        //$$     Category.CHAT -> "minecraft.chat"
        //$$     Category.MISCELLANEOUS -> "minecraft.misc"
        //$$     Category.OTHER -> "other"
        //$$     Category.PINNED -> "pinned"
        //$$ }
        //#else
        private val i18nName = category.name.lowercase()
        //#endif

        private val title by EssentialUIText(I18n.format("gamerule.category.$i18nName")).constrain {
            x = CenterConstraint()
            textScale = GuiScaleOffsetConstraint()
            color = EssentialPalette.TEXT.toConstraint()
        } childOf titleTextContainer

        private val content by UIContainer().constrain {
            y = SiblingConstraint(6f)
            width = 100.percent
            height = ChildBasedSizeConstraint()
        } childOf this

        init {
            constrain {
                width = 100.percent
                height = ChildBasedSizeConstraint()
                y = SiblingConstraint(6f)
            }
        }

        fun addOption(
            option: SpsOption,
        ) {
            bindOption(option, BasicState(true))
        }

        fun bindOption(
            option: SpsOption,
            visible: State<Boolean>,
        ) {
            option.bindParent(content, visible and searchText.map { option.information.matchesFilter(it) })
        }

        fun filterChildren(
            search: String,
        ): Boolean {
            searchText.set(search)
            return content.children.isNotEmpty()
        }

        fun sortContent() {
            content.children.sortedBy {
                ((it as SpsOption).information as SettingInformation.SettingWithDescription).title
            }
        }
    }

    /**
     * Utility class for storing the states a game rule can be in.
     */
    sealed class GameRuleState {
        data class IntState(val state: State<Int>) : GameRuleState()

        data class BooleanState(val state: State<Boolean>) : GameRuleState()
    }

    /**
     * Category for the game rule settings.
     */
    enum class Category(
        val contents: Set<String>,
    ) {
        PINNED(emptySet()),
        PLAYER(
            setOf(
                "disableElytraMovementCheck",
                "doImmediateRespawn",
                "doLimitedCrafting",
                "drowningDamage",
                "fallDamage",
                "fireDamage",
                "freezeDamage",
                "keepInventory",
                "naturalRegeneration",
                "playersSleepingPercentage",
                "pvp",
                "spawnRadius",
                "spectatorsGenerateChunks",
            )
        ),
        MOBS(
            setOf(
                "disableRaids",
                "forgiveDeadPlayers",
                "maxEntityCramming",
                "mobGriefing",
                "universalAnger",
            )
        ),
        SPAWNING(
            setOf(
                "doInsomnia",
                "doMobSpawning",
                "doPatrolSpawning",
                "doTraderSpawning",
                "doWardenSpawning",
            )
        ),
        DROPS(
            setOf(
                "doEntityDrops",
                "doMobLoot",
                "doTileDrops",
            )
        ),
        WORLD_UPDATES(
            setOf(
                "doDaylightCycle",
                "doFireTick",
                "doWeatherCycle",
                "randomTickSpeed",
            )
        ),
        CHAT(
            setOf(
                "announceAdvancements",
                "commandBlockOutput",
                "logAdminCommands",
                "sendCommandFeedback",
                "showDeathMessages"
            )
        ),
        MISCELLANEOUS(
            setOf(
                "maxCommandChainLength",
                "reducedDebugInfo"
            )
        ),
        OTHER(emptySet()),
        ;

        companion object {
            fun fromRule(rule: String): Category {
                return values().find { rule in it.contents } ?: OTHER
            }
        }
    }
}
/** Utility type aliases for dealing with game rules across different versions */
//#if MC>=12111
//$$ typealias GameRuleValue = Any
//#elseif MC<=11202
typealias GameRuleValue = MixinGameRulesValueAccessor
//#else
//$$ typealias GameRuleValue = GameRules.RuleValue<*>
//#endif

//#if MC>=12111
//$$ typealias GameRuleKey = net.minecraft.world.rule.GameRule<*>
//#elseif MC<=11202
typealias GameRuleKey = String
//#else
//$$ typealias GameRuleKey = GameRules.RuleKey<*>
//#endif