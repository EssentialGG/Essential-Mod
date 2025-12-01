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
package gg.essential.handlers

import net.minecraft.server.MinecraftServer
import net.minecraft.world.GameRules

//#if MC>=12102
//$$ import net.minecraft.resource.featuretoggle.FeatureSet
//#endif

//#if MC>=11600
//$$ import gg.essential.mixins.transformers.server.GameRulesAccessor
//$$ import gg.essential.mixins.transformers.server.GameRulesBooleanValueAccessor
//#endif

class EssentialGameRules {

    //#if MC>=12109
    //$$ // Minecraft now has a builtin `pvp` gamerule
    //#else
    val pvpGameRule =
        //#if MC>=11600
        //$$ if (!ruleExists("pvp")) {
        //$$     GameRulesAccessor.invokeRegister("pvp", GameRules.Category.PLAYER, GameRulesBooleanValueAccessor.invokeCreate(true))
        //$$ } else {
        //$$     null
        //$$ }
        //#else
        "pvp"
        //#endif

    //#if MC<11600
    fun registerGameRules(instance: GameRules) {
        instance.addGameRule(pvpGameRule, "true", GameRules.ValueType.BOOLEAN_VALUE)
    }
    //#endif

    //#if MC>=11600
    //$$ fun getBoolean(server: MinecraftServer, rule: GameRules.RuleKey<GameRules.BooleanValue>) =
    //$$     server.gameRules.getBoolean(rule)
    //#else
    fun getBoolean(server: MinecraftServer, rule: String) =
        server.getWorld(0).gameRules.getBoolean(rule)
    //#endif

    //#if MC>=11600
    //$$ private fun ruleExists(name: String): Boolean {
    //$$     var exists = false
        //#if MC>=12102
        //$$ // Note: FeatureSet value doesn't actually matter, `accept` will (at least currently) visit all gamerules
        //$$ GameRules(FeatureSet.empty()).accept(object : GameRules.Visitor {
        //#else
        //$$ GameRules.visitAll(object : GameRules.IRuleEntryVisitor {
        //#endif
    //$$         override fun <T : GameRules.RuleValue<T>?> visit(key: GameRules.RuleKey<T>, type: GameRules.RuleType<T>) {
    //$$             if (key.name == name) exists = true
    //$$         }
    //$$     })
    //$$     return exists
    //$$ }
    //#endif
    //#endif

}