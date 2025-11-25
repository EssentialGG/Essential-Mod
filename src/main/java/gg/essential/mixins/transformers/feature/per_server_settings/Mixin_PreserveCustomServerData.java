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
package gg.essential.mixins.transformers.feature.per_server_settings;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import gg.essential.Essential;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Whenever the server list is edited, Minecraft fully re-constructs a new nbt tree from its POJOs. That means that
 * any custom data is lost if the mod is not currently installed because the POJOs don't store unknown data.
 * To prevent Essential's settings (especially the privacy one) getting lost if Essential is temporarily disabled,
 * this class keeps a backup servers file in the Essential folder and tries to recover any custom data if the backup
 * does not match up with the vanilla data.
 */
@Mixin(ServerList.class)
public abstract class Mixin_PreserveCustomServerData {

    //#if MC>=12004
    //$$ private static final String NBT_READ = "Lnet/minecraft/nbt/NbtIo;read(Ljava/nio/file/Path;)Lnet/minecraft/nbt/NbtCompound;";
    //#else
    private static final String NBT_READ = "Lnet/minecraft/nbt/CompressedStreamTools;read(Ljava/io/File;)Lnet/minecraft/nbt/NBTTagCompound;";
    //#endif

    //#if MC>=12004
    //$$ private static final String NBT_WRITE = "Lnet/minecraft/nbt/NbtIo;write(Lnet/minecraft/nbt/NbtCompound;Ljava/nio/file/Path;)V";
    //#elseif MC>=11600
    //$$ private static final String NBT_WRITE = "Lnet/minecraft/nbt/CompressedStreamTools;write(Lnet/minecraft/nbt/CompoundNBT;Ljava/io/File;)V";
    //#else
    private static final String NBT_WRITE = "Lnet/minecraft/nbt/CompressedStreamTools;safeWrite(Lnet/minecraft/nbt/NBTTagCompound;Ljava/io/File;)V";
    //#endif

    @Shadow
    @Final
    private Minecraft mc;

    @Unique
    private File backupFile() {
        return new File(this.mc.mcDataDir, "servers.essential.dat");
    }

    @ModifyArg(method = "saveServerList", at = @At(value = "INVOKE", target = NBT_WRITE))
    private NBTTagCompound backupServerList(NBTTagCompound tag) throws IOException {
        //#if MC>=12004
        //$$ NbtIo.write(tag, backupFile().toPath());
        //#else
        CompressedStreamTools.write(tag, backupFile());
        //#endif
        return tag;
    }

