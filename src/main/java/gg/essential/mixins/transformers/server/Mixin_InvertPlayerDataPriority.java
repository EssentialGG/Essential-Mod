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
package gg.essential.mixins.transformers.server;

import gg.essential.mixins.impl.forge.PlayerListHook;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.management.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//#if MC>=12109
//$$ import net.minecraft.server.PlayerConfigEntry;
//#endif

//#if MC>=12106
//$$ import net.minecraft.storage.ReadView;
//$$ import net.minecraft.util.ErrorReporter;
//#endif

//#if MC>=12005
//$$ import java.util.Optional;
//#endif

//#if MC>=11600
//$$ import net.minecraft.world.storage.PlayerData;
//#else
import net.minecraft.world.storage.IPlayerFileData;
//#endif

/**
 * If a player transfers their world to another player, their inventories will get mismatched, since the world host
 * will always take their inventory from the level.dat file, instead of loading it from their respective playerdata entry.
 * <p></p>
 * There is theoretically no issues with preferring to load from playerData, as this is what the dedicated server will do
 * anyway, and these two entries are both kept in sync.
 */
@Mixin(PlayerList.class)
public class Mixin_InvertPlayerDataPriority {
    //#if MC>=11600
    //$$ @Shadow
    //$$ private PlayerData playerDataManager;
    //#else
    @Shadow
    private IPlayerFileData playerDataManager;
    //#endif

    @Inject(method = "readPlayerDataFromFile", at = @At("HEAD"), cancellable = true)
    private void essential$ignoreLevelDatIfPossible(
        //#if MC>=12109
        //$$ PlayerConfigEntry player,
        //#else
        EntityPlayerMP player,
        //#if MC>=12106
        //$$ ErrorReporter errorReporter,
        //#endif
        //#endif
        //#if MC>=12106 && MC<12109
        //$$ CallbackInfoReturnable<Optional<ReadView>> cir
        //#elseif MC>=12005
        //$$ CallbackInfoReturnable<Optional<NbtCompound>> cir
        //#else
        CallbackInfoReturnable<NBTTagCompound> cir
        //#endif
    ) {
        PlayerListHook.suppressForgeEventIfLoadFails = true;
        //#if MC>=12106 && MC<12109
        //$$ ReadView
        //#else
        NBTTagCompound
        //#endif
        playerData = this.playerDataManager
            //#if MC>=11600
            //#if MC>=12106 && MC<12109
            //$$ .loadPlayerData(player, errorReporter)
            //#else
            //$$ .loadPlayerData(player)
            //#endif
            //#if MC>=12005
            //$$ .orElse(null);
            //#else
            //$$ ;
            //#endif
            //#else
            .readPlayerData(player);
            //#endif
        PlayerListHook.suppressForgeEventIfLoadFails = false;

        if (playerData == null) {
            // If we can't read the player's data from the `playerdata` folder, fallback to the vanilla way of loading a
            // player's data (usually attempts to use the level.dat file first).
            return;
        }

        //#if MC>=12005
        //$$ cir.setReturnValue(Optional.of(playerData));
        //#else
        cir.setReturnValue(playerData);
        //#endif
    }
}
