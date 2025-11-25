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
package gg.essential.mixins.transformers.resources;

import gg.essential.mixins.ext.client.resource.ResourcePackWithPath;
import net.minecraft.resource.ZipResourcePack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.io.File;
import java.nio.file.Path;

@Mixin(ZipResourcePack.class)
public class ZipResourcePackMixin_Ext implements ResourcePackWithPath {

    @Shadow @Final private File backingZipFile;

    @Override
    public Path getEssential$path() {
        File file = this.backingZipFile;
        return file != null ? file.toPath() : null;
    }
}