    @ModifyExpressionValue(method = "loadServerList", at = @At(value = "INVOKE", target = NBT_READ))
    private NBTTagCompound validateAndRestoreServerList(NBTTagCompound vanillaTag) {
        if (vanillaTag == null) {
            return null; // vanilla list is empty, nothing to validate
        }
        NBTTagCompound backupTag;
        try {
            //#if MC>=12004
            //$$ backupTag = NbtIo.read(backupFile().toPath());
            //#else
            backupTag = CompressedStreamTools.read(backupFile());
            //#endif
        } catch (Exception e) {
            Essential.logger.error("Couldn't load server list backup", e);
            // If we cannot load our backup, re-write it
            try {
                //#if MC>=12004
                //$$ NbtIo.write(vanillaTag, backupFile().toPath());
                //#else
                CompressedStreamTools.write(vanillaTag, backupFile());
                //#endif
            } catch (Exception e1) {
                Essential.logger.error("Couldn't re-write server list backup", e1);
            }
            backupTag = null;
        }
        if (backupTag == null) {
            return vanillaTag; // we don't have a backup, so we can't validate anything
        }
        if (vanillaTag.equals(backupTag)) {
            return vanillaTag; // vanilla and backup match, all good
        }

        // There's a mismatch between the vanilla data and our backup.
        // Usually this means that the vanilla data was written without the mod installed (and therefore any modded data
        // has been wiped).
        // We need to try to match the custom data in our backup with the vanilla entries so we can restore that data.
        //#if MC>=12105
        //$$ NbtList vanillaServers = vanillaTag.getList("servers").orElseGet(NbtList::new);
        //$$ NbtList backupServers = backupTag.getList("servers").orElseGet(NbtList::new);
        //#else
        NBTTagList vanillaServers = vanillaTag.getTagList("servers", 10);
        NBTTagList backupServers = backupTag.getTagList("servers", 10);
        //#endif

        // Unfortunately server entries do not have a unique id, so we'll have to use a bunch of heuristics.
        // We'll do multiple passes, becoming progressively more lenient in what we match up (but never matching stuff
        // we already matched).
        // Pass 0 only matches if name and address are identical
        // Pass 1 only matches if address is identical
        // Pass 2 only matches if name is identical and no entries with the same name were added or deleted
        Set<NBTTagCompound> alreadyMatched = new HashSet<>();
        for (int pass = 0; pass < 3; pass++) {
            for (int vanillaIdx = 0; vanillaIdx < vanillaServers.tagCount(); vanillaIdx++) {
                //#if MC>=12105
                //$$ NbtCompound vanillaServer = vanillaServers.getCompound(vanillaIdx).orElseThrow();
                //#else
                NBTTagCompound vanillaServer = vanillaServers.getCompoundTagAt(vanillaIdx);
                //#endif
                if (alreadyMatched.contains(vanillaServer)) {
                    continue;
                }

                //#if MC>=12105
                //$$ String name = vanillaServer.getString("name").orElse("");
                //$$ String address = vanillaServer.getString("ip").orElse("");
                //#else
                String name = vanillaServer.getString("name");
                String address = vanillaServer.getString("ip");
                //#endif

                List<NBTTagCompound> backupMatches;
                List<NBTTagCompound> vanillaMatches;
                switch (pass) {
                    case 0:
                        // Look for exact (name and address) match
                        backupMatches = find(backupServers, alreadyMatched, name, address);
                        vanillaMatches = find(vanillaServers, alreadyMatched, name, address);
                        break;
                    case 1:
                        // Look for matching server address only
                        backupMatches = find(backupServers, alreadyMatched, null, address);
                        vanillaMatches = find(vanillaServers, alreadyMatched, null, address);
                        break;
                    case 2:
                        // Finally, look for matching name only
                        backupMatches = find(backupServers, alreadyMatched, name, null);
                        vanillaMatches = find(vanillaServers, alreadyMatched, name, null);
                        // Names aren't particularly great of an indicator (e.g. what if someone just leaves all the
                        // names at the default "Minecraft Server" value), so we'll only match them up if they didn't
                        // add or remove any entries.
                        // Because if they did, there's really no telling which entry belongs to which backup.
                        if (backupMatches.size() != vanillaMatches.size()) {
                            continue;
                        }
                        break;
                    default: throw new AssertionError();
                }

                if (backupMatches.isEmpty()) {
                    // Couldn't find any matches. Either they edited the entry or it's new. Re-try in a later pass
                    continue;
                }

                // If we found more than one match, try to line them up based on their index
                int index = vanillaMatches.indexOf(vanillaServer);
                if (index >= backupMatches.size()) index = backupMatches.size() - 1;
                NBTTagCompound backupServer = backupMatches.get(index);

                // We've successfully matched these up, ignore them in later passes
                alreadyMatched.add(vanillaServer);
                alreadyMatched.add(backupServer);

                // And finally, copy over custom data
                for (String key : backupServer.getKeySet()) {
                    if (key.startsWith("essential:")) {
                        vanillaServer.setTag(key, backupServer.getTag(key));
                    }
                }
            }
        }
        return vanillaTag;
    }

    @Unique
    private List<NBTTagCompound> find(NBTTagList list, Set<NBTTagCompound> alreadyMatched, String name, String address) {
        List<NBTTagCompound> result = new ArrayList<>();
        for (int i = 0; i < list.tagCount(); i++) {
            //#if MC>=12105
            //$$ NbtCompound server = list.getCompound(i).orElseThrow();
            //$$ String serverName = server.getString("name").orElse("");
            //$$ String serverAddress = server.getString("ip").orElse("");
            //#else
            NBTTagCompound server = list.getCompoundTagAt(i);
            String serverName = server.getString("name");
            String serverAddress = server.getString("ip");
            //#endif
            if (alreadyMatched.contains(server)) continue;
            if (name != null && !serverName.equals(name)) continue;
            if (address != null && !serverAddress.equals(address)) continue;
            result.add(server);
        }
        return result;
    }
}
